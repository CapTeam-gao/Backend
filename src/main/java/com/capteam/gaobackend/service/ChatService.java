package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.chat.ChatAdminUnreadEventDto;
import com.capteam.gaobackend.dto.chat.ChatChannelRequestDto;
import com.capteam.gaobackend.dto.chat.ChatChannelEventDto;
import com.capteam.gaobackend.dto.chat.ChatChannelResponseDto;
import com.capteam.gaobackend.dto.chat.ChatChannelSummaryResponseDto;
import com.capteam.gaobackend.dto.chat.ChatMessageEventDto;
import com.capteam.gaobackend.dto.chat.ChatMessageRequestDto;
import com.capteam.gaobackend.dto.chat.ChatMessageResponseDto;
import com.capteam.gaobackend.dto.chat.ChatMessageUpdateRequestDto;
import com.capteam.gaobackend.dto.chat.ChatRoomResponseDto;
import com.capteam.gaobackend.dto.chat.ChatUnreadSummaryResponseDto;
import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatMessage;
import com.capteam.gaobackend.entity.ChatReadStatus;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.repository.ChatChannelRepository;
import com.capteam.gaobackend.repository.ChatMessageRepository;
import com.capteam.gaobackend.repository.ChatReadStatusRepository;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final String CHAT_CHANNEL_EVENTS_DESTINATION_FORMAT = "/sub/chat/rooms/%d/channels";
    private static final String CHAT_MESSAGE_EVENTS_DESTINATION_FORMAT = "/sub/chat/%d/events";
    private static final String ADMIN_CHAT_UNREAD_DESTINATION = "/sub/admin/chat/unread";

    // 채팅 메시지 저장, 조회, unread 계산에 사용하는 Repository 필드입니다.
    private final ChatMessageRepository chatMessageRepository;

    // 채팅방 안의 채널 생성, 수정, 삭제, 목록 조회에 사용하는 Repository 필드입니다.
    private final ChatChannelRepository chatChannelRepository;

    // 팀별 채팅방 생성, 조회, 삭제에 사용하는 Repository 필드입니다.
    private final ChatRoomRepository chatRoomRepository;

    // 사용자별 채널 마지막 읽음 시간을 저장/조회하는 Repository 필드입니다.
    private final ChatReadStatusRepository chatReadStatusRepository;

    // 채팅방/채널 접근 권한과 사용자 조회를 담당하는 Service 필드입니다.
    private final ChatAccessService chatAccessService;

    // 관리자 채팅방 생성 시 대상 팀을 조회하는 Repository 필드입니다.
    private final TeamRepository teamRepository;

    // 프로젝트 기획서에 작성된 팀 이름을 채팅방 표시명으로 조회하는 Repository 필드입니다.
    private final TeamProjectRepository teamProjectRepository;

    // 채널 생성/수정/삭제 권한 검증에 사용하는 팀원 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 관리자 unread 이벤트 대상 계정을 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 채팅 메시지/채널 변경 이벤트를 WebSocket 구독자에게 발행하는 필드입니다.
    private final SimpMessagingTemplate messagingTemplate;


    // 로그인한 사용자가 속한 팀의 채팅방과 채널 목록을 조회하는 기능입니다.
    public ChatRoomResponseDto getMyChatRoom(String userId) {
        TeamUser myTeamUser = teamUserRepository.findByUserUserId(userId).orElse(null);
        if (myTeamUser == null) {
            return null;
        }

        ChatRoom room = chatRoomRepository.findByTeamId(myTeamUser.getTeam().getId()).orElse(null);
        if (room == null) {
            return null;
        }

        return buildRoomResponse(room, myTeamUser);
    }

    // 특정 채팅방을 조회하되 학생은 자기 팀 채팅방만 접근 가능하게 검사하는 기능입니다.
    public ChatRoomResponseDto getRoom(Long roomId, String userId) {
        ChatRoom room = chatAccessService.getAccessibleRoom(roomId, userId);
        return buildRoomResponse(room);
    }

    // 내 팀 채팅방의 채널별 마지막 메시지와 읽지 않은 메시지 수를 조회하는 기능입니다.
    public List<ChatChannelSummaryResponseDto> getMyChannelSummaries(String userId) {
        ChatRoom room = teamUserRepository.findByUserUserId(userId)
                .flatMap(teamUser -> chatRoomRepository.findByTeamId(teamUser.getTeam().getId()))
                .orElse(null);

        if (room == null) {
            return List.of();
        }

        // 헤더나 메인 화면 알림용 목록입니다.
        // 채널별 마지막 메시지와 읽지 않은 메시지 수를 같이 내려줍니다.
        return chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId())
                .stream()
                .map(channel -> buildChannelSummary(channel, userId))
                .toList();
    }

    // 관리자가 전체 팀 채팅에서 아직 읽지 않은 학생 메시지 수를 조회하는 기능입니다.
    public ChatUnreadSummaryResponseDto getAdminUnreadSummary(String adminId) {
        return ChatUnreadSummaryResponseDto.builder()
                .totalUnreadCount(countAdminTotalUnread(adminId))
                .build();
    }

    // 관리자가 특정 채팅방의 채널별 마지막 메시지와 unreadCount를 조회하는 기능입니다.
    public List<ChatChannelSummaryResponseDto> getAdminChannelSummaries(Long roomId, String adminId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        return chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId())
                .stream()
                .map(channel -> buildAdminChannelSummary(channel, adminId))
                .toList();
    }

    // 관리자가 전체 팀 채팅방과 채널 목록을 조회하는 기능입니다.
    public List<ChatRoomResponseDto> getAdminRooms() {
        // 관리자 화면에서는 모든 팀 채팅방을 한 번에 볼 수 있어야 합니다.
        // 각 방마다 채널 목록까지 같이 담아 내려줍니다.
        return chatRoomRepository.findAll()
                .stream()
                .map(this::buildRoomResponse)
                .toList();
    }

    // 관리자가 특정 채팅방 상세를 조회하는 기능입니다.
    public ChatRoomResponseDto getAdminRoom(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        return buildRoomResponse(room);
    }

    // 관리자가 특정 팀에 연결된 채팅방을 조회하는 기능입니다.
    public ChatRoomResponseDto getAdminTeamRoom(Long teamId) {
        ChatRoom room = chatRoomRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("해당 팀의 채팅방이 없습니다."));

        return buildRoomResponse(room);
    }

    // 관리자가 특정 팀에 채팅방과 기본 채널을 생성하는 기능입니다.
    @Transactional
    public ChatRoomResponseDto createAdminRoom(Long teamId, String channelName, String creatorId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 팀입니다."));

        if (chatRoomRepository.findByTeamId(teamId).isPresent()) {
            throw new IllegalArgumentException("이미 해당 팀의 채팅방이 있습니다.");
        }

        User creator = chatAccessService.getUser(creatorId);
        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .team(team)
                .build());
        chatChannelRepository.save(ChatChannel.builder()
                .chatRoom(room)
                .channelName(normalize(channelName).isEmpty() ? "공통" : normalize(channelName))
                .createdBy(creator)
                .build());

        return buildRoomResponse(room);
    }

    // 관리자가 채팅방을 삭제하면서 하위 채널/메시지/읽음 상태도 함께 삭제하는 기능입니다.
    @Transactional
    public void deleteAdminRoom(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        List<ChatChannel> channels = chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId());
        for (ChatChannel channel : channels) {
            chatReadStatusRepository.deleteByChannelId(channel.getId());
            chatMessageRepository.deleteByChannelId(channel.getId());
        }
        chatChannelRepository.deleteByChatRoomId(room.getId());
        chatRoomRepository.delete(room);
    }

    // 팀원이 자기 팀 채팅방 안에 새 채널을 생성하는 기능입니다.
    @Transactional
    public ChatChannelResponseDto createChannel(Long roomId, String userId, ChatChannelRequestDto request) {
        ChatRoom room = chatAccessService.getAccessibleRoom(roomId, userId);
        assertTeamLeader(room.getTeam().getId(), userId);
        User creator = chatAccessService.getUser(userId);
        String channelName = normalize(request.getChannelName());

        if (channelName.isEmpty()) {
            throw new IllegalArgumentException("채널 이름이 필요합니다.");
        }

        // 채널은 한 팀 채팅방 안의 주제 구분입니다.
        // 예: 프론트엔드, 백엔드, 공통처럼 팀 내부에서 자유롭게 나눌 수 있습니다.
        ChatChannel channel = ChatChannel.builder()
                .chatRoom(room)
                .channelName(channelName)
                .createdBy(creator)
                .build();

        ChatChannelResponseDto response = ChatChannelResponseDto.from(chatChannelRepository.save(channel));
        publishChannelEventAfterCommit(room.getId(), ChatChannelEventDto.created(response));
        return response;
    }

    // 팀원이 자기 팀 채널 이름을 수정하는 기능입니다.
    @Transactional
    public ChatChannelResponseDto updateChannel(Long channelId, String userId, ChatChannelRequestDto request) {
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);
        assertTeamLeader(channel.getChatRoom().getTeam().getId(), userId);
        String channelName = normalize(request.getChannelName());

        if (channelName.isEmpty()) {
            throw new IllegalArgumentException("채널 이름이 필요합니다.");
        }

        channel.updateName(channelName);
        ChatChannelResponseDto response = ChatChannelResponseDto.from(channel);
        publishChannelEventAfterCommit(channel.getChatRoom().getId(), ChatChannelEventDto.updated(response));
        return response;
    }

    // 팀원이 자기 팀 채널과 해당 채널 메시지/읽음 상태를 삭제하는 기능입니다.
    @Transactional
    public void deleteChannel(Long channelId, String userId) {
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);
        assertTeamLeader(channel.getChatRoom().getTeam().getId(), userId);
        Long roomId = channel.getChatRoom().getId();

        if (isDefaultChannel(channel)) {
            throw new IllegalArgumentException("공통 채널은 삭제할 수 없습니다.");
        }

        // 현재는 채널 삭제 시 메시지도 같이 삭제합니다.
        // 추후 기여도 분석에 채팅 기록이 필요하면 soft delete 방식으로 바꾸는 게 좋습니다.
        chatReadStatusRepository.deleteByChannelId(channelId);
        chatMessageRepository.deleteByChannelId(channelId);
        chatChannelRepository.delete(channel);
        publishChannelEventAfterCommit(roomId, ChatChannelEventDto.deleted(channelId, roomId));
    }

    // 채널 접근 권한을 검사한 뒤 텍스트 또는 파일 메시지를 DB에 저장하는 기능입니다.
    @Transactional
    public ChatMessageResponseDto saveMessage(Long channelId, String senderId, ChatMessageRequestDto request) {
        // WebSocket에서는 Principal에서 userId만 받을 수 있습니다.
        // 그래서 userId로 User를 찾고, 채널 접근 권한도 여기서 같이 검사합니다.
        User sender = chatAccessService.getUser(senderId);
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, senderId);

        // 텍스트와 파일 URL 중 하나라도 있으면 채팅 메시지로 인정합니다.
        // 파일명/타입/크기는 화면 표시용 메타데이터라서 fileUrl이 있을 때만 같이 저장합니다.
        String messageText = normalize(request.getMessage());
        String fileUrl = normalize(request.getFileUrl());
        if (messageText.isEmpty() && fileUrl.isEmpty()) {   // 내용이 없으면 메시지 못 보냄
            throw new IllegalArgumentException("메시지 내용 또는 파일이 필요합니다.");
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .channel(channel)   //어느 채널에서 채팅하는지
                .sender(sender)     // 보내는 사람
                // 빈 문자열 대신 null을 저장하면 DB에서 "값 없음"을 구분하기 쉽습니다.
                .message(messageText.isEmpty() ? null : messageText)
                .fileUrl(fileUrl.isEmpty() ? null : fileUrl)
                .fileName(fileUrl.isEmpty() ? null : normalizeToNull(request.getFileName()))
                .fileType(fileUrl.isEmpty() ? null : normalizeToNull(request.getFileType()))
                .fileSize(fileUrl.isEmpty() ? null : request.getFileSize())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        ChatMessageResponseDto response = ChatMessageResponseDto.from(savedMessage);
        publishAdminUnreadEventAfterCommit("MESSAGE_CREATED", channel.getChatRoom().getId(), channel.getId());
        return response;
    }

    // 작성자가 본인이 보낸 텍스트 채팅 메시지를 수정하는 기능입니다.
    @Transactional
    public ChatMessageResponseDto updateMessage(Long messageId, String userId, ChatMessageUpdateRequestDto request) {
        ChatMessage chatMessage = getEditableMessage(messageId, userId);
        String messageText = normalize(request.getMessage());
        if (messageText.isEmpty()) {
            throw new IllegalArgumentException("수정할 메시지 내용이 필요합니다.");
        }

        chatMessage.updateMessage(messageText);
        chatMessageRepository.flush();

        ChatMessageResponseDto response = ChatMessageResponseDto.from(chatMessage);
        publishMessageEventAfterCommit(response.getChannelId(), ChatMessageEventDto.updated(response));
        return response;
    }

    // 작성자가 본인이 보낸 채팅 메시지를 삭제하는 기능입니다.
    @Transactional
    public void deleteMessage(Long messageId, String userId) {
        ChatMessage chatMessage = getEditableMessage(messageId, userId);
        Long channelId = chatMessage.getChannel().getId();
        chatMessageRepository.delete(chatMessage);
        publishMessageEventAfterCommit(channelId, ChatMessageEventDto.deleted(messageId, channelId));
    }

    // 사용자가 채널을 마지막으로 읽은 시간을 현재 시각으로 저장하는 기능입니다.
    @Transactional
    public void markAsRead(Long channelId, String userId) {
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);
        User user = chatAccessService.getUser(userId);

        saveReadStatus(channel, user);
    }

    // 관리자가 특정 채널의 학생 메시지를 읽음 처리하는 기능입니다.
    @Transactional
    public void markAdminChannelAsRead(Long channelId, String adminId) {
        ChatChannel channel = chatAccessService.getAdminChannel(channelId);
        User admin = chatAccessService.getUser(adminId);

        saveReadStatus(channel, admin);
        publishAdminUnreadEventAfterCommit("CHANNEL_READ", channel.getChatRoom().getId(), channelId, adminId);
    }

    // 사용자가 채널을 마지막으로 읽은 시간을 현재 시각으로 저장하는 기능입니다.
    private void saveReadStatus(ChatChannel channel, User user) {
        // 사용자가 채널 화면을 열었거나 마지막 메시지까지 확인했을 때 호출합니다.
        // 이후 unreadCount는 이 시간 이후에 온 다른 사람 메시지만 계산합니다.
        ChatReadStatus readStatus = chatReadStatusRepository.findByChannelIdAndUserUserId(channel.getId(), user.getUserId())
                .orElseGet(() -> ChatReadStatus.builder()
                        .channel(channel)
                        .user(user)
                        .lastReadAt(LocalDateTime.now())
                        .build());

        readStatus.updateLastReadAt(LocalDateTime.now());
        chatReadStatusRepository.save(readStatus);
    }

    // 특정 채널 메시지를 최신순 페이지로 조회하는 기능입니다.
    public Page<ChatMessageResponseDto> findMessages(Long channelId, String userId, Pageable pageable) {
        // 메시지가 많아지면 전체 조회는 느려집니다.
        // 그래서 최신순 페이지로 가져오고, 프론트에서 필요할 때 다음 페이지를 요청하게 합니다.
        chatAccessService.getAccessibleChannel(channelId, userId);

        return chatMessageRepository.findByChannelIdOrderByCreatedAtDesc(channelId, pageable)
                .map(ChatMessageResponseDto::from);
    }


    // 채팅방 엔티티와 채널 목록을 묶어 응답 DTO로 만드는 기능입니다.
    private ChatRoomResponseDto buildRoomResponse(ChatRoom room) {
        return ChatRoomResponseDto.from(
                room,
                resolveDisplayTeamName(room),
                chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId()),
                null
        );
    }

    // 채팅방 엔티티, 채널 목록, 로그인한 팀원 정보를 묶어 응답 DTO로 만드는 기능입니다.
    private ChatRoomResponseDto buildRoomResponse(ChatRoom room, TeamUser myTeamUser) {
        return ChatRoomResponseDto.from(
                room,
                resolveDisplayTeamName(room),
                chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId()),
                myTeamUser
        );
    }

    // 프로젝트 기획서 팀명이 있으면 우선 사용하고 없으면 기본 팀명을 반환하는 기능입니다.
    private String resolveDisplayTeamName(ChatRoom room) {
        return teamProjectRepository.findByTeamId(room.getTeam().getId())
                .map(TeamProject::getTeamName)
                .orElse(room.getTeam().getTeamName());
    }

    // 채널 정보, 마지막 메시지, unreadCount를 묶어 채널 요약 DTO로 만드는 기능입니다.
    private ChatChannelSummaryResponseDto buildChannelSummary(ChatChannel channel, String userId) {
        ChatMessageResponseDto lastMessage = chatMessageRepository.findTopByChannelIdOrderByCreatedAtDesc(channel.getId())
                .map(ChatMessageResponseDto::from)
                .orElse(null);

        long unreadCount = chatReadStatusRepository.findByChannelIdAndUserUserId(channel.getId(), userId)
                .map(readStatus -> chatMessageRepository.countByChannelIdAndCreatedAtAfterAndSenderUserIdNot(
                        channel.getId(),
                        readStatus.getLastReadAt(),
                        userId
                ))
                .orElseGet(() -> chatMessageRepository.countByChannelIdAndSenderUserIdNot(channel.getId(), userId));

        return ChatChannelSummaryResponseDto.builder()
                .channel(ChatChannelResponseDto.from(channel))
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .build();
    }

    // 관리자 기준 채널 정보, 마지막 메시지, unreadCount를 묶어 채널 요약 DTO로 만드는 기능입니다.
    private ChatChannelSummaryResponseDto buildAdminChannelSummary(ChatChannel channel, String adminId) {
        ChatMessageResponseDto lastMessage = chatMessageRepository.findTopByChannelIdOrderByCreatedAtDesc(channel.getId())
                .map(ChatMessageResponseDto::from)
                .orElse(null);

        return ChatChannelSummaryResponseDto.builder()
                .channel(ChatChannelResponseDto.from(channel))
                .lastMessage(lastMessage)
                .unreadCount(countAdminUnread(channel, adminId))
                .build();
    }

    // 관리자가 아직 읽지 않은 전체 학생 메시지 수를 계산하는 기능입니다.
    private long countAdminTotalUnread(String adminId) {
        return chatChannelRepository.findAll()
                .stream()
                .mapToLong(channel -> countAdminUnread(channel, adminId))
                .sum();
    }

    // 관리자가 특정 채널에서 아직 읽지 않은 학생 메시지 수를 계산하는 기능입니다.
    private long countAdminUnread(ChatChannel channel, String adminId) {
        return chatReadStatusRepository.findByChannelIdAndUserUserId(channel.getId(), adminId)
                .map(readStatus -> chatMessageRepository.countByChannelIdAndCreatedAtAfterAndSenderAccountRole(
                        channel.getId(),
                        readStatus.getLastReadAt(),
                        AccountRole.STUDENT
                ))
                .orElseGet(() -> chatMessageRepository.countByChannelIdAndSenderAccountRole(
                        channel.getId(),
                        AccountRole.STUDENT
                ));
    }

    // 메시지가 존재하고, 현재 사용자가 메시지 채널에 접근 가능하며 작성자인지 확인하는 기능입니다.
    private ChatMessage getEditableMessage(Long messageId, String userId) {
        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅 메시지입니다."));

        chatAccessService.getAccessibleChannel(chatMessage.getChannel().getId(), userId);
        if (!chatMessage.getSender().getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인이 보낸 메시지만 수정하거나 삭제할 수 있습니다.");
        }

        return chatMessage;
    }

    // null 문자열을 빈 문자열로 바꾸고 앞뒤 공백을 제거하는 기능입니다.
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    // 빈 문자열은 null로 바꿔 DB에 값 없음으로 저장하게 하는 기능입니다.
    private String normalizeToNull(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }

    // 로그인한 사용자의 팀원 정보를 조회하는 기능입니다.
    private TeamUser getMyTeamUser(String userId) {
        return teamUserRepository.findByUserUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("아직 배정된 팀이 없습니다."));
    }

    private void assertTeamLeader(Long teamId, String userId) {
        TeamUser teamUser = teamUserRepository.findByUserUserId(userId)
                .orElseThrow(() -> new AccessDeniedException("팀장만 채널을 관리할 수 있습니다."));

        if (!teamUser.getTeam().getId().equals(teamId) || teamUser.getLeaderRole() != LeaderRole.LEADER) {
            throw new AccessDeniedException("팀장만 채널을 관리할 수 있습니다.");
        }
    }

    private boolean isDefaultChannel(ChatChannel channel) {
        return "공통".equals(channel.getChannelName());
    }

    private void publishChannelEventAfterCommit(Long roomId, ChatChannelEventDto event) {
        publishAfterCommit(() -> messagingTemplate.convertAndSend(
                CHAT_CHANNEL_EVENTS_DESTINATION_FORMAT.formatted(roomId),
                event
        ));
    }

    private void publishMessageEventAfterCommit(Long channelId, ChatMessageEventDto event) {
        publishAfterCommit(() -> messagingTemplate.convertAndSend(
                CHAT_MESSAGE_EVENTS_DESTINATION_FORMAT.formatted(channelId),
                event
        ));
    }

    private void publishAdminUnreadEventAfterCommit(String type, Long roomId, Long channelId) {
        userRepository.findByAccountRole(AccountRole.ADMIN)
                .stream()
                .findFirst()
                .map(User::getUserId)
                .ifPresent(adminId -> publishAdminUnreadEventAfterCommit(type, roomId, channelId, adminId));
    }

    private void publishAdminUnreadEventAfterCommit(String type, Long roomId, Long channelId, String adminId) {
        ChatChannel channel = chatAccessService.getAdminChannel(channelId);
        ChatAdminUnreadEventDto event = ChatAdminUnreadEventDto.of(
                type,
                roomId,
                channelId,
                countAdminUnread(channel, adminId),
                countAdminTotalUnread(adminId)
        );

        publishAfterCommit(() -> messagingTemplate.convertAndSend(ADMIN_CHAT_UNREAD_DESTINATION, event));
    }

    private void publishAfterCommit(Runnable publisher) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publisher.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publisher.run();
            }
        });
    }
}
