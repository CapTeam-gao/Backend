package com.capteam.gaobackend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FrontendApiContractTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void frontend에서_사용하는_REST_API가_백엔드에_등록되어_있다() {
        Map<String, RequestMethod> frontendContracts = new LinkedHashMap<>();
        frontendContracts.put("POST /api/auth/login", RequestMethod.POST);
        frontendContracts.put("POST /api/auth/reissue", RequestMethod.POST);
        frontendContracts.put("POST /api/auth/logout", RequestMethod.POST);
        frontendContracts.put("PUT /api/auth/password", RequestMethod.PUT);
        frontendContracts.put("GET /api/user/header", RequestMethod.GET);
        frontendContracts.put("POST /api/user/fcm-token", RequestMethod.POST);
        frontendContracts.put("DELETE /api/user/fcm-token", RequestMethod.DELETE);
        frontendContracts.put("GET /api/user/survey", RequestMethod.GET);
        frontendContracts.put("POST /api/user/survey", RequestMethod.POST);
        frontendContracts.put("GET /api/admin/dashboard", RequestMethod.GET);
        frontendContracts.put("GET /api/user/dashboard", RequestMethod.GET);
        frontendContracts.put("GET /api/notices", RequestMethod.GET);
        frontendContracts.put("GET /api/notices/{noticeId}", RequestMethod.GET);
        frontendContracts.put("POST /api/admin/notices", RequestMethod.POST);
        frontendContracts.put("PUT /api/admin/notices/{noticeId}", RequestMethod.PUT);
        frontendContracts.put("DELETE /api/admin/notices/{noticeId}", RequestMethod.DELETE);
        frontendContracts.put("GET /api/admin/journals", RequestMethod.GET);
        frontendContracts.put("GET /api/admin/journals/{journalId}", RequestMethod.GET);
        frontendContracts.put("GET /api/journals", RequestMethod.GET);
        frontendContracts.put("POST /api/journals", RequestMethod.POST);
        frontendContracts.put("GET /api/journals/{journalId}", RequestMethod.GET);
        frontendContracts.put("PATCH /api/journals/{journalId}", RequestMethod.PATCH);
        frontendContracts.put("GET /api/admin/students", RequestMethod.GET);
        frontendContracts.put("GET /api/admin/students/{userId}", RequestMethod.GET);
        frontendContracts.put("POST /api/admin/team-recommendations", RequestMethod.POST);
        frontendContracts.put("GET /api/admin/team-recommendations", RequestMethod.GET);
        frontendContracts.put("GET /api/admin/team-recommendations/grade/{grade}", RequestMethod.GET);
        frontendContracts.put("GET /api/admin/team-recommendations/{recommendationId}", RequestMethod.GET);
        frontendContracts.put("POST /api/admin/team-recommendations/swap", RequestMethod.POST);
        frontendContracts.put("POST /api/admin/team-recommendations/{recommendationId}/accept", RequestMethod.POST);
        frontendContracts.put("POST /api/admin/team-recommendations/accept-all/{grade}", RequestMethod.POST);
        frontendContracts.put("GET /api/admin/teams", RequestMethod.GET);
        frontendContracts.put("GET /api/admin/teams/{teamId}", RequestMethod.GET);
        frontendContracts.put("GET /api/teams/my-team", RequestMethod.GET);
        frontendContracts.put("GET /api/teams/project", RequestMethod.GET);
        frontendContracts.put("PUT /api/teams/project", RequestMethod.PUT);
        frontendContracts.put("GET /api/admin/chat/rooms", RequestMethod.GET);
        frontendContracts.put("GET /api/admin/chat/rooms/{roomId}", RequestMethod.GET);
        frontendContracts.put("GET /api/admin/chat/channels/{channelId}/messages", RequestMethod.GET);
        frontendContracts.put("GET /api/admin/chat/unread-summary", RequestMethod.GET);
        frontendContracts.put("GET /api/admin/chat/rooms/{roomId}/channel-summaries", RequestMethod.GET);
        frontendContracts.put("POST /api/admin/chat/channels/{channelId}/read", RequestMethod.POST);
        frontendContracts.put("GET /api/chat/rooms/my", RequestMethod.GET);
        frontendContracts.put("GET /api/chat/channels/{channelId}/messages", RequestMethod.GET);
        frontendContracts.put("GET /api/chat/rooms/my/channel-summaries", RequestMethod.GET);
        frontendContracts.put("POST /api/chat/channels/{channelId}/read", RequestMethod.POST);
        frontendContracts.put("POST /api/chat/rooms/{roomId}/channels", RequestMethod.POST);
        frontendContracts.put("PATCH /api/chat/channels/{channelId}", RequestMethod.PATCH);
        frontendContracts.put("DELETE /api/chat/channels/{channelId}", RequestMethod.DELETE);
        frontendContracts.put("POST /api/chat/channels/{channelId}/files", RequestMethod.POST);
        frontendContracts.put("GET /api/chat/channels/{channelId}/presence", RequestMethod.GET);
        frontendContracts.put("PATCH /api/chat/messages/{messageId}", RequestMethod.PATCH);
        frontendContracts.put("DELETE /api/chat/messages/{messageId}", RequestMethod.DELETE);

        Set<String> registeredMappings = handlerMapping.getHandlerMethods()
                .keySet()
                .stream()
                .flatMap(mappingInfo -> toContracts(mappingInfo).stream())
                .collect(Collectors.toSet());

        assertThat(registeredMappings).containsAll(frontendContracts.keySet());
    }

    private Set<String> toContracts(RequestMappingInfo mappingInfo) {
        Set<String> patterns = mappingInfo.getPathPatternsCondition().getPatternValues();
        Set<RequestMethod> methods = mappingInfo.getMethodsCondition().getMethods();

        return patterns.stream()
                .flatMap(pattern -> methods.stream().map(method -> method.name() + " " + pattern))
                .collect(Collectors.toSet());
    }
}
