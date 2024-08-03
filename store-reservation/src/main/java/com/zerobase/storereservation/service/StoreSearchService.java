package com.zerobase.storereservation.service;

import com.zerobase.storereservation.domain.store.dto.StoreDto;
import com.zerobase.storereservation.domain.store.entity.Store;
import com.zerobase.storereservation.exception.ErrorCode;
import com.zerobase.storereservation.exception.StoreException;
import com.zerobase.storereservation.repository.StoreRepository;
import com.zerobase.storereservation.util.KaKakoApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.ParseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StoreSearchService {
    private final StoreRepository storeRepository;
    private final KaKakoApi kaKakoApi;

    /**
     * 키워드가 포함된 매장명을 가진 매장들 리턴
     * @param keyword
     * @param pageable
     * @return
     */
    public Page<StoreDto> searchStoreByName(String keyword, Pageable pageable) {
        // 대문자, 소문자 상광없이 키워드를 포함하고 삭제되지 않은 매장들 가져옴
        return storeRepository.findByNameContainingIgnoreCaseAndDeleted(keyword, false, pageable)
                .map(StoreDto::from);
    }

    /**
     * 가나다 순으로 매장들 리턴
     * @param pageable
     * @return
     */
    public Page<StoreDto> searchStoreAlphabeticalOrder(Pageable pageable) {
        // 삭제되지않은 매장들을 name 으로 오름차순 정렬해 가져옴
        return storeRepository.findByDeletedOrderByName(false, pageable)
                .map(StoreDto::from);

    }

    /**
     * 별점순 매장들 리턴
     * @param pageable
     * @return
     */
    public Page<StoreDto> searchStoreRatingOrder(Pageable pageable) {
        // 삭제되지않은 매장들을 rating 으로 내림차순 정렬해 가져옴
        return storeRepository.findByDeletedOrderByRatingDesc(false, pageable)
                .map(StoreDto::from);
    }

    /**
     * 요청한 주소와 가까운 순으로 매장들 리턴
     * @param address
     * @param pageable
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public Page<StoreDto> searchStoreDistanceOrder(String address, Pageable pageable) throws IOException, ParseException {
        // 삭제되지않은 매장들 가져옴
        List<Store> stores = storeRepository.findByDeleted(false);

        // 요청한 주소의 위도, 경도 구함
        List<Float> coordinates = kaKakoApi.getCoordinateFromApi(address);

        // 위도, 경도 이용한 거리순으로 정렬한 매장정보 가져옴
        List<StoreDto> storeByDistance = distance(stores, coordinates);

        // list 를 page 로 변환
        int start = (int) pageable.getOffset();
        int end = (int) (start + pageable.getPageSize()) > storeByDistance.size() ? storeByDistance.size() : start + pageable.getPageSize();

        return new PageImpl<>(storeByDistance.subList(start, end), pageable, storeByDistance.size());
    }


    /**
     * 위도, 경도 이용해 거리순으로 정렬
     * @param stores
     * @param coordinates
     * @return
     */
    private List<StoreDto> distance(List<Store> stores, List<Float> coordinates) {
        // map을 이용해 store당 거리 정보 저장
        Map<Store, Double> storeMap = new HashMap<>();
        stores.forEach(store -> {
            float dX = store.getLon() - coordinates.get(0);
            float dY = store.getLat() - coordinates.get(1);
            double distance = Math.sqrt((dX * dX) + (dY * dY));
            storeMap.put(store, distance);
        });

        // 거리순으로 매장 정렬
        List<Store> keySetList = new ArrayList<>(storeMap.keySet());
        keySetList.sort(Comparator.comparing(storeMap::get));

        return keySetList.stream().map(StoreDto::from).collect(Collectors.toList());
    }

    /**
     * 파트너 유저가 등록한 매장들 리턴
     * @param partnerId
     * @param pageable
     * @return
     */
    public Page<StoreDto> searchStoreByPartner(Long partnerId, Pageable pageable) {
        return storeRepository.findByPartnerId(partnerId, pageable)
                .map(StoreDto::from);
    }

    /**
     * 특정 매장의 상세정보 리턴
     * @param id
     * @return
     */
    public StoreDto detailStore(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new StoreException(ErrorCode.NOT_FOUND_STORE));
        return StoreDto.from(store);
    }
}

