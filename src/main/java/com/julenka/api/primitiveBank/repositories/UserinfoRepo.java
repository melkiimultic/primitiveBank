package com.julenka.api.primitiveBank.repositories;

import com.julenka.api.primitiveBank.domain.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface UserinfoRepo extends JpaRepository<UserInfo, Long> {
}
