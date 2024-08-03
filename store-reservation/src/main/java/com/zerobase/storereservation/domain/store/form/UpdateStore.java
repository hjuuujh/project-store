package com.zerobase.storereservation.domain.store.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStore {
        // 매장 수정 위한 form
        private Long id;
        private String name;
        private String description;
        private String address;
        private List<LocalDate> dates;
        private LocalTime openAt;
        private LocalTime closeAt;
        private List<RegisterStoreReservationInfo> infos;

}
