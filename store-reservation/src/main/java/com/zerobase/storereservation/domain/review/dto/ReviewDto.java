package com.zerobase.storereservation.domain.review.dto;

import com.zerobase.storereservation.domain.review.entity.Review;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {
    private Long id;
    private Long customerId;
    private Long partnerId;
    private Long reservationId;
    private Long storeId;
    private float rating;
    private String comment;

    public static ReviewDto from(Review review) {
        return ReviewDto.builder()
                .id(review.getId())
                .customerId(review.getCustomerId())
                .partnerId(review.getPartnerId())
                .storeId(review.getStoreId())
                .reservationId(review.getReservation().getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .build();
    }
}
