package com.zerobase.storereservation.service;

import com.zerobase.storereservation.domain.reservation.entity.Reservation;
import com.zerobase.storereservation.domain.review.dto.ReviewDto;
import com.zerobase.storereservation.domain.review.entity.Review;
import com.zerobase.storereservation.domain.review.form.CreateReview;
import com.zerobase.storereservation.domain.review.form.UpdateReview;
import com.zerobase.storereservation.domain.store.entity.Store;
import com.zerobase.storereservation.domain.store.entity.StoreReservationInfo;
import com.zerobase.storereservation.exception.ErrorCode;
import com.zerobase.storereservation.exception.ReservationException;
import com.zerobase.storereservation.exception.ReviewException;
import com.zerobase.storereservation.repository.ReservationRepository;
import com.zerobase.storereservation.repository.ReviewRepository;
import com.zerobase.storereservation.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.Optional;

import static com.zerobase.storereservation.domain.reservation.type.Status.APPROVED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceTest.class);
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private StoreRepository storeRepository;
    @InjectMocks
    private ReviewService reviewService;

    @Test
    void successCreateReview() {
        //given
        CreateReview form = CreateReview.builder()
                .reservationId(1L)
                .rating(3.3f)
                .comment("후기 작성")
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(Store.builder().id(1L).name("매장").build())
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(true)
                .build();

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        given(reviewRepository.existsByCustomerIdAndReservationId(anyLong(), anyLong()))
                .willReturn(false);

        float reviewSum = 99f;
        long reviewCount = 30;
        float rating = 3.3f;

        Store store = Store.builder()
                .id(1L)
                .reviewSum(reviewSum)
                .reviewCount(reviewCount)
                .rating(rating)
                .build();

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));


        Review review = Review.builder()
                .id(1L)
                .customerId(1L)
                .partnerId(1L)
                .reservation(reservation)
                .rating(3.3f)
                .comment("후기 작성")
                .build();

        given(reviewRepository.save(any()))
                .willReturn(review);


        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);

        //when
        ReviewDto reviewDto = reviewService.createReview(1L, form);

        //then
        verify(reviewRepository, times(1)).save(captor.capture());
        assertEquals(3.3f, captor.getValue().getRating());
        assertEquals("후기 작성", captor.getValue().getComment());
        assertEquals(reviewCount + 1, store.getReviewCount());
        assertEquals(reviewSum + rating, store.getReviewSum());
        assertEquals((reviewSum + rating) / (reviewCount + 1), store.getRating());
    }

    @Test
    void failCreateReview_UNMATCHED_CUSTOMER_RESERVATION() {
        //given
        CreateReview form = CreateReview.builder()
                .reservationId(1L)
                .rating(3.3f)
                .comment("후기 작성")
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(Store.builder().name("매장").build())
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.empty());

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reviewService.createReview(1L, form));

        //then
        assertEquals(ErrorCode.UNMATCHED_CUSTOMER_RESERVATION, exception.getErrorCode());
        assertEquals("고객 정보와 예약 정보가 일치하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void failCreateReview_VISIT_NOT_TRUE() {
        //given
        CreateReview form = CreateReview.builder()
                .reservationId(1L)
                .rating(3.3f)
                .comment("후기 작성")
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(Store.builder().name("매장").build())
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(false)
                .build();

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.createReview(1L, form));

        //then
        assertEquals(ErrorCode.VISIT_NOT_TRUE, exception.getErrorCode());
        assertEquals("방문 정보가 존재하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void failCreateReview_OVER_RATING_LIMIT() {
        //given
        CreateReview form = CreateReview.builder()
                .reservationId(1L)
                .rating(5.3f)
                .comment("후기 작성")
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(Store.builder().name("매장").build())
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(true)
                .build();

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.createReview(1L, form));

        //then
        assertEquals(ErrorCode.OVER_RATING_LIMIT, exception.getErrorCode());
        assertEquals("별점은 최대 5점까지 가능합니다.", exception.getErrorMessage());
    }

    @Test
    void failCreateReview_ALREADY_CREATED_REVIEW() {
        //given
        CreateReview form = CreateReview.builder()
                .reservationId(1L)
                .rating(3.3f)
                .comment("후기 작성")
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(Store.builder().name("매장").build())
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();
        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(true)
                .build();

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));
        given(reviewRepository.existsByCustomerIdAndReservationId(anyLong(), anyLong()))
                .willReturn(true);
        //when
        ReviewException exception = assertThrows(ReviewException.class,
                () -> reviewService.createReview(1L, form));

        //then
        assertEquals(ErrorCode.ALREADY_CREATED_REVIEW, exception.getErrorCode());
        assertEquals("이미 리뷰를 작성하였습니다.", exception.getErrorMessage());
    }

    @Test
    void successUpdateReview() {
        //given
        float newRating = 3.3f;
        float reviewSum = 99f;
        long reviewCount = 30;
        float storeRating = 4.3f;

        UpdateReview form = UpdateReview.builder()
                .id(1L)
                .rating(newRating)
                .comment("후기 수정")
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(Store.builder().id(1L).name("매장").build())
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(true)
                .build();

        Review review = Review.builder()
                .id(1L)
                .customerId(1L)
                .partnerId(1L)
                .reservation(reservation)
                .rating(storeRating)
                .comment("후기 작성")
                .build();

        given(reviewRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(review));

        Store store = Store.builder()
                .id(1L)
                .reviewSum(reviewSum)
                .reviewCount(reviewCount)
                .rating(storeRating)
                .build();

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));

        //when
        ReviewDto reviewDto = reviewService.updateReview(1L, form);

        //then
        assertEquals("후기 수정", reviewDto.getComment());
        assertEquals(newRating, reviewDto.getRating());
        assertEquals(reviewCount, store.getReviewCount());
        assertEquals((reviewSum-storeRating+newRating)/reviewCount, store.getRating());
        assertEquals(reviewSum-storeRating+newRating, store.getReviewSum());
    }

    @Test
    void successDeleteReviewByPartner(){
        //given
        float reviewSum = 99f;
        long reviewCount = 30;
        float rating = 3.3f;

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(Store.builder().id(1L).name("매장").build())
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(true)
                .build();

        Review review = Review.builder()
                .id(1L)
                .customerId(1L)
                .partnerId(2L)
                .reservation(reservation)
                .rating(rating)
                .comment("후기 작성")
                .build();

        given(reviewRepository.findByIdAndPartnerId( anyLong(),anyLong()))
                .willReturn(Optional.ofNullable(review));

        Store store = Store.builder()
                .id(1L)
                .reviewSum(reviewSum)
                .reviewCount(reviewCount)
                .build();

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));

        //when
        String result = reviewService.deleteReviewByPartner(1L, 1L);

        //then
        verify(reviewRepository,times(1)).delete(any());
        assertEquals(reviewSum-rating, store.getReviewSum());
        assertEquals(reviewCount-1, store.getReviewCount());
        assertEquals((reviewSum-rating)/ (reviewCount-1), store.getRating());
    }

}