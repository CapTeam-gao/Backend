package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.chat.ChatChannelPresenceResponseDto;
import com.capteam.gaobackend.dto.chat.ChatMemberPresenceResponseDto;
import com.capteam.gaobackend.dto.chat.ChatPresenceEventDto;
import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.repository.TeamUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatPresenceService {

    // 팀원 목록을 조회해 온라인 멤버 수와 presence 응답을 구성하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // WebSocket 구독자에게 온라인/오프라인 이벤트를 발행하는 필드입니다.
    private final SimpMessagingTemplate messagingTemplate;

    // presence 조회 시 채널 접근 권한을 확인하는 Service 필드입니다.
    private final ChatAccessService chatAccessService;

    // userId -> 현재 접속 중인 WebSocket sessionId 목록입니다.
    // 같은 사용자가 브라우저 탭을 여러 개 열 수 있으므로 session을 Set으로 관리합니다.
    private final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    // sessionId -> userId 역방향 저장소입니다.
    // DISCONNECT 이벤트에는 user 정보가 불안정할 수 있어서 sessionId로 사용자를 다시 찾습니다.
    private final ConcurrentHashMap<String, String> sessionUsers = new ConcurrentHashMap<>();

    // WebSocket 연결 시 사용자 세션을 온라인 상태로 등록하는 기능입니다.
    public void connect(String sessionId, String userId) {
        Set<String> sessions = userSessions.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = sessions.isEmpty();

        sessions.add(sessionId);
        sessionUsers.put(sessionId, userId);

        // 첫 번째 탭/세션이 연결될 때만 online 이벤트를 보냅니다.
        // 이미 온라인인 사용자가 탭을 하나 더 열 때마다 이벤트가 중복으로 나가지 않게 하기 위함입니다.
        if (wasOffline) {
            publishPresence(userId, true);
        }
    }

    // WebSocket 연결 종료 시 세션을 제거하고 마지막 세션이면 오프라인 처리하는 기능입니다.
    public void disconnect(String sessionId) {
        String userId = sessionUsers.remove(sessionId);
        if (userId == null) {
            return;
        }

        Set<String> sessions = userSessions.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(sessionId);

        // 사용자의 마지막 WebSocket 세션이 끊겼을 때만 offline으로 봅니다.
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
            publishPresence(userId, false);
        }
    }

    // 특정 사용자가 현재 온라인인지 확인하는 기능입니다.
    public boolean isOnline(String userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    // 특정 팀에 속한 온라인 팀원 수를 계산하는 기능입니다.
    public long countOnlineMembersByTeamId(Long teamId) {
        return teamUserRepository.findByTeamId(teamId)
                .stream()
                .filter(teamUser -> isOnline(teamUser.getUser().getUserId()))
                .count();
    }

    // 특정 채널이 속한 팀의 팀원별 온라인 상태 목록을 조회하는 기능입니다.
    public ChatChannelPresenceResponseDto findChannelPresence(Long channelId, String userId) {
        // 온라인 목록도 팀 정보이므로 채널 접근 권한을 확인하고 내려줍니다.
        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);

        Long teamId = channel.getChatRoom().getTeam().getId();

        List<ChatMemberPresenceResponseDto> members = teamUserRepository.findByTeamId(teamId)
                .stream()
                .map(teamUser -> ChatMemberPresenceResponseDto.of(
                        teamUser,
                        isOnline(teamUser.getUser().getUserId())
                ))
                .toList();

        return ChatChannelPresenceResponseDto.builder()
                .teamId(teamId)
                .members(members)
                .build();
    }

    // 사용자의 온라인/오프라인 변경 이벤트를 해당 팀 presence 구독 주소로 발행하는 기능입니다.
    private void publishPresence(String userId, boolean online) {
        // 현재 프로젝트에서는 학생은 하나의 팀에 속한다고 보고 팀 채팅 상태를 broadcast 합니다.
        // 팀이 없는 관리자나 아직 팀 배정 전 학생은 presence 이벤트를 보낼 팀이 없으므로 무시합니다.
        teamUserRepository.findByUserUserId(userId)
                .ifPresent(teamUser -> messagingTemplate.convertAndSend(
                        "/sub/presence/teams/" + teamUser.getTeam().getId(),
                        buildEvent(teamUser, online)
                ));
    }

    // TeamUser와 온라인 여부를 WebSocket 이벤트 DTO로 변환하는 기능입니다.
    private ChatPresenceEventDto buildEvent(TeamUser teamUser, boolean online) {
        return ChatPresenceEventDto.builder()
                .userId(teamUser.getUser().getUserId())
                .name(teamUser.getUser().getName())
                .online(online)
                .build();
    }
}
