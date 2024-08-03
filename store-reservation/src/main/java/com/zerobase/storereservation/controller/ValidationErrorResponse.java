package com.zerobase.storereservation.controller;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;

@Component
public class ValidationErrorResponse
{

    /**
     * validation 에러 메세지 리스트를 리턴하는 클래스
     * @param errors
     * @return
     */
    public List<ResponseError> checkValidation(Errors errors){
        List<ResponseError> responseErrors = new ArrayList<>();

        if(errors.hasErrors()){
            errors.getAllErrors().forEach(error -> {
                responseErrors.add(ResponseError.of((FieldError) error));
            });
        }

        return responseErrors;
    }
}
