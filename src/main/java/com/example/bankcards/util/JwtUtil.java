package com.example.bankcards.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Утилитный класс для работы с JWT-токенами: генерация, парсинг и валидация.
 */
@Component
public class JwtUtil {
    /** Секретный ключ для подписи JWT-токенов. */
    private final String secretKey;

    /** Время жизни JWT-токена в миллисекундах. */
    private final long jwtTokenValidity;

    /**
     * Создаёт экземпляр утилиты с указанными параметрами конфигурации.
     *
     * @param secretKey секретный ключ для подписи JWT-токенов
     * @param jwtTokenValidity время жизни JWT-токена в миллисекундах
     */
    public JwtUtil(@Value("${jwt.secret}") String secretKey, @Value("${jwt.expiration}") long jwtTokenValidity) {
        this.secretKey = secretKey;
        this.jwtTokenValidity = jwtTokenValidity;
    }

    /**
     * Получает секретный ключ для подписи JWT (HS256).
     *
     * @return секретный ключ
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Извлекает email из JWT-токена.
     *
     * @param token JWT-токен
     * @return email, извлечённый из токена
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Извлекает дату истечения JWT-токена.
     *
     * @param token JWT-токен
     * @return дата истечения токена
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Извлекает произвольное значение из claims JWT-токена.
     *
     * @param token JWT-токен
     * @param claimsResolver функция для извлечения значения из claims
     * @param <T> тип извлекаемого значения
     * @return извлечённое значение
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Извлекает все claims из JWT-токена.
     *
     * @param token JWT-токен
     * @return claims из токена
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Проверяет, истёк ли JWT-токен.
     *
     * @param token JWT-токен
     * @return true, если токен истёк, false в противном случае
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Генерирует JWT-токен для указанного email.
     *
     * @param email email пользователя
     * @return сгенерированный JWT-токен
     */
    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email);
    }

    /**
     * Создаёт JWT-токен с указанными claims и subject.
     *
     * @param claims дополнительные claims для токена
     * @param subject subject токена (email)
     * @return сгенерированный JWT-токен
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtTokenValidity))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Валидирует JWT-токен для указанного email.
     *
     * @param token JWT-токен
     * @param email email для проверки
     * @return true, если токен валиден, false в противном случае
     */
    public Boolean validateToken(String token, String email) {
        try {
            final String extractedEmail = extractEmail(token);
            return (extractedEmail.equals(email) && !isTokenExpired(token));
        } catch (ExpiredJwtException e) {
            // Токен просрочен
            return false;
        } catch (Exception e) {
            // Любая другая ошибка — считаем токен недействительным
            return false;
        }
    }
}
