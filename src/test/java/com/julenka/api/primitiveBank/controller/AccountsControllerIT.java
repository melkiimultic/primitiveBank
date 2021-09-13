package com.julenka.api.primitiveBank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julenka.api.primitiveBank.ResourceConverter;
import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.domain.Roles;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.domain.UserInfo;
import com.julenka.api.primitiveBank.dto.AuthResponseDTO;
import com.julenka.api.primitiveBank.repositories.AccountRepo;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AccountRepo accounts;

    @Autowired
    TransactionTemplate template;

    @Autowired
    PasswordEncoder encoder;

    @SneakyThrows
    private AuthResponseDTO login(String loginRequest) {
        MvcResult loggedIn = mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON).content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();
        return new ObjectMapper().readValue(loggedIn.getResponse().getContentAsString(), AuthResponseDTO.class);
    }

    Long createAndSafeSimpleUser() {
        User user = new User();
        user.setUsername("test");
        user.setAuthorities(Set.of(Roles.USER));
        user.setPassword(encoder.encode("test1"));
        UserInfo info = new UserInfo();
        info.setFirstName("First");
        info.setLastName("Last");
        user.setUserInfo(info);
        userRepo.save(user);
        return userRepo.findOneByUsername(user.getUsername()).get().getId();
    }

    @AfterEach
    @Transactional
    public void deleteUsers() {
        userRepo.deleteAll();
    }

    @Test
    @SneakyThrows
    @DisplayName("Create account for current user happy path")
    public void createAccountForCurrentUser() {
        template.executeWithoutResult(tr -> {
            userRepo.deleteAll();
            createAndSafeSimpleUser();
        });
        final String loginRequest = ResourceConverter.getString(new ClassPathResource("test.requests/auth.json"));
        mockMvc.perform(post("/accounts/create")
                .header("Authorization", "Bearer " + login(loginRequest).getJwtToken()))
                .andExpect(status().isOk());

        template.executeWithoutResult(tr -> {
            User test = userRepo.findOneByUsername("test").get();
            Optional<Account> account = accounts.findAllByUser(test).get(0);
            assertTrue(account.isPresent());
            assertEquals(1, accounts.findAllByUser(test).size());
            assertEquals(new BigDecimal("0.00"), account.get().getBalance());
            assertEquals("test", account.get().getUser().getUsername());
        });

    }

    @Test
    @SneakyThrows
    @DisplayName("Create account if header doesn't contain user")
    public void createAccountWithoutCurrentUser() {
        template.executeWithoutResult(tr -> {
            userRepo.deleteAll();
            createAndSafeSimpleUser();
        });
        mockMvc.perform(post("/accounts/create"))
                .andExpect(status().isUnauthorized());

        template.executeWithoutResult(tr -> {
            User test = userRepo.findOneByUsername("test").get();
            assertEquals(0, accounts.findAllByUser(test).size());
        });

    }


}