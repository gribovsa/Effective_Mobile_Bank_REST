package com.example.bankcards.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений для REST-контроллеров.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Обрабатывает общее исключение RuntimeException.
     *
     * @param ex исключение
     * @return ответ с сообщением об ошибке и статусом 400
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        logger.error("Общая ошибка: {}", ex.getMessage());
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Обрабатывает непредвиденные исключения.
     *
     * @param ex исключение
     * @return ответ с сообщением об ошибке и статусом 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        logger.error("Внутренняя ошибка сервера: {}", ex.getMessage());
        return new ResponseEntity<>("Внутренняя ошибка сервера", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Обрабатывает исключение неверных учетных данных.
     *
     * @param ex исключение
     * @return ответ с сообщением об ошибке и статусом 401
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(BadCredentialsException ex) {
        logger.error("Неверные учетные данные: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Неверные учетные данные");
    }

    /**
     * Обрабатывает исключение отсутствия прав доступа.
     *
     * @param ex исключение
     * @return ответ с сообщением об ошибке и статусом 403
     */
    @ExceptionHandler(com.example.bankcards.exception.AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(com.example.bankcards.exception.AccessDeniedException ex) {
        logger.error("Доступ запрещён: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Доступ запрещён");
    }

    /**
     * Обрабатывает исключение отсутствия ресурса.
     *
     * @param ex исключение
     * @return ответ с сообщением об ошибке и статусом 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ResourceNotFoundException ex) {
        logger.error("Ресурс не найден: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    /**
     * Обрабатывает ошибки валидации входных данных.
     *
     * @param ex исключение валидации
     * @return ответ с картой ошибок валидации и статусом 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        logger.error("Ошибка валидации: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }
}
