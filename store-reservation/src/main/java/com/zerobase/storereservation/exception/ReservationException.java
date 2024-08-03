package com.zerobase.storereservation.exception;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationException extends RuntimeException {
    // reservation 관련 에러발생한 경우

    private ErrorCode errorCode;
    private String errorMessage;

    public ReservationException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }
}
