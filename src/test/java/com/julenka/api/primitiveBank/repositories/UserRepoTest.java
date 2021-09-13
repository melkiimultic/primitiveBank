package com.julenka.api.primitiveBank.repositories;

import com.julenka.api.primitiveBank.domain.Roles;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.domain.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import javax.transaction.Transactional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserRepoTest {

    @Autowired
    UserRepo userRepo;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    TransactionTemplate template;

    private User createUser() {
        User user = new User();
        user.setUsername("test");
        user.setAuthorities(Set.of(Roles.USER));
        user.setPassword(encoder.encode("test1"));
        UserInfo info = new UserInfo();
        info.setFirstName("First");
        info.setLastName("Last");
        user.setUserInfo(info);
        return user;
    }

    @BeforeEach
    @Transactional
    void deleteAllUsers() {
        userRepo.deleteAll();
    }

    @Test
    @DisplayName("a user can be saved")
    void canSaveUser() {
        User user = createUser();
        template.executeWithoutResult(tr -> {
            User saved = userRepo.save(user);
            Long id = saved.getId();
            assertTrue(userRepo.count() == 1);
            assertTrue(userRepo.existsByUsername("test"));
            assertTrue(userRepo.existsById(id));
            assertTrue(encoder.matches("test1", saved.getPassword()));
            assertEquals("First", saved.getUserInfo().getFirstName());
            assertEquals("Last", saved.getUserInfo().getLastName());
            assertTrue(saved.getAuthorities().contains(Roles.USER));
        });
    }

    @Test
    @DisplayName("Can delete user by ID")
    void canDeleteUserById() {
        User user = createUser();
        template.executeWithoutResult(tr -> {
            User saved = userRepo.save(user);
            Long id = saved.getId();
            assertTrue(userRepo.existsByUsername("test"));
            userRepo.deleteById(id);
            assertFalse(userRepo.existsByUsername("test"));
            assertFalse(userRepo.findOneByUsername("test").isPresent());
            assertFalse(userRepo.existsById(id));
            assertTrue(userRepo.count() == 0);

        });
    }


}