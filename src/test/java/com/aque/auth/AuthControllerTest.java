package com.aque.auth;

import com.aque.BaseIntegrationTest;
import com.aque.auth.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginValido_deveRetornarToken() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("test", "test123"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void loginInvalido_deveRetornar401() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest("test", "senha-errada"));

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
    }

    @Test
    void semToken_deveRetornar403() throws Exception {
        mockMvc.perform(get("/categories")).andExpect(status().isForbidden());
    }

    @Test
    void tokenInvalido_deveRetornar403() throws Exception {
        mockMvc.perform(get("/categories").header("Authorization", "Bearer token-invalido")).andExpect(status().isForbidden());
    }
}