package com.zerobase.storereservation.service;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zerobase.storereservation.exception.ErrorCode.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreReservationInfoRepository storeReservationInfoRepository;
    private final ReservationRepository reservationRepository;
    private final KaKakoApi kaKakoApi;

    /**
     * 매장 정보 등록
     *
     * @param partnerId
     * @param form      : name, description, address, openAt, closedAt
     * @return : 저장한 매장 정보
     * @throws IOException
     * @throws ParseException
     */
    public StoreDto registerStore(Long partnerId, RegisterStore form) throws IOException, ParseException {

        checkDuplicateStoreName(form.getName());
        checkStoreHours(form.getOpenAt(), form.getCloseAt());

        // 카카오 api 이용해 매장 위치의 위도/경도 알아옴
        List<Float> coordinates = kaKakoApi.getCoordinateFromApi(form.getAddress());

        Store store = Store.of(partnerId, coordinates, form);
        storeRepository.save(store);
        return StoreDto.from(store);
    }

    /**
     * 매장 마감시간이 오픈시간 보다 빠르지 않은 지 확인
     * exception : CHECK_RESERVATION_TIME "매장 운영시간을 확인해주세요."
     *
     * @param openAt
     * @param closeAt
     */
    private void checkStoreHours(LocalTime openAt, LocalTime closeAt) {
        if (openAt.isAfter(closeAt)) {
            String errorDescription = String.format("[매장 오픈시간 : %s, 마감시간 : %s] 를 확인해주세요.", openAt, closeAt);
            throw new StoreException(ErrorCode.CHECK_RESERVATION_TIME, errorDescription);
        }
    }

    /**
     * 매장 이름 중복 확인
     * exception : DUPLICATE_STORE_NAME "매장명은 중복일 수 없습니다."
     * @param name
     */
    private void checkDuplicateStoreName(String name) {
        boolean exists = storeRepository.existsByName(name);

        if (exists) {
            throw new StoreException(ErrorCode.DUPLICATE_STORE_NAME);
        }
    }

    /**
     * 매장 예약 상세정보 등록
     *
     * @param partnerId
     * @param form : storeId, startAt(예약 가능 시작시간), endAt(예약 가능 마감시간), minCount(예약 최소인원), maxCount(예약 최대인원)
     * @return : 저장한 매장 정보
     */
    @Transactional
    public StoreDto addStoreReservationInfo(Long partnerId, List<RegisterStoreReservationInfo> form) {
        List<LocalTime> startAtList = form.stream().map(RegisterStoreReservationInfo::getStartAt).collect(Collectors.toList());
        List<LocalTime> endAtList = form.stream().map(RegisterStoreReservationInfo::getEndAt).collect(Collectors.toList());
        checkReservationInfoTime(startAtList, endAtList);

        // 예약 정보 추가할 매장 정보 가져옴
        Store store = getStore(form.get(0).getStoreId(), partnerId);

        // 매장에 예약정보 추가
        form.forEach(info -> {
            store.getStoreReservationInfos()
                    .add(StoreReservationInfo.of(partnerId, info));
        });

        return StoreDto.from(store);
    }


    /**
     * 매장 예약정보 중 마감시간이 시작시간보다 빠른지 확인
     * 매장 예약정보 중 시작시간이 이전 타임 마감시간보다 빠른지 확인
     * @param startAt
     * @param endAt
     */
    private void checkReservationInfoTime(List<LocalTime> startAt, List<LocalTime> endAt) {

        for (int i = 0; i < startAt.size(); i++) {
            if (startAt.get(i).isAfter(endAt.get(i))) {
                String errorDescription = String.format("[예약 시작시간 : %s, 마감시간 : %s] 를 확인해주세요.", startAt.get(i), endAt.get(i));
                throw new StoreException(ErrorCode.CHECK_RESERVATION_TIME, errorDescription);
            }
        }

        for (int i = 0; i < startAt.size() - 1; i++) {
            if (endAt.get(i).isAfter(startAt.get(i + 1))) {
                String errorDescription = String.format("[예약정보 이전타임 마감시간 : %s, 다음타임 시작시간: %s] 를 확인해주세요.", endAt.get(i), startAt.get(i+1));
                throw new StoreException(ErrorCode.CHECK_RESERVATION_TIME, errorDescription);
            }
        }

    }

    /**
     * 매장 정보 수정
     *
     * @param partnerId
     * @param form      : id, name, description, address, openAt, closedAt, infos(예약 상세 정보), 예약 가능 날짜
     * @return 수정한 매장 정보
     * @throws IOException
     * @throws ParseException
     */
    @Transactional
    public StoreDto updateStore(Long partnerId, UpdateStore form) throws IOException, ParseException {
        checkStoreHours(form.getOpenAt(), form.getCloseAt());

        List<LocalTime> startAtList = form.getInfos().stream().map(RegisterStoreReservationInfo::getStartAt).collect(Collectors.toList());
        List<LocalTime> endAtList = form.getInfos().stream().map(RegisterStoreReservationInfo::getEndAt).collect(Collectors.toList());
        checkReservationInfoTime(startAtList, endAtList);
        checkDuplicateStoreName(form.getName());

        // 수정할 매장 정보 가져옴
        Store store = getStore(form.getId(), partnerId);

        // 카카오 api 이용해 매장 위치의 위도/경도 알아옴
        List<Float> coordinates = kaKakoApi.getCoordinateFromApi(form.getAddress());

        store.update(form, coordinates);

        return StoreDto.from(store);
    }

    /**
     * 매장 예약 가능 날짜 수정
     * @param partnerId
     * @param form
     * @return 수정한 매장 정보
     */
    @Transactional
    public StoreDto updateStoreReservationDate(Long partnerId, UpdateStoreDate form) {
        // 수정할 매장 정보 가져옴
        Store store = getStore(form.getId(), partnerId);

        // 매장의 예약정보 전부 가져옴
        List<StoreReservationInfo> infos = storeReservationInfoRepository.findByStoreId(store.getId());

        // 기존의 예약 가능 날짜
        List<LocalDate> storeDates = store.getDates();

        infos.forEach(info -> {
            Map<LocalDate, Integer> dates = new HashMap<>();
            form.getDates().forEach(date -> {
                if (storeDates.contains(date)) {
                    //  예약 가능하던 날짜면 기존 정보
                    dates.put(date, info.getClosed().get(date));
                } else {
                    // 새로운 날짜면 정보 추가
                    dates.put(date, info.getCount());
                }
            });
            info.updateClosed(dates);
        });

        // 새로운 예약 가능 날짜 업데이트
        store.updateDates(form.getDates());

        return StoreDto.from(store);
    }

    /**
     * 매장 정보 가져옴
     * @param storeId
     * @param partnerId
     * exceotion : UNMATCHED_PARTNER_STORE "매장 정보와 파트너 정보가 일치하지 않습니다."
     * @return
     */
    private Store getStore(Long storeId, Long partnerId) {
        return storeRepository.findByIdAndPartnerId(storeId, partnerId)
                .orElseThrow(() -> new StoreException(ErrorCode.UNMATCHED_PARTNER_STORE));
    }

    /**
     * 매장 정보 삭제 -> deleted field 를 true 로 수정
     * @param partnerId
     * @param id
     * @return
     */
    @Transactional
    public StoreDto deleteStore(Long partnerId, Long id) {
        // 삭제할 매장 정보 가져옴
        Store store = getStore(id, partnerId);

        // 이미 삭제된 매장인 경우 예외 발생 : ALREADY_DELETED_STORE "이미 삭제된 매장입니다."
        if (store.isDeleted()) {
            throw new StoreException(ALREADY_DELETED_STORE);
        }

        // 매장 예약 상세 정보에 매장이 삭제되었다고 수정
        reservationRepository.findByStoreId(store.getId())
                .forEach(reservation -> reservation.changeStatus(Status.STORE_DELETED));

        store.delete(true);

        return StoreDto.builder()
                .deleted(true)
                .build();
    }

    /**
     * 매장 예약 상세정보 수정
     * @param id
     * @param form
     * @return : 수정한 매장 정보
     */
    @Transactional
    public StoreDto updateStoreReservationInfo(Long id, List<UpdateReservationInfo> form) {
        // 수정할 매장 정보 가져옴
        Store store = getStore(form.get(0).getStoreId(), id);

        List<LocalTime> startAtList = form.stream().map(UpdateReservationInfo::getStartAt).collect(Collectors.toList());
        List<LocalTime> endAtList = form.stream().map(UpdateReservationInfo::getEndAt).collect(Collectors.toList());
        checkReservationInfoTime(startAtList, endAtList);

        form.forEach(info -> {
            // 예약 정보가 남아있는 경우 수정 불가능
            StoreReservationInfo storeReservationInfo = checkReservation(info.getId());
            Map<LocalDate, Integer> dates = new HashMap<>();
            if (info.isExist()) {
                // 기존에 있던 정보면 수정
                storeReservationInfo.update(info);
            } else {
                // 없던 정보면 새로 추가
                store.getDates().forEach(date -> {
                    dates.put(date, info.getCount());
                });
                store.getStoreReservationInfos()
                        .add(StoreReservationInfo.of(id, info, dates));
            }
        });

        return StoreDto.from(store);
    }

    /**
     * 예약있으면 삭제, 수정 불가능하게
     * @param infoId
     * @return : 매장예약 상세 정보
     */
    private StoreReservationInfo checkReservation(Long infoId) {
        StoreReservationInfo storeReservationInfo = storeReservationInfoRepository.findById(infoId)
                .orElseThrow(() -> new StoreException(NOT_FOUND_RESERVATION_INFO));

        // 매장 예약 상세정보에 해당하는 예약있는 경우 예외 발생 : STILL_HAVE_RESERVATION "해당 매장에 예약이 남아 있습니다."
        boolean exists = reservationRepository.existsByStoreReservationInfo(storeReservationInfo);
        if (exists) {
            throw new StoreException(STILL_HAVE_RESERVATION);
        }

        return storeReservationInfo;
    }

    /**
     * 매장예약 상세정보 삭제
     * @param partnerId
     * @param form
     * @return : 수정된 매장 정보
     */
    @Transactional
    public StoreDto deleteStoreReservationInfo(Long partnerId, DeleteReservationInfo form) {
        // 삭제할 매장 정보 가져옴
        Store store = getStore(form.getStoreId(), partnerId);

        // 예약정보 남아있는 경우 삭제 불가능
        form.getIds().forEach(this::checkReservation);

        storeReservationInfoRepository.deleteAllByIdIn(form.getIds());
        return StoreDto.from(store);
    }

    /**
     * 매장 예약 마감 정보 수정
     * @param partnerId
     * @param form
     * @return
     */
    @Transactional
    public StoreReservationInfoDto updateStoreReservationClosed(Long partnerId, UpdateReservationClosed form) {
        StoreReservationInfo storeReservationInfo = storeReservationInfoRepository.findByIdAndPartnerId(form.getId(), partnerId)
                .orElseThrow(() -> new StoreException(NOT_FOUND_RESERVATION_INFO));

        // 예약 가능한 날짜가아닌데 수정하려고 시도하는 경우 예외 발생 : CANNOT_UPDATE_INFO "예약이 열려있지 않은 날짜입니다."
        if (!storeReservationInfo.getClosed().containsKey(form.getDate())) {
            throw new StoreException(CANNOT_UPDATE_INFO);
        }

        storeReservationInfo.updateDateClosed(form.getDate(), form.getClosed());
        return StoreReservationInfoDto.from(storeReservationInfo);
    }
}
