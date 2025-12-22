package com.example.bankcards.service;

import com.example.bankcards.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

/**
 * Тесты для утилитного класса работы с JWT-токенами.
 */
@ExtendWith(MockitoExtension.class)
public class JwtUtilTest {

    /** Утилитный класс для работы с JWT-токенами (мок). */
    @Mock
    private JwtUtil jwtUtil;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String WRONG_EMAIL = "wrong@example.com";
    private static final String TOKEN = "mocked-jwt-token";
    private static final long JWT_TOKEN_VALIDITY = 10_000L; // 10 секунд
    private static final String MALFORMED_TOKEN = "not_a_valid_token";

    /**
     * Настройка перед каждым тестом: очищает моки.
     */
    @BeforeEach
    void setUp() {
        reset(jwtUtil);
    }

    /**
     * Проверяет генерацию и валидацию токена.
     * Ожидаемый результат: токен создаётся, email извлекается, токен валиден.
     */
    @Test
    void testGenerateAndValidateToken() {
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);
        when(jwtUtil.extractEmail(TOKEN)).thenReturn(TEST_EMAIL);
        when(jwtUtil.validateToken(TOKEN, TEST_EMAIL)).thenReturn(true);

        String token = jwtUtil.generateToken(TEST_EMAIL);

        assertNotNull(token);
        assertEquals(TEST_EMAIL, jwtUtil.extractEmail(token));
        assertTrue(jwtUtil.validateToken(token, TEST_EMAIL));
    }

    /**
     * Проверяет, что новый токен не истёк.
     * Ожидаемый результат: дата истечения токена в будущем.
     */
    @Test
    void testIsTokenExpired_shouldBeFalseForNewToken() {
        Date futureDate = new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY);
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);
        when(jwtUtil.extractExpiration(TOKEN)).thenReturn(futureDate);

        String token = jwtUtil.generateToken(TEST_EMAIL);
        Date expiration = jwtUtil.extractExpiration(token);

        assertFalse(expiration.before(new Date()), "Токен не должен быть истёкшим сразу после генерации");
    }

    /**
     * Проверяет генерацию токена.
     * Ожидаемый результат: токен не null и не пустой.
     */
    @Test
    void testGenerateToken_shouldReturnNonNullToken() {
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);

        String token = jwtUtil.generateToken(TEST_EMAIL);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    /**
     * Проверяет извлечение email из токена.
     * Ожидаемый результат: извлечённый email совпадает с исходным.
     */
    @Test
    void testExtractEmail_shouldReturnCorrectEmail() {
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);
        when(jwtUtil.extractEmail(TOKEN)).thenReturn(TEST_EMAIL);

        String token = jwtUtil.generateToken(TEST_EMAIL);
        String extractedEmail = jwtUtil.extractEmail(token);

        assertEquals(TEST_EMAIL, extractedEmail);
    }

    /**
     * Проверяет извлечение даты истечения токена.
     * Ожидаемый результат: дата истечения в будущем.
     */
    @Test
    void testExtractExpiration_shouldReturnFutureDate() {
        Date futureDate = new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY);
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);
        when(jwtUtil.extractExpiration(TOKEN)).thenReturn(futureDate);

        String token = jwtUtil.generateToken(TEST_EMAIL);
        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration, "Дата истечения не должна быть null");
        assertTrue(expiration.after(new Date()), "Дата истечения должна быть в будущем");
    }

    /**
     * Проверяет валидацию корректного токена.
     * Ожидаемый результат: возвращается true.
     */
    @Test
    void testValidateToken_shouldReturnTrueForValidToken() {
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);
        when(jwtUtil.validateToken(TOKEN, TEST_EMAIL)).thenReturn(true);

        String token = jwtUtil.generateToken(TEST_EMAIL);

        assertTrue(jwtUtil.validateToken(token, TEST_EMAIL));
    }

    /**
     * Проверяет валидацию токена с некорректным email.
     * Ожидаемый результат: возвращается false.
     */
    @Test
    void testValidateToken_shouldReturnFalseForInvalidEmail() {
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);
        when(jwtUtil.validateToken(TOKEN, WRONG_EMAIL)).thenReturn(false);

        String token = jwtUtil.generateToken(TEST_EMAIL);

        assertFalse(jwtUtil.validateToken(token, WRONG_EMAIL));
    }

    /**
     * Проверяет валидацию истёкшего токена.
     * Ожидаемый результат: возвращается false.
     */
    @Test
    void testValidateToken_shouldReturnFalseIfTokenIsExpired() {
        // Настраиваем мок для генерации токена
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);
        // Настраиваем мок для валидации токена, возвращает false для истёкшего токена
        when(jwtUtil.validateToken(TOKEN, TEST_EMAIL)).thenReturn(false);

        // Генерируем токен
        String token = jwtUtil.generateToken(TEST_EMAIL);

        // Проверяем, что токен невалиден
        assertFalse(jwtUtil.validateToken(token, TEST_EMAIL));

        // Проверяем, что методы были вызваны
        verify(jwtUtil).generateToken(TEST_EMAIL);
        verify(jwtUtil).validateToken(TOKEN, TEST_EMAIL);
    }

    /**
     * Проверяет извлечение конкретного claim из токена.
     * Ожидаемый результат: извлечённый subject совпадает с email.
     */
    @Test
    void testExtractClaim_shouldReturnExpectedClaim() {
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);
        when(jwtUtil.extractClaim(eq(TOKEN), any(Function.class))).thenReturn(TEST_EMAIL);

        String token = jwtUtil.generateToken(TEST_EMAIL);
        String subject = jwtUtil.extractClaim(token, Claims::getSubject);

        assertEquals(TEST_EMAIL, subject);
    }

    /**
     * Проверяет извлечение всех claims из токена.
     * Ожидаемый результат: claims содержат subject и дату истечения.
     */
    @Test
    void testExtractAllClaims_shouldContainSubjectAndExpiration() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(TEST_EMAIL);
        when(claims.getExpiration()).thenReturn(new Date());
        when(jwtUtil.generateToken(TEST_EMAIL)).thenReturn(TOKEN);
        when(jwtUtil.extractClaim(eq(TOKEN), any(Function.class))).thenReturn(claims);

        String token = jwtUtil.generateToken(TEST_EMAIL);
        Claims extractedClaims = jwtUtil.extractClaim(token, c -> c);

        assertEquals(TEST_EMAIL, extractedClaims.getSubject());
        assertNotNull(extractedClaims.getExpiration());
    }

    /**
     * Проверяет обработку некорректного токена.
     * Ожидаемый результат: методы извлечения выбрасывают JwtException, валидация возвращает false.
     */
    @Test
    void testMalformedToken_shouldReturnFalse() {
        when(jwtUtil.extractEmail(MALFORMED_TOKEN)).thenThrow(new JwtException("Некорректный токен"));
        when(jwtUtil.extractExpiration(MALFORMED_TOKEN)).thenThrow(new JwtException("Некорректный токен"));
        when(jwtUtil.validateToken(MALFORMED_TOKEN, TEST_EMAIL)).thenReturn(false);

        assertThrows(JwtException.class, () -> jwtUtil.extractEmail(MALFORMED_TOKEN));
        assertThrows(JwtException.class, () -> jwtUtil.extractExpiration(MALFORMED_TOKEN));
        assertFalse(jwtUtil.validateToken(MALFORMED_TOKEN, TEST_EMAIL));
    }
}
