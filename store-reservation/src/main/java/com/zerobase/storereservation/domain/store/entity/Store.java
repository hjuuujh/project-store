package com.zerobase.storereservation.domain.store.entity;

import com.zerobase.storereservation.domain.BaseEntity;
import com.zerobase.storereservation.domain.store.form.RegisterStore;
import com.zerobase.storereservation.domain.store.form.UpdateStore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.envers.AuditOverride;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@AuditOverride(forClass = BaseEntity.class)
public class Store extends BaseEntity {
    // 매장 entity

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long partnerId; // 매장 소유 파트너 id

    private String name; // 매장명
    private String description; // 매장 설명

    private String address; // 매장 주소

    private LocalTime openAt; // 매장 운영 시작시간
    private LocalTime closeAt; // 매장 운영 마감 시간

    @ElementCollection(fetch = FetchType.EAGER)
    private List<LocalDate> dates; // 예약 가능 날짜 리스트

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "store_id")
    private List<StoreReservationInfo> storeReservationInfos; // 매장 예약 상세정보

    private float lon; // 주소로 얻어온 경도
    private float lat; // 주소로 언어온 위도

    @ColumnDefault("0")
    private float rating; // 매장 별점

    // 별점 업데이트 할때마다 리뷰테이블 전체읽어서 정보 얻어오는 것보다
    // 리뷰 총 개수와 별점 총합 저장해두는게 효율적이라 생각함
    @ColumnDefault("0")
    private float reviewSum; // 매장에 등록된 후기들의 별점 총합
    @ColumnDefault("0")
    private long reviewCount; // 매장에 등록된 후기들의 개수 총합

    @ColumnDefault("false")
    private boolean deleted; // 매장 삭제 정보


    public static Store of(Long partnerId, List<Float> coordinates, RegisterStore form) {
        return Store.builder()
                .partnerId(partnerId)
                .name(form.getName())
                .description(form.getDescription())
                .address(form.getAddress())
                .openAt(form.getOpenAt())
                .closeAt(form.getCloseAt())
                .lon(coordinates.get(0))
                .lat(coordinates.get(1))
                .storeReservationInfos(new ArrayList<>())
                .build();
    }

    // 매장 수정
    public void update(UpdateStore form, List<Float> coordinates){
        this.name = form.getName();
        this.description = form.getDescription();
        this.address = form.getAddress();
        this.openAt = form.getOpenAt();
        this.closeAt = form.getCloseAt();
        this.lon = coordinates.get(0);
        this.lat = coordinates.get(1);
    }

    // 매장 삭제 정보 업데이트
    public void delete(boolean deleted) {
        this.deleted = deleted;
    }

    // 후기 등록된 경우 별점 업데이트
    public void updateRating(float newRating) {
        this.reviewCount++; // 후기 총개수 증가
        this.reviewSum += newRating; // 별점 총합 증가
        this.rating = this.reviewSum / this.reviewCount;
    }

    // 후기 삭제된 경우 별점 업데이트
    public void cancelRating(float oldRating) {
        this.reviewCount--; // 후기 총개수 감소
        this.reviewSum -= oldRating; // 별점 총합 감소
        this.rating = this.reviewSum / this.reviewCount;
    }

    // 후기 수정한 경우 별점 업데이트
    public void updateRating(float oldRating, float newRating) {
        cancelRating(oldRating); // 기존의 별점 으로 별점 총합 감소, 후기 총개수 감소
        updateRating(newRating); // 기존의 별점 으로 별점 총합 증가, 후기 총개수 증가
    }

    // 예역 가능 날짜 업데이트
    public void updateDates(List<LocalDate> dates) {
        this.dates = dates;
    }
}
