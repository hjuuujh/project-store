package com.zerobase.storereservation.domain.store.form;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterStore {
    // 매장 등록 위한 form
    @NotBlank(message = "매장 이름은 필수입니다.")
    private String name;
    private String description;
    @NotBlank(message = "매장 주소는 필수입니다.")
    private String address;
    @NotNull(message = "매장 오픈 시간은 필수입니다.")
    private LocalTime openAt;
    @NotNull(message = "매장 마감 시간은 필수입니다.")
    private LocalTime closeAt;

}
