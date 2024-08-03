package com.zerobase.storereservation.exception;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewException extends RuntimeException {
    // review 관련 에러발생한 경우

    private ErrorCode errorCode;
    private String errorMessage;

    public ReviewException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }
}
