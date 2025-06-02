package br.com.kandu.security.jwt;

import br.com.kandu.entity.Usuario;
import io.jsonwebtoken.*; // Adicionar Claims, ExpiredJwtException, etc.
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException; // Adicionar SignatureException
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger; // Para logging
import org.slf4j.LoggerFactory; // Para logging
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class); // Para logs

    @Value("${kandu.jwt.secret}")
    private String jwtSecretString;

    @Value("${kandu.jwt.expiration-ms}")
    private long jwtExpirationInMs;

    private SecretKey jwtSecretKey;

    @PostConstruct
    protected void init() {
        this.jwtSecretKey = Keys.hmacShaKeyFor(jwtSecretString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Usuario usuario) {
        String username = usuario.getNomeUsuario();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        Map<String, Object> claims = new HashMap<>();
        // claims.put("userId", usuario.getId()); // Exemplo de claim adicional

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extrai o nome de usuário (subject) de um token JWT.
     *
     * @param token O token JWT.
     * @return O nome de usuário contido no token.
     */
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSecretKey) // Usa a chave secreta para verificar a assinatura
                .build()
                .parseSignedClaims(token)
                .getPayload(); // Alterado de getBody() para getPayload() na v0.12.x do jjwt

        return claims.getSubject();
    }

    /**
     * Valida um token JWT.
     * Verifica se a assinatura é válida e se o token não está expirado ou malformado.
     *
     * @param authToken O token JWT a ser validado.
     * @return true se o token for válido, false caso contrário.
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(jwtSecretKey).build().parseSignedClaims(authToken);
            return true;
        } catch (SignatureException ex) {
            logger.error("Assinatura JWT inválida: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Token JWT malformado: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Token JWT expirado: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Token JWT não suportado: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("String de claims JWT está vazia: {}", ex.getMessage());
        }
        return false;
    }
}