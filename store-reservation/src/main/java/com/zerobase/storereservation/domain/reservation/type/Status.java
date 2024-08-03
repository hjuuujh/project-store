package com.zerobase.storereservation.domain.reservation.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Status {
    // 예약 상태 enum 타입으로 관리
    PENDING("pending"), // 대기
    APPROVED("approved"), // 승인
    REJECTED("rejected"), // 거절
    STORE_DELETED("storeDeleted"); // 매장삭제됨

    private final String value;

    Status(String value) {
        this.value = value;
    }

    @JsonCreator
    public static Status of(String status) {
        return Arrays.stream(Status.values())
                .filter(i -> i.value.equals(status))
                .findAny()
                .orElse(null);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
