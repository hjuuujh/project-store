package com.zerobase.storereservation.domain.member.form;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    // 로그인 성공시 발급된 토큰을 리턴하기 위해
    private String token;
    public static TokenResponse from(String token) {
        return TokenResponse.builder()
                .token(token)
                .build();
    }
}
