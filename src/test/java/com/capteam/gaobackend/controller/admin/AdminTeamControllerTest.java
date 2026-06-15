package com.capteam.gaobackend.controller.admin;

import com.capteam.gaobackend.dto.admin.AdminTeamDetailResponseDto;
import com.capteam.gaobackend.dto.common.ApiResponse;
import com.capteam.gaobackend.service.TeamService;
import com.capteam.gaobackend.service.admin.AdminTeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminTeamControllerTest {

    @Mock private AdminTeamService adminTeamService;
    @Mock private TeamService teamService;

    @InjectMocks
    private AdminTeamController adminTeamController;

    @Test
    void wrapsTeamDetailWithApiResponse() {
        Long teamId = 1L;
        AdminTeamDetailResponseDto detail = mock(AdminTeamDetailResponseDto.class);
        when(adminTeamService.getTeamDetail(teamId)).thenReturn(detail);

        ResponseEntity<ApiResponse<AdminTeamDetailResponseDto>> response =
                adminTeamController.getTeamDetail(teamId);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getMessage()).isEqualTo("success");
        assertThat(response.getBody().getData()).isSameAs(detail);
    }
}
