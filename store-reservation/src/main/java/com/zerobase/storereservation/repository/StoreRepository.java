package com.zerobase.storereservation.repository;

import com.zerobase.storereservation.domain.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    boolean existsByName(String name);

    @EntityGraph(attributePaths = {"storeReservationInfos"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<Store> findByNameContainingIgnoreCaseAndDeleted(String name, boolean deleted, Pageable pageable);

    @EntityGraph(attributePaths = {"storeReservationInfos"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<Store> findByDeletedOrderByRatingDesc(boolean deleted, Pageable pageable);


    @EntityGraph(attributePaths = {"storeReservationInfos"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<Store> findByDeletedOrderByName(boolean deleted, Pageable pageable);

    @EntityGraph(attributePaths = {"storeReservationInfos"}, type = EntityGraph.EntityGraphType.LOAD)
    List<Store> findByDeleted(boolean deleted);

    @EntityGraph(attributePaths = {"storeReservationInfos"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<Store> findByPartnerId(Long partnerId, Pageable pageable);

    Optional<Store> findByIdAndPartnerId(Long id, Long partnerId);

    boolean existsByIdAndDatesContains(Long id, LocalDate date);
}
