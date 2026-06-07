package com.capteam.gaobackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins}")
    private String[] allowedOrigins;

    private final JwtChannelInterceptor jwtChannelInterceptor;

    public WebSocketConfig(JwtChannelInterceptor jwtChannelInterceptor) {
        this.jwtChannelInterceptor = jwtChannelInterceptor;
    }

    @Bean
    public ThreadPoolTaskScheduler webSocketMessageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트로 나가는 메시지를 처리하는 "간단한 내장 브로커"를 켭니다.
        // 클라이언트는 /sub 로 시작하는 주소를 구독하고, 서버는 그 주소로 메시지를 발행합니다.
        // 예) 클라이언트 구독: /sub/chat/1, 서버 발행: /sub/chat/1
        //
        // /queue 는 특정 사용자에게만 보내는 1:1 메시지용 prefix 입니다.
        // 예) /user/queue/errors 를 구독한 사용자에게만 에러 메시지를 보낼 수 있습니다.
        //
        // 지금은 서버 메모리 안에서만 동작하는 SimpleBroker 입니다.
        // 서버를 여러 대로 늘리면 메시지가 서버 간 공유되지 않으므로 RabbitMQ 같은 외부 broker relay로 바꾸는 편이 안전합니다.
        registry.enableSimpleBroker("/sub", "/queue")
                // SimpleBroker에서 heartbeat를 쓰려면 주기적으로 신호를 보낼 scheduler가 필요합니다.
                // 이 scheduler가 없으면 서버 시작 시 simpleBrokerMessageHandler가 실패합니다.
                .setTaskScheduler(webSocketMessageBrokerTaskScheduler())
                // heartbeat는 서로 "아직 연결 살아있음"을 확인하는 신호입니다.
                // [서버가 보내는 주기, 서버가 받기를 기대하는 주기]이며 단위는 ms 입니다.
                .setHeartbeatValue(new long[]{10_000, 10_000});

        // 클라이언트 -> 서버로 보내는 메시지 주소 prefix 입니다.
        // @MessageMapping("/chat/{channelId}/send") 로 받으려면 클라이언트는 /pub/chat/{channelId}/send 로 보냅니다.
        registry.setApplicationDestinationPrefixes("/pub");

        // convertAndSendToUser(...) 를 사용할 때 붙는 사용자 전용 prefix 입니다.
        // 클라이언트는 보통 /user/queue/errors 처럼 구독하면 되고, Spring이 현재 사용자 세션에 맞게 라우팅합니다.
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 브라우저가 처음 WebSocket 또는 SockJS 연결을 여는 HTTP endpoint 입니다.
        // 실제 STOMP 메시지 송수신 주소가 아니라, 연결을 시작하는 입구라고 보면 됩니다.
        registry.addEndpoint("/ws")
                // StompJS가 native WebSocket으로 ws://localhost:8080/ws 에 직접 붙는 경우를 지원합니다.
                .setAllowedOriginPatterns(allowedOrigins);

        registry.addEndpoint("/ws")
                // 프론트엔드 개발 서버 주소를 허용합니다. 운영에서는 "*" 대신 실제 도메인만 열어두는 것이 좋습니다.
                .setAllowedOriginPatterns(allowedOrigins)
                // WebSocket을 못 쓰는 환경에서도 polling 등으로 fallback 할 수 있게 SockJS를 켭니다.
                .withSockJS()
                // SockJS 자체 heartbeat입니다. STOMP heartbeat보다 길게 두어 중복 신호가 과하게 나가지 않게 합니다.
                .setHeartbeatTime(25_000)
                // 연결이 끊긴 뒤 바로 세션을 버리지 않고 잠깐 기다립니다. 짧은 네트워크 흔들림을 흡수하기 위함입니다.
                .setDisconnectDelay(5_000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 클라이언트 -> 서버로 들어오는 모든 STOMP 프레임이 지나가는 통로입니다.
        // CONNECT 프레임에서 JWT를 검사하려고 인터셉터를 여기에 붙입니다.
        registration.interceptors(jwtChannelInterceptor);

        // 메시지를 처리할 worker thread 설정입니다.
        // 동시 접속자나 메시지량이 늘면 core/max/queue를 같이 조정해야 합니다.
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .queueCapacity(100);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 서버 -> 클라이언트로 나가는 메시지 전송 통로입니다.
        // 채팅방 인원이 많으면 같은 메시지를 여러 세션으로 뿌리므로 outbound 부하가 먼저 커질 수 있습니다.
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .queueCapacity(100);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // 너무 큰 메시지나 너무 느린 전송이 서버 메모리를 잡아먹지 않도록 제한합니다.
        registration.setMessageSizeLimit(64 * 1024)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(20_000);
    }
}
