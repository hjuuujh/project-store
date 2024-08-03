package com.zerobase.storereservation.service;

import com.zerobase.storereservation.domain.store.dto.StoreDto;
import com.zerobase.storereservation.domain.store.entity.Store;
import com.zerobase.storereservation.domain.store.entity.StoreReservationInfo;
import com.zerobase.storereservation.repository.StoreRepository;
import com.zerobase.storereservation.util.KaKakoApi;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class StoreSearchServiceTest {

    @Mock
    StoreRepository storeRepository;
    @Mock
    KaKakoApi kaKakoApi;
    @InjectMocks
    StoreSearchService storeSearchService;

    @Test
    void successSearchStoreByName() {
        //given
        List<Store> stores = getStores();
        List<Store> result = stores.stream().filter(store -> !store.isDeleted() && store.getName().contains("광화문"))
                .collect(Collectors.toList());
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Store> page = new PageImpl<>(result);
        given(storeRepository.findByNameContainingIgnoreCaseAndDeleted(anyString(), anyBoolean(), any()))
                .willReturn(page);

        //when
        Page<StoreDto> storeDtos = storeSearchService.searchStoreByName("광화문", pageable);

        //then
        assertEquals(2, storeDtos.getTotalElements());
    }


    @Test
    void successSearchStoreAlphabeticalOrder() {
        //given
        List<Store> stores = getStores();
        List<Store> result = stores.stream().sorted(Comparator.comparing(Store::getName))
                .collect(Collectors.toList());
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Store> page = new PageImpl<>(result);
        given(storeRepository.findByDeletedOrderByName(anyBoolean(), any()))
                .willReturn(page);

        //when
        Page<StoreDto> storeDtos = storeSearchService.searchStoreAlphabeticalOrder(pageable);

        //then
        assertEquals("광화문양가", storeDtos.getContent().get(0).getName());
        assertEquals("익선디미방", storeDtos.getContent().get(1).getName());
        assertEquals("차알 광화문 디타워점", storeDtos.getContent().get(2).getName());
    }

    @Test
    void successSearchStoreRatingOrder() {
        //given
        List<Store> stores = getStores();
        List<Store> result = stores.stream().sorted(Comparator.comparing(Store::getRating).reversed())
                .collect(Collectors.toList());
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Store> page = new PageImpl<>(result);
        given(storeRepository.findByDeletedOrderByRatingDesc(anyBoolean(), any()))
                .willReturn(page);

        //when
        Page<StoreDto> storeDtos = storeSearchService.searchStoreRatingOrder(pageable);

        //then
        assertEquals("차알 광화문 디타워점", storeDtos.getContent().get(0).getName());
        assertEquals("익선디미방", storeDtos.getContent().get(1).getName());
        assertEquals("광화문양가", storeDtos.getContent().get(2).getName());
    }

    @Test
    void successSearchStoreDistanceOrder() throws IOException, ParseException {
        //given
        List<Store> stores = getStores();
        given(storeRepository.findByDeleted(anyBoolean()))
                .willReturn(stores);
        List<Float> coordinates = Arrays.asList(20f, 120f);
        given(kaKakoApi.getCoordinateFromApi(anyString()))
                .willReturn(coordinates);
        List<StoreDto> storeByDistance = distance(stores, coordinates);

        PageRequest pageable = PageRequest.of(0, 2);
        int start = (int) pageable.getOffset();
        int end = (int) (start + pageable.getPageSize()) > storeByDistance.size() ? storeByDistance.size() : start + pageable.getPageSize();

        Page<StoreDto> result = new PageImpl<>(storeByDistance.subList(start, end), pageable, storeByDistance.size());

        //when
        Page<StoreDto> storeDtos = storeSearchService.searchStoreDistanceOrder("대구광역시 북구 대학로 80", pageable);

        //then
        assertEquals("차알 광화문 디타워점", storeDtos.getContent().get(0).getName());
        assertEquals("익선디미방", storeDtos.getContent().get(1).getName());
    }

    private List<StoreDto> distance(List<Store> stores, List<Float> coordinates) {
        Map<Store, Double> storeMap = new HashMap<>();
        stores.forEach(store -> {
            float dX = store.getLon() - coordinates.get(0);
            float dY = store.getLat() - coordinates.get(1);
            double distance = Math.sqrt((dX * dX) + (dY * dY));
            storeMap.put(store, distance);
        });

        List<Store> keySetList = new ArrayList<>(storeMap.keySet());
        keySetList.sort(Comparator.comparing(storeMap::get));
        for (Store store : storeMap.keySet()) {
            System.out.println(store.getName() + " " + storeMap.get(store));
        }

        return keySetList.stream().map(StoreDto::from).collect(Collectors.toList());
    }

    private List<Store> getStores() {
        List<Store> stores = new ArrayList<>();
        stores.add(Store.builder()
                .id(1L)
                .partnerId(1L)
                .name("광화문양가")
                .description("한식")
                .address("서울 종로구 종로 19")
                .openAt(LocalTime.parse("11:00"))
                .closeAt(LocalTime.parse("21:40"))
                .storeReservationInfos(new ArrayList<>())
                .lon(35f)
                .lat(127f)
                .rating(3.4f)
                .deleted(false)
                .build());
        stores.get(0).getStoreReservationInfos().add(StoreReservationInfo.builder()
                .id(1L)
                .partnerId(1L)
                .store(stores.get(0))
                .startAt(LocalTime.parse("11:00"))
                .endAt(LocalTime.parse("12:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build());
        stores.get(0).getStoreReservationInfos().add(StoreReservationInfo.builder()
                .id(1L)
                .partnerId(1L)
                .store(stores.get(0))
                .startAt(LocalTime.parse("13:00"))
                .endAt(LocalTime.parse("14:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build());
        stores.get(0).getStoreReservationInfos().add(StoreReservationInfo.builder()
                .id(1L)
                .partnerId(1L)
                .store(stores.get(0))
                .startAt(LocalTime.parse("17:00"))
                .endAt(LocalTime.parse("19:00"))
                .minCount(1)
                .maxCount(4)
                .count(20)
                .build());


        stores.add(Store.builder()
                .id(2L)
                .partnerId(1L)
                .name("차알 광화문 디타워점")
                .description("중식당")
                .address("서울 종로구 종로3길 17")
                .openAt(LocalTime.parse("11:00"))
                .closeAt(LocalTime.parse("21:00"))
                .storeReservationInfos(new ArrayList<>())
                .lon(20f)
                .lat(127f)
                .rating(4.4f)
                .deleted(false)
                .build());

        stores.get(1).getStoreReservationInfos().add(StoreReservationInfo.builder()
                .id(4L)
                .partnerId(1L)
                .store(stores.get(1))
                .startAt(LocalTime.parse("11:00"))
                .endAt(LocalTime.parse("12:00"))
                .minCount(2)
                .maxCount(4)
                .count(10)
                .build());
        stores.get(1).getStoreReservationInfos().add(StoreReservationInfo.builder()
                .id(5L)
                .partnerId(1L)
                .store(stores.get(1))
                .startAt(LocalTime.parse("13:00"))
                .endAt(LocalTime.parse("14:00"))
                .minCount(2)
                .maxCount(4)
                .count(10)
                .build());
        stores.get(1).getStoreReservationInfos().add(StoreReservationInfo.builder()
                .id(6L)
                .partnerId(1L)
                .store(stores.get(1))
                .startAt(LocalTime.parse("17:00"))
                .endAt(LocalTime.parse("19:00"))
                .minCount(2)
                .maxCount(4)
                .count(10)
                .build());


        stores.add(Store.builder()
                .id(3L)
                .partnerId(2L)
                .name("익선디미방")
                .description("양식")
                .address("서울 종로구 돈화문로11나길 30")
                .openAt(LocalTime.parse("11:00"))
                .closeAt(LocalTime.parse("22:00"))
                .storeReservationInfos(new ArrayList<>())
                .lon(25f)
                .lat(127f)
                .rating(3.7f)
                .deleted(false)
                .build());
        stores.get(2).getStoreReservationInfos().add(StoreReservationInfo.builder()
                .id(4L)
                .partnerId(1L)
                .store(stores.get(1))
                .startAt(LocalTime.parse("11:00"))
                .endAt(LocalTime.parse("12:00"))
                .minCount(2)
                .maxCount(4)
                .count(10)
                .build());
        stores.get(2).getStoreReservationInfos().add(StoreReservationInfo.builder()
                .id(5L)
                .partnerId(1L)
                .store(stores.get(1))
                .startAt(LocalTime.parse("13:00"))
                .endAt(LocalTime.parse("14:00"))
                .minCount(2)
                .maxCount(4)
                .count(10)
                .build());
        stores.get(2).getStoreReservationInfos().add(StoreReservationInfo.builder()
                .id(6L)
                .partnerId(1L)
                .store(stores.get(1))
                .startAt(LocalTime.parse("17:00"))
                .endAt(LocalTime.parse("19:00"))
                .minCount(2)
                .maxCount(4)
                .count(10)
                .build());

        return stores;
    }
}