package com.zerobase.storereservation.repository;

import com.zerobase.storereservation.domain.reservation.entity.Reservation;
import com.zerobase.storereservation.domain.store.entity.StoreReservationInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByCustomerIdAndStoreReservationInfo(Long memberId, StoreReservationInfo storeReservationInfo);

    Optional<Reservation> findByIdAndCustomerId(Long id, Long memberId);
    boolean existsByStoreReservationInfo(StoreReservationInfo info);

    Page<Reservation> findByCustomerId(Long memberId, Pageable pageable);

    Page<Reservation> findByCustomerIdAndStoreId(Long memberId, Long storeId, Pageable pageable);

    Page<Reservation> findByStoreIdAndReservationDate(Long storeId, LocalDate date, Pageable pageable);

    List<Reservation> findByStoreId(Long storeId);

}
