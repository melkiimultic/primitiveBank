package com.julenka.api.primitiveBank.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserInfoDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("UserInfoDTO can be serialized and deserialized")
    void canBeSerializedAndDeserialized() throws JsonProcessingException {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setFirstName("First");
        dto.setLastName("Last");
        UserInfoDTO userInfoDTO = mapper.readValue(mapper.writeValueAsString(dto), UserInfoDTO.class);
        assertEquals(dto,userInfoDTO);

    }



}