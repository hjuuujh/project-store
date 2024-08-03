package com.zerobase.storereservation.service;

import com.zerobase.storereservation.domain.reservation.dto.ReservationDto;
import com.zerobase.storereservation.domain.reservation.entity.Reservation;
import com.zerobase.storereservation.domain.reservation.form.ConfirmReservation;
import com.zerobase.storereservation.domain.reservation.form.MakeReservation;
import com.zerobase.storereservation.domain.reservation.type.Status;
import com.zerobase.storereservation.domain.store.entity.Store;
import com.zerobase.storereservation.domain.store.entity.StoreReservationInfo;
import com.zerobase.storereservation.exception.ErrorCode;
import com.zerobase.storereservation.exception.ReservationException;
import com.zerobase.storereservation.exception.StoreException;
import com.zerobase.storereservation.repository.ReservationRepository;
import com.zerobase.storereservation.repository.StoreRepository;
import com.zerobase.storereservation.repository.StoreReservationInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.zerobase.storereservation.domain.reservation.type.Status.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    ReservationRepository reservationRepository;
    @Mock
    StoreReservationInfoRepository storeReservationInfoRepository;
    @Mock
    StoreRepository storeRepository;
    @InjectMocks
    ReservationService reservationService;


    @Test
    void successMakeReservation() {
        //given
        MakeReservation form = MakeReservation.builder()
                .reservationInfoId(1L)
                .headCount(3)
                .reservationDate(LocalDate.parse("2024-02-01"))
                .build();

        Store store = Store.builder()
                .id(1L)
                .name("매장")
                .dates(Arrays.asList(LocalDate.parse("2024-02-01")
                        ,LocalDate.parse("2024-02-02")
                        , LocalDate.parse("2024-02-03")))
                .build();

        Map<LocalDate, Integer> closed = new HashMap<>();
        closed.put(LocalDate.parse("2024-02-01"),20);
        closed.put(LocalDate.parse("2024-02-02"),20);
        closed.put(LocalDate.parse("2024-02-03"),20);
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(store)
                .closed(closed)
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeRepository.existsByIdAndDatesContains(anyLong(),any()))
                .willReturn(true);

        Reservation reservation = Reservation.builder()
                .customerId(1L)
                .storeReservationInfo(info)
                .headCount(3)
                .status(Status.PENDING)
                .visit(false)
                .build();

        given(reservationRepository.save(any()))
                .willReturn(reservation);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);

        //when
        ReservationDto reservationDto = reservationService.makeReservation(1L, form);

        //then
        verify(reservationRepository).save(captor.capture());
        assertEquals(info, captor.getValue().getStoreReservationInfo());
        assertEquals(3, captor.getValue().getHeadCount());
        assertEquals(Status.PENDING, captor.getValue().getStatus());
    }

    @Test
    void failMakeReservation_ALREADY_DELETED_STORE() {
        //given
        MakeReservation form = MakeReservation.builder()
                .reservationInfoId(1L)
                .headCount(3)
                .reservationDate(LocalDate.parse("2024-02-21"))
                .build();

        Store store = Store.builder()
                .id(1L)
                .name("매장")
                .dates(Arrays.asList(LocalDate.parse("2024-02-21")
                        , LocalDate.parse("2024-03-21")))
                .deleted(true)
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(store)
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();


        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeRepository.existsByIdAndDatesContains(anyLong(),any()))
                .willReturn(true);

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.makeReservation(1L, form));

        //then
        assertEquals(ErrorCode.ALREADY_DELETED_STORE, exception.getErrorCode());
        assertEquals("이미 삭제된 매장입니다.", exception.getErrorMessage());
    }

    @Test
    void failMakeReservation_NOT_FOUND_RESERVATION_INFO() {
        //given
        MakeReservation form = MakeReservation.builder()
                .reservationInfoId(1L)
                .headCount(1)
                .build();

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.makeReservation(1L, form));

        //then
        assertEquals(ErrorCode.NOT_FOUND_RESERVATION_INFO, exception.getErrorCode());
        assertEquals("매장 예약 상세정보가 존재하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void failMakeReservation_ALREADY_MAKE_RESERVATION() {
        //given
        MakeReservation form = MakeReservation.builder()
                .reservationInfoId(1L)
                .headCount(3)
                .reservationDate(LocalDate.parse("2024-07-26"))
                .build();

        Store store = Store.builder()
                .id(1L)
                .name("매장")
                .dates(Arrays.asList(LocalDate.parse("2024-07-26")
                        , LocalDate.parse("2024-03-21")))
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(store)
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();


        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeRepository.existsByIdAndDatesContains(anyLong(),any()))
                .willReturn(true);

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .reservationDate(LocalDate.parse("2024-07-26"))
                .customerId(1L)
                .headCount(3)
                .status(Status.PENDING)
                .visit(false)
                .build();

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(reservationRepository.findByCustomerIdAndStoreReservationInfo(anyLong(), any()))
                .willReturn(Optional.ofNullable(reservation));

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.makeReservation(1L, form));

        //then
        assertEquals(ErrorCode.ALREADY_MAKE_RESERVATION, exception.getErrorCode());
        assertEquals("예약정보가 존재합니다.", exception.getErrorMessage());
    }

    @Test
    void failMakeReservation_LOWER_STORE_MIN_CAPACITY() {
        //given
        MakeReservation form = MakeReservation.builder()
                .reservationInfoId(1L)
                .headCount(0)
                .reservationDate(LocalDate.parse("2024-07-26"))
                .build();

        Store store = Store.builder()
                .id(1L)
                .name("매장")
                .dates(Arrays.asList(LocalDate.parse("2024-07-26")
                        , LocalDate.parse("2024-03-21")))
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(store)
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();


        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeRepository.existsByIdAndDatesContains(anyLong(),any()))
                .willReturn(true);

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.makeReservation(1L, form));

        //then
        assertEquals(ErrorCode.LOWER_STORE_MIN_CAPACITY, exception.getErrorCode());
        assertEquals("예약 가능 인원이 부족합니다.", exception.getErrorMessage());
    }

    @Test
    void failMakeReservation_OVER_STORE_MAX_CAPACITY() {
        //given
        MakeReservation form = MakeReservation.builder()
                .reservationInfoId(1L)
                .headCount(30)
                .reservationDate(LocalDate.parse("2024-07-26"))
                .build();

        Store store = Store.builder()
                .id(1L)
                .name("매장")
                .dates(Arrays.asList(LocalDate.parse("2024-07-26")
                        , LocalDate.parse("2024-03-21")))
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(store)
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();


        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeRepository.existsByIdAndDatesContains(anyLong(),any()))
                .willReturn(true);
        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.makeReservation(1L, form));

        //then
        assertEquals(ErrorCode.OVER_STORE_MAX_CAPACITY, exception.getErrorCode());
        assertEquals("예약 가능인원을 초과하였습니다.", exception.getErrorMessage());
    }

    @Test
    void failMakeReservation_RESERVATION_CLOSED() {
        //given
        MakeReservation form = MakeReservation.builder()
                .reservationInfoId(1L)
                .headCount(3)
                .reservationDate(LocalDate.parse("2024-02-01"))
                .build();

        Store store = Store.builder()
                .id(1L)
                .name("매장")
                .dates(Arrays.asList(LocalDate.parse("2024-02-01")
                        ,LocalDate.parse("2024-02-02")
                        , LocalDate.parse("2024-02-03")))
                .build();

        Map<LocalDate, Integer> closed = new HashMap<>();
        closed.put(LocalDate.parse("2024-02-01"),-1);
        closed.put(LocalDate.parse("2024-02-02"),20);
        closed.put(LocalDate.parse("2024-02-03"),20);
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(store)
                .closed(closed)
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeRepository.existsByIdAndDatesContains(anyLong(),any()))
                .willReturn(true);

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.makeReservation(1L, form));

        //then
        assertEquals(ErrorCode.RESERVATION_CLOSED, exception.getErrorCode());
        assertEquals("예약이 마감되었습니다.", exception.getErrorMessage());
    }
    @Test
    void failMakeReservation_CANNOT_RESERVATION_DATE() {
        //given
        MakeReservation form = MakeReservation.builder()
                .reservationInfoId(1L)
                .headCount(3)
                .reservationDate(LocalDate.parse("2024-07-26"))
                .build();

        Store store = Store.builder()
                .id(1L)
                .name("매장")
                .dates(Arrays.asList(LocalDate.parse("2024-02-21")
                        , LocalDate.parse("2024-03-21")))
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .store(store)
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();


        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeRepository.existsByIdAndDatesContains(anyLong(),any()))
                .willReturn(false);

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.makeReservation(1L, form));

        //then
        assertEquals(ErrorCode.CANNOT_RESERVATION_DATE, exception.getErrorCode());
        assertEquals("예약 가능한 날짜가 아닙니다.", exception.getErrorMessage());
    }
    @Test
    void successChangeReservationStatus_APPROVED() {
        //given
        Map<LocalDate, Integer> closed = new HashMap<>();
        closed.put(LocalDate.parse("2024-02-01"),20);
        closed.put(LocalDate.parse("2024-02-02"),20);
        closed.put(LocalDate.parse("2024-02-03"),20);

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .closed(closed)
                .store(Store.builder().id(1L).name("매장").build())
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .reservationDate(LocalDate.parse("2024-02-01"))
                .customerId(2L)
                .headCount(3)
                .status(Status.PENDING)
                .visit(false)
                .build();

        Store store = Store.builder()
                .id(1L)
                .partnerId(1L)
                .build();

        given(reservationRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));

        ConfirmReservation form = ConfirmReservation.builder()
                .reservationId(1L)
                .status(APPROVED)
                .build();


        //when
        ReservationDto reservationDto = reservationService.changeReservationStatus(1L, form);
        //then
        assertEquals(APPROVED, reservationDto.getStatus());
        assertEquals(Optional.of(17), info.getClosed().get(LocalDate.parse("2024-02-01")));
    }

    @Test
    void successChangeReservationStatus_REJECTED() {
        //given
        Map<LocalDate, Integer> closed = new HashMap<>();
        closed.put(LocalDate.parse("2024-02-01"),20);
        closed.put(LocalDate.parse("2024-02-02"),20);
        closed.put(LocalDate.parse("2024-02-03"),20);

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().id(1L).name("매장").build())
                .closed(closed)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .reservationDate(LocalDate.parse("2024-02-01"))
                .customerId(1L)
                .headCount(3)
                .status(Status.PENDING)
                .visit(false)
                .build();

        Store store = Store.builder()
                .id(1L)
                .partnerId(1L)
                .build();

        given(reservationRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));

        ConfirmReservation form = ConfirmReservation.builder()
                .reservationId(1L)
                .status(REJECTED)
                .build();


        //when
        ReservationDto reservationDto = reservationService.changeReservationStatus(1L, form);

        //then
        assertEquals(REJECTED, reservationDto.getStatus());
        assertEquals(20, info.getClosed().get(LocalDate.parse("2024-02-01")));
    }

    @Test
    void failChangeReservationStatus_OVER_RESERVATION_COUNT() {
        //given

        ConfirmReservation form = ConfirmReservation.builder()
                .reservationId(1L)
                .status(APPROVED)
                .build();

        Map<LocalDate, Integer> closed = new HashMap<>();
        closed.put(LocalDate.parse("2024-02-01"),20);
        closed.put(LocalDate.parse("2024-02-02"),20);
        closed.put(LocalDate.parse("2024-02-03"),20);

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().id(1L).name("매장").build())
                .closed(closed)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(10)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .reservationDate(LocalDate.parse("2024-02-01"))
                .customerId(1L)
                .headCount(20)
                .status(Status.PENDING)
                .visit(false)
                .build();

        Store store = Store.builder()
                .id(1L)
                .partnerId(1L)
                .build();

        given(reservationRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));


        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.changeReservationStatus(1L, form));

        //then
        assertEquals(ErrorCode.OVER_RESERVATION_COUNT, exception.getErrorCode());
        assertEquals("예약 가능 인원을 초과합니다.", exception.getErrorMessage());
    }

    @Test
    void failChangeReservationStatus_UNMATCHED_PARTNER_STORE() {
        //given
        ConfirmReservation form = ConfirmReservation.builder()
                .reservationId(1L)
                .status(APPROVED)
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().id(1L).name("매장").build())
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(20)
                .status(Status.PENDING)
                .visit(false)
                .build();
        given(reservationRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.empty());

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> reservationService.changeReservationStatus(2L, form));

        //then
        assertEquals(ErrorCode.UNMATCHED_PARTNER_STORE, exception.getErrorCode());
        assertEquals("매장 정보와 파트너 정보가 일치하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void failChangeReservationStatus_ALREADY_CHANGE_STATUS_APPROVED() {
        //given
        ConfirmReservation form = ConfirmReservation.builder()
                .reservationId(1L)
                .status(APPROVED)
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().id(1L).name("매장").build())
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(20)
                .status(APPROVED)
                .visit(false)
                .build();
        Store store = Store.builder()
                .id(1L)
                .partnerId(1L)
                .build();

        given(reservationRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));
        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));


        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.changeReservationStatus(1L, form));

        //then
        assertEquals(ErrorCode.ALREADY_CHANGE_STATUS, exception.getErrorCode());
        assertEquals("이미 승인된 예약입니다.", exception.getErrorMessage());
    }

    @Test
    void failChangeReservationStatus_ALREADY_CHANGE_STATUS_REJECTED() {
        //given
        ConfirmReservation form = ConfirmReservation.builder()
                .reservationId(1L)
                .status(APPROVED)
                .build();

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().id(1L).name("매장").build())
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(20)
                .status(REJECTED)
                .visit(false)
                .build();
        Store store = Store.builder()
                .id(1L)
                .partnerId(1L)
                .build();

        given(reservationRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));
        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.changeReservationStatus(1L, form));

        //then
        assertEquals(ErrorCode.ALREADY_CHANGE_STATUS, exception.getErrorCode());
        assertEquals("이미 거절된 예약입니다.", exception.getErrorMessage());
    }

    @Test
    void successCancelReservation_APPROVED() {
        //given
        Map<LocalDate, Integer> closed = new HashMap<>();
        closed.put(LocalDate.parse("2024-02-01"),17);
        closed.put(LocalDate.parse("2024-02-02"),20);
        closed.put(LocalDate.parse("2024-02-03"),20);

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .closed(closed)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .reservationDate(LocalDate.parse("2024-02-01"))
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(false)
                .build();

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));
        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);

        //when
        ReservationDto reservationDto = reservationService.cancelReservation(1L, 1L);

        //then
        verify(reservationRepository, times(1)).delete(captor.capture());
        assertEquals(Optional.of(20), info.getClosed().get(LocalDate.parse("2024-02-01")));
    }

    @Test
    void successCancelReservation_REJECTED() {
        //given
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(17)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(REJECTED)
                .visit(false)
                .build();

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));
        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(info));

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);

        //when
        ReservationDto reservationDto = reservationService.cancelReservation(1L, 1L);

        //then
        verify(reservationRepository, times(1)).delete(captor.capture());
        assertEquals(17, info.getCount());
    }

    @Test
    void failCancelReservation_UNMATCHED_MEMBER_RESERVATION() {
        //given

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.empty());

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.cancelReservation(1L, 1L));

        //then
        assertEquals(ErrorCode.UNMATCHED_MEMBER_RESERVATION, exception.getErrorCode());
        assertEquals("예약 정보와 고객 정보가 일치하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void failCancelReservation_NOT_FOUND_RESERVATION_INFO() {
        //given

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(17)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(REJECTED)
                .visit(false)
                .build();

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));
        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> reservationService.cancelReservation(1L, 1L));

        //then
        assertEquals(ErrorCode.NOT_FOUND_RESERVATION_INFO, exception.getErrorCode());
        assertEquals("매장 예약 상세정보가 존재하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void successVisitReservation() {
        //given
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("17:00"))
                .endAt(LocalTime.parse("18:00"))
                .minCount(1)
                .maxCount(4)
                .count(17)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .reservationDate(LocalDate.parse("2024-07-30"))
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(false)
                .build();
        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        //when
        ReservationDto reservationDto = reservationService.visitReservation(1L, 1L);

        //then
        assertTrue(reservationDto.isVisit());
    }

    @Test
    void failVisitReservation_NOT_FOUND_RESERVATION() {
        //given
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("17:00"))
                .endAt(LocalTime.parse("18:00"))
                .minCount(1)
                .maxCount(4)
                .count(17)
                .build();

        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.empty());

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.visitReservation(1L, 1L));

        //then
        assertEquals(ErrorCode.NOT_FOUND_RESERVATION, exception.getErrorCode());
        assertEquals("예약 정보가 존재하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void failVisitReservation_CHECK_RESERVATION_STATUS_REJECTED() {
        //given
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("17:00"))
                .endAt(LocalTime.parse("18:00"))
                .minCount(1)
                .maxCount(4)
                .count(17)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .reservationDate(LocalDate.parse("2024-07-25"))
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(REJECTED)
                .visit(false)
                .build();
        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.visitReservation(1L, 1L));

        //then
        assertEquals(ErrorCode.CHECK_RESERVATION_STATUS, exception.getErrorCode());
        assertEquals("예약이 거절되었습니다.", exception.getErrorMessage());
    }

    @Test
    void failVisitReservation_CHECK_RESERVATION_STATUS_PENDING() {
        //given
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("17:00"))
                .endAt(LocalTime.parse("18:00"))
                .minCount(1)
                .maxCount(4)
                .count(17)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .reservationDate(LocalDate.parse("2024-07-25"))
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(PENDING)
                .visit(false)
                .build();
        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.visitReservation(1L, 1L));

        //then
        assertEquals(ErrorCode.CHECK_RESERVATION_STATUS, exception.getErrorCode());
        assertEquals("예약이 확인중입니다.", exception.getErrorMessage());
    }

    @Test
    void failVisitReservation_NOT_TODAY_RESERVATION() {
        //given
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("17:00"))
                .endAt(LocalTime.parse("18:00"))
                .minCount(1)
                .maxCount(4)
                .count(17)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .reservationDate(LocalDate.parse("2024-07-26"))
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(false)
                .build();
        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.visitReservation(1L, 1L));

        //then
        assertEquals(ErrorCode.NOT_TODAY_RESERVATION, exception.getErrorCode());
        String errorDescription = String.format("[예약 날짜 : %s] 를 확인해주세요.", reservation.getReservationDate());
        assertEquals(errorDescription, exception.getErrorMessage());
    }

    @Test
    void failVisitReservation_CANNOT_CHECK_YET() {
        //given
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("18:00"))
                .endAt(LocalTime.parse("19:00"))
                .minCount(1)
                .maxCount(4)
                .count(17)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .reservationDate(LocalDate.parse("2024-07-25"))
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(false)
                .build();
        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.visitReservation(1L, 1L));

        //then
        assertEquals(ErrorCode.CANNOT_CHECK_YET, exception.getErrorCode());
        assertEquals("예약 확인은 10분전부터 가능합니다.", exception.getErrorMessage());
    }

    @Test
    void failVisitReservation_OVER_RESERVATION_TIME() {
        //given
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .store(Store.builder().name("매장").build())
                .startAt(LocalTime.parse("17:00"))
                .endAt(LocalTime.parse("19:00"))
                .minCount(1)
                .maxCount(4)
                .count(17)
                .build();

        Reservation reservation = Reservation.builder()
                .id(1L)
                .reservationDate(LocalDate.parse("2024-07-25"))
                .storeReservationInfo(info)
                .customerId(1L)
                .headCount(3)
                .status(APPROVED)
                .visit(false)
                .build();
        given(reservationRepository.findByIdAndCustomerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(reservation));

        //when
        ReservationException exception = assertThrows(ReservationException.class,
                () -> reservationService.visitReservation(1L, 1L));

        //then
        assertEquals(ErrorCode.OVER_RESERVATION_TIME, exception.getErrorCode());
        assertEquals("예약 시간이 지났습니다.", exception.getErrorMessage());
    }

}