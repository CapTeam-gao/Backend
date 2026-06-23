package com.capteam.gaobackend.service.admin;

import com.capteam.gaobackend.dto.ai.AiTeamSummaryResponseDto;

import java.util.Locale;
import java.util.Map;

final class AiTeamMemberUserResolver {

    private AiTeamMemberUserResolver() {
    }

    static String resolveUserId(
            AiTeamSummaryResponseDto.MemberDto member,
            Map<String, String> nameToUserId
    ) {
        if (member == null) {
            return null;
        }

        String userId = normalizeIdentifier(member.getUserId());
        if (userId != null && nameToUserId.containsValue(userId)) {
            return userId;
        }

        return resolveUserIdByName(member.getName(), nameToUserId);
    }

    static String resolveUserIdByName(String name, Map<String, String> nameToUserId) {
        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : nameToUserId.entrySet()) {
            if (normalizedName.equals(normalizeName(entry.getKey()))) {
                return entry.getValue();
            }
        }
        return null;
    }

    static boolean isSameStudent(
            AiTeamSummaryResponseDto.MemberDto member,
            String studentName,
            Map<String, String> nameToUserId
    ) {
        String memberUserId = resolveUserId(member, nameToUserId);
        String studentUserId = resolveUserIdByName(studentName, nameToUserId);
        if (memberUserId != null && studentUserId != null) {
            return memberUserId.equals(studentUserId);
        }

        String memberName = normalizeName(member != null ? member.getName() : null);
        String normalizedStudentName = normalizeName(studentName);
        return memberName != null && memberName.equals(normalizedStudentName);
    }

    private static String normalizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
