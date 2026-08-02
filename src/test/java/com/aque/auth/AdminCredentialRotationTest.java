package com.aque.auth;

import com.aque.BaseIntegrationTest;
import com.aque.auth.dto.request.LoginRequest;
import com.aque.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminCredentialRotationTest extends BaseIntegrationTest {

    private static final String NOVA_SENHA = "nova-senha-admin-123";

    @BeforeEach
    void setupAdmin() {
        // hash gerado pelo encoder da aplicação ($2a$) com o prefixo trocado para $2y$,
        // simulando a saída do htpasswd -B externo — o KDF é o mesmo, só a tag de versão difere
        String hash2a = passwordEncoder.encode(NOVA_SENHA);
        String hash2y = "$2y$" + hash2a.substring(4);

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(hash2y);
        userRepository.save(admin);
    }

    @Test
    void login_hashExternoBcrypt2y_autentica() {
        String token = authService.login(new LoginRequest("admin", NOVA_SENHA)).token();

        assertThat(token).isNotBlank();
    }

    @Test
    void login_senhaAntigaAposRotacao_lancaExcecao() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "senha-antiga-seed")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
