package com.arcturus.streamapi.controller;

import com.arcturus.streamapi.domain.User;
import com.arcturus.streamapi.dto.LoginRequest;
import com.arcturus.streamapi.dto.RegisterRequest;
import com.arcturus.streamapi.repository.UserRepository;
import com.arcturus.streamapi.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// 👇 IMPORT NOVO (Spring Boot 3.4+)
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @DisplayName("Login: Deve retornar 200 e Token quando credenciais válidas")
    void login_ShouldReturnToken_WhenCredentialsValid() throws Exception {

        LoginRequest loginRequest = new LoginRequest("marianna", "senha123");
        User mockUser = new User("marianna", "encodedPass", "USER");

        when(userRepository.findByUsername("marianna")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("senha123", "encodedPass")).thenReturn(true);
        when(tokenService.generateToken(any(User.class))).thenReturn("token-fake-jwt");


        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-fake-jwt"));
    }

    @Test
    @DisplayName("Registro: Deve retornar 409 Conflict se usuário já existe")
    void register_ShouldReturn409_WhenUserExists() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("marianna", "senha123", "USER");
        User existingUser = new User("marianna", "pass", "USER");

        when(userRepository.findByUsername("marianna")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }
}