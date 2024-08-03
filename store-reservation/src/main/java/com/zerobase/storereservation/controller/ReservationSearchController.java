package com.zerobase.storereservation.controller;

import com.zerobase.storereservation.security.TokenProvider;
import com.zerobase.storereservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservation/search")
public class ReservationSearchController {
    private final TokenProvider tokenProvider;
    private final ReservationService reservationService;

    /**
     * 고객이 자신이 예약한 리스트 확인
     * @param token
     * @param pageable
     * @return 예약 리스트
     */
    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> searchReservationByCustomer(@RequestHeader(name = "Authorization") String token
            , Pageable pageable) {

        return ResponseEntity.ok(reservationService.searchReservationByMember(tokenProvider.getUserIdFromToken(token),pageable));
    }

    /**
     * 고객이 특정 매장에 예약한 리스트 확인
     * @param token
     * @param storeId
     * @param pageable
     * @return 예약 리스트
     */
    @GetMapping("/customer/{storeId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> searchReservationByCustomerWithStore(@RequestHeader(name = "Authorization") String token,
                                                                    @PathVariable Long storeId, Pageable pageable) {

        return ResponseEntity.ok(reservationService.searchReservationByMemberWithStore(tokenProvider.getUserIdFromToken(token), storeId, pageable));
    }

    /**
     * 파트너가 자신의 특정 매장 예약 리스트 날짜별 확인
     * @param token
     * @param storeId
     * @param date
     * @param pageable
     * @return 예약 리스트
     */
    @GetMapping("/partner/{storeId}")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> searchReservationByPartner(@RequestHeader(name = "Authorization") String token,
                                                        @PathVariable Long storeId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
                                                        Pageable pageable) {

        return ResponseEntity.ok(reservationService.searchReservationByPartner(tokenProvider.getUserIdFromToken(token), storeId, date, pageable));
    }
}
