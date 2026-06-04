package com.capteam.gaobackend.util;

import com.capteam.gaobackend.entity.User;
import com.capteam.gaobackend.entity.UserDevelopmentScore;
import com.capteam.gaobackend.entity.UserPersonalityScore;

import java.util.List;
import java.util.Objects;

public class ScoreCalculator {

    private ScoreCalculator() {}

    // 학생의 종합 점수를 계산하는 기능입니다.
    // 기술 스택, 구현 경험, 팀장 희망, 선호 팀원, 성격 성향, 개발 성향 점수를 합산합니다.
    public static double calculate(User user) {
        List<String> skills = safeList(user.getSkill());
        List<String> experiences = safeList(user.getExperience());

        double score = 10;
        score += Math.min(skills.size(), 6) * 5.0;          // 기술 스택 최대 30점
        score += Math.min(experiences.size(), 5) * 4.0;     // 구현 경험 최대 20점
        score += user.isWantsLeader() ? 4.0 : 0;            // 팀장 희망 +4점
        score += safeList(user.getPreferredTeammates()).size(); // 선호 팀원 수 +1점씩
        score += calcPersonalityScore(user.getPersonalityScores()); // 성격 성향 최대 10점
        score += calcDevelopmentScore(user.getDevelopmentScores()); // 개발 성향 최대 10점

        return score;
    }

    // 성격 성향 5개 항목 평균을 0~10 범위로 환산하는 기능입니다.
    private static double calcPersonalityScore(UserPersonalityScore p) {
        if (p == null) return 0;
        double avg = (safeScore(p.getCommunication())
                + safeScore(p.getResponsibility())
                + safeScore(p.getCollaboration())
                + safeScore(p.getFlexibility())
                + safeScore(p.getEmotionalStability())) / 5.0;
        return avg * 2.0; // 1~5점 평균 → 2~10점으로 환산
    }

    // 개발 성향 5개 항목 평균을 0~10 범위로 환산하는 기능입니다.
    private static double calcDevelopmentScore(UserDevelopmentScore d) {
        if (d == null) return 0;
        double avg = (safeScore(d.getLeadership())
                + safeScore(d.getProblemSolving())
                + safeScore(d.getImplementation())
                + safeScore(d.getLearningAbility())
                + safeScore(d.getPlanning())) / 5.0;
        return avg * 2.0; // 1~5점 평균 → 2~10점으로 환산
    }

    private static double safeScore(Integer score) {
        return score == null ? 0 : score;
    }

    private static List<String> safeList(List<String> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).toList();
    }
}

