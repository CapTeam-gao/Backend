package com.capteam.gaobackend.controller;

import com.capteam.gaobackend.dto.chat.ChatFileUploadResponseDto;
import com.capteam.gaobackend.dto.chat.ChatChannelPresenceResponseDto;
import com.capteam.gaobackend.dto.chat.ChatChannelRequestDto;
import com.capteam.gaobackend.dto.chat.ChatChannelResponseDto;
import com.capteam.gaobackend.dto.chat.ChatChannelSummaryResponseDto;
import com.capteam.gaobackend.dto.chat.ChatMessageRequestDto;
import com.capteam.gaobackend.dto.chat.ChatMessageResponseDto;
import com.capteam.gaobackend.dto.chat.ChatMessageUpdateRequestDto;
import com.capteam.gaobackend.dto.chat.ChatPinRequestDto;
import com.capteam.gaobackend.dto.chat.ChatRoomResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.service.ChatFileStorageService;
import com.capteam.gaobackend.service.ChatPresenceService;
import com.capteam.gaobackend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatFileStorageService chatFileStorageService;
    private final ChatPresenceService chatPresenceService;

    // 로그인한 사용자가 속한 팀의 채팅방과 채널 목록을 조회하는 기능입니다.
    @GetMapping("/rooms/my")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> getMyChatRoom(Authentication authentication) {
        // 학생이 채팅 화면에 들어올 때 가장 먼저 호출할 API입니다.
        // 본인이 속한 팀의 채팅방과 채널 목록을 한 번에 내려줍니다.
        return ApiResponse.ok(chatService.getMyChatRoom(authentication.getName()));
    }

    // 내 팀 채팅방의 채널별 마지막 메시지와 unreadCount를 조회하는 기능입니다.
    @GetMapping("/rooms/my/channel-summaries")
    public ResponseEntity<ApiResponse<List<ChatChannelSummaryResponseDto>>> getMyChannelSummaries(Authentication authentication) {
        // 헤더 채팅 알림이나 메인 화면의 채팅 미리보기용입니다.
        // 각 채널의 마지막 메시지와 읽지 않은 메시지 수를 내려줍니다.
        return ApiResponse.ok(chatService.getMyChannelSummaries(authentication.getName()));
    }

    // 특정 채팅방 상세를 조회하고 사용자 접근 권한을 검사하는 기능입니다.
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> getRoom(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        return ApiResponse.ok(chatService.getRoom(roomId, authentication.getName()));
    }

    // 팀 채팅방 안에 새 대화 채널을 생성하는 기능입니다.
    @PostMapping("/rooms/{roomId}/channels")
    public ResponseEntity<ApiResponse<ChatChannelResponseDto>> createChannel(
            @PathVariable Long roomId,
            @RequestBody ChatChannelRequestDto request,
            Authentication authentication
    ) {
        // 채널은 팀 내부 대화 주제입니다.
        // 예: 공통, 프론트엔드, 백엔드처럼 팀원이 필요에 따라 만들 수 있습니다.
        return ApiResponse.ok(chatService.createChannel(roomId, authentication.getName(), request));
    }

    // 팀 채팅 채널 이름을 수정하는 기능입니다.
    @PatchMapping("/channels/{channelId}")
    public ResponseEntity<ApiResponse<ChatChannelResponseDto>> updateChannel(
            @PathVariable Long channelId,
            @RequestBody ChatChannelRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(chatService.updateChannel(channelId, authentication.getName(), request));
    }

    // 팀 채팅 채널과 해당 채널의 메시지/읽음 상태를 삭제하는 기능입니다.
    @DeleteMapping("/channels/{channelId}")
    public ResponseEntity<ApiResponse<String>> deleteChannel(
            @PathVariable Long channelId,
            Authentication authentication
    ) {
        chatService.deleteChannel(channelId, authentication.getName());
        return ApiResponse.ok("채널이 삭제되었습니다.");
    }

    // 특정 채널에 첨부 파일을 업로드하고 채팅 메시지에서 사용할 파일 URL을 반환하는 기능입니다.
    @PostMapping("/channels/{channelId}/files")
    public ResponseEntity<ApiResponse<ChatFileUploadResponseDto>> uploadFile(
            @PathVariable Long channelId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        // 파일 업로드도 채널 기준으로 받습니다.
        // 그래야 이 사용자가 해당 팀 채널에 파일을 올릴 수 있는지 검사할 수 있습니다.
        return ApiResponse.ok(chatFileStorageService.upload(channelId, authentication.getName(), file));
    }

    // 특정 채널의 과거 메시지를 페이지 단위로 조회하는 기능입니다.
    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponseDto>>> getMessages(
            @PathVariable Long channelId,
            Pageable pageable,
            Authentication authentication
    ) {
        // 과거 메시지는 REST로 페이지 단위 조회하고, 새 메시지는 WebSocket 구독으로 받습니다.
        // 기본 정렬은 서비스에서 최신순으로 고정되어 있습니다.
        return ApiResponse.ok(chatService.findMessages(channelId, authentication.getName(), pageable));
    }

    // REST 방식으로 채팅 메시지를 저장하는 fallback 전송 기능입니다.
    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponseDto>> sendMessage(
            @PathVariable Long channelId,
            @RequestBody ChatMessageRequestDto request,
            Authentication authentication
    ) {
        // 기본 실시간 전송은 WebSocket /pub/chat/{channelId}/send 입니다.
        // 이 REST API는 HTTP 기반 fallback과 API 테스트 편의를 위해 같은 저장 로직을 재사용합니다.
        return ApiResponse.ok(chatService.saveMessage(channelId, authentication.getName(), request));
    }

    // 작성자가 본인이 보낸 채팅 메시지를 수정하는 기능입니다.
    @PatchMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<ChatMessageResponseDto>> updateMessage(
            @PathVariable Long messageId,
            @RequestBody @Valid ChatMessageUpdateRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(chatService.updateMessage(messageId, authentication.getName(), request));
    }

    // 작성자가 본인이 보낸 채팅 메시지를 삭제하는 기능입니다.
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<String>> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication
    ) {
        chatService.deleteMessage(messageId, authentication.getName());
        return ApiResponse.ok("메시지가 삭제되었습니다.");
    }

    // 특정 채널의 메시지를 현재 사용자 기준으로 읽음 처리하는 기능입니다.
    @PostMapping("/channels/{channelId}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @PathVariable Long channelId,
            Authentication authentication
    ) {
        // 사용자가 채널의 메시지를 확인했을 때 호출합니다.
        // 이후 channel-summaries의 unreadCount가 줄어듭니다.
        chatService.markAsRead(channelId, authentication.getName());
        return ApiResponse.ok("읽음 처리되었습니다.");
    }

    // 채널에 메시지를 상단 고정하는 기능입니다.
    @PostMapping("/channels/{channelId}/pin")
    public ResponseEntity<ApiResponse<ChatChannelResponseDto>> pinMessage(
            @PathVariable Long channelId,
            @RequestBody ChatPinRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(chatService.pinMessage(channelId, request.getMessageId(), authentication.getName()));
    }

    // 채널에 고정된 메시지를 해제하는 기능입니다.
    @DeleteMapping("/channels/{channelId}/pin")
    public ResponseEntity<ApiResponse<ChatChannelResponseDto>> unpinMessage(
            @PathVariable Long channelId,
            Authentication authentication
    ) {
        return ApiResponse.ok(chatService.unpinMessage(channelId, authentication.getName()));
    }

    // 특정 채널이 속한 팀의 팀원별 온라인 상태를 조회하는 기능입니다.
    @GetMapping("/channels/{channelId}/presence")
    public ResponseEntity<ApiResponse<ChatChannelPresenceResponseDto>> getPresence(
            @PathVariable Long channelId,
            Authentication authentication
    ) {
        // 화면에 처음 들어왔을 때 현재 팀원들의 online/offline 목록을 한 번 내려줍니다.
        // 이후 상태 변화는 WebSocket /sub/presence/teams/{teamId} 구독으로 실시간 반영하면 됩니다.
        return ApiResponse.ok(chatPresenceService.findChannelPresence(channelId, authentication.getName()));
    }
}
