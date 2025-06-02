package br.com.kandu.security.jwt;

import br.com.kandu.entity.Usuario;
import br.com.kandu.enums.NivelHierarquia;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils; // Para injetar valores nos campos privados

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String testSecret = "TesteSuperSecretoMuitoLongoParaNaoDarErroDeTamanhoMinimoComHS256"; // 64 chars
    private final long testExpirationMs = 3600000; // 1 hora
    private SecretKey testSecretKey;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        // Injeta os valores de @Value manualmente para o teste unitário
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecretString", testSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationInMs", testExpirationMs);
        jwtTokenProvider.init(); // Chama o @PostConstruct manualmente

        this.testSecretKey = Keys.hmacShaKeyFor(testSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Deve gerar um token JWT válido")
    void deveGerarTokenValido() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .nomeUsuario("testuser")
                .nivelHierarquia(NivelHierarquia.COMUM)
                .build();

        String token = jwtTokenProvider.generateToken(usuario);

        assertThat(token).isNotNull().isNotEmpty();

        // Decodifica o token para verificar o subject (nome de usuário)
        Claims claims = Jwts.parser()
                .verifyWith(testSecretKey) // Usa a mesma chave para verificação
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.getExpiration()).isAfter(new Date()); // Verifica se a expiração está no futuro
    }
}