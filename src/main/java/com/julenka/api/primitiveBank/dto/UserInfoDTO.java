package com.julenka.api.primitiveBank.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("Информация о пользователе")
public class UserInfoDTO {

    @NotNull
    @NotEmpty
    @ApiModelProperty("Имя")
    private String firstName;

    @NotNull
    @NotEmpty
    @ApiModelProperty("Фамилия")
    private String lastName;
}
