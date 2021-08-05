package com.julenka.api.primitiveBank.repositories;

import com.julenka.api.primitiveBank.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.Optional;

@Repository
@Transactional
public interface UserRepo extends JpaRepository<User, Long> {

  Optional<User> findOneByUsername(String username);

   boolean existsByUsername(String username);
}
