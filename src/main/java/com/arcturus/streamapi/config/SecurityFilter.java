package com.arcturus.streamapi.config;

import com.arcturus.streamapi.domain.User;
import com.arcturus.streamapi.repository.UserRepository;
import com.arcturus.streamapi.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. Pega o token que veio no cabeçalho da requisição
        String token = recoverToken(request);

        // 2. Se o token existir, valida ele
        if (token != null) {
            String login = tokenService.validateToken(token);

            if (login != null) {
                // 3. Busca o usuário no banco
                User user = userRepository.findByUsername(login).orElseThrow(() -> new RuntimeException("User not found"));

                // 4. Cria a "identidade" para o Spring Security entender que está logado
                var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
                var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);

                // 5. Salva essa identidade no contexto (Sessão temporária)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 6. Continua o fluxo (vai pro Controller)
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        // O token vem assim: "Bearer eyJhbGci..." -> Removemos o "Bearer "
        return authHeader.replace("Bearer ", "");
    }
}