package com.zerobase.storereservation.repository;

import com.zerobase.storereservation.domain.store.entity.StoreReservationInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreReservationInfoRepository extends JpaRepository<StoreReservationInfo, Long> {
    List<StoreReservationInfo> findByStoreId(Long storeId);
    void deleteAllByIdIn(List<Long> ids);
    List<StoreReservationInfo> findAllByStoreIdAndPartnerId(Long storeId, Long partnerId);
    Optional<StoreReservationInfo> findByIdAndPartnerId(Long id, Long productId);
}
