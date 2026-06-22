package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.notice.NoticeCreatedEventDto;
import com.capteam.gaobackend.entity.Notice;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Important;
import com.capteam.gaobackend.repository.NoticeReadRepository;
import com.capteam.gaobackend.repository.NoticeRepository;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNoticeServiceTest {

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticeReadRepository noticeReadRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private AdminNoticeService adminNoticeService;

    @BeforeEach
    void setUp() {
        adminNoticeService = new AdminNoticeService(
                noticeRepository,
                noticeReadRepository,
                userRepository,
                messagingTemplate
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsTeamAssignmentNoticeAsAuthenticatedAdminAndPublishesExistingEvent() {
        User admin = User.builder()
                .userId("admin")
                .name("관리자")
                .accountRole(AccountRole.ADMIN)
                .build();
        when(userRepository.findById("admin")).thenReturn(Optional.of(admin));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminNoticeService.createTeamAssignmentNotice("팀 배정 결과 본문");

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getTitle()).isEqualTo("캡스톤 팀 배정 결과 안내");
        assertThat(noticeCaptor.getValue().getContent()).isEqualTo("팀 배정 결과 본문");
        assertThat(noticeCaptor.getValue().getWriter()).isSameAs(admin);
        assertThat(noticeCaptor.getValue().getImportant()).isEqualTo(Important.COMMON);
        verify(messagingTemplate).convertAndSend(eq("/sub/notices"), any(NoticeCreatedEventDto.class));
    }
}
