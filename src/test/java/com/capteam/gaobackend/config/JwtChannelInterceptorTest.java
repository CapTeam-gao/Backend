package com.capteam.gaobackend.config;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.service.ChatAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private ChatAccessService access;
    private JwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtChannelInterceptor(tokenProvider, access);
    }

    private Message<byte[]> frame(StompCommand command, String destination, boolean authenticated) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(command);
        headers.setDestination(destination);
        if (authenticated) {
            headers.setUser(new UsernamePasswordAuthenticationToken("student", null, List.of()));
        }
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sub/chat/12", "/sub/chat/12/events"})
    void checksChannelMembership(String destination) {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, destination, true), null);
        verify(access).getAccessibleChannel(12L, "student");
    }

    @Test
    void rejectsOtherTeam() {
        when(access.getAccessibleChannel(12L, "student")).thenThrow(new IllegalArgumentException("Denied"));
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/sub/chat/12", true), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checksRoomAndPresenceMembership() {
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/sub/chat/rooms/4/channels", true), null);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/sub/presence/teams/5", true), null);
        verify(access).getAccessibleRoom(4L, "student");
        verify(access).checkTeamAccess(5L, "student");
    }

    @Test
    void checksSendMembership() {
        interceptor.preSend(frame(StompCommand.SEND, "/pub/chat/12/send", true), null);
        verify(access).getAccessibleChannel(12L, "student");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sub/chat/**", "/sub/chat/*", "/queue/chat/unread", "/sub/chat/unread/other", "/user/other/queue/chat/unread"})
    void rejectsUnsafeSubscriptions(String destination) {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, destination, true), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sub/chat/12", "/sub/notices", "/queue/chat/unread", "/user/queue/chat/notifications"})
    void rejectsDirectBrokerPublication(String destination) {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SEND, destination, true), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/sub/notices", "/user/queue/chat/unread", "/user/queue/chat/notifications", "/user/queue/errors", "/sub/chat/unread/student"})
    void allowsExistingFrontendPublicAndPersonalTopics(String destination) {
        Message<byte[]> message = frame(StompCommand.SUBSCRIBE, destination, true);
        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    @Test
    void adminTopicRequiresAdminRole() {
        when(access.getUser("student")).thenReturn(User.builder().accountRole(AccountRole.STUDENT).build());
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/sub/admin/chat/unread", true), null))
                .isInstanceOf(AccessDeniedException.class);
        when(access.getUser("student")).thenReturn(User.builder().accountRole(AccountRole.ADMIN).build());
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/sub/admin/chat/unread", true), null);
    }

    @Test
    void subscribeAndSendRequireSessionAuthentication() {
        for (StompCommand command : List.of(StompCommand.SUBSCRIBE, StompCommand.SEND)) {
            assertThatThrownBy(() -> interceptor.preSend(frame(command, "/sub/notices", false), null))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void connectRejectsMissingToken() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "/ws", false), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
