package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.enums.Grade;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminTeamMatchingPreparationService {

    private final TeamUserRepository teamUserRepository;
    private final UserRepository userRepository;

    // AI 호출 전에 대상 학생 검증과 전송 DTO 생성을 짧은 읽기 트랜잭션 안에서 끝냅니다.
    @Transactional(readOnly = true)
    public PreparedMatching prepare(Grade grade) {
        Set<String> assignedUserIds = teamUserRepository.findAll().stream()
                .map(teamUser -> teamUser.getUser().getUserId())
                .collect(Collectors.toSet());

        List<User> gradeStudents = userRepository.findByAccountRoleAndGrade(AccountRole.STUDENT, grade)
                .stream()
                .filter(user -> !assignedUserIds.contains(user.getUserId()))
                .toList();

        validateAllStudentsSurveyCompleted(gradeStudents);
        if (gradeStudents.isEmpty()) {
            throw new IllegalStateException("배정할 미배정 학생이 없습니다.");
        }

        Map<String, String> nameToUserId = gradeStudents.stream()
                .collect(Collectors.toMap(User::getName, User::getUserId, (first, second) -> first));
        // 지연 로딩 필드를 포함한 AI payload를 트랜잭션 안에서 완성합니다.
        List<AiStudentPayloadDto> studentPayloads = gradeStudents.stream()
                .map(AiStudentPayloadDto::from)
                .toList();
        return new PreparedMatching(nameToUserId, studentPayloads);
    }

    private void validateAllStudentsSurveyCompleted(List<User> students) {
        List<User> notCompletedStudents = students.stream()
                .filter(user -> !user.isSurveyCompleted())
                .toList();

        if (!notCompletedStudents.isEmpty()) {
            String names = notCompletedStudents.stream()
                    .map(user -> user.getName() + "(" + user.getUserId() + ")")
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("설문 미완료 학생이 있어 팀을 생성할 수 없습니다: " + names);
        }
    }

    public record PreparedMatching(
            Map<String, String> nameToUserId,
            List<AiStudentPayloadDto> studentPayloads
    ) {
    }
}
