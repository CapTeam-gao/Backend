package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.chat.ChatChannelEventDto;
import com.capteam.gaobackend.dto.chat.ChatChannelRequestDto;
import com.capteam.gaobackend.dto.chat.ChatMessageEventDto;
import com.capteam.gaobackend.dto.chat.ChatMessageUpdateRequestDto;
import com.capteam.gaobackend.entity.ChatChannel;
import com.capteam.gaobackend.entity.ChatMessage;
import com.capteam.gaobackend.entity.ChatRoom;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.repository.ChatChannelRepository;
import com.capteam.gaobackend.repository.ChatMessageRepository;
import com.capteam.gaobackend.repository.ChatReadStatusRepository;
import com.capteam.gaobackend.repository.ChatRoomRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock private SimpMessagingTemplate messagingTemplate;

    private ChatService chatService;
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
                messagingTemplate
        );

        Team team = Team.builder()
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
        assertThat(event.getChannel().getChannelName()).isEqualTo("프론트엔드");
    }

    @Test
    void updateChannelPublishesChannelUpdatedEvent() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);

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
        assertThat(event.getChannel().getChannelName()).isEqualTo("백엔드");
    }

    @Test
    void deleteChannelPublishesChannelDeletedEvent() {
        when(chatAccessService.getAccessibleChannel(10L, "stu2301")).thenReturn(channel);

        chatService.deleteChannel(10L, "stu2301");

        ArgumentCaptor<ChatChannelEventDto> eventCaptor = ArgumentCaptor.forClass(ChatChannelEventDto.class);
        verify(messagingTemplate).convertAndSend(eq("/sub/chat/rooms/100/channels"), eventCaptor.capture());

        ChatChannelEventDto event = eventCaptor.getValue();
        assertThat(event.getType()).isEqualTo("CHANNEL_DELETED");
        assertThat(event.getChannelId()).isEqualTo(10L);
    }
}
