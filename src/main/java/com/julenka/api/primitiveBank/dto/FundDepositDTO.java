package com.julenka.api.primitiveBank.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class FundDepositDTO {

    private Long toId;
    @NotNull
    @NotEmpty
    private BigDecimal amount;
}
