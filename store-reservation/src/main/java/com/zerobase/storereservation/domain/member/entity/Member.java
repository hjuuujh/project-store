package com.zerobase.storereservation.domain.member.entity;

import com.zerobase.storereservation.domain.BaseEntity;
import com.zerobase.storereservation.domain.member.form.SignUp;
import lombok.*;
import org.hibernate.envers.AuditOverride;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "member")
@AuditOverride(forClass = BaseEntity.class)
public class Member extends BaseEntity implements UserDetails {
    // 고객 / 파트너 entity
    // Spring Security 를 이용 : UserDetail 를 구현

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password;
    private String name;
    private String phone;
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());}

    @Override
    public String getPassword() {
        return this.password;
    }

    // email을 계정의 고유한 값으로 이용
    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static Member of(SignUp form, String password){
        return Member.builder()
                .email(form.getEmail())
                .password(password)
                .name(form.getName())
                .phone(form.getPhone())
                .roles(form.getRoles())
                .build();
    }
}
