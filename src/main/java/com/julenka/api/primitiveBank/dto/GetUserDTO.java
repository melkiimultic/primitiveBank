package com.julenka.api.primitiveBank.dto;

import com.julenka.api.primitiveBank.domain.Roles;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Collection;

@Data
@ApiModel("Пользователь")
public class GetUserDTO {

    @ApiModelProperty("id пользователя")
    private Long id;

    @ApiModelProperty("Логин пользователя")
    private String username;

    @ApiModelProperty("Информация о пользователе")
    private UserInfoDTO userinfo;

    @ApiModelProperty("Роли")
    private Collection<Roles> authorities;
}
