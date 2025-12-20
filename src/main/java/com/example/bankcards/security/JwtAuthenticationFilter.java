package com.example.bankcards.security;

import com.example.bankcards.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;



/**
 * Фильтр для обработки JWT-токенов в запросах и установки аутентификации.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * Cоздает логгер для класса JwtAuthenticationFilter с использованием библиотеки SLF4J (Simple Logging Facade for Java)
     */
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** Утилита для работы с JWT-токенами. */
    private final JwtUtil jwtUtil;

    /** Сервис для загрузки данных пользователя. */
    private final CustomUserDetailsService userDetailsService;

    /**
     * Создаёт экземпляр фильтра с указанными зависимостями.
     *
     * @param jwtUtil утилита для работы с JWT-токенами
     * @param userDetailsService сервис для загрузки данных пользователя
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Переопределим метод doFilterInternal абстрактного класса OncePerRequestFilter
     * Обрабатывает входящий HTTP-запрос, проверяет JWT-токен и устанавливает аутентификацию.
     *
     * @param request HTTP-запрос
     * @param response HTTP-ответ
     * @param filterChain цепочка фильтров
     * @throws ServletException если произошла ошибка обработки запроса
     * @throws IOException если произошла ошибка ввода-вывода
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtUtil.validateToken(jwt, jwtUtil.extractEmail(jwt))) {
                String email = jwtUtil.extractEmail(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.error("Недействительная подпись JWT: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Недействительная подпись JWT");
            return;
        } catch (Exception e) {
            logger.error("Не удалось установить аутентификацию пользователя: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Ошибка аутентификации: " + e.getMessage());
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Извлекает JWT-токен из заголовка Authorization.
     *
     * @param request HTTP-запрос
     * @return JWT-токен или null, если заголовок отсутствует или некорректен
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
