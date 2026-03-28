package com.aque;

import com.aque.auth.AuthService;
import com.aque.auth.dto.request.LoginRequest;
import com.aque.user.User;
import com.aque.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected AuthService authService;

    protected String token;

    // ObjectMapper instanciado localmente — sem depender de bean do contexto
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setupAuth() {
        userRepository.deleteAll();

        User user = new User();
        user.setUsername("test");
        user.setPassword(passwordEncoder.encode("test123"));
        userRepository.save(user);

        token = "Bearer " + authService.login(
                new LoginRequest("test", "test123")
        ).token();
    }
}