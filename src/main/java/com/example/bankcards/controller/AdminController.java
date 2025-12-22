package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для административных операций с пользователями и картами.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    /** Сервис для работы с картами. */
    private final CardService cardService;

    /** Сервис для работы с пользователями. */
    private final UserService userService;

    /**
     * Создаёт экземпляр контроллера с указанными зависимостями.
     *
     * @param cardService сервис для работы с картами
     * @param userService сервис для работы с пользователями
     */
    public AdminController(CardService cardService, UserService userService) {
        this.cardService = cardService;
        this.userService = userService;
    }

    /**
     * Создаёт новую карту.
     *
     * @param createDTO DTO с данными для создания карты
     * @return ответ с DTO созданной карты
     */
    @PostMapping("/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDTO> createCard(@RequestBody @Valid CardCreateDTO createDTO) {
        logger.info("Создание карты для пользователя: {}", createDTO.getOwnerUsername());
        return ResponseEntity.ok(cardService.createCard(createDTO));
    }

    /**
     * Активирует карту по её идентификатору.
     *
     * @param cardId идентификатор карты
     * @return ответ с пустым телом и статусом 200
     */
    @PutMapping("/cards/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateCard(@PathVariable Long cardId) {
        logger.info("Активация карты с ID: {}", cardId);
        cardService.activateCard(cardId);
        return ResponseEntity.ok().build();
    }

    /**
     * Блокирует карту по её идентификатору.
     *
     * @param cardId идентификатор карты
     * @return ответ с пустым телом и статусом 200
     */
    @PutMapping("/cards/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> blockCard(@PathVariable Long cardId) {
        logger.info("Блокировка карты с ID: {}", cardId);
        cardService.blockCard(cardId);
        return ResponseEntity.ok().build();
    }

    /**
     * Удаляет карту по её идентификатору.
     *
     * @param cardId идентификатор карты
     * @return ответ с пустым телом и статусом 200
     */
    @DeleteMapping("/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        logger.info("Удаление карты с ID: {}", cardId);
        cardService.deleteCard(cardId);
        return ResponseEntity.ok().build();
    }

    /**
     * Получает список всех карт в системе с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return ответ со страницей DTO карт
     */
    @GetMapping("/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardDTO>> getAllCards(Pageable pageable) {
        logger.info("Получение списка всех карт");
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }

    /**
     * Создаёт нового пользователя.
     *
     * @param user данные пользователя
     * @return ответ с сообщением об успешном создании
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createUser(@RequestBody @Valid User user) {
        logger.info("Создание пользователя: {}", user.getUsername());
        return ResponseEntity.ok(userService.createUser(user));
    }

    /**
     * Удаляет пользователя по его идентификатору.
     *
     * @param userId идентификатор пользователя
     * @return ответ с пустым телом и статусом 200
     */
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        logger.info("Удаление пользователя с ID: {}", userId);
        userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Обновляет пароль пользователя.
     *
     * @param userId      идентификатор пользователя
     * @param passwordDTO DTO с новым паролем
     * @return ответ с сообщением об успешном обновлении
     */
    @PutMapping("/users/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserPassword(@PathVariable Long userId, @RequestBody @Valid PasswordUpdateDTO passwordDTO) {
        logger.info("Обновление пароля для пользователя с ID: {}", userId);
        return ResponseEntity.ok(userService.updateUserPassword(userId, passwordDTO));
    }

    /**
     * Обновляет роль пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param roleDTO DTO с новой ролью
     * @return ответ с сообщением об успешном обновлении
     */
    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserRole(@PathVariable Long userId, @RequestBody @Valid RoleUpdateDTO roleDTO) {
        logger.info("Обновление роли для пользователя с ID: {}", userId);
        return ResponseEntity.ok(userService.updateUserRole(userId, roleDTO));
    }

    /**
     * Получает список пользователей с пагинацией и возможностью поиска по имени.
     *
     * @param username имя пользователя для поиска (опционально)
     * @param pageable параметры пагинации
     * @return ответ со страницей DTO пользователей
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getUsers(@RequestParam(required = false) String username, Pageable pageable) {
        logger.info("Получение списка пользователей с поиском: {}", username);
        return ResponseEntity.ok(userService.getUsers(username, pageable));
    }
}
