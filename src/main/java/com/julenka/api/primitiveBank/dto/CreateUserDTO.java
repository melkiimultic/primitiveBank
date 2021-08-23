package com.julenka.api.primitiveBank.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ApiModel("Создание пользователя")
public class CreateUserDTO {
    @NotNull
    @NotEmpty
    @ApiModelProperty("Логин пользователя")
    private String username;
    @NotNull
    @NotEmpty
    @ApiModelProperty("Пароль пользователя")
    private String password;
    @Valid
    @ApiModelProperty("Информация о пользователе")
    private UserInfoDTO userinfo;

}
