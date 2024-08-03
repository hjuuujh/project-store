package com.zerobase.storereservation.exception;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreException extends RuntimeException {
    // store 관련 에러발생한 경우

    private ErrorCode errorCode;
    private String errorMessage;

    public StoreException(ErrorCode errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDescription();
    }
}
