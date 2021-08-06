package com.julenka.api.primitiveBank.services;

import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.domain.Roles;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.dto.MoneyTransferDTO;
import com.julenka.api.primitiveBank.repositories.AccountRepo;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class AccountServiceIT {

    @Autowired
    private TransactionTemplate template;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private AccountService accountService;

    @MockBean
    private CurrentUserService currentUserService;

    User saveUserWithAccount(String username, String password, BigDecimal balance) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setAuthorities(List.of(Roles.USER));
        Account acc = new Account();
        acc.setBalance(balance);
        acc.setUser(user);
        userRepo.saveAndFlush(user);
        accountRepo.saveAndFlush(acc);
        return userRepo.findOneByUsername(username).get();
    }

    @BeforeEach
    @Transactional
    void deleteAllUsers() {
        userRepo.deleteAll();
    }

    @Test
    @DisplayName("Money can be transferred if dto is valid")
    void transferMoneyWithValidDTO() {
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(10));
        template.executeWithoutResult(tr -> {
            Long fromAccId = accountRepo.findAllByUser(from).get(0).get().getId();
            Long toAccId = accountRepo.findAllByUser(to).get(0).get().getId();
            dto.setFromId(fromAccId);
            dto.setToId(toAccId);
            accountService.transferMoney(dto);
            Account accFrom = accountRepo.findById(fromAccId).get();
            assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(10)));
            assertEquals(from.getId(), accFrom.getUser().getId());
            Account accTo = accountRepo.findById(toAccId).get();
            assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(15)));
            assertEquals(to.getId(), accTo.getUser().getId());
        });
    }


}