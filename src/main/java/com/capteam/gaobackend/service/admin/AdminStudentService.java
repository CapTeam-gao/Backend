package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.admin.AdminStudentDetailResponseDto;
import com.capteam.gaobackend.dto.admin.AdminStudentListResponseDto;
import com.capteam.gaobackend.entity.TeamUser;
import com.capteam.gaobackend.entity.UserAnalysis;
import com.capteam.gaobackend.exception.UserNotFoundException;
import com.capteam.gaobackend.repository.TeamUserRepository;
import com.capteam.gaobackend.repository.UserAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStudentService {

    private final TeamUserRepository teamUserRepository;
    private final UserAnalysisRepository userAnalysisRepository;

    //전체 학생 조회(어드민)
    public List<AdminStudentListResponseDto> getAllStudents() {

        List<TeamUser> teamUsers = teamUserRepository.findAll();
        Map<String, UserAnalysis> userAnalysisMap = userAnalysisRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        UserAnalysis::getUserId,
                        userAnalysis -> userAnalysis
                ));

        return teamUsers.stream()
                .map(teamUser -> AdminStudentListResponseDto.from(
                        teamUser,
                        userAnalysisMap.get(teamUser.getUser().getUserId())
                ))
                .toList();

    }


    //학생 상세 조회 어드민_
    public AdminStudentDetailResponseDto getStudentDetail(String userId) {
        TeamUser teamUser = teamUserRepository.findByUserUserId(userId)
                .orElseThrow(UserNotFoundException::new);

        UserAnalysis userAnalysis = userAnalysisRepository.findByUserUserId(userId)
                .orElse(null);

        return AdminStudentDetailResponseDto.from(teamUser, userAnalysis);
    }
}
