package com.zerobase.storereservation.controller;

import com.zerobase.storereservation.domain.reservation.form.ConfirmReservation;
import com.zerobase.storereservation.domain.reservation.form.MakeReservation;
import com.zerobase.storereservation.security.TokenProvider;
import com.zerobase.storereservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservation")
public class ReservationController {
    private final ReservationService reservationService;
    private final TokenProvider tokenProvider;
    private final ValidationErrorResponse validationErrorResponse;

    /**
     * 매장 예약
     * @param token
     * @param form : reservationInfoId, headCount(예약 인원), phone, reservationDate
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return : 저장된 예약 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> makeReservation(@RequestHeader(name = "Authorization") String token,
                                             @RequestBody @Valid MakeReservation form, Errors errors) {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(reservationService.makeReservation(tokenProvider.getUserIdFromToken(token), form));
    }

    /**
     * 신청된 예약 승인, 거절
     * @param token
     * @param form : reservationId, status(예약/승인)
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return : 수정한 예약 정보
     */
    @PatchMapping
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> changeReservationStatus(@RequestHeader(name = "Authorization") String token,
                                                @RequestBody @Valid ConfirmReservation form, Errors errors) {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(reservationService.changeReservationStatus(tokenProvider.getUserIdFromToken(token), form));
    }

    /**
     * 매장 예약 취소
     * @param token
     * @param id
     * @return : 취소한 예약 정보
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelReservation(@RequestHeader(name = "Authorization") String token,
                                               @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.cancelReservation(tokenProvider.getUserIdFromToken(token), id));
    }

    /**
     * 매장 방문 확인
     * @param token
     * @param id
     * @return : 수정한 예약 정보
     */
    @PatchMapping("/visit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> visitReservation(@RequestHeader(name = "Authorization") String token,
                                               @RequestParam Long id) {
        return ResponseEntity.ok(reservationService.visitReservation(tokenProvider.getUserIdFromToken(token), id));
    }



}
