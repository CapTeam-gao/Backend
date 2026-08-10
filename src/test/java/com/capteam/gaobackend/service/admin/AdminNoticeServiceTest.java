package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.notice.NoticeCreatedEventDto;
import com.capteam.gaobackend.dto.notice.NoticeCreateRequestDto;
import com.capteam.gaobackend.entity.NotificationLog;
import com.capteam.gaobackend.entity.Notice;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Important;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.NotificationStatus;
import com.capteam.gaobackend.repository.NoticeReadRepository;
import com.capteam.gaobackend.repository.NoticeRepository;
import com.capteam.gaobackend.repository.TeamRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.NotificationLogRepository;
import com.capteam.gaobackend.repository.UserFcmTokenRepository;
import com.capteam.gaobackend.repository.UserRepository;
import com.capteam.gaobackend.service.push.PushNotificationGateway;
import com.capteam.gaobackend.service.NotificationLogPersistenceService;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminNoticeServiceTest {

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticeReadRepository noticeReadRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private UserFcmTokenRepository userFcmTokenRepository;
    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private NotificationLogPersistenceService notificationLogPersistenceService;
    @Mock private PushNotificationGateway pushNotificationGateway;

    private AdminNoticeService adminNoticeService;

    @BeforeEach
    void setUp() {
        adminNoticeService = new AdminNoticeService(
                noticeRepository,
                noticeReadRepository,
                userRepository,
                teamRepository,
                teamUserRepository,
                messagingTemplate,
                userFcmTokenRepository,
                notificationLogRepository,
                notificationLogPersistenceService,
                pushNotificationGateway
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

        when(noticeRepository.findByTitle("캡스톤 2학년 팀 배정 결과 안내"))
                .thenReturn(Optional.empty());

        adminNoticeService.createTeamAssignmentNotice(Grade.GRADE_2, "팀 배정 결과 본문");

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(noticeCaptor.capture());
        assertThat(noticeCaptor.getValue().getTitle()).isEqualTo("캡스톤 2학년 팀 배정 결과 안내");
        assertThat(noticeCaptor.getValue().getContent()).isEqualTo("팀 배정 결과 본문");
        assertThat(noticeCaptor.getValue().getWriter()).isSameAs(admin);
        assertThat(noticeCaptor.getValue().getImportant()).isEqualTo(Important.IMPORTANT);
        verify(messagingTemplate).convertAndSend(eq("/sub/notices"), any(NoticeCreatedEventDto.class));
    }

    @Test
    void persistsFailedNotificationLogWhenStudentHasNoFcmToken() {
        User admin = User.builder()
                .userId("admin")
                .name("관리자")
                .accountRole(AccountRole.ADMIN)
                .build();
        User student = User.builder()
                .userId("stu2301")
                .name("학생")
                .accountRole(AccountRole.STUDENT)
                .build();
        NoticeCreateRequestDto request = new NoticeCreateRequestDto();
        ReflectionTestUtils.setField(request, "title", "테스트 공지");
        ReflectionTestUtils.setField(request, "content", "테스트 본문");
        ReflectionTestUtils.setField(request, "important", Important.COMMON);

        when(userRepository.findById("admin")).thenReturn(Optional.of(admin));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
            Notice savedNotice = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedNotice, "id", 11L);
            return savedNotice;
        });
        when(userRepository.findByAccountRole(AccountRole.STUDENT)).thenReturn(List.of(student));

        adminNoticeService.createNotice(request);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogPersistenceService).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(logCaptor.getValue().getErrorMessage()).isEqualTo("등록된 FCM 토큰이 없습니다.");
    }

    @Test
    void updatesExistingGradeNoticeInsteadOfCreatingDuplicate() {
        User admin = User.builder()
                .userId("admin")
                .name("관리자")
                .accountRole(AccountRole.ADMIN)
                .build();
        Notice existingNotice = Notice.builder()
                .title("캡스톤 3학년 팀 배정 결과 안내")
                .content("기존 본문")
                .writer(admin)
                .important(Important.COMMON)
                .build();
        ReflectionTestUtils.setField(existingNotice, "id", 7L);

        when(userRepository.findById("admin")).thenReturn(Optional.of(admin));
        when(noticeRepository.findByTitle("캡스톤 3학년 팀 배정 결과 안내"))
                .thenReturn(Optional.of(existingNotice));

        adminNoticeService.createTeamAssignmentNotice(Grade.GRADE_3, "새 본문");

        assertThat(existingNotice.getContent()).isEqualTo("새 본문");
        assertThat(existingNotice.getImportant()).isEqualTo(Important.IMPORTANT);
        verify(noticeReadRepository).deleteByNoticeId(7L);
        verify(noticeRepository, org.mockito.Mockito.never()).save(any(Notice.class));
        verify(messagingTemplate).convertAndSend(eq("/sub/notices"), any(NoticeCreatedEventDto.class));
    }

    @Test
    void deletesNoticeReadRowsBeforeDeletingNotice() {
        User admin = User.builder()
                .userId("admin")
                .name("관리자")
                .accountRole(AccountRole.ADMIN)
                .build();
        Notice notice = Notice.builder()
                .title("공지")
                .content("본문")
                .writer(admin)
                .important(Important.COMMON)
                .build();
        ReflectionTestUtils.setField(notice, "id", 10L);

        when(noticeRepository.findById(10L)).thenReturn(Optional.of(notice));

        adminNoticeService.deleteNotice(10L);

        var inOrder = inOrder(noticeReadRepository, noticeRepository);
        inOrder.verify(noticeReadRepository).deleteByNoticeId(10L);
        inOrder.verify(noticeRepository).delete(notice);
    }
}
