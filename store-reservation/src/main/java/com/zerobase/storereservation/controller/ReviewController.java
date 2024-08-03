package com.zerobase.storereservation.controller;

import com.zerobase.storereservation.domain.review.form.CreateReview;
import com.zerobase.storereservation.domain.review.form.UpdateReview;
import com.zerobase.storereservation.security.TokenProvider;
import com.zerobase.storereservation.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private final TokenProvider tokenProvider;
    private final ValidationErrorResponse validationErrorResponse;

    /**
     * 리뷰 등록
     * @param token
     * @param form : reservationId, rating, comment
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return : 등록한 리뷰 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createReview(@RequestHeader(name = "Authorization") String token,
                                          @RequestBody @Valid CreateReview form, Errors errors) {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(reviewService.createReview(tokenProvider.getUserIdFromToken(token), form));
    }

    /**
     * 리뷰 수정
     * @param token
     * @param form : id, rating, comment
     * @param errors : form의 validation 체크후 잘못된 형식의 메세지 리턴
     * @return : 수정한 리뷰 정보
     */
    @PatchMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateReview(@RequestHeader(name = "Authorization") String token,
                                          @RequestBody @Valid UpdateReview form, Errors errors) {
        List<ResponseError> responseErrors = validationErrorResponse.checkValidation(errors);
        if (!responseErrors.isEmpty()) {
            return new ResponseEntity<>(responseErrors, HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(reviewService.updateReview(tokenProvider.getUserIdFromToken(token), form));
    }

    /**
     * 고객이 자신이 등록한 리뷰 삭제
     * @param token
     * @param id
     * @return : 삭제한 id + 삭제되었습니다.
     */
    @DeleteMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> deleteReviewByCustomer(@RequestHeader(name = "Authorization") String token,
                                                    @RequestParam Long id) {

        return ResponseEntity.ok(reviewService.deleteReviewByCustomer(tokenProvider.getUserIdFromToken(token), id));
    }

    /**
     * 파트너가 자신의 매장에 등록된 리뷰 삭제
     * @param token
     * @param id
     * @return : 삭제한 id + 삭제되었습니다.
     */
    @DeleteMapping("/partner")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> deleteReviewByPartner(@RequestHeader(name = "Authorization") String token,
                                                   @RequestParam Long id) {

        return ResponseEntity.ok(reviewService.deleteReviewByPartner(tokenProvider.getUserIdFromToken(token), id));
    }

    /**
     * 고객이 등록한 모든 리뷰
     * @param token
     * @param pageable
     * @return : 등록한 리뷰 리스트
     */
    @GetMapping("/search/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> searchReview(@RequestHeader(name = "Authorization") String token,
                                          final Pageable pageable) {

        return ResponseEntity.ok(reviewService.searchReview(tokenProvider.getUserIdFromToken(token), pageable));
    }

    /**
     * 파트너의 특정 매장에 등록된 모든 리뷰
     * @param token
     * @param storeId
     * @param pageable
     * @return : 등록된 리뷰 리스트
     */
    @GetMapping("/search/partner")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> searchReviewByStore(@RequestHeader(name = "Authorization") String token,
                                                 @RequestParam Long storeId, final Pageable pageable) {

        return ResponseEntity.ok(reviewService.searchReviewByStore(tokenProvider.getUserIdFromToken(token), storeId, pageable));
    }

}
