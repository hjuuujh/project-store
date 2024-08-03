package com.zerobase.storereservation.domain.review.entity;

import com.zerobase.storereservation.domain.BaseEntity;
import com.zerobase.storereservation.domain.reservation.entity.Reservation;
import com.zerobase.storereservation.domain.review.form.CreateReview;
import com.zerobase.storereservation.domain.review.form.UpdateReview;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.AuditOverride;

import javax.persistence.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@AuditOverride(forClass = BaseEntity.class)
public class Review extends BaseEntity{
    // 리뷰 entity

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId; // 등록하는 고객 id
    private Long storeId; // 등록하는 매장 id
    private Long partnerId; // 등록하는 매장의 파트너 id

    @OneToOne(fetch = FetchType.LAZY)
    private Reservation reservation; // 리뷰 등록하려는 예약 정보

    private float rating; // 별점
    private String comment; // 리뷰 내용

    public static Review of(Long customerId, CreateReview form, Reservation reservation) {
        return Review.builder()
                .customerId(customerId)
                .partnerId(reservation.getStoreReservationInfo().getPartnerId())
                .storeId(reservation.getStoreReservationInfo().getStore().getId())
                .reservation(reservation)
                .rating(form.getRating())
                .comment(form.getComment())
                .build();
    }

    // 리뷰 내용 수정
    public void update(UpdateReview form) {
        rating = form.getRating();
        comment = form.getComment();
    }
}
