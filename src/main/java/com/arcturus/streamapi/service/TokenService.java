package com.arcturus.streamapi.service;

import com.arcturus.streamapi.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    // Gera o Token quando o login é bem sucedido
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername()) // Salva o usuário dentro do token
                .setIssuer("Arcturus API")      // Quem emitiu
                .setIssuedAt(new Date())        // Quando foi criado
                .setExpiration(generateExpirationDate()) // Quando vence (2h)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Assina com a chave
                .compact();
    }

    // Valida se o token que chegou é autêntico
    public String validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject(); // Retorna o username se estiver tudo ok
        } catch (Exception e) {
            return null; // Token inválido ou expirado
        }
    }

    // Helper para transformar a String do properties em uma Chave Criptográfica
    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Define que o token vale por 2 horas
    private Date generateExpirationDate() {
        return Date.from(LocalDateTime.now().plusHours(2)
                .atZone(ZoneId.systemDefault()).toInstant());
    }
}