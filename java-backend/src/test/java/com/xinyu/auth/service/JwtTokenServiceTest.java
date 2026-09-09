package com.xinyu.auth.service;

import com.xinyu.auth.security.UserPrincipal;
import com.xinyu.common.security.JwtKeyConfig;
import com.xinyu.common.security.JwtProperties;
import com.xinyu.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    @Test
    void signsAndDecodesRs256AccessToken() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        JwtProperties properties = new JwtProperties();
        properties.setPrivateKeyPem(pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        properties.setPublicKeyPem(pem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setIssuer("test-issuer");

        JwtKeyConfig keyConfig = new JwtKeyConfig();
        com.nimbusds.jose.jwk.RSAKey rsaKey = keyConfig.rsaKey(properties);
        JwtEncoder encoder = keyConfig.jwtEncoder(rsaKey);
        JwtDecoder decoder = keyConfig.jwtDecoder(rsaKey, properties);
        JwtTokenService service = new JwtTokenService(encoder, properties);

        UserEntity user = new UserEntity();
        user.setId(42L);
        user.setUsername("alice");
        user.setPasswordHash("{bcrypt}not-used");
        user.setRole("USER");
        user.setStatus("ACTIVE");

        JwtTokenService.IssuedAccessToken issued = service.issue(UserPrincipal.from(user));
        Jwt decoded = decoder.decode(issued.value());

        assertThat(issued.expiresIn()).isEqualTo(900);
        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("test-issuer");
        assertThat(decoded.getClaims()).doesNotContainKey("username");
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(decoded.getHeaders().get("alg")).isEqualTo("RS256");
        assertThat(decoded.getId()).isNotBlank();
    }

    @Test
    void rejectsExpiredAccessToken() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        JwtProperties properties = new JwtProperties();
        properties.setPrivateKeyPem(pem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        properties.setPublicKeyPem(pem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
        properties.setIssuer("test-issuer");

        JwtKeyConfig keyConfig = new JwtKeyConfig();
        com.nimbusds.jose.jwk.RSAKey rsaKey = keyConfig.rsaKey(properties);
        JwtEncoder encoder = keyConfig.jwtEncoder(rsaKey);
        JwtDecoder decoder = keyConfig.jwtDecoder(rsaKey, properties);
        Instant now = Instant.now();
        String expired = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.getKeyId()).build(),
                JwtClaimsSet.builder()
                        .issuer(properties.getIssuer())
                        .subject("42")
                        .issuedAt(now.minusSeconds(120))
                        .expiresAt(now.minusSeconds(60))
                        .id("expired-token")
                        .claim("roles", List.of("USER"))
                        .build())).getTokenValue();

        assertThatThrownBy(() -> decoder.decode(expired))
                .isInstanceOf(JwtValidationException.class);
    }

    private String pem(String type, byte[] encoded) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----";
    }
}
