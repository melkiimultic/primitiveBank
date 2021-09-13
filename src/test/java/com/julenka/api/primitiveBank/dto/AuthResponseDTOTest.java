package com.julenka.api.primitiveBank.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthResponseDTOTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final AuthResponseDTO dto = new AuthResponseDTO("content");

    @Test
    @DisplayName("Can serialize and deserialize")
    void canSerializeAndDeserialize() throws JsonProcessingException {
        AuthResponseDTO roundtrip = mapper.readValue(mapper.writeValueAsString(dto), AuthResponseDTO.class);
        assertEquals(dto, roundtrip);
    }

}