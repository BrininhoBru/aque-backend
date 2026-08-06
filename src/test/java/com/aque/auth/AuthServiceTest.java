package com.aque.auth;

import com.aque.auth.dto.request.LoginRequest;
import com.aque.auth.dto.response.LoginResponse;
import com.aque.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_credenciaisValidas_retornaTokenEExpiracao() {
        var request = new LoginRequest("admin", "senha-correta");
        when(jwtService.generateToken("admin")).thenReturn("token-gerado");
        when(jwtService.getExpirationMs()).thenReturn(3_600_000L);

        LoginResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("token-gerado");
        assertThat(response.expiresIn()).isEqualTo(3_600_000L);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("admin");
        assertThat(captor.getValue().getCredentials()).isEqualTo("senha-correta");
    }

    @Test
    void login_credenciaisInvalidas_propagaBadCredentialsException() {
        var request = new LoginRequest("admin", "senha-errada");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }
}
