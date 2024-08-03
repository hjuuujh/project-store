package com.zerobase.storereservation.domain.store.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.zerobase.storereservation.domain.BaseEntity;
import com.zerobase.storereservation.domain.store.form.RegisterStoreReservationInfo;
import com.zerobase.storereservation.domain.store.form.UpdateReservationInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.AuditOverride;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@AuditOverride(forClass = BaseEntity.class)
public class StoreReservationInfo extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long partnerId; // 매장 파트너 id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @JsonBackReference
    private Store store; // 매장 정보

    private LocalTime startAt; // 예약 시작 시간
    private LocalTime endAt; // 예약 끝 시간

    private int minCount; // 예약 가능 최소 인원
    private int maxCount; // 예약 가능 최대 인원
    private int count;

    @ElementCollection(fetch = FetchType.EAGER)
    private Map<LocalDate, Integer> closed; // 예약 가능 날짜, 예약가능 잔여인원(-1 : 마감)

    public static StoreReservationInfo of(Long partnerId, RegisterStoreReservationInfo info) {
        return StoreReservationInfo.builder()
                .partnerId(partnerId)
                .startAt(info.getStartAt())
                .endAt(info.getEndAt())
                .minCount(info.getMinCount())
                .maxCount(info.getMaxCount())
                .count(info.getCount())
                .closed(new HashMap<>())
                .build();
    }

    public static StoreReservationInfo of(Long partnerId, UpdateReservationInfo info, Map<LocalDate, Integer> dates) {
        return StoreReservationInfo.builder()
                .partnerId(partnerId)
                .startAt(info.getStartAt())
                .endAt(info.getEndAt())
                .minCount(info.getMinCount())
                .maxCount(info.getMaxCount())
                .count(info.getCount())
                .closed(dates)
                .build();
    }

    // 예약 취소한 경우 해당 날짜의 예약 가능 잔여 인원 감소
    public void decreaseCount(LocalDate reservationDate, int headCount) {
        Integer newCount = this.closed.get(reservationDate) - headCount;
        this.closed.put(reservationDate, newCount);
    }

    // 예약 승인된 경우 해당 날짜의 예약 가능 잔여 인원 증가
    public void increaseCount(LocalDate reservationDate, int headCount) {
        Integer newCount = this.closed.get(reservationDate)+ headCount;
        this.closed.put(reservationDate, newCount);
    }

    // 예약 상세정보 수정
    public void update(UpdateReservationInfo form) {
        startAt = form.getStartAt();
        endAt = form.getEndAt();
        minCount = form.getMinCount();
        maxCount = form.getMaxCount();
        count = form.getCount();
    }

    // 예약 가능 날짜 업데이트
    public void updateClosed(Map<LocalDate, Integer> dates) {
        this.closed = dates;
    }

    // 예약 마감 여부 업데이트
    public void updateDateClosed(LocalDate date, Integer closed) {
        this.closed.put(date, closed);
    }
}
