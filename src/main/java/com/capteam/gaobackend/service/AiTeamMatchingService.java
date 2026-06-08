package com.capteam.gaobackend.service;

import com.capteam.gaobackend.ai.AiClient;
import com.capteam.gaobackend.dto.ai.AiStudentPayloadDto;
import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;
import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.enums.AccountRole;
import com.capteam.gaobackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiTeamMatchingService {

    // 외부 AI 서버에서 팀 요약 및 매칭을 요청할 때 사용하는 Client 필드입니다.
    private final AiClient aiClient;

    // 백엔드 학생 데이터를 AI 서버 payload로 변환하기 위해 사용하는 Repository 필드입니다.
    private final UserRepository userRepository;

    // 외부 AI 서버에서 현재 팀 요약을 조회하는 기능입니다.
    public AiTeamSummaryResponseDto getTeamSummary() {
        return aiClient.getTeamSummary();
    }

    // 외부 AI 서버에 학생 분석 실행을 요청하는 기능입니다. (payload 없이 AI DB 사용)
    public void runAnalysis() {
        aiClient.runAnalysis(buildStudentPayloads());
    }

    // 외부 AI 서버에 학생 분석을 먼저 실행한 뒤 최신 분석 결과로 팀 매칭을 요청하는 기능입니다.
    public AiTeamSummaryResponseDto runMatching() {
        List<AiStudentPayloadDto> students = buildStudentPayloads();
        aiClient.runAnalysis(students);
        return aiClient.runMatching(students);
    }

    private List<AiStudentPayloadDto> buildStudentPayloads() {
        return userRepository.findAll().stream()
                .filter(user -> user.getAccountRole() == AccountRole.STUDENT)
                .filter(User::isSurveyCompleted)
                .map(AiStudentPayloadDto::from)
                .toList();
    }
}
