package com.capteam.gaobackend.service;

import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.TeamStatus;
import com.capteam.gaobackend.repository.TeamUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatPresenceServiceTest {

    @Mock private TeamUserRepository teamUserRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ChatAccessService chatAccessService;

    private ChatPresenceService chatPresenceService;
    private TeamUser teamUser;
    private ChatChannel channel;

    @BeforeEach
    void setUp() {
        chatPresenceService = new ChatPresenceService(
                teamUserRepository,
                messagingTemplate,
                chatAccessService
        );

        Team team = Team.builder()
                .teamName("1팀")
                .grade(Grade.GRADE_2)
                .status(TeamStatus.APPROVED)
                .build();
        ReflectionTestUtils.setField(team, "id", 1L);

        // stu2301은 테스트에서만 쓰는 더미 학생 계정입니다.
        // 실제 DB 계정에 의존하지 않고, "1팀의 팀원이 채팅 채널을 구독했을 때 online이 되는지"만 검증합니다.
        User user = User.builder()
                .userId("stu2301")
                .name("홍길동")
                .accountRole(AccountRole.STUDENT)
                .build();
        teamUser = TeamUser.builder()
                .team(team)
                .user(user)
                .studentRole(StudentRole.BACKEND)
                .leaderRole(LeaderRole.MEMBER)
                .build();

        ChatRoom room = ChatRoom.builder()
                .team(team)
                .build();
        ReflectionTestUtils.setField(room, "id", 1L);
        channel = ChatChannel.builder()
                .chatRoom(room)
                .channelName("공통")
                .createdBy(user)
                .build();
        ReflectionTestUtils.setField(channel, "id", 10L);
    }

    @Test
    void tracksPresenceOnlyAfterChatSubscription() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));

        // WebSocket CONNECT만으로는 채팅방에 들어온 것으로 보지 않습니다.
        chatPresenceService.connect("session-1", "stu2301");

        assertThat(chatPresenceService.isOnline("stu2301")).isFalse();

        // 프론트가 /sub/chat/{channelId}를 구독해야 채팅방 presence가 online으로 바뀝니다.
        chatPresenceService.enterChat("session-1", "sub-1", "stu2301", 10L);

        assertThat(chatPresenceService.isOnline("stu2301")).isTrue();
        verify(messagingTemplate).convertAndSend(eq("/sub/presence/teams/1"), isA(Object.class));
    }

    @Test
    void removesPresenceWhenChatSubscriptionLeaves() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));

        // 채팅방 라우트에서 나가며 STOMP 구독을 해제하는 상황입니다.
        chatPresenceService.connect("session-1", "stu2301");
        chatPresenceService.enterChat("session-1", "sub-1", "stu2301", 10L);
        chatPresenceService.leaveChat("session-1", "sub-1");

        assertThat(chatPresenceService.isOnline("stu2301")).isFalse();
    }

    @Test
    void disconnectRemovesAllChatSubscriptionsForSession() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));

        // 브라우저 새로고침/탭 종료처럼 unsubscribe 없이 연결이 끊기는 상황입니다.
        // DISCONNECT만 와도 해당 세션의 채팅 구독이 모두 제거되어야 합니다.
        chatPresenceService.connect("session-1", "stu2301");
        chatPresenceService.enterChat("session-1", "sub-1", "stu2301", 10L);
        chatPresenceService.disconnect("session-1");

        assertThat(chatPresenceService.isOnline("stu2301")).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void disconnectRemovesOrphanSubscriptionEvenWhenSessionIndexIsMissing() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));

        chatPresenceService.connect("session-1", "stu2301");
        chatPresenceService.enterChat("session-1", "sub-1", "stu2301", 10L);

        // 세션별 구독 인덱스만 유실된 비정상 상태를 재현합니다.
        Map<String, Set<String>> sessionSubscriptions =
                (Map<String, Set<String>>) ReflectionTestUtils.getField(chatPresenceService, "sessionSubscriptions");
        assertThat(sessionSubscriptions).isNotNull();
        sessionSubscriptions.remove("session-1");

        chatPresenceService.disconnect("session-1");

        assertThat(chatPresenceService.isOnline("stu2301")).isFalse();
    }

    @Test
    void channelPresenceUsesChatSubscriptionState() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(teamUser));

        // presence API가 userSessions(WebSocket 연결)이 아니라 채팅 구독 상태를 기준으로 응답하는지 확인합니다.
        chatPresenceService.connect("session-1", "stu2301");
        chatPresenceService.enterChat("session-1", "sub-1", "stu2301", 10L);

        assertThat(chatPresenceService.findChannelPresence(10L, "stu2301").getMembers())
                .singleElement()
                .satisfies(member -> assertThat(member.isOnline()).isTrue());
    }

    @Test
    void rejectsSubscriptionArrivingAfterDisconnect() {
        chatPresenceService.connect("session-1", "stu2301");
        chatPresenceService.disconnect("session-1");

        // 연결 종료보다 늦게 처리된 SUBSCRIBE 이벤트는 presence를 다시 살리면 안 됩니다.
        chatPresenceService.enterChat("session-1", "sub-1", "stu2301", 10L);

        assertThat(chatPresenceService.isOnline("stu2301")).isFalse();
    }
}
