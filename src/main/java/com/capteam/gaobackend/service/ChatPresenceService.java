package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.chat.ChatChannelPresenceResponseDto;
import com.capteam.gaobackend.dto.chat.ChatMemberPresenceResponseDto;
import com.capteam.gaobackend.dto.chat.ChatPresenceEventDto;
import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.repository.TeamUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatPresenceService {

    // 팀원 목록을 조회해 온라인 멤버 수와 presence 응답을 구성하는 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // WebSocket 구독자에게 온라인/오프라인 이벤트를 발행하는 필드입니다.
    private final SimpMessagingTemplate messagingTemplate;

    // presence 조회 시 채널 접근 권한을 확인하는 Service 필드입니다.
    private final ChatAccessService chatAccessService;

    // userId -> teamId -> 현재 채팅 채널을 구독 중인 subscription key 목록입니다.
    // "online"은 WebSocket 연결 여부가 아니라 이 구조에 subscription이 남아 있는지로 판단합니다.
    // 같은 사용자가 여러 탭이나 여러 채널을 열 수 있으므로 sessionId가 아니라 subscription 단위로 관리합니다.
    private final ConcurrentHashMap<String, Map<Long, Set<String>>> userTeamSubscriptions = new ConcurrentHashMap<>();

    // sessionId -> userId 역방향 저장소입니다.
    // DISCONNECT 이벤트에는 user 정보가 불안정할 수 있어서 sessionId 기준 정리에 사용합니다.
    private final ConcurrentHashMap<String, String> sessionUsers = new ConcurrentHashMap<>();

    // subscription key -> 채팅방 presence 구독 정보입니다.
    // subscription key는 sessionId:subscriptionId 조합이라 같은 세션에서 여러 채널을 구독해도 구분됩니다.
    private final ConcurrentHashMap<String, ChatPresenceSubscription> chatSubscriptions = new ConcurrentHashMap<>();

    // sessionId -> 해당 세션에서 활성화된 채팅 subscription key 목록입니다.
    private final ConcurrentHashMap<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();

    // WebSocket 연결 시 사용자와 sessionId를 연결하는 기능입니다.
    public synchronized void connect(String sessionId, String userId) {
        String previousUserId = sessionUsers.put(sessionId, userId);
        log.info(
                "[PRESENCE CONNECT] sessionId={}, userId={}, previousUserId={}",
                sessionId,
                userId,
                previousUserId
        );
    }

    // 채팅 채널 구독 시 사용자를 해당 팀 채팅방 presence에 등록하는 기능입니다.
    // 이 메서드가 호출되어야 presence API에서 online=true가 됩니다.
    public synchronized void enterChat(String sessionId, String subscriptionId, String userId, Long channelId) {
        String connectedUserId = sessionUsers.get(sessionId);
        if (!userId.equals(connectedUserId)) {
            log.warn(
                    "[PRESENCE SUBSCRIBE REJECT] sessionId={}, userId={}, connectedUserId={}",
                    sessionId,
                    userId,
                    connectedUserId
            );
            return;
        }

        ChatChannel channel = chatAccessService.getAccessibleChannel(channelId, userId);
        Long teamId = channel.getChatRoom().getTeam().getId();
        String subscriptionKey = buildSubscriptionKey(sessionId, subscriptionId);

        ChatPresenceSubscription previousSubscription = chatSubscriptions.put(
                subscriptionKey,
                new ChatPresenceSubscription(userId, teamId)
        );
        if (previousSubscription != null) {
            // 같은 sessionId:subscriptionId가 재사용되면 이전 팀 presence를 먼저 정리합니다.
            removeSubscription(subscriptionKey, previousSubscription);
        }

        Map<Long, Set<String>> teamSubscriptions = userTeamSubscriptions.computeIfAbsent(
                userId,
                key -> new ConcurrentHashMap<>()
        );
        Set<String> subscriptions = teamSubscriptions.computeIfAbsent(teamId, key -> ConcurrentHashMap.newKeySet());
        boolean wasAbsent = subscriptions.isEmpty();
        subscriptions.add(subscriptionKey);

        sessionSubscriptions.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet())
                .add(subscriptionKey);

        if (wasAbsent) {
            // 이 팀에서 첫 채팅 구독이 생긴 순간에만 online 이벤트를 한 번 발행합니다.
            publishPresence(userId, teamId, true);
        }
    }

    // 채팅 채널 구독 해제 시 해당 subscription만 presence에서 제거하는 기능입니다.
    // 같은 사용자가 다른 탭/채널을 아직 구독 중이면 offline으로 바꾸지 않습니다.
    public synchronized void leaveChat(String sessionId, String subscriptionId) {
        String subscriptionKey = buildSubscriptionKey(sessionId, subscriptionId);
        ChatPresenceSubscription subscription = chatSubscriptions.remove(subscriptionKey);
        if (subscription == null) {
            log.warn(
                    "[PRESENCE UNSUBSCRIBE MISS] sessionId={}, subscriptionId={}",
                    sessionId,
                    subscriptionId
            );
            return;
        }

        Set<String> sessionSubscriptionSet = sessionSubscriptions.get(sessionId);
        if (sessionSubscriptionSet != null) {
            sessionSubscriptionSet.remove(subscriptionKey);
            if (sessionSubscriptionSet.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
            }
        }

        removeSubscription(subscriptionKey, subscription);
    }

    // WebSocket 연결 종료 시 해당 세션의 채팅방 presence와 연결 정보를 정리하는 기능입니다.
    // 프론트가 unsubscribe를 못 보내고 브라우저가 닫혀도 이 경로에서 해당 세션의 구독을 모두 제거합니다.
    public synchronized void disconnect(String sessionId) {
        String userId = sessionUsers.remove(sessionId);
        log.info("[PRESENCE DISCONNECT] sessionId={}, userId={}", sessionId, userId);

        Set<String> subscriptionKeys = sessionSubscriptions.remove(sessionId);
        Set<String> keysToRemove = subscriptionKeys == null
                ? new HashSet<>()
                : new HashSet<>(subscriptionKeys);

        // sessionSubscriptions 인덱스가 예상치 못하게 누락되어도 실제 구독 저장소에 남은 값을 찾아 정리합니다.
        // 이 방어 로직 덕분에 DISCONNECT 이후 presence가 계속 online으로 고정되는 상태를 막을 수 있습니다.
        String subscriptionKeyPrefix = sessionId + ":";
        chatSubscriptions.keySet().stream()
                .filter(subscriptionKey -> subscriptionKey.startsWith(subscriptionKeyPrefix))
                .forEach(keysToRemove::add);

        if (keysToRemove.isEmpty()) {
            log.info("[PRESENCE SESSIONS] userId={}, sessions=[]", userId);
            return;
        }

        for (String subscriptionKey : keysToRemove) {
            ChatPresenceSubscription subscription = chatSubscriptions.remove(subscriptionKey);
            if (subscription != null) {
                removeSubscription(subscriptionKey, subscription);
            }
        }

        log.info(
                "[PRESENCE SESSIONS] userId={}, sessions={}",
                userId,
                findSessionIdsByUserId(userId)
        );
    }

    // 특정 사용자가 현재 어느 팀 채팅방에 입장해 있는지 확인하는 기능입니다.
    // WebSocket만 연결되어 있고 /sub/chat/{channelId} 구독이 없으면 false입니다.
    public boolean isOnline(String userId) {
        Map<Long, Set<String>> teamSubscriptions = userTeamSubscriptions.get(userId);
        return teamSubscriptions != null && teamSubscriptions.values()
                .stream()
                .anyMatch(sessions -> !sessions.isEmpty());
    }

    // 특정 팀에 속한 온라인 팀원 수를 계산하는 기능입니다.
    public long countOnlineMembersByTeamId(Long teamId) {
        return teamUserRepository.findByTeamId(teamId)
                .stream()
                .filter(teamUser -> isOnlineInTeam(teamUser.getUser().getUserId(), teamId))
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
                        isOnlineInTeam(teamUser.getUser().getUserId(), teamId)
                ))
                .toList();

        return ChatChannelPresenceResponseDto.builder()
                .teamId(teamId)
                .members(members)
                .build();
    }

    // 사용자의 온라인/오프라인 변경 이벤트를 해당 팀 presence 구독 주소로 발행하는 기능입니다.
    private void publishPresence(String userId, Long teamId, boolean online) {
        teamUserRepository.findByUserUserId(userId)
                .filter(teamUser -> teamUser.getTeam().getId().equals(teamId))
                .ifPresent(teamUser -> messagingTemplate.convertAndSend(
                        "/sub/presence/teams/" + teamId,
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

    private void removeSubscription(String subscriptionKey, ChatPresenceSubscription subscription) {
        Map<Long, Set<String>> teamSubscriptions = userTeamSubscriptions.get(subscription.userId());
        if (teamSubscriptions == null) {
            return;
        }

        Set<String> subscriptions = teamSubscriptions.get(subscription.teamId());
        if (subscriptions == null) {
            return;
        }

        subscriptions.remove(subscriptionKey);
        if (subscriptions.isEmpty()) {
            // 해당 팀에서 마지막 채팅 구독이 사라진 경우에만 offline 이벤트를 발행합니다.
            teamSubscriptions.remove(subscription.teamId());
            publishPresence(subscription.userId(), subscription.teamId(), false);
        }

        if (teamSubscriptions.isEmpty()) {
            userTeamSubscriptions.remove(subscription.userId());
        }
    }

    private boolean isOnlineInTeam(String userId, Long teamId) {
        Map<Long, Set<String>> teamSubscriptions = userTeamSubscriptions.get(userId);
        if (teamSubscriptions == null) {
            return false;
        }

        Set<String> subscriptions = teamSubscriptions.get(teamId);
        return subscriptions != null && !subscriptions.isEmpty();
    }

    private String buildSubscriptionKey(String sessionId, String subscriptionId) {
        return sessionId + ":" + subscriptionId;
    }

    private Set<String> findSessionIdsByUserId(String userId) {
        if (userId == null) {
            return Set.of();
        }

        Set<String> sessionIds = new HashSet<>();
        sessionUsers.forEach((sessionId, connectedUserId) -> {
            if (userId.equals(connectedUserId)) {
                sessionIds.add(sessionId);
            }
        });
        return sessionIds;
    }

    private record ChatPresenceSubscription(String userId, Long teamId) {
    }
}
