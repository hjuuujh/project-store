package com.zerobase.storereservation.controller;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.FieldError;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseError {
    private String field;
    private String message;

    /**
     * Runtime validation error가 발생하는 경우 내용 담는 클래스
     * @param error
     * @return
     */
    public static ResponseError of(FieldError error) {
        return ResponseError.builder()
                .field(error.getField())
                .message(error.getDefaultMessage())
                .build();
    }
}
