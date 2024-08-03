package com.zerobase.storereservation.controller;

import com.zerobase.storereservation.domain.store.form.*;
import com.zerobase.storereservation.security.TokenProvider;
import com.zerobase.storereservation.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store")
public class StoreController {

    private final StoreService storeService;
    private final TokenProvider tokenProvider;
    private final ValidationErrorResponse validationErrorResponse;

    /**
     * 매장 정보 등록
     * @param token
     * @param form : name, description, address, openAt, closedAt
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return : 저장한 매장 정보
     * @throws IOException
     * @throws ParseException
     */
    @PostMapping
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> registerStore(@RequestHeader(name = "Authorization") String token,
                                           @RequestBody @Valid RegisterStore form, Errors errors) throws IOException, ParseException {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(storeService.registerStore(tokenProvider.getUserIdFromToken(token), form));
    }

    /**
     * 매장 예약 상세정보 등록
     * @param token
     * @param form : storeId, startAt(예약 가능 시작시간), endAt(예약 가능 마감시간), minCount(예약 최소인원), maxCount(예약 최대인원)
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return : 저장한 매장 예약 정보
     */
    @PostMapping("/reservation/info")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> addStoreReservationInfo(@RequestHeader(name = "Authorization") String token,
                                                     @RequestBody @Valid List<RegisterStoreReservationInfo> form, Errors errors) {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(storeService.addStoreReservationInfo(tokenProvider.getUserIdFromToken(token), form));
    }

    /**
     * 매장 정보 수정
     * @param token
     * @param form : id, name, description, address, openAt, closedAt, infos(예약 상세 정보), 예약 가능 날짜
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return : 수정한 매장 정보
     * @throws IOException
     * @throws ParseException
     */
    @PutMapping
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> updateStore(@RequestHeader(name = "Authorization") String token,
                                         @RequestBody @Valid UpdateStore form, Errors errors) throws IOException, ParseException {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(storeService.updateStore(tokenProvider.getUserIdFromToken(token), form));
    }

    /**
     * 매장 예약 상세정보 수정
     * @param token
     * @param form : id, storeId, startAt(매장 오픈 시간), endAt(매장 마감 시간), minCount(예약 최소인원), maxCount(예약 최대인원), exist(기존에 있던 정보인지)
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return : 수정한 매장 예약 정보
     */
    @PatchMapping("/reservation/info")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> updateStoreReservationInfo(@RequestHeader(name = "Authorization") String token,
                                                        @RequestBody @Valid List<UpdateReservationInfo> form, Errors errors) {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(storeService.updateStoreReservationInfo(tokenProvider.getUserIdFromToken(token), form));
    }

    /**
     * 예약 가능 날짜 수정
     * @param token
     * @param form : id, dates(예약 가능 날짜들)
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return : 수정한 매장 정보
     */
    @PatchMapping("/reservation/date")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> updateStoreReservationDate(@RequestHeader(name = "Authorization") String token,
                                             @RequestBody @Valid UpdateStoreDate form, Errors errors) {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok(storeService.updateStoreReservationDate(tokenProvider.getUserIdFromToken(token), form));
    }

    /**
     * 예약 마감 정보 수정
     * @param token
     * @param form : id, date(해당 날짜), closed(-1: 예약 마감, int: 예약가능 인원)
     * @return : 수정한 매장 예약 정보
     */
    @PatchMapping("/reservation/date/closed")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> updateStoreReservationClosed(@RequestHeader(name = "Authorization") String token,
                                                          @RequestBody UpdateReservationClosed form) {

        return ResponseEntity.ok(storeService.updateStoreReservationClosed(tokenProvider.getUserIdFromToken(token),form));
    }

    /**
     * 매장 정보 삭제
     * @param token
     * @param id
     * @return 삭제된 매장 정보
     */
    @PatchMapping
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> deleteStore(@RequestHeader(name = "Authorization") String token
            , @RequestParam Long id) {

        return ResponseEntity.ok(storeService.deleteStore(tokenProvider.getUserIdFromToken(token), id));
    }

    /**
     * 매장 예약 정보 삭제
     * @param token
     * @param form : ids(삭제할 매장 예약 상세정보 id 리스트), storeId
     * @return : 수정한 매장 정보
     */
    @DeleteMapping("/reservation/info")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> deleteStoreReservationInfo(@RequestHeader(name = "Authorization") String token
            , @RequestBody DeleteReservationInfo form) {

        return ResponseEntity.ok(storeService.deleteStoreReservationInfo(tokenProvider.getUserIdFromToken(token), form));
    }


}
