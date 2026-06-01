package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.chat.ChatChannelRequestDto;
import com.capteam.gaobackend.dto.chat.ChatChannelResponseDto;
import com.capteam.gaobackend.dto.chat.ChatChannelSummaryResponseDto;
import com.capteam.gaobackend.dto.chat.ChatMessageRequestDto;
import com.capteam.gaobackend.dto.chat.ChatMessageResponseDto;
import com.capteam.gaobackend.dto.chat.ChatRoomResponseDto;
import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatMessage;
import com.capteam.gaobackend.entity.ChatReadStatus;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.repository.ChatChannelRepository;
import com.capteam.gaobackend.repository.ChatMessageRepository;
import com.capteam.gaobackend.repository.ChatReadStatusRepository;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatChannelRepository chatChannelRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatReadStatusRepository chatReadStatusRepository;
    private final ChatAccessService chatAccessService;

    public ChatRoomResponseDto getMyChatRoom(String userId) {
        ChatRoom room = chatAccessService.getMyChatRoom(userId);
        return buildRoomResponse(room);
    }

    public ChatRoomResponseDto getRoom(Long roomId, String userId) {
        ChatRoom room = chatAccessService.getAccessibleRoom(roomId, userId);
        return buildRoomResponse(room);
    }

    public List<ChatChannelSummaryResponseDto> getMyChannelSummaries(String userId) {
        ChatRoom room = chatAccessService.getMyChatRoom(userId);

        // 헤더나 메인 화면 알림용 목록입니다.
        // 채널별 마지막 메시지와 읽지 않은 메시지 수를 같이 내려줍니다.
        return chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId())
                .stream()
                .map(channel -> buildChannelSummary(channel, userId))
                .toList();
    }

    public List<ChatRoomResponseDto> getAdminRooms() {
        // 관리자 화면에서는 모든 팀 채팅방을 한 번에 볼 수 있어야 합니다.
        // 각 방마다 채널 목록까지 같이 담아 내려줍니다.
        return chatRoomRepository.findAll()
                .stream()
                .map(this::buildRoomResponse)
                .toList();
    }

    public ChatRoomResponseDto getAdminRoom(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

        return buildRoomResponse(room);
    }

    public ChatRoomResponseDto getAdminTeamRoom(Long teamId) {
        ChatRoom room = chatRoomRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("해당 팀의 채팅방이 없습니다."));

        return buildRoomResponse(room);
    }

    @Transactional
    public ChatChannelResponseDto createChannel(Long roomId, String userId, ChatChannelRequestDto request) {
        ChatRoom room = chatAccessService.getAccessibleRoom(roomId, userId);
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

        return ChatChannelResponseDto.from(chatChannelRepository.save(channel));
    }

    @Transactional
    public ChatChannelResponseDto updateChannel(Long channelId, String userId, ChatChannelRequestDto request) {
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);
        String channelName = normalize(request.getChannelName());

        if (channelName.isEmpty()) {
            throw new IllegalArgumentException("채널 이름이 필요합니다.");
        }

        channel.updateName(channelName);
        return ChatChannelResponseDto.from(channel);
    }

    @Transactional
    public void deleteChannel(Long channelId, String userId) {
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);

        // 현재는 채널 삭제 시 메시지도 같이 삭제합니다.
        // 추후 기여도 분석에 채팅 기록이 필요하면 soft delete 방식으로 바꾸는 게 좋습니다.
        chatReadStatusRepository.deleteByChannelId(channelId);
        chatMessageRepository.deleteByChannelId(channelId);
        chatChannelRepository.delete(channel);
    }

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

        return ChatMessageResponseDto.from(chatMessageRepository.save(chatMessage));
    }

    @Transactional
    public void markAsRead(Long channelId, String userId) {
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);
        User user = chatAccessService.getUser(userId);

        // 사용자가 채널 화면을 열었거나 마지막 메시지까지 확인했을 때 호출합니다.
        // 이후 unreadCount는 이 시간 이후에 온 다른 사람 메시지만 계산합니다.
        ChatReadStatus readStatus = chatReadStatusRepository.findByChannelIdAndUserUserId(channelId, userId)
                .orElseGet(() -> ChatReadStatus.builder()
                        .channel(channel)
                        .user(user)
                        .lastReadAt(LocalDateTime.now())
                        .build());

        readStatus.updateLastReadAt(LocalDateTime.now());
        chatReadStatusRepository.save(readStatus);
    }

    public Page<ChatMessageResponseDto> findMessages(Long channelId, String userId, Pageable pageable) {
        // 메시지가 많아지면 전체 조회는 느려집니다.
        // 그래서 최신순 페이지로 가져오고, 프론트에서 필요할 때 다음 페이지를 요청하게 합니다.
        chatAccessService.getAccessibleChannel(channelId, userId);

        return chatMessageRepository.findByChannelIdOrderByCreatedAtDesc(channelId, pageable)
                .map(ChatMessageResponseDto::from);
    }

    private ChatRoomResponseDto buildRoomResponse(ChatRoom room) {
        return ChatRoomResponseDto.from(
                room,
                chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId())
        );
    }

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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeToNull(String value) {
        String normalized = normalize(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
