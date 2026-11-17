package com.becajava.ms_transaction_worker.infra.integration;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioClientConfigTest {

    private static final String SEGREDO = "segredo-de-teste";

    private String tokenEnviado() {
        RequestTemplate template = new RequestTemplate();
        new UsuarioClientConfig().tokenDeServicoInterceptor(SEGREDO).apply(template);

        String header = template.headers().get("Authorization").iterator().next();
        assertTrue(header.startsWith("Bearer "));
        return header.substring("Bearer ".length());
    }

    @Test
    @DisplayName("Interceptor envia token de serviço que o ms-user consegue validar")
    void enviaTokenDeServicoValido() {
        // Mesma verificacao feita pelo TokenService do ms-user
        DecodedJWT jwt = JWT.require(Algorithm.HMAC256(SEGREDO))
                .withIssuer("ms-user")
                .build()
                .verify(tokenEnviado());

        assertEquals("ms-transaction-worker", jwt.getSubject());
        assertEquals("SERVICE", jwt.getClaim("role").asString());
        assertTrue(jwt.getExpiresAtAsInstant().isAfter(Instant.now()));
        assertFalse(jwt.getExpiresAtAsInstant().isAfter(Instant.now().plusSeconds(120)));
    }

    @Test
    @DisplayName("Token assinado com outro segredo é rejeitado")
    void segredoDiferenteNaoValida() {
        String token = tokenEnviado();

        assertThrows(JWTVerificationException.class, () ->
                JWT.require(Algorithm.HMAC256("outro-segredo")).withIssuer("ms-user").build().verify(token));
    }
}
