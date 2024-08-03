package com.zerobase.storereservation.service;

import com.zerobase.storereservation.domain.member.dto.MemberDto;
import com.zerobase.storereservation.domain.member.entity.Member;
import com.zerobase.storereservation.domain.member.form.SignIn;
import com.zerobase.storereservation.domain.member.form.SignUp;
import com.zerobase.storereservation.exception.ErrorCode;
import com.zerobase.storereservation.exception.MemberException;
import com.zerobase.storereservation.repository.MemberRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Slf4j
@AllArgsConstructor
@Transactional
public class MemberService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    /**
     * Spring Security를 이용
     * 유저의 정보를 불러오기 위해서 구현
     * @param email: email을 계정의 고유한 값으로 이용
     * @return 유저 정보
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }

    /**
     * form으로 받아온사용자 등록
     * @param form : email, name, password, phone, roles
     * @return 저장된 사용자 정보
     */
    @Transactional
    public MemberDto registerMember(SignUp form) {
        checkAlreadyExists(form);

        Member member = Member.of(form, passwordEncoder.encode(form.getPassword()));

        Member save = memberRepository.save(member);
        return MemberDto.from(save);
    }

    /**
     * 가입하려는 이메일이 이미 존재하는 이메일인지 확인
     * excpetion : ALREADY_REGISTERED_USER "이미 가입된 이메일입니다."
     * @param form
     */
    private void checkAlreadyExists(SignUp form) {
        // 이미 등록된 이메일인 경우 예외 발생 : ALREADY_REGISTERED_USER "이미 가입된 이메일입니다."
        if (memberRepository.existsByEmail(form.getEmail())) {
            throw new MemberException(ErrorCode.ALREADY_REGISTERED_USER);
        }
    }

    /**
     * 이메일과 패스워드로 로그인
     * @param form : email, password
     * excpetion : LOGIN_CHECK_FAIL "이메일과 패스워드를 확인해주세요."
     * @return 이메일, 비밀번호 확인 통해 얻은 유저 정보
     */
    public MemberDto signInMember(SignIn form) {
        // 이메일 이용해 유저정보 찾음
        // 이메일로 가입된 정보가 없는 경우 예외발생
        Member member = memberRepository.findByEmail(form.getEmail())
                .orElseThrow(() -> new MemberException(ErrorCode.NOT_FOUND_USER));

        // 로그인 시도한 비밀번호와 저장된 비밀번호가 같은지 확인
        if(!passwordEncoder.matches(form.getPassword(), member.getPassword())) {
            throw new MemberException(ErrorCode.LOGIN_CHECK_FAIL);
        }
        return MemberDto.from(member);
    }

    /**
     * token 이용해 찾은 user id로 유저 정보 찾음
     * @param id
     * excpetion : NOT_FOUND_USER "일치하는 회원이 없습니다."
     * @return 유저 정보
     */
    public MemberDto findMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new MemberException(ErrorCode.NOT_FOUND_USER));
        return MemberDto.from(member);
    }
}
