package com.capteam.gaobackend.repository;

import com.capteam.gaobackend.entity.UserAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAnalysisRepository extends JpaRepository<UserAnalysis,Long> {
    Optional<UserAnalysis> findByUserUserId(String userId); //유저의 유저 아이디로 찾기
}
