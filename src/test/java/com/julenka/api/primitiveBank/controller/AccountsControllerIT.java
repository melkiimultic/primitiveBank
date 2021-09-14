package com.julenka.api.primitiveBank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julenka.api.primitiveBank.ResourceConverter;
import com.julenka.api.primitiveBank.domain.Account;
import com.julenka.api.primitiveBank.domain.Roles;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.domain.UserInfo;
import com.julenka.api.primitiveBank.dto.AuthResponseDTO;
import com.julenka.api.primitiveBank.dto.FundDepositDTO;
import com.julenka.api.primitiveBank.repositories.AccountRepo;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    Long createAndSafeSimpleUser(String name, String password, String firstname, String lastname) {
        User user = new User();
        user.setUsername(name);
        user.setAuthorities(Set.of(Roles.USER));
        user.setPassword(encoder.encode(password));
        UserInfo info = new UserInfo();
        info.setFirstName(firstname);
        info.setLastName(lastname);
        user.setUserInfo(info);
        userRepo.save(user);
        return userRepo.findOneByUsername(user.getUsername()).get().getId();
    }

    @AfterEach
    @BeforeEach
    @Transactional
    public void deleteUsers() {
        userRepo.deleteAll();
    }

    @Test
    @SneakyThrows
    @DisplayName("Create account for current user happy path")
    public void createAccountForCurrentUser() {
        template.executeWithoutResult(tr -> {
            createAndSafeSimpleUser("test", "test1", "First", "Last");
        });
        final String loginRequest = ResourceConverter.getString(new ClassPathResource("test.requests/auth.json"));
        mockMvc.perform(post("/accounts/create")
                .header("Authorization", "Bearer " + login(loginRequest).getJwtToken()))
                .andExpect(status().isOk());

        template.executeWithoutResult(tr -> {
            User test = userRepo.findOneByUsername("test").get();
            Optional<Account> oneByUser = accounts.findOneByUser(test);
            assertTrue(oneByUser.isPresent());
            assertEquals(new BigDecimal("0.00"), oneByUser.get().getBalance());
            assertEquals("test", oneByUser.get().getUser().getUsername());
        });

    }

    @Test
    @SneakyThrows
    @DisplayName("Create account if header doesn't contain user")
    public void createAccountWithoutCurrentUser() {
        template.executeWithoutResult(tr -> {
            createAndSafeSimpleUser("test", "test1", "First", "Last");
        });
        mockMvc.perform(post("/accounts/create"))
                .andExpect(status().isUnauthorized());

        template.executeWithoutResult(tr -> {
            User test = userRepo.findOneByUsername("test").get();
            assertEquals(0, accounts.findAllByUser(test).size());
        });

    }

    @Test
    @SneakyThrows
    @DisplayName("Fund less than 0 sum")
    public void fundNegativeSum() {
        FundDepositDTO dto = new FundDepositDTO();
        template.executeWithoutResult(tr -> {
            Long userId = createAndSafeSimpleUser("test", "test1", "First", "Last");
            User user = userRepo.getById(userId);
            Account acc = new Account();
            acc.setBalance(BigDecimal.ZERO);
            acc.setUser(user);
            Account account = accounts.saveAndFlush(acc);
            dto.setAmount(new BigDecimal(-1000));
            dto.setToId(account.getId());
        });
        final String loginRequest = ResourceConverter.getString(new ClassPathResource("test.requests/auth.json"));

        mockMvc.perform(post("/accounts/fund")
                .header("Authorization", "Bearer " + login(loginRequest).getJwtToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        template.executeWithoutResult(tr -> {
            User user = userRepo.findOneByUsername("test").get();
            Account account = accounts.findOneByUser(user).get();
            assertEquals(0, account.getBalance().compareTo(BigDecimal.ZERO));
            assertEquals("test", account.getUser().getUsername());
        });


    }


}