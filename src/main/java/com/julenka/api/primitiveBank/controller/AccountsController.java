package com.julenka.api.primitiveBank.controller;

import com.julenka.api.primitiveBank.services.AccountService;
import com.julenka.api.primitiveBank.services.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountsController {
    @Autowired
    private final CurrentUserService currentUserService;
    @Autowired
    private final AccountService accountService;


}
