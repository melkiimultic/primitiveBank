package com.julenka.api.primitiveBank.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@RequiredArgsConstructor
@Data
@ApiModel("Перевод средств")
public class MoneyTransferDTO {
    @ApiModelProperty("ID счета отправителя")
    private Long fromId;
    @NotNull
    @NotEmpty
    @ApiModelProperty("ID счета получателя")
    private Long toId;
    @NotNull
    @NotEmpty
    @ApiModelProperty("Сумма перевода")
    private BigDecimal amount;
}
