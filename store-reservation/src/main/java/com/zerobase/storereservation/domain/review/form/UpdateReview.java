package com.zerobase.storereservation.domain.review.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class UpdateReview {
        // 리뷰 수정하는 form
        @NotNull(message = "리뷰 정보는 필수 입니다.")
        private Long id;
        @NotNull(message = "별점은 필수입니다.")
        private float rating;
        @NotNull(message = "리뷰 내용은 필수입니다.")
        private String comment;
}
