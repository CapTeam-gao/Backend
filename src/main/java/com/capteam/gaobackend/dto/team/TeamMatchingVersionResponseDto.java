package com.capteam.gaobackend.dto.team;

import com.capteam.gaobackend.entity.TeamMatchingVersion;
import com.capteam.gaobackend.enums.TeamMatchingVersionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeamMatchingVersionResponseDto {

    // 버전 상세 조회와 apply/discard 호출에 사용할 버전 식별자입니다.
    private Long versionId;

    // 같은 학년 내에서 몇 번째 생성 결과인지 화면에 순서대로 보여주기 위한 번호입니다.
    private Integer versionNumber;

    // 초안/적용/폐기 상태를 그대로 노출해 화면 분기를 단순하게 만듭니다.
    private TeamMatchingVersionStatus status;

    // 언제 생성된 버전인지 비교 화면에서 확인할 수 있게 내려줍니다.
    private LocalDateTime createdAt;

    // 어떤 재생성 지시문으로 만든 버전인지 검토할 수 있게 함께 내려줍니다.
    private String regenerationPrompt;

    public static TeamMatchingVersionResponseDto from(TeamMatchingVersion version) {
        return TeamMatchingVersionResponseDto.builder()
                .versionId(version.getId())
                .versionNumber(version.getVersionNumber())
                .status(version.getStatus())
                .createdAt(version.getCreatedAt())
                .regenerationPrompt(version.getRegenerationPrompt())
                .build();
    }
}
