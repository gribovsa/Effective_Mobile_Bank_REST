package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.PasswordUpdateDTO;
import com.example.bankcards.dto.RoleUpdateDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Тесты для контроллера административных операций с пользователями и картами.
 */

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AdminController.class)
public class AdminControllerTest {
    /** MockMvc для тестирования HTTP-запросов. */
    @Autowired
    private MockMvc mockMvc;

    /** Сервис для работы с картами (мок). */
    @MockitoBean
    private CardService cardService;

    /** Сервис для работы с пользователями (мок). */
    @MockitoBean
    private UserService userService;

    /** Объект для преобразования JSON. */
    @Autowired
    private ObjectMapper objectMapper;

    /** Утилита для работы с JWT-токенами (мок). */
    @MockitoBean
    private JwtUtil jwtUtil;

    /** Сервис для загрузки данных пользователя (мок). */
    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    /**
     * Настройка перед каждым тестом: устанавливает аутентификацию администратора.
     */
    @BeforeEach
    void setup() {
        // Устанавливаем mock-аутентификацию с ролью ADMIN
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    /**
     * Проверяет обновление пароля для несуществующего пользователя.
     * Ожидаемый результат: возвращается статус 404 (Not Found).
     */
    @Test
    @DisplayName("Обновление пароля для несуществующего пользователя возвращает 404")
    void updateUserPassword_shouldReturnNotFound_whenUserNotExists() throws Exception {
        PasswordUpdateDTO dto = new PasswordUpdateDTO();
        dto.setPassword("newPass123");

        when(userService.updateUserPassword(eq(999L), any(PasswordUpdateDTO.class)))
                .thenThrow(new ResourceNotFoundException("Пользователь не найден"));

        mockMvc.perform(put("/api/admin/users/999/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    /**
     * Проверяет обновление роли с недопустимой ролью.
     * Ожидаемый результат: возвращается статус 400 (Bad Request).
     */
    @Test
    @DisplayName("Обновление роли с недопустимой ролью возвращает 400")
    void updateUserRole_shouldReturnBadRequest_whenRoleInvalid() throws Exception {
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRole("INVALID_ROLE");

        User user = new User();
        user.setId(1L);
        user.setRole("USER");

        when(userService.updateUserRole(eq(1L), any(RoleUpdateDTO.class)))
                .thenThrow(new IllegalArgumentException("Недопустимая роль. Используйте 'USER' или 'ADMIN'"));

        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Недопустимая роль. Используйте 'USER' или 'ADMIN'"));
    }

    /**
     * Проверяет создание новой карты.
     * Ожидаемый результат: возвращается DTO созданной карты и статус 200.
     */
    @Test
    @DisplayName("Создание карты возвращает созданную карту")
    void createCard_shouldReturnCreatedCard() throws Exception {
        CardCreateDTO createDTO = new CardCreateDTO();
        createDTO.setOwnerUsername("admin");
        createDTO.setInitialBalance(1000.0);
        createDTO.setExpiryDate(LocalDate.of(2027, 12, 31)); // Устанавливаем валидную дату

        CardDTO cardDTO = new CardDTO();
        cardDTO.setId(1L);

        when(cardService.createCard(any(CardCreateDTO.class))).thenReturn(cardDTO);

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    /**
     * Проверяет активацию карты.
     * Ожидаемый результат: возвращается статус 200.
     */
    @Test
    @DisplayName("Активация карты возвращает 200")
    void activateCard_shouldReturnOk() throws Exception {
        doNothing().when(cardService).activateCard(1L);

        mockMvc.perform(put("/api/admin/cards/1/activate"))
                .andExpect(status().isOk());

        verify(cardService).activateCard(1L);
    }

    /**
     * Проверяет блокировку карты.
     * Ожидаемый результат: возвращается статус 200.
     */
    @Test
    @DisplayName("Блокировка карты возвращает 200")
    void blockCard_shouldReturnOk() throws Exception {
        doNothing().when(cardService).blockCard(1L);

        mockMvc.perform(put("/api/admin/cards/1/block"))
                .andExpect(status().isOk());

        verify(cardService).blockCard(1L);
    }

    /**
     * Проверяет удаление карты.
     * Ожидаемый результат: возвращается статус 200.
     */
    @Test
    @DisplayName("Удаление карты возвращает 200")
    void deleteCard_shouldReturnOk() throws Exception {
        doNothing().when(cardService).deleteCard(1L);

        mockMvc.perform(delete("/api/admin/cards/1"))
                .andExpect(status().isOk());

        verify(cardService).deleteCard(1L);
    }

    /**
     * Проверяет получение списка всех карт.
     * Ожидаемый результат: возвращается страница с DTO карт и статус 200.
     */
    @Test
    @DisplayName("Получение списка всех карт возвращает страницу карт")
    void getAllCards_shouldReturnList() throws Exception {
        CardDTO cardDTO = new CardDTO();
        cardDTO.setId(1L);

        Page<CardDTO> page = new PageImpl<>(List.of(cardDTO));

        when(cardService.getAllCards(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/cards")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    /**
     * Проверяет создание нового пользователя.
     * Ожидаемый результат: возвращается сообщение "Пользователь создан" и статус 200.
     */
    @Test
    @DisplayName("Создание пользователя возвращает успех")
    void createUser_shouldReturnSuccess() throws Exception {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("password");
        user.setRole("USER");

        when(userService.createUser(any(User.class))).thenReturn("Пользователь создан");

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("Пользователь создан"));
    }

    /**
     * Проверяет удаление пользователя.
     * Ожидаемый результат: возвращается статус 200.
     */
    @Test
    @DisplayName("Удаление пользователя возвращает 200")
    void deleteUser_shouldReturnOk() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk());

        verify(userService).deleteUser(1L);
    }
}
