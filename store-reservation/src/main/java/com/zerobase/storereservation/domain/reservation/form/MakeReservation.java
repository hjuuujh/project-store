package com.zerobase.storereservation.domain.reservation.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class MakeReservation {
    // 예약 등록위한 form
    @NotNull(message = "예약 정보는 필수 입니다.")
    private Long reservationInfoId;
    @NotNull(message = "예약인원은 필수입니다.")
    private int headCount;
    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])[.-]?(\\d{3}|\\d{4})[.-]?(\\d{4})$", message = "10 ~ 11 자리의 숫자만 입력 가능합니다.")
    private String phone;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate reservationDate;
}
