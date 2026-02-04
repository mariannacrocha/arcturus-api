package com.arcturus.streamapi.controller;

import com.arcturus.streamapi.domain.User;
import com.arcturus.streamapi.dto.LoginRequest;
import com.arcturus.streamapi.dto.LoginResponse;
import com.arcturus.streamapi.dto.RegisterRequest;
import com.arcturus.streamapi.repository.UserRepository;
import com.arcturus.streamapi.service.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
// 🚀 REMOVIDO O @CrossOrigin DAQUI (Agora o SecurityConfig manda em tudo)
public class AuthenticationController {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthenticationController(UserRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity login(@RequestBody LoginRequest body) {
        User user = repository.findByUsername(body.username()).orElseThrow(() -> new RuntimeException("User not found"));
        if (passwordEncoder.matches(body.password(), user.getPassword())) {
            String token = tokenService.generateToken(user);
            return ResponseEntity.ok(new LoginResponse(token));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody RegisterRequest body) {
        Optional<User> user = repository.findByUsername(body.username());

        if (user.isEmpty()) {
            User newUser = new User();
            newUser.setPassword(passwordEncoder.encode(body.password()));
            newUser.setUsername(body.username());
            newUser.setRole(body.role() != null ? body.role() : "USER");

            repository.save(newUser);
            String token = tokenService.generateToken(newUser);
            return ResponseEntity.ok(new LoginResponse(token));
        }

        return ResponseEntity.status(HttpStatus.CONFLICT).body("Usuário já existe");
    }
}