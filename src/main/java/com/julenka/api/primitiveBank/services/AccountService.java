package com.julenka.api.primitiveBank.services;

import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.dto.MoneyTransferDTO;
import com.julenka.api.primitiveBank.exceptions.ForbiddenOperationException;
import com.julenka.api.primitiveBank.exceptions.UncertainAccountException;
import com.julenka.api.primitiveBank.repositories.AccountRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final CurrentUserService currentUserService;
    private final AccountRepo accountRepo;

    @Transactional
    public void transferMoney(MoneyTransferDTO dto) {
        checkFromField(dto);
        checkToField(dto);
        checkAmount(dto);
        actualTransfer(dto);
    }

    private void checkFromField(MoneyTransferDTO dto) {
        List<Account> accounts = currentUserService.getCurrentUser().getAccounts();
        if (dto.getFromId() == null) {
            if (accounts.size() == 1) {
                dto.setFromId(accounts.get(0).getId());
            } else {
                throw new UncertainAccountException("User has no or more than 1 account");
            }
        } else {
            if (accounts.size() > 0) {
                Optional<Long> first = accounts.stream().
                        map(account -> account.getId()).
                        filter(aLong -> aLong.compareTo(dto.getFromId()) == 0)
                        .findFirst();
                if (first.isEmpty()) {
                    throw new UncertainAccountException("Forbidden.User has not such an account");
                }
            } else {
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
}
