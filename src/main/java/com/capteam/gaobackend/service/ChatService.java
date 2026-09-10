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
import com.capteam.gaobackend.dto.chat.ChatNotificationEventDto;
import com.capteam.gaobackend.dto.chat.ChatRoomResponseDto;
import com.capteam.gaobackend.dto.chat.ChatUnreadEventDto;
import com.capteam.gaobackend.dto.chat.ChatUnreadSummaryResponseDto;
import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatMessage;
import com.capteam.gaobackend.entity.ChatReadStatus;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserFcmToken;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.NotificationStatus;
import com.capteam.gaobackend.enums.NotificationType;
import com.capteam.gaobackend.repository.ChatChannelRepository;
import com.capteam.gaobackend.repository.ChatMessageRepository;
import com.capteam.gaobackend.repository.ChatReadStatusRepository;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import com.capteam.gaobackend.repository.NotificationLogRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserFcmTokenRepository;
import com.capteam.gaobackend.repository.UserRepository;
import com.capteam.gaobackend.service.push.PushDispatchResult;
import com.capteam.gaobackend.service.push.PushMessageRequest;
import com.capteam.gaobackend.service.push.PushNotificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final String CHAT_CHANNEL_EVENTS_DESTINATION_FORMAT = "/sub/chat/rooms/%d/channels";
    private static final String CHAT_MESSAGE_EVENTS_DESTINATION_FORMAT = "/sub/chat/%d/events";
    private static final String ADMIN_CHAT_UNREAD_DESTINATION = "/sub/admin/chat/unread";
    private static final String USER_CHAT_UNREAD_DESTINATION = "/queue/chat/unread";
    private static final String USER_CHAT_UNREAD_BROADCAST_DESTINATION_FORMAT = "/sub/chat/unread/%s";
    private static final String USER_CHAT_NOTIFICATION_DESTINATION = "/queue/chat/notifications";
    private static final String CHAT_PUSH_CLICK_URL = "/user/chat";

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

    // 수신자가 채팅 화면(웹소켓 chat 구독)을 보고 있는지 판단해 토스트/푸시를 분기하는 필드입니다.
    private final ChatPresenceService chatPresenceService;

    // 백그라운드 수신자에게 보낼 FCM 등록 토큰을 조회하는 Repository 필드입니다.
    private final UserFcmTokenRepository userFcmTokenRepository;

    // 채팅 푸시 발송 이력을 남기고 조회하기 위한 Repository 필드입니다.
    private final NotificationLogRepository notificationLogRepository;

    // 원본 채팅 트랜잭션 커밋 이후에도 발송 로그를 별도 트랜잭션으로 저장하는 필드입니다.
    private final NotificationLogPersistenceService notificationLogPersistenceService;

    // 실제 FCM 전송을 Firebase 구현체에 위임하는 필드입니다.
    private final PushNotificationGateway pushNotificationGateway;


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
        ChatMessageResponseDto response = ChatMessageResponseDto.from(
                savedMessage,
                countReadMembers(savedMessage)
        );
        publishAdminUnreadEventAfterCommit("MESSAGE_CREATED", channel.getChatRoom().getId(), channel.getId());
        publishTeamUnreadEventsAfterCommit("MESSAGE_CREATED", channel, sender.getUserId());
        notifyTeamMembersOfNewMessage(channel, sender, savedMessage);
        return response;
    }

    // 새 메시지를 보낸 사람을 제외한 팀원 전원에게, 실시간 토스트와(앱을 안 보고 있으면) FCM 푸시를 보내는 기능입니다.
    private void notifyTeamMembersOfNewMessage(ChatChannel channel, User sender, ChatMessage message) {
        String teamName = resolveDisplayTeamName(channel.getChatRoom());
        String preview = message.getMessage() != null ? message.getMessage() : "파일을 보냈습니다.";

        teamUserRepository.findByTeamId(channel.getChatRoom().getTeam().getId())
                .stream()
                .map(TeamUser::getUser)
                .filter(recipient -> !recipient.getUserId().equals(sender.getUserId()))
                .forEach(recipient -> notifyChatMessage(recipient, channel, message, teamName, sender.getName(), preview));
    }

    // 한 명의 수신자에게 실시간 토스트와, 앱을 안 보고 있으면 FCM 푸시까지 보내는 기능입니다.
    private void notifyChatMessage(
            User recipient,
            ChatChannel channel,
            ChatMessage message,
            String teamName,
            String senderName,
            String preview
    ) {
        ChatNotificationEventDto event = ChatNotificationEventDto.of(
                channel.getId(),
                channel.getChannelName(),
                teamName,
                senderName,
                preview,
                message.getCreatedAt()
        );

        // 실시간 토스트는 WebSocket에 붙어 있기만 하면 항상 보낸다.
        // isOnline은 "채팅 채널(/sub/chat/{id})을 구독 중"이라는 뜻이라, 이 조건으로 걸러버리면
        // 채팅 화면 밖(대시보드 등)에 있는 팀원은 프론트가 토스트를 띄우려 해도 이벤트 자체가 안 왔다.
        // "지금 이 채널을 보고 있어서 토스트가 필요 없는지"는 프론트가 판단한다.
        publishAfterCommit(() -> messagingTemplate.convertAndSendToUser(
                recipient.getUserId(),
                USER_CHAT_NOTIFICATION_DESTINATION,
                event
        ));

        // FCM 푸시는 앱이 닫혀 있어도 알림을 받게 하기 위한 것이므로,
        // 채팅 채널을 보고 있는 사람에게는 중복이라 보내지 않는다.
        if (!chatPresenceService.isOnline(recipient.getUserId())) {
            publishAfterCommit(
                    () -> sendChatPushNotification(recipient, channel, message, teamName, senderName, preview));
        }
    }

    // 오프라인 수신자에게 FCM 푸시를 보내고 발송 이력을 남기는 기능입니다.
    private void sendChatPushNotification(
            User recipient,
            ChatChannel channel,
            ChatMessage message,
            String teamName,
            String senderName,
            String preview
    ) {
        // NotificationLog.targetId는 (user, type, targetId) 유니크 제약이 있어 메시지마다 겹치지 않게
        // 채팅 메시지 id를 쓴다. 클라이언트로 보내는 payload의 targetId(딥링크용)는 별개로 channelId를 쓴다.
        String logTargetId = String.valueOf(message.getId());
        LocalDateTime now = LocalDateTime.now();
        List<String> tokens = userFcmTokenRepository.findAllByUserUserId(recipient.getUserId())
                .stream()
                .map(UserFcmToken::getToken)
                .distinct()
                .toList();

        String title = teamName + " · " + senderName;

        if (tokens.isEmpty()) {
            notificationLogPersistenceService.save(NotificationLog.builder()
                    .user(recipient)
                    .type(NotificationType.CHAT_MESSAGE)
                    .targetId(logTargetId)
                    .status(NotificationStatus.FAILED)
                    .sentAt(now)
                    .title(title)
                    .body(preview)
                    .errorMessage("등록된 FCM 토큰이 없습니다.")
                    .build());
            return;
        }

        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", NotificationType.CHAT_MESSAGE.name());
        data.put("targetId", String.valueOf(channel.getId()));
        data.put("clickUrl", CHAT_PUSH_CLICK_URL);

        PushDispatchResult result = pushNotificationGateway.sendToTokens(
                tokens,
                new PushMessageRequest(title, preview, data)
        );

        String errorMessage = result.failureReasons().isEmpty()
                ? null
                : String.join(" | ", result.failureReasons());

        NotificationLog notificationLog = NotificationLog.builder()
                .user(recipient)
                .type(NotificationType.CHAT_MESSAGE)
                .targetId(logTargetId)
                .status(NotificationStatus.FAILED)
                .sentAt(now)
                .title(title)
                .body(preview)
                .targetTokenCount(tokens.size())
                .errorMessage(errorMessage)
                .build();

        if (result.successCount() > 0) {
            notificationLog.markSent(now, result.successCount(), result.failureCount());
        } else {
            notificationLog.markFailed(now, errorMessage);
        }

        notificationLogPersistenceService.save(notificationLog);
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

        ChatMessageResponseDto response = ChatMessageResponseDto.from(
                chatMessage,
                countReadMembers(chatMessage)
        );
        publishMessageEventAfterCommit(response.getChannelId(), ChatMessageEventDto.updated(response));
        return response;
    }

    // 작성자가 본인이 보낸 채팅 메시지를 삭제하는 기능입니다.
    @Transactional
    public void deleteMessage(Long messageId, String userId) {
        ChatMessage chatMessage = getEditableMessage(messageId, userId);
        ChatChannel channel = chatMessage.getChannel();
        Long channelId = channel.getId();

        if (messageId.equals(channel.getPinnedMessageId())) {
            channel.unpinMessage();
        }

        chatMessageRepository.delete(chatMessage);
        publishMessageEventAfterCommit(channelId, ChatMessageEventDto.deleted(messageId, channelId));
    }

    // 팀원이 채널에 메시지를 상단 고정하는 기능입니다.
    @Transactional
    public ChatChannelResponseDto pinMessage(Long channelId, Long messageId, String userId) {
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅 메시지입니다."));

        if (!message.getChannel().getId().equals(channelId)) {
            throw new IllegalArgumentException("해당 채널의 메시지가 아닙니다.");
        }

        channel.pinMessage(messageId);
        ChatChannelResponseDto response = ChatChannelResponseDto.from(channel);
        publishChannelEventAfterCommit(channel.getChatRoom().getId(), ChatChannelEventDto.updated(response));
        return response;
    }

    // 팀원이 채널에 고정된 메시지를 해제하는 기능입니다.
    @Transactional
    public ChatChannelResponseDto unpinMessage(Long channelId, String userId) {
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);
        channel.unpinMessage();

        ChatChannelResponseDto response = ChatChannelResponseDto.from(channel);
        publishChannelEventAfterCommit(channel.getChatRoom().getId(), ChatChannelEventDto.updated(response));
        return response;
    }

    // 사용자가 채널을 마지막으로 읽은 시간을 현재 시각으로 저장하는 기능입니다.
    @Transactional
    public void markAsRead(Long channelId, String userId) {
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);
        User user = chatAccessService.getUser(userId);

        saveReadStatus(channel, user);
        publishUserUnreadEventAfterCommit("CHANNEL_READ", channel, userId);
        publishMessageEventAfterCommit(channelId, ChatMessageEventDto.readStatusUpdated(channelId));
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
                .map(message -> ChatMessageResponseDto.from(message, countReadMembers(message)));
    }

    // 채널 인원(발신자 제외) 중 이 메시지가 온 시점 이후로 읽은 사람 수를 계산하는 기능입니다.
    private long countReadMembers(ChatMessage message) {
        return chatReadStatusRepository.countByChannelIdAndUserUserIdNotAndLastReadAtGreaterThanEqual(
                message.getChannel().getId(),
                message.getSender().getUserId(),
                message.getCreatedAt()
        );
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

    // 특정 사용자의 팀 채팅 전체 unreadCount를 계산하는 기능입니다.
    private long countUserTotalUnread(Long roomId, String userId) {
        return chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId)
                .stream()
                .mapToLong(channel -> countUserUnread(channel, userId))
                .sum();
    }

    // 특정 사용자의 특정 채널 unreadCount를 계산하는 기능입니다.
    private long countUserUnread(ChatChannel channel, String userId) {
        return chatReadStatusRepository.findByChannelIdAndUserUserId(channel.getId(), userId)
                .map(readStatus -> chatMessageRepository.countByChannelIdAndCreatedAtAfterAndSenderUserIdNot(
                        channel.getId(),
                        readStatus.getLastReadAt(),
                        userId
                ))
                .orElseGet(() -> chatMessageRepository.countByChannelIdAndSenderUserIdNot(channel.getId(), userId));
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
                .forEach(admin -> publishAdminUnreadEventAfterCommit(type, roomId, channelId, admin.getUserId()));
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

    private void publishTeamUnreadEventsAfterCommit(String type, ChatChannel channel, String senderId) {
        teamUserRepository.findByTeamId(channel.getChatRoom().getTeam().getId())
                .stream()
                .map(TeamUser::getUser)
                .filter(user -> !user.getUserId().equals(senderId))
                .forEach(user -> publishUserUnreadEventAfterCommit(type, channel, user.getUserId()));
    }

    private void publishUserUnreadEventAfterCommit(String type, ChatChannel channel, String userId) {
        Long roomId = channel.getChatRoom().getId();
        ChatUnreadEventDto event = ChatUnreadEventDto.of(
                type,
                roomId,
                channel.getId(),
                countUserUnread(channel, userId),
                countUserTotalUnread(roomId, userId)
        );

        publishAfterCommit(() -> {
            messagingTemplate.convertAndSendToUser(userId, USER_CHAT_UNREAD_DESTINATION, event);
            messagingTemplate.convertAndSend(
                    USER_CHAT_UNREAD_BROADCAST_DESTINATION_FORMAT.formatted(userId),
                    event
            );
        });
    }

    private void publishAfterCommit(Runnable publisher) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runIsolated(publisher);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runIsolated(publisher);
            }
        });
    }

    // 커밋 후 처리(unread 이벤트 발행, 실시간 토스트, FCM 푸시·발송 이력)는 채팅의 "부가 기능"이다.
    // 여기서 던진 예외는 STOMP 핸들러(ChatWebSocketController.sendMessage)까지 올라가
    // @SendTo("/sub/chat/{channelId}") 브로드캐스트를 건너뛰게 만든다
    // (= "메시지는 저장됐는데 실시간으로 안 보임"). 그래서 여기서 끊고 로그만 남긴다.
    // 예: notification_logs 유니크 제약 충돌(과거 로그와 chat_messages id가 겹칠 때).
    private void runIsolated(Runnable publisher) {
        try {
            publisher.run();
        } catch (Exception e) {
            log.warn("[CHAT] 커밋 후 알림 처리에 실패했습니다. 채팅 전달은 계속 진행합니다.", e);
        }
    }
}
