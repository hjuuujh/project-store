package com.zerobase.storereservation.domain.store.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteReservationInfo {
    // 예약 취소 위한 form
    private List<Long> ids;
    private Long storeId;
}
