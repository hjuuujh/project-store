package com.zerobase.storereservation.security;
import com.zerobase.storereservation.service.MemberService;
import com.zerobase.storereservation.util.Aes256Util;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenProvider {

    private static final long TOKEN_EXPIRE_TIME = 1000L * 60 * 60 * 24; // token 만료시간 : 하루
    private static final String KEY_ROLES = "roles";
    private final MemberService memberService;
    public static final String TOKEN_PREFIX = "Bearer ";

    @Value("${spring.jwt.secret}")
    private String secretKey;

    // 토큰 생성
    public String generateToken(Long id, String email, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(Aes256Util.encrypt(email))
                .setId(Aes256Util.encrypt(id.toString())); // 사용자 정보 암호화해 저장
        claims.put(KEY_ROLES, roles); // 권한 정보 저장
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TOKEN_EXPIRE_TIME); // 만료 기간 : 1일

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now) // 토큰 생성 시간
                .setExpiration(expiryDate) // 토큰 만료 시간
                .signWith(SignatureAlgorithm.HS512, this.secretKey) // 사용할 암호화 알고리즘, 비밀키
                .compact();

    }

    // 토큰 이용해 인증 정보 반환
    public Authentication getAuthentication(String token) {
        UserDetails userDetails = memberService.loadUserByUsername(this.getUsernameFromToken(token));
        log.info("{} {}", userDetails.getAuthorities(), userDetails.getUsername());
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    // 토큰이용해 유저 이메일 복호화해 리턴
    public String getUsernameFromToken(String token) {
        return Aes256Util.decrypt(this.parseClaims(token).getSubject());
    }

    // 토큰이용해 유저 id 복호화해 리턴
    public Long getUserIdFromToken(String token) {
        String bearer = token.substring(TOKEN_PREFIX.length());
        return Long.valueOf(Objects.requireNonNull(Aes256Util.decrypt(this.parseClaims(bearer).getId())));
    }

    // jwt 파싱해 claims 얻어옴
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(this.secretKey).parseClaimsJws(token).getBody();

        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // 토큰 만료되었는지 확인 (1일이 지났는지)
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) return false;

        Jws<Claims> claimsJws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);

        return !claimsJws.getBody().getExpiration().before(new Date());
    }
}