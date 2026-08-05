package com.capteam.gaobackend.dto.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiTeamSummaryResponseDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesRoleCountsFromObjectResponse() throws JsonProcessingException {
        String json = """
                {
                  "total_students": 2,
                  "total_teams": 1,
                  "teams": [
                    {
                      "team_name": "1팀",
                      "total_people": 2,
                      "role_counts": {
                        "backend": 1,
                        "frontend": 1
                      },
                      "members": []
                    }
                  ]
                }
                """;

        AiTeamSummaryResponseDto response = objectMapper.readValue(json, AiTeamSummaryResponseDto.class);

        assertThat(response.getTeams().get(0).getRoleCounts())
                .extracting(AiTeamSummaryResponseDto.RoleCountDto::getRoleGroup)
                .containsExactly("backend", "frontend");
        assertThat(response.getTeams().get(0).getRoleCounts())
                .extracting(AiTeamSummaryResponseDto.RoleCountDto::getCount)
                .containsExactly(1, 1);
    }

    @Test
    void deserializesRoleCountsFromListResponse() throws JsonProcessingException {
        String json = """
                {
                  "total_students": 2,
                  "total_teams": 1,
                  "teams": [
                    {
                      "team_name": "1팀",
                      "total_people": 2,
                      "role_counts": [
                        { "role_group": "backend", "count": 1 },
                        { "role_group": "frontend", "count": 1 }
                      ],
                      "members": []
                    }
                  ]
                }
                """;

        AiTeamSummaryResponseDto response = objectMapper.readValue(json, AiTeamSummaryResponseDto.class);

        assertThat(response.getTeams().get(0).getRoleCounts())
                .extracting(AiTeamSummaryResponseDto.RoleCountDto::getRoleGroup)
                .containsExactly("backend", "frontend");
        assertThat(response.getTeams().get(0).getRoleCounts())
                .extracting(AiTeamSummaryResponseDto.RoleCountDto::getCount)
                .containsExactly(1, 1);
    }
}
