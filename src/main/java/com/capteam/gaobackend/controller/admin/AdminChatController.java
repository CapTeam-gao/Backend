package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.chat.ChatMessageResponseDto;
import com.capteam.gaobackend.dto.chat.ChatRoomResponseDto;
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

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> getRooms() {
        // 선생님 관리 화면에서 전체 팀 채팅방 목록을 볼 때 사용합니다.
        // SecurityConfig에서 /api/admin/**는 ADMIN만 접근 가능하게 막고 있습니다.
        return ApiResponse.ok(chatService.getAdminRooms());
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> getRoom(@PathVariable Long roomId) {
        return ApiResponse.ok(chatService.getAdminRoom(roomId));
    }

    @GetMapping("/teams/{teamId}/room")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> getTeamRoom(@PathVariable Long teamId) {
        return ApiResponse.ok(chatService.getAdminTeamRoom(teamId));
    }

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
}
