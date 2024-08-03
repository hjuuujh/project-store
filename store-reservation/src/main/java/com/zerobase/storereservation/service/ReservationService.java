package com.zerobase.storereservation.service;

import com.zerobase.storereservation.domain.reservation.dto.ReservationDto;
import com.zerobase.storereservation.domain.reservation.entity.Reservation;
import com.zerobase.storereservation.domain.reservation.form.ConfirmReservation;
import com.zerobase.storereservation.domain.reservation.form.MakeReservation;
import com.zerobase.storereservation.domain.store.entity.Store;
import com.zerobase.storereservation.domain.store.entity.StoreReservationInfo;
import com.zerobase.storereservation.exception.ReservationException;
import com.zerobase.storereservation.exception.StoreException;
import com.zerobase.storereservation.repository.ReservationRepository;
import com.zerobase.storereservation.repository.StoreRepository;
import com.zerobase.storereservation.repository.StoreReservationInfoRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static com.zerobase.storereservation.domain.reservation.type.Status.*;
import static com.zerobase.storereservation.exception.ErrorCode.*;

@Service
@Slf4j
@AllArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final StoreReservationInfoRepository storeReservationInfoRepository;
    private final StoreRepository storeRepository;


    /**
     * 매장 예약
     * @param id
     * @param form : reservationInfoId, headCount(예약 인원), phone, reservationDate
     * @return 저장된 예약 정보
     */
    public ReservationDto makeReservation(Long id, MakeReservation form) {

        // 예약 상세 정보 가져옴
        StoreReservationInfo storeReservationInfo = storeReservationInfoRepository.findById(form.getReservationInfoId())
                .orElseThrow(() -> new ReservationException(NOT_FOUND_RESERVATION_INFO));

        // 매장 정보 가져옴
        Store store = storeRepository.findById(storeReservationInfo.getStore().getId())
                .orElseThrow(() -> new StoreException(NOT_FOUND_STORE));


        checkReservationDate(form, store);
        checkStoreIsDeleted(storeReservationInfo);
        checkMaked(id, storeReservationInfo, form.getReservationDate());
        checkCount(form.getHeadCount(), storeReservationInfo.getMinCount(), storeReservationInfo.getMaxCount());
        checkReservationIsClosed(storeReservationInfo.getClosed(), form.getReservationDate());

        Reservation reservation = reservationRepository.save(
                Reservation.of(id, form, storeReservationInfo));

        return ReservationDto.from(reservation);
    }

    /**
     * 예약 신청한 날짜가 예약 오픈된 날짜인지 확인
     * @param form
     * @param store
     */
    private void checkReservationDate(MakeReservation form, Store store) {
        // 예약이 오픈되지 않은 날짜에 신청한 경우 예외 발생 : CANNOT_RESERVATION_DATE "예약 가능한 날짜가 아닙니다."
        if (!storeRepository.existsByIdAndDatesContains(store.getId(), form.getReservationDate())) {
            throw new ReservationException(CANNOT_RESERVATION_DATE);
        }
    }

    /**
     * 예약마감되었는데 예약 신청했는지 확인
     * @param closed
     * @param reservationDate
     */
    private void checkReservationIsClosed(Map<LocalDate, Integer> closed, LocalDate reservationDate) {
        // 마감된 정보에(closed의 value가 -1일때) 신청한 경우 예외 발생 : RESERVATION_CLOSED "예약이 마감되었습니다."
        if (closed.get(reservationDate) == -1)
            throw new ReservationException(RESERVATION_CLOSED);
    }

    /**
     * 삭제된 매장에 예약 신청했는지 확인
     * @param storeReservationInfo
     */
    private void checkStoreIsDeleted(StoreReservationInfo storeReservationInfo) {
        // 삭제된 매장에 deleted==true 신청한 경우 예외 발생 : ALREADY_DELETED_STORE "이미 삭제된 매장입니다."
        if (storeReservationInfo.getStore().isDeleted()) {
            throw new ReservationException(ALREADY_DELETED_STORE);
        }
    }

    /**
     * 이미 신청한 예약정보에 중복신청 하는지 확인
     * @param id
     * @param storeReservationInfo
     * @param reservationDate
     */
    private void checkMaked(Long id, StoreReservationInfo storeReservationInfo, LocalDate reservationDate) {
        Optional<Reservation> reservation = reservationRepository.findByCustomerIdAndStoreReservationInfo(id, storeReservationInfo);

        // 예약 정보에 같은 날짜로 중복 신청하는 경우 예외 발생 : ALREADY_MAKE_RESERVATION "예약정보가 존재합니다."
        if (reservation.isPresent() && reservationDate.isEqual(reservation.get().getReservationDate())) {
            throw new ReservationException(ALREADY_MAKE_RESERVATION);
        }
    }

    /**
     * 예약 최소인원보다 적게 신청하거나 최대인원보다 많게 신청했는지 확인
     * @param headCount
     * @param minCount
     * @param maxCount
     */
    private void checkCount(int headCount, int minCount, int maxCount) {
        if (headCount < minCount) {
            // 예약 인원이 최소인원보다 적은 경우 예외 발생 : LOWER_STORE_MIN_CAPACITY "예약 가능 인원이 부족합니다."
            throw new ReservationException(LOWER_STORE_MIN_CAPACITY);
        } else if (headCount > maxCount) {
            // 예약 인원이 최대인원보다 많은 경우 예외 발생 : OVER_STORE_MAX_CAPACITY "예약 가능인원을 초과하였습니다."
            throw new ReservationException(OVER_STORE_MAX_CAPACITY);
        }
    }

    /**
     * 신청된 예약 승인, 거절
     * @param partnerId
     * @param form : reservationId, status(예약/승인)
     * @return 수정한 예약 정보
     */
    @Transactional
    public ReservationDto changeReservationStatus(Long partnerId, ConfirmReservation form) {

        Reservation reservation = reservationRepository.findById(form.getReservationId())
                .orElseThrow(() -> new ReservationException(NOT_FOUND_RESERVATION));

        // 신청한 매장 예약 상세정보 가져옴
        StoreReservationInfo storeReservationInfo = storeReservationInfoRepository.findById(reservation.getStoreReservationInfo().getId())
                .orElseThrow(() -> new ReservationException(NOT_FOUND_RESERVATION_INFO));

        // 본인의 매장에 신청한 예약정보를 수정하려고 하는 건지 확인
        Long storeId = storeReservationInfo.getStore().getId();
        storeRepository.findByIdAndPartnerId(storeId, partnerId)
                .orElseThrow(() -> new StoreException(UNMATCHED_PARTNER_STORE));

        checkReservationStatus(reservation);
        if (APPROVED.equals(form.getStatus())) {
            checkCanReservationCount(reservation.getHeadCount(), storeReservationInfo.getCount());
            // 해당날짜의 신청한 예약인원만큼 잔여 인원 감소
            storeReservationInfo.decreaseCount(reservation.getReservationDate(), reservation.getHeadCount());
        }

        reservation.changeStatus(form.getStatus());

        return ReservationDto.from(reservation);
    }

    /**
     * 이미 예약 상태를 변경했는지 확인
     * @param reservation
     */
    private void checkReservationStatus(Reservation reservation) {
        if (APPROVED.equals(reservation.getStatus()))
            // 이미 승인한 예약인 경우 예외 발생 : ALREADY_CHANGE_STATUS "이미 승인된 예약입니다."
            throw new ReservationException(ALREADY_CHANGE_STATUS, "이미 승인된 예약입니다.");
        else if (REJECTED.equals(reservation.getStatus())) {
            // 이미 거절한 예약인 경우 예외 발생 : ALREADY_CHANGE_STATUS "이미 거절된 예약입니다."
            throw new ReservationException(ALREADY_CHANGE_STATUS, "이미 거절된 예약입니다.");
        }
    }

    /**
     * 예약 가능한 잔여 인원을 신청인원이 초과하는지 확인
     * @param headCount
     * @param count
     */
    private void checkCanReservationCount(int headCount, int count) {
        if (headCount > count) {
            // 신청인원이 예약 가능 잔여인원보다 많은 경우 예외 발생 : OVER_RESERVATION_COUNT "예약 가능 인원을 초과합니다."
            throw new ReservationException(OVER_RESERVATION_COUNT);
        }
    }

    /**
     * 고객이 자신이 예약한 리스트 확인
     * @param memberId
     * @param pageable
     * @return 예약 리스트
     */
    public Page<ReservationDto> searchReservationByMember(Long memberId, Pageable pageable) {

        return reservationRepository.findByCustomerId(memberId, pageable)
                .map(ReservationDto::from);
    }

    /**
     * 고객이 특정 매장에 예약한 리스트 확인
     * @param memberId
     * @param storeId
     * @param pageable
     * @return 예약 리스트
     */
    public Page<ReservationDto> searchReservationByMemberWithStore(Long memberId, Long storeId, Pageable pageable) {
        return reservationRepository.findByCustomerIdAndStoreId(memberId, storeId, pageable)
                .map(ReservationDto::from);
    }

    /**
     * 파트너가 자신의 특정 매장 예약 리스트 확인
     * @param partnerId
     * @param storeId
     * @param date
     * @param pageable
     * @return 예약 리스트
     */
    public Page<ReservationDto> searchReservationByPartner(Long partnerId, Long storeId, LocalDate date, Pageable pageable) {
        // 자신의 매장의 예약리스트를 보는지 확인하기위해
        // 바로 store id를 이용하지 않고
        // partnerId와 매장명을 이용해 받아온 store의 id를 이용
        // 예외가 발생하면 요청한 유저의 매장이 아닌것
        Store store = storeRepository.findByIdAndPartnerId(storeId, partnerId)
                .orElseThrow(() -> new StoreException(UNMATCHED_PARTNER_STORE));
        return reservationRepository.findByStoreIdAndReservationDate(store.getId(), date, pageable)
                .map(ReservationDto::from);
    }

    /**
     * 매장 예약 취소
     * @param customerId
     * @param id
     * @return 취소한 예약 정보
     */
    @Transactional
    public ReservationDto cancelReservation(Long customerId, Long id) {
        // 취소하려는 예약이 본인이 신청한 예약인지 확인
        Reservation reservation = reservationRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ReservationException(UNMATCHED_MEMBER_RESERVATION));

        Long infoId = reservation.getStoreReservationInfo().getId();
        StoreReservationInfo storeReservationInfo = storeReservationInfoRepository.findById(infoId)
                .orElseThrow(() -> new StoreException(NOT_FOUND_RESERVATION_INFO));

        // 예약가능 잔여인원을 취소한 인원만큼 증가
        if (APPROVED.equals(reservation.getStatus())) {
            storeReservationInfo.increaseCount(reservation.getReservationDate(), reservation.getHeadCount());
        }

        reservationRepository.delete(reservation);
        return ReservationDto.from(reservation);
    }

    /**
     * 매장 방문 확인
     * @param customerId
     * @param id
     * @return : 수정한 예약 정보
     */
    @Transactional
    public ReservationDto visitReservation(Long customerId, Long id) {
        // 취소하려는 예약이 본인이 신청한 예약인지 확인
        Reservation reservation = reservationRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ReservationException(NOT_FOUND_RESERVATION));

        if (REJECTED.equals(reservation.getStatus())) {
            // 예약이 거절당한 경우 예외 발생 : CHECK_RESERVATION_STATUS "예약이 거절되었습니다."
            throw new ReservationException(CHECK_RESERVATION_STATUS, "예약이 거절되었습니다.");
        } else if (PENDING.equals(reservation.getStatus())) {
            // 예약이 확정되지 않은 경우  예외 발생 : CHECK_RESERVATION_STATUS "예약이 확인중입니다."
            throw new ReservationException(CHECK_RESERVATION_STATUS, "예약이 확인중입니다.");
        }

        checkReservationDate(reservation);
        checkCanInStore(reservation);

        reservation.changeVisited(true);

        return ReservationDto.from(reservation);
    }

    /**
     * 예약한 시간 10분전 보다 방문 확인하는지 확인
     * 예약시간을 지나 방문 확인하는지 확인
     * @param reservation
     */
    private void checkCanInStore(Reservation reservation) {
        LocalTime canCheckTime = reservation.getStoreReservationInfo().getStartAt();

        // 예약시간 10분전보다 일찍 방문확인 하는 경우 예외 발생 : CANNOT_CHECK_YET "[예약 시간 : %s , 현재 시간 : %s] 방문 확인은 10분전부터 가능합니다.", canCheckTime, LocalTime.now()
        if (LocalTime.now().isBefore(canCheckTime.minusMinutes(10))) {
            String errorDescription = String.format("[예약 시간 : %s , 현재 시간 : %s] 방문 확인은 10분전부터 가능합니다.", canCheckTime, LocalTime.now());
            throw new ReservationException(CANNOT_CHECK_YET, errorDescription);
        }

        // 예약 시간을 지나 방문 확인하는 경우 예외 발생 : OVER_RESERVATION_TIME "[예약 시간 : %s , 현재 시간 : %s] 예약 시간이 지났습니다.", canCheckTime, LocalTime.now()
        if (LocalTime.now().isAfter(canCheckTime)) {
            String errorDescription = String.format("[예약 시간 : %s , 현재 시간 : %s] 예약 시간이 지났습니다.", canCheckTime, LocalTime.now());
            throw new ReservationException(OVER_RESERVATION_TIME, errorDescription);
        }
    }

    /**
     * 예약한날 방문 확인하는지 확인
     * @param reservation
     */
    private void checkReservationDate(Reservation reservation) {
        // 예약한 날이 아닌 다른날 방문 확인 하는 경우 예외 발생 "[예약 날짜 : %s] 를 확인해주세요.", reservation.getReservationDate()
        if (!LocalDate.now().isEqual(reservation.getReservationDate())) {
            String errorDescription = String.format("[예약 날짜 : %s] 를 확인해주세요.", reservation.getReservationDate());
            throw new ReservationException(NOT_TODAY_RESERVATION, errorDescription);
        }
    }
}
