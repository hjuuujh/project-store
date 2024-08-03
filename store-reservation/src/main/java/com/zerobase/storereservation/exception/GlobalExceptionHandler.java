package com.zerobase.storereservation.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.zerobase.storereservation.exception.ErrorCode.INTERNAL_SERVER_ERROR;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 에러를 발생시키지 않고 응답 메시지로 클라이언트에게 전달
    @ExceptionHandler(MemberException.class)
    public ErrorResponse handleMemberException(MemberException e) {
        errorLog(e.getErrorCode());

        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(StoreException.class)
    public ErrorResponse handleStoreException(StoreException e) {
        errorLog(e.getErrorCode());

        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(ReservationException.class)
    public ErrorResponse handleReservationException(ReservationException e) {
        errorLog(e.getErrorCode());

        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler(ReviewException.class)
    public ErrorResponse handleReviewException(ReviewException e) {
        errorLog(e.getErrorCode());

        return new ErrorResponse(e.getErrorCode(), e.getErrorMessage());
    }

    // 그외 에러
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception e) {
        log.error("Exception is occurred", e);

        return new ErrorResponse(INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private static void errorLog(ErrorCode e) {
        log.error("{} is occurred", e);
    }
}
