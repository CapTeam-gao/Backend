package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.chat.ChatChannelEventDto;
import com.capteam.gaobackend.dto.chat.ChatChannelRequestDto;
import com.capteam.gaobackend.dto.chat.ChatMessageEventDto;
import com.capteam.gaobackend.dto.chat.ChatMessageUpdateRequestDto;
import com.capteam.gaobackend.dto.chat.ChatRoomResponseDto;
import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatMessage;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.repository.ChatChannelRepository;
import com.capteam.gaobackend.repository.ChatMessageRepository;
import com.capteam.gaobackend.repository.ChatReadStatusRepository;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceEventTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatChannelRepository chatChannelRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatReadStatusRepository chatReadStatusRepository;
    @Mock private ChatAccessService chatAccessService;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private ChatService chatService;
    private Team team;
    private ChatRoom room;
    private ChatChannel channel;
    private ChatMessage message;
    private User user;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                chatMessageRepository,
                chatChannelRepository,
                chatRoomRepository,
                chatReadStatusRepository,
                chatAccessService,
                teamRepository,
                teamUserRepository,
                messagingTemplate
        );

        team = Team.builder()
                .teamName("1팀")
                .build();
        ReflectionTestUtils.setField(team, "id", 1L);

        room = ChatRoom.builder()
                .team(team)
                .build();
        ReflectionTestUtils.setField(room, "id", 100L);

        user = User.builder()
                .userId("stu2301")
                .name("장준민")
                .build();

        channel = ChatChannel.builder()
                .chatRoom(room)
                .channelName("프론트엔드")
                .createdBy(user)
                .build();
        ReflectionTestUtils.setField(channel, "id", 10L);

        message = ChatMessage.builder()
                .channel(channel)
                .sender(user)
                .message("기존 메시지")
                .build();
        ReflectionTestUtils.setField(message, "id", 1L);
    }

    @Test
    void getMyChatRoomIncludesMyMemberRole() {
        when(chatAccessService.getMyChatRoom("stu2301")).thenReturn(room);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser(LeaderRole.LEADER)));
        when(chatChannelRepository.findByChatRoomIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(channel));

        ChatRoomResponseDto response = chatService.getMyChatRoom("stu2301");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTeamName()).isEqualTo("1팀");
        assertThat(response.getMyMember().getUserId()).isEqualTo("stu2301");
        assertThat(response.getMyMember().getName()).isEqualTo("장준민");
        assertThat(response.getMyMember().getLeaderRole()).isEqualTo(LeaderRole.LEADER);
        assertThat(response.getChannels()).hasSize(1);
    }

    @Test
    void updateMessagePublishesMessageUpdatedEvent() {
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        ChatMessageUpdateRequestDto request = new ChatMessageUpdateRequestDto();
        ReflectionTestUtils.setField(request, "message", "수정된 메시지");

        chatService.updateMessage(
                1L,
                "stu2301",
                request
        );

        ArgumentCaptor<ChatMessageEventDto> eventCaptor = ArgumentCaptor.forClass(ChatMessageEventDto.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat/10/events"), eventCaptor.capture());

        ChatMessageEventDto event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("MESSAGE_UPDATED");
        assertThat(event.getMessage().getId()).isEqualTo(1L);
        assertThat(event.getMessage().getChannelId()).isEqualTo(10L);
        assertThat(event.getMessage().getMessage()).isEqualTo("수정된 메시지");
    }

    @Test
    void deleteMessagePublishesMessageDeletedEvent() {
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);

        chatService.deleteMessage(1L, "stu2301");

        ArgumentCaptor<ChatMessageEventDto> eventCaptor = ArgumentCaptor.forClass(ChatMessageEventDto.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat/10/events"), eventCaptor.capture());

        ChatMessageEventDto event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("MESSAGE_DELETED");
        assertThat(event.getMessageId()).isEqualTo(1L);
        assertThat(event.getChannelId()).isEqualTo(10L);
    }

    @Test
    void createChannelPublishesChannelCreatedEvent() {
        when(chatAccessService.getAccessibleRoom(100L, "stu2301")).thenReturn(room);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser(LeaderRole.LEADER)));
        when(chatAccessService.getUser("stu2301")).thenReturn(user);
        when(chatChannelRepository.save(org.mockito.ArgumentMatchers.any(ChatChannel.class)))
                .thenAnswer(invocation -> {
                    ChatChannel savedChannel = invocation.getArgument(0);
                    ReflectionTestUtils.setField(savedChannel, "id", 12L);
                    return savedChannel;
                });

        chatService.createChannel(
                100L,
                "stu2301",
                ChatChannelRequestDto.builder()
                        .channelName("프론트엔드")
                        .build()
        );

        ArgumentCaptor<ChatChannelEventDto> eventCaptor = ArgumentCaptor.forClass(ChatChannelEventDto.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat/rooms/100/channels"), eventCaptor.capture());

        ChatChannelEventDto event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("CHANNEL_CREATED");
        assertThat(event.getChannel().getId()).isEqualTo(12L);
        assertThat(event.getChannel().getRoomId()).isEqualTo(100L);
        assertThat(event.getChannel().getChannelName()).isEqualTo("프론트엔드");
    }

    @Test
    void updateChannelPublishesChannelUpdatedEvent() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser(LeaderRole.LEADER)));

        chatService.updateChannel(
                10L,
                "stu2301",
                ChatChannelRequestDto.builder()
                        .channelName("백엔드")
                        .build()
        );

        ArgumentCaptor<ChatChannelEventDto> eventCaptor = ArgumentCaptor.forClass(ChatChannelEventDto.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat/rooms/100/channels"), eventCaptor.capture());

        ChatChannelEventDto event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("CHANNEL_UPDATED");
        assertThat(event.getChannel().getId()).isEqualTo(10L);
        assertThat(event.getChannel().getRoomId()).isEqualTo(100L);
        assertThat(event.getChannel().getChannelName()).isEqualTo("백엔드");
    }

    @Test
    void deleteChannelPublishesChannelDeletedEvent() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser(LeaderRole.LEADER)));

        chatService.deleteChannel(10L, "stu2301");

        ArgumentCaptor<ChatChannelEventDto> eventCaptor = ArgumentCaptor.forClass(ChatChannelEventDto.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat/rooms/100/channels"), eventCaptor.capture());

        ChatChannelEventDto event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("CHANNEL_DELETED");
        assertThat(event.getChannelId()).isEqualTo(10L);
        assertThat(event.getRoomId()).isEqualTo(100L);
    }

    @Test
    void createChannelRejectsNonLeader() {
        when(chatAccessService.getAccessibleRoom(100L, "stu2301")).thenReturn(room);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser(LeaderRole.MEMBER)));

        assertThatThrownBy(() -> chatService.createChannel(
                100L,
                "stu2301",
                ChatChannelRequestDto.builder()
                        .channelName("프론트엔드")
                        .build()
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("팀장만 채널을 관리할 수 있습니다.");
    }

    @Test
    void updateChannelRejectsNonLeader() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser(LeaderRole.MEMBER)));

        assertThatThrownBy(() -> chatService.updateChannel(
                10L,
                "stu2301",
                ChatChannelRequestDto.builder()
                        .channelName("백엔드")
                        .build()
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("팀장만 채널을 관리할 수 있습니다.");
    }

    @Test
    void deleteChannelRejectsNonLeader() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser(LeaderRole.MEMBER)));

        assertThatThrownBy(() -> chatService.deleteChannel(10L, "stu2301"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("팀장만 채널을 관리할 수 있습니다.");
    }

    @Test
    void deleteChannelRejectsDefaultChannel() {
        ChatChannel defaultChannel = ChatChannel.builder()
                .chatRoom(room)
                .channelName("공통")
                .createdBy(user)
                .build();
        ReflectionTestUtils.setField(defaultChannel, "id", 11L);
        when(chatAccessService.getAccessibleChannel(11L, "stu2301")).thenReturn(defaultChannel);
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser(LeaderRole.LEADER)));

        assertThatThrownBy(() -> chatService.deleteChannel(11L, "stu2301"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("공통 채널은 삭제할 수 없습니다.");
    }

    private TeamUser teamUser(LeaderRole leaderRole) {
        return TeamUser.builder()
                .team(team)
                .user(user)
                .studentRole(StudentRole.BACKEND)
                .leaderRole(leaderRole)
                .build();
    }
}
