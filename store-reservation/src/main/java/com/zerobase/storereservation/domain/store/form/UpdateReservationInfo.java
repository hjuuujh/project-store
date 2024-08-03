package com.zerobase.storereservation.domain.store.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReservationInfo {
        // 예약 상세정보 수정위한 form

        private Long id;
        private Long storeId;
        private LocalTime startAt;
        private LocalTime endAt;

        private int minCount;
        private int maxCount;
        private int count;

        private boolean exist;
}
