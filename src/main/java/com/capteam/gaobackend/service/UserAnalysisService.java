package com.capteam.gaobackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAnalysisService {

    // 현재 사용자 분석 전용 비즈니스 로직은 AiTeamAutoCreationService에서 처리하고 있어 비워둔 서비스입니다.
}
