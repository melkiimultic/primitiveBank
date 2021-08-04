package com.julenka.api.primitiveBank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julenka.api.primitiveBank.ResourceConverter;
import com.julenka.api.primitiveBank.domain.Roles;
import com.julenka.api.primitiveBank.domain.User;
import com.julenka.api.primitiveBank.dto.AuthResponseDTO;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import com.julenka.api.primitiveBank.services.CurrentUserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    @MockBean
    PasswordEncoder encoder;

    @SneakyThrows
    private AuthResponseDTO login() {
        final String loginRequest = ResourceConverter.getString(new ClassPathResource("test.requests/auth.json"));
        MvcResult loggedIn = mockMvc.perform(post("/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON).content(loginRequest))
                .andExpect(status().isOk())
                .andReturn();
        return new ObjectMapper().readValue(loggedIn.getResponse().getContentAsString(), AuthResponseDTO.class);
    }



    @Test
    @DisplayName("User can be created")
    @SneakyThrows
    @Order(1)
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
            assertEquals(encoder.encode("test1"), user.get().getPassword());
            assertEquals("First", user.get().getUserInfo().getFirstName());
            assertEquals("Last", user.get().getUserInfo().getLastName());
            assertTrue(user.get().getAuthorities().contains(Roles.USER));
            assertTrue(user.get().getAccounts().size() == 0);
        });

    }

    @Test
    @DisplayName("Userinfo can be changed")
    @SneakyThrows
    @Order(2)
    void changeUserInfo() {
        String body = ResourceConverter.getString(new ClassPathResource("test.requests/userInfo.json"));
        mockMvc.perform(post("/users/changeInfo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header("Authorization", "Bearer " + login().getJwtToken()))
                .andExpect(status().isOk());
        User currentUser = currentUserService.getCurrentUser();
        assertEquals("Second", currentUser.getUserInfo().getFirstName());
        assertEquals("Third", currentUser.getUserInfo().getLastName());

    }

}
