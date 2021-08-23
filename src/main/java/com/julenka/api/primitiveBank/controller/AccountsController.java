package com.julenka.api.primitiveBank.controller;

import com.julenka.api.primitiveBank.dto.FundDepositDTO;
import com.julenka.api.primitiveBank.dto.MoneyTransferDTO;
import com.julenka.api.primitiveBank.services.AccountService;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
@Tag(name = "", description = "Контроллер для работы со счетами")
public class AccountsController {

    private final AccountService accountService;

    @PostMapping("/create")
    @ApiOperation("Создание счёта")
    public Long createAccount() {
        return accountService.createAccountForCurrentUser();
    }

    @PostMapping("/fund")
    @ApiOperation("Пополнение счета")
    public Long fundDepositOfCurrentUser(@RequestBody FundDepositDTO dto) {
        return accountService.fundDepositOfCurrentUser(dto);
    }

    @PostMapping("/transfer")
    @ApiOperation("Перевод денег между счетами")
    public void transferMoneyBetweenAccounts(@RequestBody MoneyTransferDTO dto) {
        accountService.transferMoney(dto);
    }

    @DeleteMapping("/close/{id}")
    @ApiOperation("Закрытие счета")
    public void closeAccount(@PathVariable("id") Long id) {
        accountService.deleteAccount(id);
    }

}
