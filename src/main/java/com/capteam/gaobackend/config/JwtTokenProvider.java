package com.capteam.gaobackend.config;


import com.capteam.gaobackend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String secretKey;   // 시크릿키 가져오기

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration; //엑세스 토큰 기간 가져오기    처음 로그인할 때 사용

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;    //리프레시 토큰 기간 가져오기   엑세스 끝나면 사용

    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));  //서명키 생성
    }


    public String createAccessToken(User user) {   //엑세스 토큰 생성
        return createToken(user,accessTokenExpiration);
    }

    public String createRefreshToken(User user) {  // 리프레시 토큰 생성
        var token = createToken(user,refreshTokenExpiration);
        refreshTokenStore.put(user.getUserId().toString(),token);   //토큰에다 유저 아이디 넣기
        return token;
    }

    // refresh token 쿠키의 Max-Age를 JWT 만료 시간과 동일하게 설정할 때 사용합니다.
    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpiration / 1000;
    }


    private String createToken(User user,long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId()); //페이로드 안에 정보 넣기 유저랑 역할
        claims.put("role",user.getAccountRole().name());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUserId())
                .setIssuedAt(new Date()) //발생시간
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(),Jwts.SIG.HS256)
                .compact();
    }


    public String extractUserIdClaim(String token) {
        return extractClaim(token, claims -> {
            Long id = claims.get("userId", Long.class);
            return id != null ? id.toString() : null;
        });
    }


    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaim(token);
        return claimsResolver.apply(claims);
    }


    public Claims extractAllClaim(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }


    //토큰이 만료되었는지 확인
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (ExpiredJwtException e) {
            return false;
        }
    }


    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    // 유저아이디(식별자)로 토큰을 꺼내와서 검증하는 메서드
    public boolean validateRefreshToken(String userId, String refreshToken) {
        var stored = refreshTokenStore.get(userId);     //토큰 꺼내오기
        return stored != null && stored.equals(refreshToken) && validateToken(refreshToken);
        //null 확인, 유저 아이디로 가져온 토큰과 서버에 있는 리프레시 토큰이 같은지 보기, 토큰이 만료된 토큰인지
    }

    public String extractRole(String token) {
        return extractClaim(token,claims -> claims.get("role",String.class));
    }

    public Authentication getAuthentication(String token) {
        // WebSocket CONNECT 인증 후 STOMP 세션에 넣을 Spring Security Authentication 객체를 만듭니다.
        // principal name은 userId가 되며, 이후 @MessageMapping 메서드의 Principal#getName()으로 꺼낼 수 있습니다.
        String userId = extractUserId(token);
        String role = extractRole(token);

        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(userId, "", authorities);

        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public void invalidateRefreshToken(String userId) {
        refreshTokenStore.remove(userId);
    }



}
