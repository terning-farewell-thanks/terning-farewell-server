package com.terning.farewell_server.auth.jwt;

import com.terning.farewell_server.auth.exception.AuthErrorCode;
import com.terning.farewell_server.auth.exception.AuthException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret.key}")
    private String secretKeyString;

    @Value("${jwt.expiration.ms}")
    private long expirationTime;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

    public String generateTemporaryToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        throw new AuthException(AuthErrorCode.INVALID_JWT_TOKEN);
    }

    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (SecurityException e) {
            log.warn("유효하지 않은 JWT 서명입니다.", e);
            throw new AuthException(AuthErrorCode.INVALID_JWT_SIGNATURE);
        } catch (MalformedJwtException e) {
            log.warn("잘못된 형식의 JWT 토큰입니다.", e);
            throw new AuthException(AuthErrorCode.MALFORMED_JWT_TOKEN);
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다.", e);
            throw new AuthException(AuthErrorCode.EXPIRED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다.", e);
            throw new AuthException(AuthErrorCode.UNSUPPORTED_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다.", e);
            throw new AuthException(AuthErrorCode.INVALID_JWT_TOKEN);
        }
    }
}
