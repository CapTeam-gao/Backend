package com.capteam.gaobackend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
public class JwtAuthenticationFilter extends GenericFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret}")
    private String secretKey;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));  //서명키 생성
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse)servletResponse;

        String token = httpServletRequest.getHeader("Authorization");
        try {
            if(token != null) {
                if(!token.startsWith(BEARER_PREFIX)) {
                    throw new AuthenticationServiceException("Bearer 형식이 아닙니다.");
                }

                String jwtToken = token.substring(BEARER_PREFIX.length()).trim();
                if (jwtToken.isBlank()) {
                    throw new AuthenticationServiceException("JWT 토큰이 비어 있습니다.");
                }

                Claims claims = Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseClaimsJws(jwtToken)
                        .getBody();


                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + claims.get("role")));
                UserDetails userDetails = new User(claims.getSubject(),"",authorities);
                Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,"",userDetails.getAuthorities() );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }




            filterChain.doFilter(servletRequest,servletResponse);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.debug("JWT 인증에 실패했습니다: {}", e.getMessage());
            httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpServletResponse.getWriter().write("{\"error\":\"invalid token\"}");
        }

    }
}
