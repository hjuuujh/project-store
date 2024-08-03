package com.zerobase.storereservation.controller;

import com.zerobase.storereservation.security.TokenProvider;
import com.zerobase.storereservation.service.StoreSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.ParseException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store/search")
public class StoreSearchController {
    private final StoreSearchService storeSearchService;
    private final TokenProvider tokenProvider;

    /**
     * 키워드가 포함된 매장명을 가진 매장들 리턴
     * @param keyword
     * @param pageable
     * @return : 매장정보 리스트
     */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('PARTNER')")
    public ResponseEntity<?> searchStoreByName(@RequestParam String keyword, final Pageable pageable) {
        return ResponseEntity.ok(storeSearchService.searchStoreByName(keyword, pageable));
    }

    /**
     * 가나다 순으로 매장들 리턴
     * @param pageable
     * @return : 매장정보 리스트
     */
    @GetMapping("/alphabet")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('PARTNER')")
    public ResponseEntity<?> searchStoreAlphabeticalOrder(final Pageable pageable) {
        return ResponseEntity.ok(storeSearchService.searchStoreAlphabeticalOrder(pageable));
    }

    /**
     * 별점순 매장들 리턴
     * @param pageable
     * @return : 매장정보 리스트
     */
    @GetMapping("/rating")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('PARTNER')")
    public ResponseEntity<?> searchStoreRatingOrder(final Pageable pageable) {
        return ResponseEntity.ok(storeSearchService.searchStoreRatingOrder(pageable));
    }

    /**
     * 요청한 주소와 가까운 순으로 매장들 리턴
     * @param address
     * @param pageable
     * @return : 매장정보 리스트
     * @throws IOException
     * @throws ParseException
     */
    @GetMapping("/distance")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('PARTNER')")
    public ResponseEntity<?> searchStoreDistanceOrder(@RequestParam String address, final Pageable pageable) throws IOException, ParseException {
        return ResponseEntity.ok(storeSearchService.searchStoreDistanceOrder(address, pageable));
    }

    /**
     * 파트너 유저가 등록한 매장들 리턴
     * @param token
     * @param pageable
     * @return : 매장정보 리스트
     */
    @GetMapping("/partner")
    @PreAuthorize("hasRole('PARTNER')")
    public ResponseEntity<?> searchStoreByPartner(@RequestHeader(name = "Authorization") String token, final Pageable pageable) {
        return ResponseEntity.ok(storeSearchService.searchStoreByPartner(tokenProvider.getUserIdFromToken(token), pageable));
    }

    /**
     * 특정 매장의 상세정보 리턴
     * @param id
     * @return : 매장 정보
     */
    @GetMapping("/detail")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('PARTNER')")
    public ResponseEntity<?> detailStore(@RequestParam Long id) {
        return ResponseEntity.ok(storeSearchService.detailStore(id));
    }

}
