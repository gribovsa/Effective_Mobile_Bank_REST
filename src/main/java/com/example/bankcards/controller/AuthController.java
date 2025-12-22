package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Контроллер для обработки операций аутентификации и регистрации пользователей.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /** Сервис для работы с пользователями и аутентификацией. */
    private final UserService userService;

    /**
     * Создаёт экземпляр контроллера с указанными зависимостями.
     *
     * @param userService сервис для работы с пользователями
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Регистрирует нового пользователя и возвращает JWT-токен.
     *
     * @param user данные пользователя
     * @return ответ с JWT-токеном
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid User user) {
        logger.info("Регистрация пользователя: {}", user.getUsername());
        return ResponseEntity.ok(userService.register(user));
    }

    /**
     * Выполняет вход пользователя и возвращает JWT-токен.
     *
     * @param loginDTO данные для входа (имя и пароль)
     * @return ответ с JWT-токеном
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        logger.info("Попытка входа пользователя: {}", loginDTO.getUsername());
        try {
            String token = userService.login(loginDTO);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(token);
        } catch (BadCredentialsException e) {
            logger.error("Неверные учетные данные для пользователя: {}", loginDTO.getUsername());
            return ResponseEntity.status(401).contentType(MediaType.APPLICATION_JSON).body("Invalid credentials");
        }
    }
}
