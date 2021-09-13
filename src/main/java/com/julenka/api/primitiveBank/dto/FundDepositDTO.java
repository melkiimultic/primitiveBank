package com.julenka.api.primitiveBank.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
@ApiModel("Пополнение счёта")
public class FundDepositDTO {

    @ApiModelProperty("ID счёта")
    private Long toId;

    @NotNull
    @NotEmpty
    @ApiModelProperty("Сумма пополнения")
    private BigDecimal amount;
}
