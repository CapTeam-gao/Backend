package com.capteam.gaobackend.config;


import com.capteam.gaobackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthService authService;





    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // CSRF (개발 단계에서는 비활성화 가능)  // 해커가 사용자한테 요청 자꾸 하게 하는거 방어 비활성화
                .csrf(csrf -> csrf.disable())

                // 세션 사용 설정(로그인할 때 생성)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)   //필요할때만(로그인)
                )

                // URL 권한 설정
                //로그인 할때는 세션 필요 없음
                // 나머지요청은 로그인한 사용자만 가능
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .anyRequest().authenticated()
                )

                // 기본 로그인 폼 비활성화 (우리가 직접 API 만들 거니까)
                // 스프링이 주는거 끄는 설정임
                .formLogin(form -> form.disable())

                // 로그아웃 설정

                .logout(logout -> logout
                        .logoutUrl("/logout")   //로그아웃 url
                        .invalidateHttpSession(true)    //로그아웃하면 session 비활성화
                        .deleteCookies("JSESSIONID")    //헤더에서 쿠키 만료 시키는거
                );

        return http.build();
    }
}
