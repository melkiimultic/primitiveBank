package com.julenka.api.primitiveBank.repositories;

import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AccountRepo extends JpaRepository<Account, Long> {

    List<Account> findAllByUser(User user);

    Optional<Account> findOneByUser(User user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Query("SELECT a FROM Account a WHERE a.id = ?1")
    Optional<Account> fetchAccountWithLockById(Long accId);

}
