package com.julenka.api.primitiveBank.controller;

import com.julenka.api.primitiveBank.dto.CreateUserDTO;
import com.julenka.api.primitiveBank.dto.GetUserDTO;
import com.julenka.api.primitiveBank.dto.UserInfoDTO;
import com.julenka.api.primitiveBank.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Tag(name = "user-controller", description = "Контроллер для работы с пользователем")
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    @ApiOperation("Создание пользователя")
    public Long createUser(@RequestBody CreateUserDTO dto) {
        return userService.createUser(dto);
    }

    @PostMapping("/changeInfo")
    @ApiOperation("Изменение информации о пользователе")
    public void changeUserInfo(@RequestBody UserInfoDTO dto) {
        userService.changeUserInfo(dto);
    }

    @DeleteMapping("/delete")
    @ApiOperation("Удаление текущего юзера")
    public void deleteCurrentUser() {
        userService.deleteCurrentUser();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/delete/{id}")
    @ApiOperation("Удаление юзера по id")
    public void deleteUserById(@PathVariable("id") Long id) {
        userService.deleteUserById(id);
    }

    @GetMapping("/get/{username}")
    @ApiOperation(("Получение юзера по логину"))
    public GetUserDTO getUserByUsername(@PathVariable("username") String username) {
        return userService.getUserByUsername(username);
    }


}
