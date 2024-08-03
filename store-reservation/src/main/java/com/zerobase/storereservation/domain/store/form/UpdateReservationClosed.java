package com.zerobase.storereservation.domain.store.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReservationClosed {
    // 예약 마감 위한 form
    private Long id;
    private LocalDate date;
    private int closed;
}
