package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.notice.*;
import com.capteam.gaobackend.entity.*;
import com.capteam.gaobackend.enums.Important;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.enums.LeaderRole;
import com.capteam.gaobackend.enums.NoticeType;
import com.capteam.gaobackend.enums.StudentRole;
import com.capteam.gaobackend.enums.TeamStatus;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용, 데이터 변경 메서드는 @Transactional 따로 붙임
public class AdminNoticeService {

    // 새 공지 생성 이벤트를 실시간으로 구독자에게 발행할 WebSocket 대상 경로입니다.
    private static final String NOTICE_CREATED_DESTINATION = "/sub/notices";

    // 공지 목록/상세/생성/수정/삭제에 사용하는 Repository 필드입니다.
    private final NoticeRepository noticeRepository;

    // 공지 삭제 시 읽음 기록과의 관계를 관리하기 위해 주입된 Repository 필드입니다.
    private final NoticeReadRepository noticeReadRepository;

    // 공지 작성자로 현재 로그인한 관리자를 조회하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 팀 배정 결과 공지 상세 응답을 구성하기 위한 팀 Repository 필드입니다.
    private final TeamRepository teamRepository;

    // 팀 배정 결과 공지 상세 응답을 구성하기 위한 팀원 Repository 필드입니다.
    private final TeamUserRepository teamUserRepository;

    // 공지 생성 완료 후 사용자 대시보드에 실시간 알림 이벤트를 보내는 WebSocket 발행 필드입니다.
    private final SimpMessagingTemplate messagingTemplate;

    // ──────────────────────────────────────────
    // 공지 목록 조회 (최신순)
    // ──────────────────────────────────────────
    public List<NoticeResponseDto> getNoticeList() {
        return noticeRepository.findAll().stream()
                .map(NoticeResponseDto::from) // 각 Notice를 DTO로 변환
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────
    // 공지 상세 조회
    // ──────────────────────────────────────────
    public NoticeDetailResponseDto getNoticeDetail(Long noticeId) {
        // findById는 Optional을 반환하므로 없으면 예외 던짐
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));
        return NoticeDetailResponseDto.from(notice, buildTeamResultIfNeeded(notice));
    }

    // ──────────────────────────────────────────
    // 공지를 저장하고 새 공지 WebSocket 이벤트를 발행하는 기능입니다.
    // ──────────────────────────────────────────
    @Transactional // 데이터를 저장하므로 쓰기 트랜잭션 필요
    public NoticeDetailResponseDto createNotice(NoticeCreateRequestDto dto) {
        User author = getAuthenticatedUser(); // 현재 로그인한 어드민 가져오기

        return saveNotice(dto.getTitle(), dto.getContent(), dto.getImportant(), author);
    }

    // 학년별 팀 최종 승인 공지를 중요 공지로 생성하거나 기존 공지를 최신 내용으로 갱신합니다.
    @Transactional
    public NoticeDetailResponseDto createTeamAssignmentNotice(Grade grade, String content) {
        User author = getAuthenticatedUser();
        String title = teamAssignmentNoticeTitle(grade);
        Notice existingNotice = noticeRepository.findByTitle(title).orElse(null);

        if (existingNotice == null) {
            return saveTeamResultNotice(title, content, author, grade);
        }

        existingNotice.updateTeamResult(title, content, Important.IMPORTANT, grade);
        noticeReadRepository.deleteByNoticeId(existingNotice.getId());

        NoticeDetailResponseDto response = NoticeDetailResponseDto.from(
                existingNotice,
                buildTeamResult(grade)
        );
        publishNoticeCreatedEventAfterCommit(response);
        return response;
    }

    private String teamAssignmentNoticeTitle(Grade grade) {
        return switch (grade) {
            case GRADE_2 -> "캡스톤 2학년 팀 배정 결과 안내";
            case GRADE_3 -> "캡스톤 3학년 팀 배정 결과 안내";
        };
    }

    private NoticeDetailResponseDto saveNotice(String title, String content, Important important, User author) {
        // 공지 엔티티 생성 (Builder 패턴)
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .writer(author)
                .important(important)
                .build();

        noticeRepository.save(notice); // DB에 저장

        NoticeDetailResponseDto response = NoticeDetailResponseDto.from(notice);
        publishNoticeCreatedEventAfterCommit(response);
        return response;
    }

    private NoticeDetailResponseDto saveTeamResultNotice(String title, String content, User author, Grade grade) {
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .writer(author)
                .important(Important.IMPORTANT)
                .noticeType(NoticeType.TEAM_RESULT)
                .resultGrade(grade)
                .build();

        noticeRepository.save(notice);

        NoticeDetailResponseDto response = NoticeDetailResponseDto.from(notice, buildTeamResult(grade));
        publishNoticeCreatedEventAfterCommit(response);
        return response;
    }

    // ──────────────────────────────────────────
    // 공지 수정
    // ──────────────────────────────────────────
    @Transactional
    public NoticeDetailResponseDto updateNotice(Long noticeId, NoticeUpdateRequestDto dto) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));

        // setter 대신 엔티티의 update() 메서드로 수정
        notice.update(dto.getTitle(), dto.getContent(), dto.getImportant());

        // @Transactional 덕분에 save() 없이도 변경사항이 자동으로 DB에 반영됨 (더티 체킹)
        return NoticeDetailResponseDto.from(notice, buildTeamResultIfNeeded(notice));
    }

    // ──────────────────────────────────────────
    // 공지 삭제
    // ──────────────────────────────────────────
    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("공지를 찾을 수 없습니다."));

        noticeReadRepository.deleteByNoticeId(noticeId);
        noticeRepository.delete(notice);
    }

    // ──────────────────────────────────────────
    // 공통: 현재 로그인한 유저 가져오기
    // SecurityContext에서 userId를 꺼내서 DB에서 User 조회
    // ──────────────────────────────────────────
    private User getAuthenticatedUser() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));
    }

    private TeamResultResponseDto buildTeamResultIfNeeded(Notice notice) {
        NoticeType noticeType = notice.getNoticeType();
        Grade grade = notice.getResultGrade();

        if (noticeType == NoticeType.TEAM_RESULT && grade != null) {
            return buildTeamResult(grade);
        }

        Grade inferredGrade = inferTeamResultGrade(notice);
        if (inferredGrade == null) {
            return null;
        }

        return buildTeamResult(inferredGrade);
    }

    private Grade inferTeamResultGrade(Notice notice) {
        return switch (notice.getTitle()) {
            case "캡스톤 2학년 팀 배정 결과 안내" -> Grade.GRADE_2;
            case "캡스톤 3학년 팀 배정 결과 안내" -> Grade.GRADE_3;
            default -> null;
        };
    }

    private TeamResultResponseDto buildTeamResult(Grade grade) {
        List<TeamResultResponseDto.TeamResultTeamDto> teams = teamRepository.findByGrade(grade).stream()
                .filter(team -> team.getStatus() == TeamStatus.APPROVED)
                .sorted(Comparator.comparing(Team::getId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toTeamResultTeam)
                .toList();

        return TeamResultResponseDto.builder()
                .grade(grade)
                .teams(teams)
                .build();
    }

    private TeamResultResponseDto.TeamResultTeamDto toTeamResultTeam(Team team) {
        List<TeamUser> members = leaderFirst(teamUserRepository.findByTeamId(team.getId()));
        TeamUser leader = members.stream()
                .filter(member -> member.getLeaderRole() == LeaderRole.LEADER)
                .findFirst()
                .orElse(null);

        return TeamResultResponseDto.TeamResultTeamDto.builder()
                .teamId(team.getId())
                .teamName(team.getTeamName())
                .leaderName(leader == null ? null : leader.getUser().getName())
                .roleSummary(formatRoleSummary(members))
                .members(members.stream()
                        .map(this::toTeamResultMember)
                        .toList())
                .build();
    }

    private TeamResultResponseDto.TeamResultMemberDto toTeamResultMember(TeamUser member) {
        boolean leader = member.getLeaderRole() == LeaderRole.LEADER;

        return TeamResultResponseDto.TeamResultMemberDto.builder()
                .userId(member.getUser().getUserId())
                .name(member.getUser().getName())
                .studentRole(member.getStudentRole())
                .leader(leader)
                .build();
    }

    private List<TeamUser> leaderFirst(List<TeamUser> members) {
        return members.stream()
                .sorted(Comparator.comparing(member -> member.getLeaderRole() == LeaderRole.LEADER ? 0 : 1))
                .toList();
    }

    private String formatRoleSummary(List<TeamUser> members) {
        Map<StudentRole, Long> roleCounts = members.stream()
                .filter(member -> member.getStudentRole() != null)
                .collect(Collectors.groupingBy(
                        TeamUser::getStudentRole,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        return roleCounts.entrySet().stream()
                .map(entry -> toRoleLabel(entry.getKey()) + " " + entry.getValue() + "명")
                .collect(Collectors.joining(" / "));
    }

    private String toRoleLabel(StudentRole role) {
        return switch (role) {
            case BACKEND -> "백엔드";
            case FRONTEND -> "프론트엔드";
            case AI -> "AI";
            case APP -> "앱";
            case DESIGN -> "디자인";
            case DEVOPS -> "DevOps";
            case GAME -> "게임";
            case FULLSTACK -> "풀스택";
            case SECURITY -> "보안";
        };
    }

    // 공지 저장 트랜잭션이 정상 커밋된 뒤 /sub/notices 구독자에게 새 공지 이벤트를 발행하는 기능입니다.
    private void publishNoticeCreatedEventAfterCommit(NoticeDetailResponseDto response) {
        NoticeCreatedEventDto event = NoticeCreatedEventDto.from(response);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            sendNoticeCreatedEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendNoticeCreatedEvent(event);
            }
        });
    }

    // 새 공지 생성 사실을 /sub/notices 구독자에게 보내 사용자 대시보드 N 표시를 즉시 갱신하는 기능입니다.
    private void sendNoticeCreatedEvent(NoticeCreatedEventDto event) {
        messagingTemplate.convertAndSend(
                NOTICE_CREATED_DESTINATION,
                event
        );
    }
}
