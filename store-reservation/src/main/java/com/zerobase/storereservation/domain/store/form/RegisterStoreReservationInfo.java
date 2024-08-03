package com.zerobase.storereservation.domain.store.form;

import lombok.*;

import javax.validation.constraints.NotBlank;
import java.time.LocalTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterStoreReservationInfo {
        // 매장 예약정보 등록 위한 form
        private Long storeId;
        @NotBlank(message = "예약 시작 시간이 비어있습니다.")
        private LocalTime startAt;
        @NotBlank(message = "예약 종료 시간이 비어있습니다.")
        private LocalTime endAt;
        @NotBlank(message = "예약 최소 인원이 비어있습니다.")
        private int minCount;
        @NotBlank(message = "예약 최대 인원이 비어있습니다.")
        private int maxCount;
        @NotBlank(message = "매장 수용 인원이 비어있습니다.")
        private int count;
}
