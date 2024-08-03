package com.zerobase.storereservation.service;

import com.zerobase.storereservation.domain.reservation.entity.Reservation;
import com.zerobase.storereservation.domain.reservation.type.Status;
import com.zerobase.storereservation.domain.store.dto.StoreDto;
import com.zerobase.storereservation.domain.store.dto.StoreReservationInfoDto;
import com.zerobase.storereservation.domain.store.entity.Store;
import com.zerobase.storereservation.domain.store.entity.StoreReservationInfo;
import com.zerobase.storereservation.domain.store.form.*;
import com.zerobase.storereservation.exception.ErrorCode;
import com.zerobase.storereservation.exception.StoreException;
import com.zerobase.storereservation.repository.ReservationRepository;
import com.zerobase.storereservation.repository.StoreRepository;
import com.zerobase.storereservation.repository.StoreReservationInfoRepository;
import com.zerobase.storereservation.util.KaKakoApi;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private StoreReservationInfoRepository storeReservationInfoRepository;
    @Mock
    private KaKakoApi kaKakoApi;
    @InjectMocks
    private StoreService storeService;

    @Test
    void successRegisterStore() throws IOException, ParseException {
        //given
        RegisterStore form = RegisterStore.builder()
                .name("매장1")
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .openAt(LocalTime.parse("12:00"))
                .closeAt(LocalTime.parse("14:00"))
                .build();

        given(storeRepository.existsByName(anyString()))
                .willReturn(false);

        List<Float> coordinate = Arrays.asList(128.614f, 35.8891f);
        given(kaKakoApi.getCoordinateFromApi(anyString()))
                .willReturn(coordinate);

        ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);

        //when
        StoreDto storeDto = storeService.registerStore(1L, form);

        //then
        verify(storeRepository, times(1)).save(captor.capture());
        assertEquals("매장1", captor.getValue().getName());
        assertEquals("첫번째 매장", captor.getValue().getDescription());
        assertEquals("대구광역시 북구 대학로 80", captor.getValue().getAddress());
        assertEquals(LocalTime.parse("12:00"), captor.getValue().getOpenAt());
        assertEquals(LocalTime.parse("14:00"), captor.getValue().getCloseAt());
        assertEquals(35.8891f, captor.getValue().getLat());
        assertEquals(128.614f, captor.getValue().getLon());
    }

    @Test
    void failRegisterStore_DUPLICATE_STORE_NAME() throws IOException, ParseException {
        //given
        RegisterStore form = RegisterStore.builder()
                .name("매장1")
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .openAt(LocalTime.parse("12:00"))
                .closeAt(LocalTime.parse("14:00"))
                .build();

        given(storeRepository.existsByName(anyString()))
                .willReturn(true);

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.registerStore(1L, form));

        //then
        assertEquals(ErrorCode.DUPLICATE_STORE_NAME, exception.getErrorCode());
        assertEquals("매장명은 중복일 수 없습니다.", exception.getErrorMessage());
    }

    @Test
    void failRegisterStore_CHECK_RESERVATION_TIME() throws IOException, ParseException {
        //given
        RegisterStore form = RegisterStore.builder()
                .name("매장1")
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .openAt(LocalTime.parse("15:00"))
                .closeAt(LocalTime.parse("14:00"))
                .build();

        given(storeRepository.existsByName(anyString()))
                .willReturn(false);

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.registerStore(1L, form));

        //then
        assertEquals(ErrorCode.CHECK_RESERVATION_TIME, exception.getErrorCode());
        assertEquals("[매장 오픈시간 : 15:00, 마감시간 : 14:00] 를 확인해주세요.", exception.getErrorMessage());
    }

    @Test
    void successAddStoreReservationInfo() {
        //given
        List<RegisterStoreReservationInfo> form = infoRequestForm();

        Store store = Store.builder()
                .name("매장1")
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .openAt(LocalTime.parse("15:00"))
                .closeAt(LocalTime.parse("14:00"))
                .storeReservationInfos(new ArrayList<>())
                .build();

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));


        //when
        StoreDto storeDto = storeService.addStoreReservationInfo(1L, form);

        //then
        assertEquals(1L, storeDto.getStoreReservationInfos().get(0).getPartnerId());
        assertEquals(LocalTime.parse("10:00"), storeDto.getStoreReservationInfos().get(0).getStartAt());
        assertEquals(LocalTime.parse("11:00"), storeDto.getStoreReservationInfos().get(0).getEndAt());
        assertEquals(1, storeDto.getStoreReservationInfos().get(0).getMinCount());
        assertEquals(4, storeDto.getStoreReservationInfos().get(0).getMaxCount());
        assertEquals(20, storeDto.getStoreReservationInfos().get(0).getCount());
    }

    @Test
    void failAddStoreReservationInfo_NOT_FOUND_STORE() {
        //given
        List<RegisterStoreReservationInfo> form = infoRequestForm();

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.empty());
        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.addStoreReservationInfo(1L, form));

        //then
        assertEquals(ErrorCode.UNMATCHED_PARTNER_STORE, exception.getErrorCode());
        assertEquals("매장 정보와 파트너 정보가 일치하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void failAddStoreReservationInfo_CHECK_RESERVATION_TIME() {
        //given
        List<RegisterStoreReservationInfo> form = infoRequestForm();
        form.get(0).setStartAt(LocalTime.parse("12:00"));

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.addStoreReservationInfo(1L, form));

        //then
        assertEquals(ErrorCode.CHECK_RESERVATION_TIME, exception.getErrorCode());
        assertEquals("[예약 시작시간 : 12:00, 마감시간 : 11:00] 를 확인해주세요.", exception.getErrorMessage());
    }

    @Test
    void failAddStoreReservationInfo_CHECK_RESERVATION_INFO_TIME() {
        //given
        List<RegisterStoreReservationInfo> form = infoRequestForm();

        form.get(1).setStartAt(LocalTime.parse("10:30"));

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.addStoreReservationInfo(1L, form));

        //then
        assertEquals(ErrorCode.CHECK_RESERVATION_TIME, exception.getErrorCode());
        assertEquals("[예약정보 이전타임 마감시간 : 11:00, 다음타임 시작시간: 10:30] 를 확인해주세요.", exception.getErrorMessage());
    }


    @Test
    void successUpdateStore() throws IOException, ParseException {
        //given
        Store store = Store.builder()
                .name("매장1")
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .openAt(LocalTime.parse("13:00"))
                .closeAt(LocalTime.parse("14:00"))
                .storeReservationInfos(getInfos())
                .build();

        UpdateStore form = UpdateStore.builder()
                .id(1L)
                .name("매장 이름 수정")
                .description("매장 정보 수정")
                .address("대구광역시 북구 대학로 85")
                .openAt(LocalTime.parse("10:00"))
                .closeAt(LocalTime.parse("14:00"))
                .infos(reservationInfos())
                .build();

        given(storeRepository.findByIdAndPartnerId(any(), anyLong()))
                .willReturn(Optional.ofNullable(store));

        List<Float> coordinate = Arrays.asList(128.614f, 35.8891f);
        given(kaKakoApi.getCoordinateFromApi(anyString()))
                .willReturn(coordinate);

        //when
        StoreDto storeDto = storeService.updateStore(1L, form);

        //then
        assertEquals("매장 이름 수정", storeDto.getName());
        assertEquals("매장 정보 수정", storeDto.getDescription());
        assertEquals("대구광역시 북구 대학로 85", storeDto.getAddress());
        assertEquals(LocalTime.parse("10:00"), storeDto.getOpenAt());
        assertEquals(LocalTime.parse("14:00"), storeDto.getCloseAt());
        assertEquals(128.614f, storeDto.getLon());
        assertEquals(35.8891f, storeDto.getLat());
        assertEquals(LocalTime.parse("10:00"), storeDto.getStoreReservationInfos().get(0).getStartAt());
        assertEquals(LocalTime.parse("11:00"), storeDto.getStoreReservationInfos().get(0).getEndAt());
        assertEquals(1, storeDto.getStoreReservationInfos().get(0).getMinCount());
        assertEquals(4, storeDto.getStoreReservationInfos().get(0).getMaxCount());
        assertEquals(20, storeDto.getStoreReservationInfos().get(0).getCount());
    }

    @Test
    void failUpdateStore_NOT_FOUND_STORE() {
        //given
        UpdateStore form = UpdateStore.builder()
                .id(1L)
                .name("매장 이름 수정")
                .description("매장 정보 수정")
                .address("대구광역시 북구 대학로 85")
                .openAt(LocalTime.parse("10:00"))
                .closeAt(LocalTime.parse("14:00"))
                .infos(reservationInfos())
                .build();

        given(storeRepository.findByIdAndPartnerId(any(), anyLong()))
                .willReturn(Optional.empty());
        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.updateStore(1L, form));

        //then
        assertEquals(ErrorCode.UNMATCHED_PARTNER_STORE, exception.getErrorCode());
        assertEquals("매장 정보와 파트너 정보가 일치하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void failUpdateStore_CHECK_STORE_HOUSRS() {
        //given
        UpdateStore form = UpdateStore.builder()
                .id(1L)
                .name("매장 이름 수정")
                .description("매장 정보 수정")
                .address("대구광역시 북구 대학로 85")
                .openAt(LocalTime.parse("15:00"))
                .closeAt(LocalTime.parse("14:00"))
                .infos(infoRequestForm())
                .build();

        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.updateStore(1L, form));

        //then
        assertEquals(ErrorCode.CHECK_RESERVATION_TIME, exception.getErrorCode());
        assertEquals("[매장 오픈시간 : 15:00, 마감시간 : 14:00] 를 확인해주세요.", exception.getErrorMessage());

    }

    @Test
    void failUpdateStore_CHECK_RESERVATION_TIME() {
        //given
        List<RegisterStoreReservationInfo> requests = reservationInfos();
        requests.get(1).setStartAt(LocalTime.parse("15:00"));
        UpdateStore form = UpdateStore.builder()
                .id(1L)
                .name("매장 이름 수정")
                .description("매장 정보 수정")
                .address("대구광역시 북구 대학로 85")
                .openAt(LocalTime.parse("10:00"))
                .closeAt(LocalTime.parse("14:00"))
                .infos(requests)
                .build();

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.updateStore(1L, form));

        //then
        assertEquals(ErrorCode.CHECK_RESERVATION_TIME, exception.getErrorCode());
        assertEquals("[예약 시작시간 : 15:00, 마감시간 : 12:00] 를 확인해주세요.", exception.getErrorMessage());
    }

    @Test
    void failUpdateStore_CHECK_RESERVATION_INFO_TIME() {
        //given
        List<RegisterStoreReservationInfo> requests = reservationInfos();
        requests.get(1).setStartAt(LocalTime.parse("09:30"));
        UpdateStore form = UpdateStore.builder()
                .id(1L)
                .name("매장 이름 수정")
                .description("매장 정보 수정")
                .address("대구광역시 북구 대학로 85")
                .openAt(LocalTime.parse("10:00"))
                .closeAt(LocalTime.parse("14:00"))
                .infos(requests)
                .build();

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.updateStore(1L, form));

        //then
        assertEquals(ErrorCode.CHECK_RESERVATION_TIME, exception.getErrorCode());
        assertEquals("[예약정보 이전타임 마감시간 : 10:00, 다음타임 시작시간: 09:30] 를 확인해주세요.", exception.getErrorMessage());

    }

    @Test
    void successDeleteStore() {
        //given
        Store store = Store.builder()
                .id(1L)
                .partnerId(1L)
                .name("매장1")
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .openAt(LocalTime.parse("13:00"))
                .closeAt(LocalTime.parse("14:00"))
                .build();
        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
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
                .customerId(1L)
                .headCount(3)
                .status(Status.PENDING)
                .visit(false)
                .build();
        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));
        given(reservationRepository.findByStoreId(anyLong()))
                .willReturn(Arrays.asList(reservation));

        //when
        StoreDto storeDto = storeService.deleteStore(1L, 1L);

        //then
        assertEquals(true, storeDto.isDeleted());
    }

    @Test
    void failDeleteStore_NOT_FOUND_STORE() {
        //given
        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.empty());

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.deleteStore(1L, 1L));

        //then
        assertEquals(ErrorCode.UNMATCHED_PARTNER_STORE, exception.getErrorCode());
        assertEquals("매장 정보와 파트너 정보가 일치하지 않습니다.", exception.getErrorMessage());
    }

    @Test
    void failDeleteStore_ALREADY_DELETED_STORE() {
        //given

        Store store = Store.builder()
                .id(1L)
                .name("매장1")
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .openAt(LocalTime.parse("13:00"))
                .closeAt(LocalTime.parse("14:00"))
                .deleted(true)
                .build();

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.deleteStore(1L, 1L));

        //then
        assertEquals(ErrorCode.ALREADY_DELETED_STORE, exception.getErrorCode());
        assertEquals("이미 삭제된 매장입니다.", exception.getErrorMessage());
    }

    @Test
    void successUpdateStoreReservationInfo() {
        //given
        Store store = Store.builder()
                .id(1L)
                .name("매장1")
                .storeReservationInfos(getInfos())
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .dates(Arrays.asList(LocalDate.parse("2024-07-01"), LocalDate.parse("2024-07-02")))
                .openAt(LocalTime.parse("13:00"))
                .closeAt(LocalTime.parse("14:00"))
                .build();

        List<UpdateReservationInfo> form = new ArrayList<>();
        form.add(UpdateReservationInfo.builder()
                .id(1L)
                .storeId(1L)
                .startAt(LocalTime.parse("10:00"))
                .endAt(LocalTime.parse("11:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .exist(true)
                .build());
        form.add(UpdateReservationInfo.builder()
                .id(2L)
                .storeId(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .exist(false)
                .build());

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(getInfos().get(0)))
                .willReturn(Optional.ofNullable(getInfos().get(1)))
                .willReturn(Optional.ofNullable(getInfos().get(2)));


        //when
        StoreDto storeDto = storeService.updateStoreReservationInfo(1L, form);

        //then
        assertEquals(4, storeDto.getStoreReservationInfos().size());
    }

    @Test
    void failUpdateStoreReservationInfo_STILL_HAVE_RESERVATION() {
        //given
        Store store = Store.builder()
                .id(1L)
                .name("매장1")
                .storeReservationInfos(getInfos())
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .dates(Arrays.asList(LocalDate.parse("2024-07-01"), LocalDate.parse("2024-07-02")))
                .openAt(LocalTime.parse("13:00"))
                .closeAt(LocalTime.parse("14:00"))
                .build();

        List<UpdateReservationInfo> form = new ArrayList<>();
        form.add(UpdateReservationInfo.builder()
                .id(1L)
                .storeId(1L)
                .startAt(LocalTime.parse("10:00"))
                .endAt(LocalTime.parse("11:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .exist(true)
                .build());
        form.add(UpdateReservationInfo.builder()
                .id(2L)
                .storeId(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .exist(false)
                .build());

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(getInfos().get(0)));

        given(reservationRepository.existsByStoreReservationInfo(any()))
                .willReturn(true);

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.updateStoreReservationInfo(1L, form));

        //then
        assertEquals(ErrorCode.STILL_HAVE_RESERVATION, exception.getErrorCode());
        assertEquals("해당 매장에 예약이 남아 있습니다.", exception.getErrorMessage());
    }

    @Test
    void successDeleteStoreReservationInfo(){
        //given
        Store store = Store.builder()
                .id(1L)
                .name("매장1")
                .storeReservationInfos(getInfos())
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .dates(Arrays.asList(LocalDate.parse("2024-07-01"), LocalDate.parse("2024-07-02")))
                .openAt(LocalTime.parse("13:00"))
                .closeAt(LocalTime.parse("14:00"))
                .build();

        DeleteReservationInfo form = DeleteReservationInfo.builder()
                .ids(Arrays.asList(2L,3L))
                .storeId(1L)
                .build();

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(getInfos().get(0)))
                .willReturn(Optional.ofNullable(getInfos().get(1)))
                .willReturn(Optional.ofNullable(getInfos().get(2)));


        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);

        //when
        StoreDto storeDto = storeService.deleteStoreReservationInfo(1L, form);

        //then
        verify(storeReservationInfoRepository, times(1)).deleteAllByIdIn(captor.capture());
    }

    @Test
    void failDeleteStoreReservationInfo_STILL_HAVE_RESERVATION(){
        //given
        Store store = Store.builder()
                .id(1L)
                .name("매장1")
                .storeReservationInfos(getInfos())
                .description("첫번째 매장")
                .address("대구광역시 북구 대학로 80")
                .dates(Arrays.asList(LocalDate.parse("2024-07-01"), LocalDate.parse("2024-07-02")))
                .openAt(LocalTime.parse("13:00"))
                .closeAt(LocalTime.parse("14:00"))
                .build();

        DeleteReservationInfo form = DeleteReservationInfo.builder()
                .ids(Arrays.asList(2L,3L))
                .storeId(1L)
                .build();

        given(storeRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(store));

        given(storeReservationInfoRepository.findById(anyLong()))
                .willReturn(Optional.ofNullable(getInfos().get(0)))
                .willReturn(Optional.ofNullable(getInfos().get(1)))
                .willReturn(Optional.ofNullable(getInfos().get(2)));

        given(reservationRepository.existsByStoreReservationInfo(any()))
                .willReturn(true);

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.deleteStoreReservationInfo(1L, form));

        //then
        assertEquals(ErrorCode.STILL_HAVE_RESERVATION, exception.getErrorCode());
        assertEquals("해당 매장에 예약이 남아 있습니다.", exception.getErrorMessage());
    }

    @Test
    void successUpdateStoreReservationClosed(){
        //given
        UpdateReservationClosed form = UpdateReservationClosed.builder()
                .id(1L)
                .date(LocalDate.parse("2024-07-01"))
                .closed(-1)
                .build();

        Map<LocalDate, Integer> closed = new HashMap<>();
        closed.put(LocalDate.parse("2024-07-01"),20);
        closed.put(LocalDate.parse("2024-07-02"),20);
        closed.put(LocalDate.parse("2024-07-03"),20);

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .partnerId(1L)
                .startAt(LocalTime.parse("10:00"))
                .endAt(LocalTime.parse("11:00"))
                .minCount(1)
                .maxCount(4)
                .closed(closed)
                .count(20)
                .build();

        given(storeReservationInfoRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(info));

        //when
        StoreReservationInfoDto storeReservationInfoDto = storeService.updateStoreReservationClosed(1L, form);

        //then
        assertEquals(-1, storeReservationInfoDto.getClosed().get(LocalDate.parse("2024-07-01")));
    }

    @Test
    void failUpdateStoreReservationClosed_CANNOT_UPDATE_INFO(){
        //given
        UpdateReservationClosed form = UpdateReservationClosed.builder()
                .id(1L)
                .date(LocalDate.parse("2024-07-20"))
                .closed(-1)
                .build();

        Map<LocalDate, Integer> closed = new HashMap<>();
        closed.put(LocalDate.parse("2024-07-01"),20);
        closed.put(LocalDate.parse("2024-07-02"),20);
        closed.put(LocalDate.parse("2024-07-03"),20);

        StoreReservationInfo info = StoreReservationInfo.builder()
                .id(1L)
                .partnerId(1L)
                .startAt(LocalTime.parse("10:00"))
                .endAt(LocalTime.parse("11:00"))
                .minCount(1)
                .maxCount(4)
                .closed(closed)
                .count(20)
                .build();

        given(storeReservationInfoRepository.findByIdAndPartnerId(anyLong(), anyLong()))
                .willReturn(Optional.ofNullable(info));

        //when
        StoreException exception = assertThrows(StoreException.class,
                () -> storeService.updateStoreReservationClosed(1L, form));

        //then
        assertEquals(ErrorCode.CANNOT_UPDATE_INFO, exception.getErrorCode());
        assertEquals("예약이 열려있지 않은 날짜입니다.", exception.getErrorMessage());
    }

    private List<StoreReservationInfo> getInfos() {
        List<StoreReservationInfo> info = new ArrayList<>();
        info.add(StoreReservationInfo.builder()
                .id(1L)
                .startAt(LocalTime.parse("10:00"))
                .endAt(LocalTime.parse("11:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build());
        info.add(StoreReservationInfo.builder()
                .id(2L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build());
        info.add(StoreReservationInfo.builder()
                .id(3L)
                .startAt(LocalTime.parse("14:00"))
                .endAt(LocalTime.parse("15:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build());

        return info;
    }

    private List<RegisterStoreReservationInfo> infoRequestForm() {
        List<RegisterStoreReservationInfo> form = new ArrayList<>();
        form.add(RegisterStoreReservationInfo.builder()
                .storeId(1L)
                .startAt(LocalTime.parse("10:00"))
                .endAt(LocalTime.parse("11:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build());
        form.add(RegisterStoreReservationInfo.builder()
                .storeId(1L)
                .startAt(LocalTime.parse("12:00"))
                .endAt(LocalTime.parse("13:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build());
        form.add(RegisterStoreReservationInfo.builder()
                .storeId(1L)
                .startAt(LocalTime.parse("14:00"))
                .endAt(LocalTime.parse("15:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build());

        return form;
    }

    private List<RegisterStoreReservationInfo> reservationInfos() {
        List<RegisterStoreReservationInfo> form = new ArrayList<>();
        form.add(RegisterStoreReservationInfo.builder()
                .startAt(LocalTime.parse("09:00"))
                .endAt(LocalTime.parse("10:00"))
                .minCount(1)
                .maxCount(5)
                .count(50)
                .build());
        form.add(RegisterStoreReservationInfo.builder()
                .startAt(LocalTime.parse("11:00"))
                .endAt(LocalTime.parse("12:00"))
                .minCount(1)
                .maxCount(5)
                .count(50)
                .build());
        form.add(RegisterStoreReservationInfo.builder()
                .startAt(LocalTime.parse("13:00"))
                .endAt(LocalTime.parse("14:00"))
                .minCount(1)
                .maxCount(5)
                .count(50)
                .build());

        return form;
    }
}