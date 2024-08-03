package com.zerobase.storereservation.domain.store.dto;

import com.zerobase.storereservation.domain.store.entity.Store;
import com.zerobase.storereservation.domain.store.entity.StoreReservationInfo;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDto {
    private Long id;
    private Long partnerId;
    private String name;
    private String description;
    private String address;
    private LocalTime openAt;
    private LocalTime closeAt;
    private List<LocalDate> dates;
    private List<StoreReservationInfo> storeReservationInfos;
    private float lon;
    private float lat;
    private double rating;
    private boolean deleted;

    public static StoreDto from(Store store) {
        return StoreDto.builder()
                .id(store.getId())
                .partnerId(store.getPartnerId())
                .name(store.getName())
                .description(store.getDescription())
                .dates(store.getDates())
                .address(store.getAddress())
                .openAt(store.getOpenAt())
                .closeAt(store.getCloseAt())
                .lon(store.getLon())
                .lat(store.getLat())
                .rating(store.getRating())
                .storeReservationInfos(store.getStoreReservationInfos())
                .deleted(store.isDeleted())
                .build();
    }
}
