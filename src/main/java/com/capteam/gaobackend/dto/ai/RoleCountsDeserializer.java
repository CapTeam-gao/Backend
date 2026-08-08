package com.capteam.gaobackend.dto.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RoleCountsDeserializer extends JsonDeserializer<List<AiTeamSummaryResponseDto.RoleCountDto>> {

    @Override
    public List<AiTeamSummaryResponseDto.RoleCountDto> deserialize(
            JsonParser parser,
            DeserializationContext context
    ) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        List<AiTeamSummaryResponseDto.RoleCountDto> roleCounts = new ArrayList<>();

        if (node == null || node.isNull()) {
            return roleCounts;
        }

        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item == null || !item.isObject()) {
                    continue;
                }
                String roleGroup = textValue(item.get("role_group"), item.get("roleGroup"));
                int count = intValue(item.get("count"));
                if (roleGroup != null && !roleGroup.isBlank()) {
                    roleCounts.add(new AiTeamSummaryResponseDto.RoleCountDto(roleGroup, count));
                }
            }
            return roleCounts;
        }

        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String roleGroup = entry.getKey();
                if (roleGroup != null && !roleGroup.isBlank()) {
                    roleCounts.add(new AiTeamSummaryResponseDto.RoleCountDto(roleGroup, intValue(entry.getValue())));
                }
            });
        }

        return roleCounts;
    }

    private static String textValue(JsonNode first, JsonNode second) {
        JsonNode node = first != null && !first.isNull() ? first : second;
        return node == null || node.isNull() ? null : node.asText();
    }

    private static int intValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        return node.asInt(0);
    }
}
