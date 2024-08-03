package com.zerobase.storereservation.domain.reservation.form;

import com.zerobase.storereservation.domain.reservation.type.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ConfirmReservation {
    // 매장 파트너가 예약을 승인/거절하는 경우 상태 업데이트 위한 form
    @NotNull(message = "예약 정보는 필수 입니다.")
    private Long reservationId;
    @NotBlank(message = "예약 승인 / 거절 정보는 필수입니다.")
    private Status status;


}
