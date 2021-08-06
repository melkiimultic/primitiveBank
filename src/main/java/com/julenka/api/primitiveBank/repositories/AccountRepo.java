package com.julenka.api.primitiveBank.repositories;

import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface AccountRepo extends JpaRepository<Account, Long> {

    List<Optional<Account>> findAllByUser(User user);



}
