package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferRequestDTO;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для контроллера операций с банковскими картами пользователей.
 * Проверяют эндпоинты получения карт, блокировки, перевода средств и получения баланса.
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CardController.class)
public class CardControllerTest {

    /** MockMvc для тестирования HTTP-запросов. */
    @Autowired
    private MockMvc mockMvc;

    /** Сервис для работы с картами (мок). */
    @MockitoBean
    private CardService cardService;

    /** Утилита для работы с JWT-токенами (мок). */
    @MockitoBean
    private JwtUtil jwtUtil;

    /** Сервис для загрузки данных пользователя (мок) — не используется в тестах контроллера. */
    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    /** Объект для преобразования JSON. */
    @Autowired
    private ObjectMapper objectMapper;

    /** Логгер для отладки тестов. */
    private static final Logger logger = LoggerFactory.getLogger(CardControllerTest.class);

    private static final String USER_CARDS_URL = "/api/user/cards";
    private static final String BLOCK_CARD_URL = "/api/cards/{cardId}/block";
    private static final String TRANSFER_URL = "/api/user/transfer";
    private static final String BALANCE_URL = "/api/cards/{cardId}/balance";

    /**
     * Проверяет получение карт пользователя с фильтрацией по поисковому запросу.
     * Ожидаемый результат: возвращается страница с отфильтрованными картами и статус 200.
     */
    @Test
    @DisplayName("Получение карт с поисковым запросом возвращает отфильтрованные карты")
    void getUserCards_withSearchParam_returnsFilteredCards() throws Exception {
        CardDTO dto = new CardDTO();
        dto.setId(1L);
        dto.setMaskedCardNumber("**** **** **** 5678");

        Page<CardDTO> page = new PageImpl<>(List.of(dto));

        when(cardService.getUserCardsBySearch(eq("5678"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(USER_CARDS_URL)
                        .param("search", "5678")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 5678"));

        verify(cardService).getUserCardsBySearch(eq("5678"), any(Pageable.class));
    }

    /**
     * Проверяет получение карт пользователя без поискового запроса.
     * Ожидаемый результат: возвращается страница с картами и статус 200.
     */
    @Test
    @DisplayName("Получение карт пользователя без поиска возвращает страницу карт")
    void getUserCards_withoutSearchParam_returnsPage() throws Exception {
        CardDTO dto = new CardDTO();
        dto.setId(1L);
        dto.setMaskedCardNumber("**** **** **** 1111");

        Page<CardDTO> page = new PageImpl<>(List.of(dto));

        when(cardService.getUserCards(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(USER_CARDS_URL)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 1111"));

        verify(cardService).getUserCards(any(Pageable.class));
    }

    /**
     * Проверяет блокировку карты, принадлежащей пользователю.
     * Ожидаемый результат: возвращается статус 200.
     */
    @Test
    @DisplayName("Блокировка карты пользователя возвращает 200")
    void requestBlockCard_blocksUserCard() throws Exception {
        doNothing().when(cardService).blockCard(1L);

        mockMvc.perform(post(BLOCK_CARD_URL, 1L))
                .andExpect(status().isOk());

        verify(cardService).blockCard(1L);
    }

    /**
     * Проверяет блокировку несуществующей карты.
     * Ожидаемый результат: возвращается статус 404 и сообщение об ошибке.
     */
    @Test
    @DisplayName("Блокировка несуществующей карты возвращает 404")
    void requestBlockCard_shouldReturnNotFound_whenCardMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Карта не найдена")).when(cardService).blockCard(99L);

        mockMvc.perform(post(BLOCK_CARD_URL, 99L))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Карта не найдена"));

        verify(cardService).blockCard(99L);
    }

    /**
     * Проверяет блокировку карты, не принадлежащей пользователю.
     * Ожидаемый результат: возвращается статус 403 и сообщение об ошибке.
     */
    @Test
    @DisplayName("Блокировка карты, не принадлежащей пользователю, возвращает 403")
    void requestBlockCard_shouldReturnForbidden_whenCardNotOwned() throws Exception {
        doThrow(new AccessDeniedException("Доступ запрещён")).when(cardService).blockCard(99L);

        mockMvc.perform(post(BLOCK_CARD_URL, 99L))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Доступ запрещён"));

        verify(cardService).blockCard(99L);
    }

    /**
     * Проверяет перевод средств между картами.
     * Ожидаемый результат: возвращается статус 200.
     */
    @Test
    @DisplayName("Перевод средств между картами возвращает 200")
    void transfer_shouldReturnOk() throws Exception {
        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setFromCardId(1L);
        dto.setToCardId(2L);
        dto.setAmount(50.0);

        doNothing().when(cardService).transfer(1L, 2L, 50.0);

        mockMvc.perform(post(TRANSFER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(cardService).transfer(1L, 2L, 50.0);
    }

    /**
     * Проверяет получение баланса карты.
     * Ожидаемый результат: возвращается баланс карты и статус 200.
     */
    @Test
    @DisplayName("Получение баланса карты возвращает корректный баланс")
    void getCardBalance_shouldReturnBalance() throws Exception {
        when(cardService.getCardBalance(1L)).thenReturn(123.45);

        mockMvc.perform(get(BALANCE_URL, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(123.45));

        verify(cardService).getCardBalance(1L);
    }

    /**
     * Проверяет получение баланса несуществующей карты.
     * Ожидаемый результат: возвращается статус 404 и сообщение об ошибке.
     */
    @Test
    @DisplayName("Получение баланса несуществующей карты возвращает 404")
    void getCardBalance_shouldReturnNotFound_whenCardMissing() throws Exception {
        when(cardService.getCardBalance(999L))
                .thenThrow(new ResourceNotFoundException("Карта не найдена"));

        mockMvc.perform(get(BALANCE_URL, 999L))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Карта не найдена"));

        verify(cardService).getCardBalance(999L);
    }
}
