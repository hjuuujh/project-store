package com.zerobase.storereservation.controller;

import com.zerobase.storereservation.domain.member.dto.MemberDto;
import com.zerobase.storereservation.domain.member.form.SignIn;
import com.zerobase.storereservation.domain.member.form.SignUp;
import com.zerobase.storereservation.domain.member.form.TokenResponse;
import com.zerobase.storereservation.security.TokenProvider;
import com.zerobase.storereservation.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {
    private final MemberService memberService;
    private final TokenProvider tokenProvider;
    private final ValidationErrorResponse validationErrorResponse;

    /**
     * 사용자 정보 등록
     * @param form : email, name, password, phone, roles
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return 저장된 유저 정보
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerMember(@RequestBody @Valid SignUp form, Errors errors) {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(memberService.registerMember(form));
    }

    /**
     * 이메일과 패스워드로 로그인
     * @param form : email, password
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return 토큰
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signInMember(@RequestBody @Valid SignIn form, Errors errors) {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }

        MemberDto memberDto = memberService.signInMember(form);
        log.info("user login -> {}", memberDto.getName());
        String token = tokenProvider.generateToken(memberDto.getId(), memberDto.getEmail(), memberDto.getRoles());

        return ResponseEntity.ok(TokenResponse.from(token));
    }

    /**
     * 사용자 정보 찾기
     * @param token
     * @return 토큰으로 찾은 유저 정보
     */
    @GetMapping
    public ResponseEntity<MemberDto> getMemberInfo(@RequestHeader(name = "Authorization") String token) {
        Long id = tokenProvider.getUserIdFromToken(token);
        MemberDto result = memberService.findMember(id);
        return ResponseEntity.ok(result);
    }


}
