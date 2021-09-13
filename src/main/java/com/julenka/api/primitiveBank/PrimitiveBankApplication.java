package com.julenka.api.primitiveBank;

import com.julenka.api.primitiveBank.dto.CreateUserDTO;
import com.julenka.api.primitiveBank.dto.UserInfoDTO;
import com.julenka.api.primitiveBank.repositories.UserRepo;
import com.julenka.api.primitiveBank.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

import static com.julenka.api.primitiveBank.config.StupidGuardingFilter.SWAGGER_PASSWORD;
import static com.julenka.api.primitiveBank.config.StupidGuardingFilter.SWAGGER_USER;

@SpringBootApplication
public class PrimitiveBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrimitiveBankApplication.class, args);
    }

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private UserService userService;

    @PostConstruct
    void createTestUser() {
        userRepo.findOneByUsername(SWAGGER_USER).ifPresentOrElse(
                (user) -> {
                },
                () -> {
                    CreateUserDTO dto = new CreateUserDTO();
                    dto.setUsername(SWAGGER_USER);
                    dto.setPassword(SWAGGER_PASSWORD);
                    UserInfoDTO userInfoDTO = new UserInfoDTO();
                    userInfoDTO.setFirstName("test user firstname");
                    userInfoDTO.setLastName("test user lastname");
                    dto.setUserinfo(userInfoDTO);
                    userService.createUser(dto);

                }
        );
    }

}
