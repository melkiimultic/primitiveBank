package com.julenka.api.primitiveBank.services;

import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.dto.FundDepositDTO;
import com.julenka.api.primitiveBank.dto.MoneyTransferDTO;
import com.julenka.api.primitiveBank.exceptions.BadRequestException;
import com.julenka.api.primitiveBank.exceptions.ForbiddenOperationException;
import com.julenka.api.primitiveBank.exceptions.UncertainAccountException;
import com.julenka.api.primitiveBank.repositories.AccountRepo;
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
        User user = currentUserService.getCurrentUser();
        if (dto.getToId() == null) {
            try {
                Account only = accountRepo.findOneByUser(user).orElseThrow();
                dto.setToId(only.getId());
            } catch (RuntimeException ex) {
                throw new UncertainAccountException("User has no or more than 1 account");
            }
        }
        Account toFund = accountRepo.fetchAccountWithLockById(dto.getToId())
                .filter(acc -> acc.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new UncertainAccountException("Forbidden.User has not such an account"));

        toFund.setBalance(toFund.getBalance().add(dto.getAmount()));
        Account savedAcc = accountRepo.saveAndFlush(toFund);
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
        if (dto == null) {
            throw new BadRequestException("Empty request body");
        }
        checkFromField(dto);
        checkToField(dto);
        checkAmount(dto);
        actualTransfer(dto);
    }

    private void checkFromField(MoneyTransferDTO dto) {
        User user = currentUserService.getCurrentUser();
        if (dto.getFromId() == null) {
            try {
                Account only = accountRepo.findOneByUser(user).orElseThrow();
                dto.setFromId(only.getId());
            } catch (RuntimeException ex) {
                throw new UncertainAccountException("User has no or more than 1 account");
            }
        }
        accountRepo.findById(dto.getFromId())
                .filter(acc -> acc.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new UncertainAccountException("Forbidden.User has not such an account"));
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

        accountRepo.findById(dto.getFromId()).ifPresent(acc -> {
            BigDecimal balance = acc.getBalance();
            if (balance.compareTo(dto.getAmount()) < 0) {
                throw new ForbiddenOperationException("Not enough money for this operation");
            }
        });
    }


    private void actualTransfer(MoneyTransferDTO dto) {
        Long fromId = dto.getFromId();
        Long toId = dto.getToId();
        BigDecimal amount = dto.getAmount();
        Account from = accountRepo.fetchAccountWithLockById(fromId).get();
        from.setBalance(from.getBalance().subtract(amount));
        Account to = accountRepo.fetchAccountWithLockById((toId)).get();
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
