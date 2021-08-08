package com.julenka.api.primitiveBank.services;

import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.domain.Roles;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.dto.MoneyTransferDTO;
import com.julenka.api.primitiveBank.exceptions.BadRequestException;
import com.julenka.api.primitiveBank.exceptions.ForbiddenOperationException;
import com.julenka.api.primitiveBank.exceptions.UncertainAccountException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        userRepo.saveAndFlush(user);
        if (balance != null) {
            Account acc = new Account();
            acc.setBalance(balance);
            acc.setUser(user);
            accountRepo.saveAndFlush(acc);
        }
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

    @Test
    @DisplayName("Can't transfer negative amount")
    void transferNegativeAmount() {
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(-10));
        template.executeWithoutResult(tr -> {
            Long fromAccId = accountRepo.findAllByUser(from).get(0).get().getId();
            Long toAccId = accountRepo.findAllByUser(to).get(0).get().getId();
            dto.setFromId(fromAccId);
            dto.setToId(toAccId);
        });
        assertThrows(ForbiddenOperationException.class, () -> accountService.transferMoney(dto),
                "You can't transfer negative amount");
        template.executeWithoutResult(tr -> {
            Account accFrom = accountRepo.findAllByUser(from).get(0).get();
            assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
            Account accTo = accountRepo.findAllByUser(to).get(0).get();
            assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
        });

    }

    @Test
    @DisplayName("Can't transfer with no amount defined")
    void transferEmptyAmount() {
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        template.executeWithoutResult(tr -> {
            Long fromAccId = accountRepo.findAllByUser(from).get(0).get().getId();
            Long toAccId = accountRepo.findAllByUser(to).get(0).get().getId();
            dto.setFromId(fromAccId);
            dto.setToId(toAccId);
        });
        assertThrows(ForbiddenOperationException.class, () -> accountService.transferMoney(dto),
                "Request doesn't contain the amount");

        template.executeWithoutResult(tr -> {
            Account accFrom = accountRepo.findAllByUser(from).get(0).get();
            assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
            Account accTo = accountRepo.findAllByUser(to).get(0).get();
            assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
        });
    }

    @Test
    @DisplayName("Can't transfer more money than user has")
    void transferMoreMoneyThanUserHas() {
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(30));
        template.executeWithoutResult(tr -> {
            Long fromAccId = accountRepo.findAllByUser(from).get(0).get().getId();
            Long toAccId = accountRepo.findAllByUser(to).get(0).get().getId();
            dto.setFromId(fromAccId);
            dto.setToId(toAccId);
        });
        assertThrows(ForbiddenOperationException.class, () -> accountService.transferMoney(dto),
                "Not enough money for this operation");

        template.executeWithoutResult(tr -> {
            Account accFrom = accountRepo.findAllByUser(from).get(0).get();
            assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
            Account accTo = accountRepo.findAllByUser(to).get(0).get();
            assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
        });
    }

    @Test
    @DisplayName("Can't transfer money if payee is not defined")
    void transferWithoutPayee() {
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(10));
        template.executeWithoutResult(tr -> {
            Long fromAccId = accountRepo.findAllByUser(from).get(0).get().getId();
            dto.setFromId(fromAccId);
        });
        assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                "Account for transfer hasn't been defined");

        template.executeWithoutResult(tr -> {
            Account accFrom = accountRepo.findAllByUser(from).get(0).get();
            assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
        });
    }

    @Test
    @DisplayName("Can't transfer money if defined payee doesn't exist")
    void transferWithNonexistentPayee() {
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(10));
        template.executeWithoutResult(tr -> {
            Long fromAccId = accountRepo.findAllByUser(from).get(0).get().getId();
            dto.setFromId(fromAccId);
            dto.setToId(4000L);
        });
        assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                "Such an account for transfer doesn't exist");

        template.executeWithoutResult(tr -> {
            Account accFrom = accountRepo.findAllByUser(from).get(0).get();
            assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
        });
    }

    @Test
    @DisplayName("moneyTransferDTO hasn't been passed")
    void moneyTransferDTOIsNull() {
        saveUserWithAccount("From", "From1", new BigDecimal(20));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        assertThrows(BadRequestException.class, () -> accountService.transferMoney(null),
                "Empty request body");
    }


    @Test
    @DisplayName("Sender is not defined but current user has only 1 account")
    void withoutSender1() {
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(10));
        template.executeWithoutResult(tr -> {
            Long fromAccId = accountRepo.findAllByUser(from).get(0).get().getId();
            Long toAccId = accountRepo.findAllByUser(to).get(0).get().getId();
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

    @Test
    @DisplayName("Sender is not defined and current user has no accounts")
    void withoutSender2() {
        saveUserWithAccount("From", "From1", null);
        User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(10));
        template.executeWithoutResult(tr -> {
            Long toAccId = accountRepo.findAllByUser(to).get(0).get().getId();
            dto.setToId(toAccId);
        });
        assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                "User has no or more than 1 account");

        template.executeWithoutResult(tr -> {
            Account accTo = accountRepo.findAllByUser(to).get(0).get();
            assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
            assertEquals(to.getId(), accTo.getUser().getId());
        });
    }

    @Test
    @DisplayName("Sender is not defined and current user has more than 1 account")
    void withoutSender3() {
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
        template.executeWithoutResult(tr -> {
            Account acc2 = new Account();
            acc2.setBalance(new BigDecimal(10));
            acc2.setUser(from);
            accountRepo.saveAndFlush(acc2);
        });
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(10));
        template.executeWithoutResult(tr -> {
            Long toAccId = accountRepo.findAllByUser(to).get(0).get().getId();
            dto.setToId(toAccId);
        });
        assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                "User has no or more than 1 account");

        template.executeWithoutResult(tr -> {
            Account accTo = accountRepo.findAllByUser(to).get(0).get();
            assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
        });
    }

    @Test
    @DisplayName("Money can't be transferred if sender's account doesn't belong to current user")
    void transferMoneyFromRandomAccount() {
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(10));
        template.executeWithoutResult(tr -> {
            Long toAccId = accountRepo.findAllByUser(to).get(0).get().getId();
            dto.setFromId(4000L);
            dto.setToId(toAccId);
        });
        assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                "Forbidden.User has not such an account");

        template.executeWithoutResult(tr -> {
            Account accFrom = accountRepo.findAllByUser(from).get(0).get();
            assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
            Account accTo = accountRepo.findAllByUser(to).get(0).get();
            assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
        });
    }


}