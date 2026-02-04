package com.arcturus.streamapi.service;

import com.arcturus.streamapi.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(tokenService, "secret", "MinhaChaveSecretaSuperDificilDeAdivinhar123456");
    }

    @Test
    @DisplayName("Deve gerar um token válido para o usuário")
    void generateToken_ShouldReturnString() {
        User user = new User("marianna", "123456", "USER");

        String token = tokenService.generateToken(user);

        Assertions.assertNotNull(token);
        Assertions.assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Deve validar um token e retornar o usuário correto")
    void validateToken_ShouldReturnUsername() {
        User user = new User("marianna", "123456", "USER");
        String token = tokenService.generateToken(user);

        String username = tokenService.validateToken(token);

        Assertions.assertEquals("marianna", username);
    }

    @Test
    @DisplayName("Deve retornar nulo se o token for inválido")
    void validateToken_ShouldReturnNull_WhenTokenInvalid() {
        String invalidToken = "token.falso.invalido";

        String username = tokenService.validateToken(invalidToken);

        Assertions.assertNull(username);
    }
}