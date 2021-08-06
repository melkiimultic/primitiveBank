package com.julenka.api.primitiveBank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julenka.api.primitiveBank.ResourceConverter;
import com.julenka.api.primitiveBank.domain.Roles;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.domain.UserInfo;
import com.julenka.api.primitiveBank.dto.AuthResponseDTO;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import com.julenka.api.primitiveBank.services.CurrentUserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    TransactionTemplate template;
    @Autowired
    CurrentUserService currentUserService;
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

    @Transactional
    Long createAndSafeAdminUser() {
        User user = new User();
        user.setUsername("admin");
        user.setAuthorities(Set.of(Roles.USER, Roles.ADMIN));
        user.setPassword(encoder.encode("admin1"));
        UserInfo info = new UserInfo();
        info.setFirstName("FirstA");
        info.setLastName("LastA");
        user.setUserInfo(info);
        userRepo.save(user);
        return userRepo.findOneByUsername(user.getUsername()).get().getId();
    }

    @Transactional
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


    @Test
    @DisplayName("User can be created")
    @SneakyThrows
    @Order(0)
    void userCanBeCreated() {
        userRepo.deleteAll();
        String body = ResourceConverter.getString(new ClassPathResource("test.requests/createUser.json"));
        mockMvc.perform(post("/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
        template.executeWithoutResult(e ->
        {
            Optional<User> user = userRepo.findOneByUsername("test");
            assertTrue(user.isPresent());
            assertTrue(encoder.matches("test1", user.get().getPassword()));
            assertEquals("First", user.get().getUserInfo().getFirstName());
            assertEquals("Last", user.get().getUserInfo().getLastName());
            assertTrue(user.get().getAuthorities().contains(Roles.USER));
            assertTrue(user.get().getAccounts().size() == 0);
        });

    }

    @Test
    @DisplayName("Can't create user with the same login")
    @SneakyThrows
    @Order(1)
    void sameLoginUserCreation() {
        String body = ResourceConverter.getString(new ClassPathResource("test.requests/createUser.json"));
       mockMvc.perform(post("/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isConflict());

        template.executeWithoutResult(e ->
        {
            assertTrue(userRepo.existsByUsername("test"));
            assertTrue(userRepo.count()==1);
        });

    }

    @Test
    @DisplayName("Userinfo can be changed")
    @SneakyThrows
    @Order(2)
    void changeUserInfo() {
        final String loginRequest = ResourceConverter.getString(new ClassPathResource("test.requests/auth.json"));
        String body = ResourceConverter.getString(new ClassPathResource("test.requests/userInfo.json"));
        mockMvc.perform(post("/users/changeInfo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Authorization", "Bearer " + login(loginRequest).getJwtToken()))
                .andExpect(status().isOk());
        template.executeWithoutResult(tr -> {
            User user = userRepo.findOneByUsername("test").get();
            assertEquals("Second", user.getUserInfo().getFirstName());
            assertEquals("Third", user.getUserInfo().getLastName());
        });
    }

    @Test
    @DisplayName("Userinfo isn't changed if empty request")
    @SneakyThrows
    @Order(3)
    void emptyRequestUserInfoChange() {
        final String loginRequest = ResourceConverter.getString(new ClassPathResource("test.requests/auth.json"));
        mockMvc.perform(post("/users/changeInfo")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + login(loginRequest).getJwtToken()))
                .andExpect(status().isBadRequest());
        template.executeWithoutResult(tr -> {
            User user = userRepo.findOneByUsername("test").get();
            assertEquals("Second", user.getUserInfo().getFirstName());
            assertEquals("Third", user.getUserInfo().getLastName());
        });
    }

    @Test
    @DisplayName("User can be deleted")
    @SneakyThrows
    @Order(4)
    void deleteUser() {
        createAndSafeAdminUser();
        final String loginRequest = ResourceConverter.getString(new ClassPathResource("test.requests/auth.json"));
        mockMvc.perform(delete("/users/delete")
                .header("Authorization", "Bearer " + login(loginRequest).getJwtToken()))
                .andExpect(status().isOk());
        template.executeWithoutResult(tr -> {
            assertFalse(userRepo.existsByUsername("test"));
            assertTrue(userRepo.existsByUsername("admin"));
        });

    }

    @Test
    @DisplayName("User can be deleted by ID by admin")
    @SneakyThrows
    @Order(5)
    void deleteUserByIdIfCurrentIsAdmin() {
        template.executeWithoutResult(e -> {
            userRepo.deleteAll();
        });
        final String loginRequest = ResourceConverter.getString(new ClassPathResource("test.requests/authAdmin.json"));
        final AtomicReference<Long> userId = new AtomicReference<>();
        template.executeWithoutResult(e -> {
            userId.set(createAndSafeSimpleUser());
            createAndSafeAdminUser();
        });
        mockMvc.perform(delete("/users/delete/" + userId.get())
                .header("Authorization", "Bearer " + login(loginRequest).getJwtToken()))
                .andExpect(status().isOk());
        assertFalse(userRepo.existsById(userId.get()));

    }

    @Test
    @DisplayName("User can't be deleted by ID by simple user")
    @SneakyThrows
    @Order(5)
    void deleteUserByIdBySimpleUser() {
        template.executeWithoutResult(e -> {
            userRepo.deleteAll();
        });
        final String loginRequest = ResourceConverter.getString(new ClassPathResource("test.requests/auth.json"));
        final AtomicReference<Long> userId = new AtomicReference<>();
        template.executeWithoutResult(e -> {
            createAndSafeSimpleUser();
            userId.set(createAndSafeAdminUser());
        });
        mockMvc.perform(delete("/users/delete/" + userId.get())
                .header("Authorization", "Bearer " + login(loginRequest).getJwtToken()))
                .andExpect(status().isForbidden());
        assertTrue(userRepo.existsById(userId.get()));

    }

}
