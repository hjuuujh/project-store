package com.zerobase.storereservation.domain.store.dto;

import com.zerobase.storereservation.domain.store.entity.StoreReservationInfo;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreReservationInfoDto {
    private Long id;

    private Long partnerId;

    private LocalTime startAt;
    private LocalTime endAt;
    private int minCount;
    private int maxCount;
    private int count;
    private Map<LocalDate, Integer> closed;

    public static StoreReservationInfoDto from(StoreReservationInfo storeReservationInfo) {
        return StoreReservationInfoDto.builder()
                .id(storeReservationInfo.getId())
                .startAt(storeReservationInfo.getStartAt())
                .endAt(storeReservationInfo.getEndAt())
                .minCount(storeReservationInfo.getMinCount())
                .maxCount(storeReservationInfo.getMaxCount())
                .count(storeReservationInfo.getCount())
                .closed(storeReservationInfo.getClosed())
                .build();
    }


}
