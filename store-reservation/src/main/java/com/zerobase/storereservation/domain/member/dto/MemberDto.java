package com.zerobase.storereservation.domain.member.dto;
import com.zerobase.storereservation.domain.member.entity.Member;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {
    private Long id;

    private String email;
    private String name;
    private String phone;
    private List<String> roles;


    public static MemberDto from(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .name(member.getName())
                .phone(member.getPhone())
                .roles(member.getRoles())
                .build();
    }

}
