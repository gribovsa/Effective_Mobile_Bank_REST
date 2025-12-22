package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferRequestDTO;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для операций с картами, доступных пользователям.
 */
@RestController
@RequestMapping("/api")
public class CardController {
    private static final Logger logger = LoggerFactory.getLogger(CardController.class);

    /** Сервис для работы с картами. */
    private final CardService cardService;

    /**
     * Создаёт экземпляр контроллера с указанным сервисом.
     *
     * @param cardService сервис для работы с картами
     */
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * Получает список карт текущего пользователя с возможностью поиска по номеру карты.
     *
     * @param search   строка для поиска по номеру карты (опционально)
     * @param pageable параметры пагинации
     * @return ответ со страницей DTO карт
     */
    @GetMapping("/user/cards")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardDTO>> getUserCards(@RequestParam(required = false) String search, Pageable pageable) {
        logger.info("Получение карт пользователя с поиском: {}", search);
        if (search != null && !search.isEmpty()) {
            return ResponseEntity.ok(cardService.getUserCardsBySearch(search, pageable));
        }
        return ResponseEntity.ok(cardService.getUserCards(pageable));
    }

    /**
     * Запрашивает блокировку карты по её идентификатору.
     *
     * @param cardId идентификатор карты
     * @return ответ с пустым телом и статусом 200
     */
    @PostMapping("/cards/{cardId}/block")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> requestBlockCard(@PathVariable Long cardId) {
        logger.info("Запрос на блокировку карты с ID: {}", cardId);
        cardService.blockCard(cardId);
        return ResponseEntity.ok().build();
    }

    /**
     * Выполняет перевод средств между картами.
     *
     * @param request DTO с данными для перевода
     * @return ответ с пустым телом и статусом 200
     */
    @PostMapping("/user/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> transfer(@RequestBody @Valid TransferRequestDTO request) {
        logger.info("Перевод с карты ID: {} на карту ID: {}, сумма: {}",
                request.getFromCardId(), request.getToCardId(), request.getAmount());
        cardService.transfer(request.getFromCardId(), request.getToCardId(), request.getAmount());
        return ResponseEntity.ok().build();
    }

    /**
     * Получает баланс карты по её идентификатору.
     *
     * @param cardId идентификатор карты
     * @return ответ с балансом карты
     */
    @GetMapping("/cards/{cardId}/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Double> getCardBalance(@PathVariable Long cardId) {
        logger.info("Получение баланса карты с ID: {}", cardId);
        return ResponseEntity.ok(cardService.getCardBalance(cardId));
    }
}
