package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.chat.AdminChatRoomCreateRequestDto;
import com.capteam.gaobackend.dto.chat.ChatChannelSummaryResponseDto;
import com.capteam.gaobackend.dto.chat.ChatMessageResponseDto;
import com.capteam.gaobackend.dto.chat.ChatRoomResponseDto;
import com.capteam.gaobackend.dto.chat.ChatUnreadSummaryResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
public class AdminChatController {

    private final ChatService chatService;

    // 관리자가 전체 팀 채팅방 목록과 채널 목록을 조회하는 기능입니다.
    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> getRooms() {
        // 선생님 관리 화면에서 전체 팀 채팅방 목록을 볼 때 사용합니다.
        // SecurityConfig에서 /api/admin/**는 ADMIN만 접근 가능하게 막고 있습니다.
        return ApiResponse.ok(chatService.getAdminRooms());
    }

    // 관리자가 전체 팀 채팅에서 아직 읽지 않은 학생 메시지 수를 조회하는 기능입니다.
    @GetMapping("/unread-summary")
    public ResponseEntity<ApiResponse<ChatUnreadSummaryResponseDto>> getUnreadSummary(Authentication authentication) {
        return ApiResponse.ok(chatService.getAdminUnreadSummary(authentication.getName()));
    }

    // 관리자가 특정 팀에 채팅방과 기본 채널을 생성하는 기능입니다.
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createRoom(
            @RequestBody AdminChatRoomCreateRequestDto request,
            Authentication authentication
    ) {
        return ApiResponse.ok(chatService.createAdminRoom(
                request.getTeamId(),
                request.getChannelName(),
                authentication.getName()
        ));
    }

    // 관리자가 특정 채팅방 상세 정보를 조회하는 기능입니다.
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> getRoom(@PathVariable Long roomId) {
        return ApiResponse.ok(chatService.getAdminRoom(roomId));
    }

    // 관리자가 특정 채팅방의 채널별 마지막 메시지와 unreadCount를 조회하는 기능입니다.
    @GetMapping("/rooms/{roomId}/channel-summaries")
    public ResponseEntity<ApiResponse<List<ChatChannelSummaryResponseDto>>> getChannelSummaries(
            @PathVariable Long roomId,
            Authentication authentication
    ) {
        return ApiResponse.ok(chatService.getAdminChannelSummaries(roomId, authentication.getName()));
    }

    // 관리자가 특정 채팅방과 하위 채널/메시지를 삭제하는 기능입니다.
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<String>> deleteRoom(@PathVariable Long roomId) {
        chatService.deleteAdminRoom(roomId);
        return ApiResponse.ok("채팅방이 삭제되었습니다.");
    }

    // 관리자가 특정 팀에 연결된 채팅방을 조회하는 기능입니다.
    @GetMapping("/teams/{teamId}/room")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> getTeamRoom(@PathVariable Long teamId) {
        return ApiResponse.ok(chatService.getAdminTeamRoom(teamId));
    }

    // 관리자가 특정 채널의 메시지를 페이지 단위로 조회하는 기능입니다.
    @GetMapping("/channels/{channelId}/messages")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponseDto>>> getMessages(
            @PathVariable Long channelId,
            Pageable pageable,
            Authentication authentication
    ) {
        // 관리자도 같은 메시지 조회 로직을 사용합니다.
        // ChatAccessService에서 ADMIN 계정은 모든 팀 채팅 접근을 허용합니다.
        return ApiResponse.ok(chatService.findMessages(channelId, authentication.getName(), pageable));
    }

    // 관리자가 특정 채널의 학생 메시지를 읽음 처리하는 기능입니다.
    @PostMapping("/channels/{channelId}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @PathVariable Long channelId,
            Authentication authentication
    ) {
        chatService.markAdminChannelAsRead(channelId, authentication.getName());
        return ApiResponse.ok("읽음 처리되었습니다.");
    }
}
