package com.julenka.api.primitiveBank.services;

import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.domain.Roles;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.dto.FundDepositDTO;
import com.julenka.api.primitiveBank.dto.MoneyTransferDTO;
import com.julenka.api.primitiveBank.exceptions.BadRequestException;
import com.julenka.api.primitiveBank.exceptions.ForbiddenOperationException;
import com.julenka.api.primitiveBank.exceptions.UncertainAccountException;
import com.julenka.api.primitiveBank.repositories.AccountRepo;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

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

    @AfterEach
    @Transactional
    void deleteAllUsers() {
        userRepo.deleteAll();
    }

    @Test
    @DisplayName("Money can be transferred if dto is valid")
    @Order(0)
    void transferMoneyWithValidDTO() {
        userRepo.deleteAll();
        User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
        User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        MoneyTransferDTO dto = new MoneyTransferDTO();
        dto.setAmount(new BigDecimal(10));
        template.executeWithoutResult(tr -> {
            Long fromAccId = accountRepo.findAllByUser(from).get(0).getId();
            Long toAccId = accountRepo.findAllByUser(to).get(0).getId();
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


    @Nested
    @DisplayName("Can't transfer")
    class CantTransfer {

        @Test
        @DisplayName("with no amount defined")
        void transferEmptyAmount() {
            User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
            User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            MoneyTransferDTO dto = new MoneyTransferDTO();
            template.executeWithoutResult(tr -> {
                Long fromAccId = accountRepo.findAllByUser(from).get(0).getId();
                Long toAccId = accountRepo.findAllByUser(to).get(0).getId();
                dto.setFromId(fromAccId);
                dto.setToId(toAccId);
            });
            assertThrows(ForbiddenOperationException.class, () -> accountService.transferMoney(dto),
                    "Request doesn't contain the amount");

            template.executeWithoutResult(tr -> {
                Account accFrom = accountRepo.findAllByUser(from).get(0);
                assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
                Account accTo = accountRepo.findAllByUser(to).get(0);
                assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
            });
        }

        @Test
        @DisplayName("more than user has")
        void transferMoreMoneyThanUserHas() {
            User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
            User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            MoneyTransferDTO dto = new MoneyTransferDTO();
            dto.setAmount(new BigDecimal(30));
            template.executeWithoutResult(tr -> {
                Long fromAccId = accountRepo.findAllByUser(from).get(0).getId();
                Long toAccId = accountRepo.findAllByUser(to).get(0).getId();
                dto.setFromId(fromAccId);
                dto.setToId(toAccId);
            });
            assertThrows(ForbiddenOperationException.class, () -> accountService.transferMoney(dto),
                    "Not enough money for this operation");

            template.executeWithoutResult(tr -> {
                Account accFrom = accountRepo.findAllByUser(from).get(0);
                assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
                Account accTo = accountRepo.findAllByUser(to).get(0);
                assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
            });
        }

        @Test
        @DisplayName("if payee is not defined")
        void transferWithoutPayee() {
            User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            MoneyTransferDTO dto = new MoneyTransferDTO();
            dto.setAmount(new BigDecimal(10));
            template.executeWithoutResult(tr -> {
                Long fromAccId = accountRepo.findAllByUser(from).get(0).getId();
                dto.setFromId(fromAccId);
            });
            assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                    "Account for transfer hasn't been defined");

            template.executeWithoutResult(tr -> {
                Account accFrom = accountRepo.findAllByUser(from).get(0);
                assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
            });
        }

        @Test
        @DisplayName("if defined payee doesn't exist")
        void transferWithNonexistentPayee() {
            User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            MoneyTransferDTO dto = new MoneyTransferDTO();
            dto.setAmount(new BigDecimal(10));
            template.executeWithoutResult(tr -> {
                Long fromAccId = accountRepo.findAllByUser(from).get(0).getId();
                dto.setFromId(fromAccId);
                dto.setToId(4000L);
            });
            assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                    "Such an account for transfer doesn't exist");

            template.executeWithoutResult(tr -> {
                Account accFrom = accountRepo.findAllByUser(from).get(0);
                assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
            });
        }

        @Test
        @DisplayName("negative amount")
        void transferNegativeAmount() {
            User from = saveUserWithAccount("From", "From1", new BigDecimal(20));
            User to = saveUserWithAccount("To", "To1", new BigDecimal(5));
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            MoneyTransferDTO dto = new MoneyTransferDTO();
            dto.setAmount(new BigDecimal(-10));
            template.executeWithoutResult(tr -> {
                Long fromAccId = accountRepo.findAllByUser(from).get(0).getId();
                Long toAccId = accountRepo.findAllByUser(to).get(0).getId();
                dto.setFromId(fromAccId);
                dto.setToId(toAccId);
            });
            assertThrows(ForbiddenOperationException.class, () -> accountService.transferMoney(dto),
                    "You can't transfer negative amount");
            template.executeWithoutResult(tr -> {
                Account accFrom = accountRepo.findAllByUser(from).get(0);
                assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
                Account accTo = accountRepo.findAllByUser(to).get(0);
                assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
            });

        }

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
            Long fromAccId = accountRepo.findAllByUser(from).get(0).getId();
            Long toAccId = accountRepo.findAllByUser(to).get(0).getId();
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
            Long toAccId = accountRepo.findAllByUser(to).get(0).getId();
            dto.setToId(toAccId);
        });
        assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                "User has no or more than 1 account");

        template.executeWithoutResult(tr -> {
            Account accTo = accountRepo.findAllByUser(to).get(0);
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
            Long toAccId = accountRepo.findAllByUser(to).get(0).getId();
            dto.setToId(toAccId);
        });
        assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                "User has no or more than 1 account");

        template.executeWithoutResult(tr -> {
            Account accTo = accountRepo.findAllByUser(to).get(0);
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
            Long toAccId = accountRepo.findAllByUser(to).get(0).getId();
            dto.setFromId(4000L);
            dto.setToId(toAccId);
        });
        assertThrows(UncertainAccountException.class, () -> accountService.transferMoney(dto),
                "Forbidden.User has not such an account");

        template.executeWithoutResult(tr -> {
            Account accFrom = accountRepo.findAllByUser(from).get(0);
            assertEquals(0, accFrom.getBalance().compareTo(new BigDecimal(20)));
            Account accTo = accountRepo.findAllByUser(to).get(0);
            assertEquals(0, accTo.getBalance().compareTo(new BigDecimal(5)));
        });
    }

    @Test
    @DisplayName("Account can be created for current user")
    public void createAccount() {
        Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
        User user = saveUserWithAccount("From", "From1", null);
        Long userId = user.getId();
        assertTrue(accountRepo.findAllByUser(user).isEmpty());
        template.executeWithoutResult(tr -> {
            accountService.createAccountForCurrentUser();
            assertTrue(accountRepo.findAllByUser(user).size() == 1);
            Account account = accountRepo.findAllByUser(user).get(0);
            assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
            assertEquals(userId, account.getUser().getId());
        });
    }

    @Nested
    @DisplayName("Fund account")
    class FundAccount {
        @Test
        @DisplayName("FundDepositDTO hasn't been passed")
        void fundDepositDTOIsNull() {
            assertThrows(BadRequestException.class, () -> accountService.fundDepositOfCurrentUser(null),
                    "Empty request body");
        }

        @Test
        @DisplayName("To ID isn't defined and current user has no accounts to replenish")
        void fundDepositWithEmptyToId1() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", null);
            FundDepositDTO dto = new FundDepositDTO();
            dto.setAmount(new BigDecimal(10));
            assertThrows(UncertainAccountException.class, () -> accountService.fundDepositOfCurrentUser(dto),
                    "User has no or more than 1 account");
            template.executeWithoutResult(tr -> {
                assertEquals(0, accountRepo.findAllByUser(user).size());
            });
        }

        @Test
        @DisplayName("To ID isn't defined and current user has more than 1 account")
        void fundDepositWithEmptyToId2() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", new BigDecimal(10));
            template.executeWithoutResult(tr -> {
                Account acc2 = new Account();
                acc2.setUser(user);
                accountRepo.saveAndFlush(acc2);
            });
            FundDepositDTO dto = new FundDepositDTO();
            dto.setAmount(new BigDecimal(10));
            assertThrows(UncertainAccountException.class, () -> accountService.fundDepositOfCurrentUser(dto),
                    "User has no or more than 1 account");
            template.executeWithoutResult(tr -> {
                assertEquals(2, accountRepo.findAllByUser(user).size());
            });
        }

        @Test
        @DisplayName("To ID isn't defined but current user has only 1 account")
        void fundDepositWithEmptyToId3() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", new BigDecimal(10));
            FundDepositDTO dto = new FundDepositDTO();
            dto.setAmount(new BigDecimal(15));
            accountService.fundDepositOfCurrentUser(dto);
            template.executeWithoutResult(tr -> {
                assertEquals(1, accountRepo.findAllByUser(user).size());
                assertEquals(0, accountRepo.findAllByUser(user).get(0).getBalance().compareTo(new BigDecimal(25)));
            });
        }

        @Test
        @DisplayName("To ID doesn't match any of user's accounts")
        void fundDepositWithWrongToId() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", new BigDecimal(10));
            FundDepositDTO dto = new FundDepositDTO();
            dto.setAmount(new BigDecimal(15));
            dto.setToId(4000L);
            assertThrows(UncertainAccountException.class, () -> accountService.fundDepositOfCurrentUser(dto),
                    "Forbidden.Current user has not such an account");
            template.executeWithoutResult(tr -> {
                assertEquals(0, accountRepo.findAllByUser(user).get(0).getBalance().compareTo(new BigDecimal(10)));
            });
        }

        @Test
        @DisplayName("To ID matches 1 of user's account")
        void fundDepositWithRightToId() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", new BigDecimal(10));
            template.executeWithoutResult(tr -> {
                Account acc2 = new Account();
                acc2.setUser(user);
                accountRepo.saveAndFlush(acc2);
            });
            FundDepositDTO dto = new FundDepositDTO();
            dto.setAmount(new BigDecimal(15));
            template.executeWithoutResult(tr -> {
                Long id = accountRepo.findAllByUser(user).get(0).getId();
                Long id2 = accountRepo.findAllByUser(user).get(1).getId();
                dto.setToId(id);
                accountService.fundDepositOfCurrentUser(dto);
                assertEquals(0, accountRepo.findById(id).get().getBalance().compareTo(new BigDecimal(25)));
                assertEquals(0, accountRepo.findById(id2).get().getBalance().compareTo(BigDecimal.ZERO));
            });

        }

        @Test
        @DisplayName("Fund deposit with negative sum")
        void fundDepositWithNegativeSum() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", new BigDecimal(10));
            FundDepositDTO dto = new FundDepositDTO();
            dto.setAmount(new BigDecimal(-15));
            assertThrows(ForbiddenOperationException.class, () -> accountService.fundDepositOfCurrentUser(dto),
                    "You can't transfer negative amount");
            template.executeWithoutResult(tr -> {
                assertEquals(0, accountRepo.findAllByUser(user).get(0).getBalance().compareTo(new BigDecimal(10)));
            });

        }

        @Test
        @DisplayName("Fund deposit with empty sum")
        void fundDepositWithEmptySum() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", new BigDecimal(10));
            FundDepositDTO dto = new FundDepositDTO();
            assertThrows(ForbiddenOperationException.class, () -> accountService.fundDepositOfCurrentUser(dto),
                    "Request doesn't contain the amount");
            template.executeWithoutResult(tr -> {
                assertEquals(0, accountRepo.findAllByUser(user).get(0).getBalance().compareTo(new BigDecimal(10)));
            });
        }
    }


    @Nested
    @DisplayName("Delete account")
    class DeleteAccount {
        @Test
        @DisplayName("with negative id")
        void deleteAccountWithNegativeId() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", new BigDecimal(10));
            AtomicReference<Long> accId = new AtomicReference<>();
            template.executeWithoutResult(tr -> {
                accId.set(accountRepo.findAllByUser(user).get(0).getId());
            });
            assertThrows(BadRequestException.class, () -> accountService.deleteAccount(-3L),
                    "Wrong id!No such account in the system");
            template.executeWithoutResult(tr -> {
                assertEquals(1, accountRepo.findAllByUser(user).size());
                assertEquals(accId.get(), accountRepo.findAllByUser(user).get(0).getId());
            });
        }

        @Test
        @DisplayName("with not user's id")
        void deleteAccountWithWrongId() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", new BigDecimal(10));
            AtomicReference<Long> accId = new AtomicReference<>();
            template.executeWithoutResult(tr -> {
                accId.set(accountRepo.findAllByUser(user).get(0).getId());
            });
            assertThrows(ForbiddenOperationException.class, () -> accountService.deleteAccount(4000L),
                    "Forbidden.User has not such an account");
            template.executeWithoutResult(tr -> {
                assertEquals(1, accountRepo.findAllByUser(user).size());
                assertEquals(accId.get(), accountRepo.findAllByUser(user).get(0).getId());
            });
        }

        @Test
        @DisplayName("with positive balance")
        void deleteAccountWithPositiveBalance() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", new BigDecimal(10));
            AtomicReference<Long> accId = new AtomicReference<>();
            template.executeWithoutResult(tr -> {
                accId.set(accountRepo.findAllByUser(user).get(0).getId());
            });
            assertThrows(ForbiddenOperationException.class, () -> accountService.deleteAccount(accId.get()),
                    "This account has positive balance");
            template.executeWithoutResult(tr -> {
                assertEquals(1, accountRepo.findAllByUser(user).size());
                assertEquals(accId.get(), accountRepo.findAllByUser(user).get(0).getId());
            });
        }

        @Test
        @DisplayName("happy path")
        void deleteAccountHappyPath() {
            Mockito.when(currentUserService.getCurrentUser()).thenAnswer((i) -> userRepo.findOneByUsername("From").get());
            User user = saveUserWithAccount("From", "From1", BigDecimal.ZERO);
            AtomicReference<Long> accId = new AtomicReference<>();
            template.executeWithoutResult(tr -> {
                accId.set(accountRepo.findAllByUser(user).get(0).getId());
            });
            accountService.deleteAccount(accId.get());
            template.executeWithoutResult(tr -> {
                assertEquals(0, accountRepo.findAllByUser(user).size());
                assertFalse(accountRepo.existsById(accId.get()));
            });
        }

    }


}