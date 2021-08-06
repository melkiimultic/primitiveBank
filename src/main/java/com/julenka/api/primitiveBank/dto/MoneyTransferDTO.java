package com.julenka.api.primitiveBank.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RequiredArgsConstructor
@Data
public class MoneyTransferDTO {

    private Long fromId;
    @NotNull
    @NotEmpty
    private Long toId;
    @NotNull
    @NotEmpty
    private BigDecimal amount;
}
