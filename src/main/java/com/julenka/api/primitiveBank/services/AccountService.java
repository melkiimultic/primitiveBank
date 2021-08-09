package com.julenka.api.primitiveBank.services;

import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.dto.FundDepositDTO;
import com.julenka.api.primitiveBank.dto.MoneyTransferDTO;
import com.julenka.api.primitiveBank.exceptions.BadRequestException;
import com.julenka.api.primitiveBank.exceptions.ForbiddenOperationException;
import com.julenka.api.primitiveBank.exceptions.UncertainAccountException;
import com.julenka.api.primitiveBank.repositories.AccountRepo;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final CurrentUserService currentUserService;
    private final AccountRepo accountRepo;

    @Transactional
    public Long createAccountForCurrentUser() {
        User currentUser = currentUserService.getCurrentUser();
        Account account = new Account();
        account.setBalance(BigDecimal.ZERO);
        account.setUser(currentUser);
        Account saved = accountRepo.saveAndFlush(account);
        return saved.getId();
    }

    @Transactional
    public Long fundDepositOfCurrentUser(FundDepositDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Empty request body");
        }
        checkAmountToFund(dto);

        List<Account> accounts = currentUserService.getCurrentUser().getAccounts();
        if (dto.getToId() == null) {
            if (accounts.size() != 1) {
                throw new UncertainAccountException("User has no or more than 1 account");
            } else {
                dto.setToId(accounts.get(0).getId());
            }
        } else {
            Optional<Long> first = accounts.stream()
                    .map(Account::getId)
                    .filter(aLong -> aLong.equals(dto.getToId()))
                    .findFirst();
            if (first.isEmpty()) {
                throw new UncertainAccountException("Forbidden.Current user has not such an account");
            }
        }
        Account account = accounts.get(0);
        account.setBalance(account.getBalance().add(dto.getAmount()));
        Account savedAcc = accountRepo.saveAndFlush(account);
        return savedAcc.getId();
    }

    private void checkAmountToFund(FundDepositDTO dto) {
        if (dto.getAmount() == null) {
            throw new ForbiddenOperationException("Request doesn't contain the amount");
        }
        if (dto.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ForbiddenOperationException("You can't transfer negative amount");
        }
    }


    @Transactional
    public void transferMoney(MoneyTransferDTO dto) {
        checkFromField(dto);
        checkToField(dto);
        checkAmount(dto);
        actualTransfer(dto);
    }

    private void checkFromField(MoneyTransferDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Empty request body");
        }
        List<Account> accounts = currentUserService.getCurrentUser().getAccounts();
        if (dto.getFromId() == null) {
            if (accounts.size() == 1) {
                dto.setFromId(accounts.get(0).getId());
            } else {
                throw new UncertainAccountException("User has no or more than 1 account");
            }
        } else {
            Optional<Long> first = accounts.stream()
                    .map(Account::getId)
                    .filter(aLong -> aLong.equals(dto.getFromId()))
                    .findFirst();
            if (first.isEmpty()) {
                throw new UncertainAccountException("Forbidden.User has not such an account");
            }
        }
    }

    void checkToField(MoneyTransferDTO dto) {
        if (dto.getToId() == null) {
            throw new UncertainAccountException("Account for transfer hasn't been defined");
        } else {
            if (!accountRepo.existsById(dto.getToId())) {
                throw new UncertainAccountException("Such an account for transfer doesn't exist");
            }
        }
    }

    private void checkAmount(MoneyTransferDTO dto) {
        if (dto.getAmount() == null) {
            throw new ForbiddenOperationException("Request doesn't contain the amount");
        }
        if (dto.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ForbiddenOperationException("You can't transfer negative amount");
        }

        BigDecimal balance = accountRepo.findById(dto.getFromId()).get().getBalance();
        if (balance.compareTo(dto.getAmount()) < 0) {
            throw new ForbiddenOperationException("Not enough money for this operation");
        }
    }

    private void actualTransfer(MoneyTransferDTO dto) {
        Long fromId = dto.getFromId();
        Long toId = dto.getToId();
        BigDecimal amount = dto.getAmount();
        Account from = accountRepo.findById(fromId).get();
        from.setBalance(from.getBalance().subtract(amount));
        Account to = accountRepo.findById((toId)).get();
        to.setBalance(to.getBalance().add(amount));
        accountRepo.saveAndFlush(from);
        accountRepo.saveAndFlush(to);
    }

    @Transactional
    public void deleteAccount(Long id) {
        if (id < 0) {
            throw new BadRequestException("Wrong id!No such account in the system");
        }
        List<Account> accounts = currentUserService.getCurrentUser().getAccounts();
        Optional<Long> first = accounts.stream()
                .map(Account::getId)
                .filter(aLong -> aLong.equals(id))
                .findFirst();
        if (first.isEmpty()) {
            throw new ForbiddenOperationException("Forbidden.User has not such an account");
        }
        if (accountRepo.findById(id).get().getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new ForbiddenOperationException("This account has positive balance");
        }
        accountRepo.deleteById(id);
    }
}
