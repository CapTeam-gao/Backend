package com.capteam.gaobackend.service;

import com.capteam.gaobackend.dto.journal.JournalCreateRequestDto;
import com.capteam.gaobackend.dto.journal.JournalDetailResponseDto;
import com.capteam.gaobackend.dto.journal.JournalListItemResponseDto;
import com.capteam.gaobackend.dto.journal.JournalResponseDto;
import com.capteam.gaobackend.dto.journal.JournalTodayResponseDto;
import com.capteam.gaobackend.dto.journal.JournalUpdateRequestDto;
import com.capteam.gaobackend.entity.Journal;
import com.capteam.gaobackend.entity.JournalEntry;
import com.capteam.gaobackend.entity.Team;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.TeamProject;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.JournalStatus;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.TeamStatus;
import com.capteam.gaobackend.repository.JournalEntryRepository;
import com.capteam.gaobackend.repository.JournalRepository;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.TeamProjectRepository;
import com.capteam.gaobackend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceTest {

    @Mock private JournalRepository journalRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private TeamUserRepository teamUserRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamProjectRepository teamProjectRepository;

    private JournalService journalService;

    @BeforeEach
    void setUp() {
        journalService = new JournalService(
                journalRepository,
                journalEntryRepository,
                teamUserRepository,
                userRepository,
                teamProjectRepository
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("stu2301", null)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTodayJournalReturnsMySubmissionState() {
        User user = user("stu2301", "홍길동");
        Team team = team(1L, "1팀");
        TeamUser teamUser = teamUser(team, user);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        Journal journal = journal(1L, team, today);
        JournalEntry entry = JournalEntry.builder()
                .journal(journal)
                .writer(user)
                .todayActivityContent("팀 전체 진행")
                .activityContent("API 개발")
                .nextPlanContent("테스트 작성")
                .reflectionContent("진행이 원활했습니다.")
                .build();

        when(userRepository.findById("stu2301")).thenReturn(Optional.of(user));
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));
        when(journalRepository.findByTeamIdAndDate(1L, today)).thenReturn(Optional.of(journal));
        when(journalEntryRepository.findByJournalId(1L)).thenReturn(List.of(entry));
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(teamUser));
        when(journalEntryRepository.countByJournalId(1L)).thenReturn(1L);

        JournalTodayResponseDto response = journalService.getTodayJournal();

        assertThat(response.getJournalId()).isEqualTo(1L);
        assertThat(response.getTeamName()).isEqualTo("1팀");
        assertThat(response.getDate()).isEqualTo(today);
        assertThat(response.getStatus()).isEqualTo(JournalStatus.IN_PROGRESS);
        assertThat(response.isSubmitted()).isTrue();
        assertThat(response.isEditable()).isTrue();
        assertThat(response.isAllSubmitted()).isTrue();
        assertThat(response.getTodayActivityContent()).isEqualTo("팀 전체 진행");
        assertThat(response.getMyEntry().getActivityContent()).isEqualTo("API 개발");
        assertThat(response.getMyEntry().getNextPlanContent()).isEqualTo("테스트 작성");
        assertThat(response.getMyEntry().getReflectionContent()).isEqualTo("진행이 원활했습니다.");
    }

    @Test
    void updateMyJournalEntryRejectsCompletedJournal() {
        User user = user("stu2301", "홍길동");
        Team team = team(1L, "1팀");
        TeamUser teamUser = teamUser(team, user);
        Journal journal = journal(1L, team, LocalDate.now(ZoneId.of("Asia/Seoul")));
        journal.complete();

        when(userRepository.findById("stu2301")).thenReturn(Optional.of(user));
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));
        when(journalRepository.findById(1L)).thenReturn(Optional.of(journal));

        assertThatThrownBy(() -> journalService.updateMyJournalEntry(1L, new JournalUpdateRequestDto()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("완료된 일지는 수정할 수 없습니다.");
        verify(journalEntryRepository, never()).findByJournalIdAndWriterUserId(1L, "stu2301");
    }

    @Test
    void memberCanCreateJournalWithoutTodayActivityContent() {
        User user = user("stu2301", "홍길동");
        Team team = team(1L, "1팀");
        TeamUser teamUser = teamUser(team, user, LeaderRole.MEMBER);
        Journal journal = journal(1L, team, LocalDate.now(ZoneId.of("Asia/Seoul")));
        JournalCreateRequestDto request = createRequest(null);

        when(userRepository.findById("stu2301")).thenReturn(Optional.of(user));
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));
        when(journalRepository.findByTeamIdAndDate(1L, journal.getDate())).thenReturn(Optional.of(journal));
        when(journalEntryRepository.findByJournalIdAndWriterUserId(1L, "stu2301")).thenReturn(Optional.empty());
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(teamUser));
        when(journalEntryRepository.findByJournalId(1L)).thenReturn(List.of());

        journalService.createMyJournalEntry(request);

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getTodayActivityContent()).isEmpty();
    }

    @Test
    void memberCanUpdateJournalWithoutTodayActivityContent() {
        User user = user("stu2301", "홍길동");
        Team team = team(1L, "1팀");
        TeamUser teamUser = teamUser(team, user, LeaderRole.MEMBER);
        Journal journal = journal(1L, team, LocalDate.now(ZoneId.of("Asia/Seoul")));
        JournalEntry entry = JournalEntry.builder()
                .journal(journal)
                .writer(user)
                .todayActivityContent("")
                .activityContent("기존 작업")
                .nextPlanContent("기존 계획")
                .reflectionContent("기존 회고")
                .build();
        JournalUpdateRequestDto request = updateRequest(null);

        when(userRepository.findById("stu2301")).thenReturn(Optional.of(user));
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));
        when(journalRepository.findById(1L)).thenReturn(Optional.of(journal));
        when(journalEntryRepository.findByJournalIdAndWriterUserId(1L, "stu2301")).thenReturn(Optional.of(entry));
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(teamUser));
        when(journalEntryRepository.findByJournalId(1L)).thenReturn(List.of(entry));

        journalService.updateMyJournalEntry(1L, request);

        assertThat(entry.getTodayActivityContent()).isEmpty();
        assertThat(entry.getActivityContent()).isEqualTo("API 수정");
    }

    @Test
    void leaderMustProvideTodayActivityContent() {
        User user = user("stu2301", "홍길동");
        Team team = team(1L, "1팀");
        TeamUser teamUser = teamUser(team, user, LeaderRole.LEADER);

        when(userRepository.findById("stu2301")).thenReturn(Optional.of(user));
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));

        assertThatThrownBy(() -> journalService.createMyJournalEntry(createRequest(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("팀장은 팀 전체 진행 상황을 작성해야 합니다.");
    }

    @Test
    void journalResponsesIncludeProjectTeamName() {
        User user = user("stu2301", "홍길동");
        Team team = team(1L, "1팀");
        TeamUser teamUser = teamUser(team, user, LeaderRole.MEMBER);
        Journal journal = journal(1L, team, LocalDate.now(ZoneId.of("Asia/Seoul")));
        TeamProject teamProject = TeamProject.builder()
                .team(team)
                .teamName("가오팀")
                .serviceName("CapTeam")
                .serviceIntro("소개")
                .mainFeatures("기능")
                .build();

        when(userRepository.findById("stu2301")).thenReturn(Optional.of(user));
        when(teamUserRepository.findByUserUserId("stu2301")).thenReturn(Optional.of(teamUser));
        when(teamProjectRepository.findByTeamId(1L)).thenReturn(Optional.of(teamProject));
        when(journalRepository.findByTeamIdOrderByDateDesc(1L)).thenReturn(List.of(journal));
        when(journalRepository.findById(1L)).thenReturn(Optional.of(journal));
        when(journalEntryRepository.findByJournalId(1L)).thenReturn(List.of());
        when(teamUserRepository.findByTeamId(1L)).thenReturn(List.of(teamUser));

        JournalResponseDto listResponse = journalService.getMyTeamJournalList().get(0);
        JournalDetailResponseDto detailResponse = journalService.getMyTeamJournalDetail(1L);
        JournalListItemResponseDto adminListResponse = JournalListItemResponseDto.from(journal, teamProject, 0, 1);

        assertThat(listResponse.getProjectTeamName()).isEqualTo("가오팀");
        assertThat(detailResponse.getProjectTeamName()).isEqualTo("가오팀");
        assertThat(adminListResponse.getProjectTeamName()).isEqualTo("가오팀");
    }

    private User user(String userId, String name) {
        User user = User.builder()
                .userId(userId)
                .name(name)
                .accountRole(AccountRole.STUDENT)
                .build();
        ReflectionTestUtils.setField(user, "grade", Grade.GRADE_2);
        return user;
    }

    private Team team(Long id, String teamName) {
        Team team = Team.builder()
                .teamName(teamName)
                .status(TeamStatus.APPROVED)
                .grade(Grade.GRADE_2)
                .build();
        ReflectionTestUtils.setField(team, "id", id);
        return team;
    }

    private TeamUser teamUser(Team team, User user) {
        return teamUser(team, user, LeaderRole.MEMBER);
    }

    private TeamUser teamUser(Team team, User user, LeaderRole leaderRole) {
        return TeamUser.builder()
                .team(team)
                .user(user)
                .studentRole(StudentRole.BACKEND)
                .leaderRole(leaderRole)
                .build();
    }

    private JournalCreateRequestDto createRequest(String todayActivityContent) {
        JournalCreateRequestDto request = new JournalCreateRequestDto();
        ReflectionTestUtils.setField(request, "todayActivityContent", todayActivityContent);
        ReflectionTestUtils.setField(request, "activityContent", "API 개발");
        ReflectionTestUtils.setField(request, "nextPlanContent", "테스트 작성");
        ReflectionTestUtils.setField(request, "reflectionContent", "원활했습니다.");
        return request;
    }

    private JournalUpdateRequestDto updateRequest(String todayActivityContent) {
        JournalUpdateRequestDto request = new JournalUpdateRequestDto();
        ReflectionTestUtils.setField(request, "todayActivityContent", todayActivityContent);
        ReflectionTestUtils.setField(request, "activityContent", "API 수정");
        ReflectionTestUtils.setField(request, "nextPlanContent", "통합 테스트");
        ReflectionTestUtils.setField(request, "reflectionContent", "수정 완료");
        return request;
    }

    private Journal journal(Long id, Team team, LocalDate date) {
        Journal journal = Journal.builder()
                .team(team)
                .title(date + " 캡스톤 일지")
                .date(date)
                .build();
        ReflectionTestUtils.setField(journal, "id", id);
        return journal;
    }
}
