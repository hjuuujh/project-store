package com.zerobase.storereservation.domain.reservation.dto;

import com.zerobase.storereservation.domain.reservation.entity.Reservation;
import com.zerobase.storereservation.domain.reservation.type.Status;
import lombok.*;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDto {
    private Long id;
    private Long memberId;
    private Long storeId;
    private String phone;
    private String storeName;
    private Long reservationInfoId;
    private LocalDate reservationDate;
    private int headCount;
    @Enumerated(EnumType.STRING)
    private Status status;
    private boolean visit;

    public static ReservationDto from(Reservation reservation) {
        return ReservationDto.builder()
                .id(reservation.getId())
                .memberId(reservation.getCustomerId())
                .storeId(reservation.getStoreId())
                .reservationDate(reservation.getReservationDate())
                .storeName(reservation.getStoreReservationInfo().getStore().getName())
                .reservationInfoId(reservation.getStoreReservationInfo().getId())
                .headCount(reservation.getHeadCount())
                .status(reservation.getStatus())
                .visit(reservation.isVisit())
                .build();
    }


}
