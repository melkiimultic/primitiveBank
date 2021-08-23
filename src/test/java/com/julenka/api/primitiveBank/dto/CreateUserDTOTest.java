package com.julenka.api.primitiveBank.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateUserDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Can (de)serialize createUserDTO")
    public void createUserDTOSerialization() throws JsonProcessingException {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setUsername("User");
        dto.setPassword("Password");
        dto.setUserinfo(new UserInfoDTO());
        CreateUserDTO dto1 = mapper.readValue(mapper.writeValueAsString(dto), CreateUserDTO.class);
        assertEquals(dto,dto1);
        assertEquals("User",dto1.getUsername());
        assertEquals("Password",dto1.getPassword());
    }

}