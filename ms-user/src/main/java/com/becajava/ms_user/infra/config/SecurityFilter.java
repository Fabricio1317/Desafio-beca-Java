package com.becajava.ms_user.infra.config;

import com.becajava.ms_user.infra.persistence.UsuarioEntity;
import com.becajava.ms_user.infra.persistence.UsuarioRepository;
import com.becajava.ms_user.infra.service.TokenService;
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
import java.util.List;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final String ROLE_SERVICO = "SERVICE";

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;

    public SecurityFilter(TokenService tokenService, UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = recuperarToken(request);

        if (token != null) {
            var login = tokenService.validarToken(token);

            if (!login.isEmpty()) {
                if (ROLE_SERVICO.equals(tokenService.obterRole(token))) {
                    // Token de servico (ex.: ms-transaction-worker): nao corresponde a um usuario do banco.
                    var authentication = new UsernamePasswordAuthenticationToken(
                            login,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + ROLE_SERVICO))
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    UsuarioEntity usuario = usuarioRepository.findByEmail(login).orElse(null);

                    if (usuario != null) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                usuario.getAuthorities()
                        );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}