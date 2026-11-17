package com.becajava.ms_transaction_worker.infra.integration;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.time.Instant;

/**
 * Configuracao exclusiva do UsuarioClient: o ms-user exige autenticacao em GET /users/{id},
 * entao cada chamada leva um token de servico de curta duracao, assinado com o mesmo segredo JWT.
 * Nao anotar com @Configuration, senao o interceptor passa a valer para todos os Feign clients.
 */
public class UsuarioClientConfig {

    static final String EMISSOR = "ms-user"; // emissor exigido pelo ms-user ao validar o token
    static final String SERVICO = "ms-transaction-worker";
    static final String ROLE_SERVICO = "SERVICE";
    static final long VALIDADE_SEGUNDOS = 60;

    @Bean
    public RequestInterceptor tokenDeServicoInterceptor(@Value("${api.security.token.secret}") String secret) {
        return template -> template.header("Authorization", "Bearer " + gerarTokenDeServico(secret));
    }

    static String gerarTokenDeServico(String secret) {
        return JWT.create()
                .withIssuer(EMISSOR)
                .withSubject(SERVICO)
                .withClaim("role", ROLE_SERVICO)
                .withExpiresAt(Instant.now().plusSeconds(VALIDADE_SEGUNDOS))
                .sign(Algorithm.HMAC256(secret));
    }
}
