package com.julenka.api.primitiveBank.controller;

import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.dto.CreateUserDTO;
import com.julenka.api.primitiveBank.dto.UserInfoDTO;
import com.julenka.api.primitiveBank.services.CurrentUserService;
import com.julenka.api.primitiveBank.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "", description = "Контроллер для работы с пользователем")
public class UserController {

    private final UserService userService;


    @PostMapping("/create")
    @ApiOperation("Создание пользователя")
    public Long createUser(@RequestBody CreateUserDTO dto){
        return userService.createUser(dto);
    }

    @PostMapping("/changeInfo")
    @ApiOperation("Изменение информации о пользователе")
    public void changeUserInfo (@RequestBody UserInfoDTO dto){
         userService.changeUserInfo(dto);
    }





}
