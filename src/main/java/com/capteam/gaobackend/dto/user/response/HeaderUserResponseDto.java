package com.capteam.gaobackend.dto.user.response;


import com.capteam.gaobackend.enums.AccountRole;
import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class HeaderUserResponseDto {
    // 헤더에 표시하거나 인증 사용자 식별에 사용할 userId를 내려주는 필드입니다.
    private String userId;

    // 헤더에 표시할 사용자 이름을 내려주는 필드입니다.
    private String name;

    // 관리자/학생 화면 분기에 사용할 계정 권한을 내려주는 필드입니다.
    private AccountRole accountRole;

    // 설문 완료 여부에 따라 사용자 플로우를 분기하기 위한 필드입니다.
    private boolean surveyCompleted;
}
