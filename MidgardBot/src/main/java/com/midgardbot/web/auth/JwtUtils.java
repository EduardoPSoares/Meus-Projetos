package com.midgardbot.web.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Utilitário para geração e verificação de tokens JWT.
 */
public class JwtUtils {

    private static Algorithm algorithm;
    private static JWTVerifier verifier;

    public static void init(String secret) {
        algorithm = Algorithm.HMAC256(secret);
        verifier = JWT.require(algorithm)
                .withIssuer("midgardbot")
                .build();
    }

    /**
     * Gera um token JWT para o usuário Discord autenticado.
     */
    public static String generateToken(String userId, String username, String avatar) {
        return JWT.create()
                .withIssuer("midgardbot")
                .withSubject(userId)
                .withClaim("username", username)
                .withClaim("avatar", avatar)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plus(8, ChronoUnit.HOURS))
                .sign(algorithm);
    }

    /**
     * Verifica e decodifica um token JWT. Retorna null se inválido.
     */
    public static DecodedJWT verify(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
