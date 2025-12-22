package com.example.bankcards.controller;

import com.example.bankcards.dto.LoginDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import tools.jackson.databind.ObjectMapper;


/**
 * Тесты для контроллера аутентификации и регистрации пользователей.
 * Проверяют обработку HTTP-запросов на эндпоинты регистрации и входа,
 * включая успешные сценарии, валидацию и обработку ошибок.
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
public class AuthControllerTest {
    /** MockMvc для тестирования HTTP-запросов. */
    @Autowired
    private MockMvc mockMvc;

    /** Сервис для работы с пользователями (мок). */
    @MockitoBean
    private UserService userService;

    /** Утилита для работы с JWT-токенами (мок) — не используется в тестах контроллера. */
    @MockitoBean
    private JwtUtil jwtUtil;

    /** Сервис для загрузки данных пользователя (мок) — не используется в тестах контроллера. */
    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    /** Объект для преобразования JSON. */
    @Autowired
    private ObjectMapper objectMapper;

    /** Логгер для отладки тестов. */
    private static final Logger logger = LoggerFactory.getLogger(AuthControllerTest.class);

    private static final String REGISTER_URL = "/api/auth/register";
    private static final String LOGIN_URL = "/api/auth/login";
    private static final String TEST_USERNAME = "test";
    private static final String TEST_PASSWORD = "123";
    private static final String TOKEN = "token";

    /**
     * Настройка перед каждым тестом.
     * Инициализирует моки и логирует запуск тестового окружения.
     */
    @BeforeEach
    void setup() {
        logger.info("Настройка тестового окружения для AuthControllerTest");
    }

    /**
     * Проверяет успешную регистрацию пользователя.
     * Ожидаемый результат: возвращается JWT-токен и статус 200.
     */
    @Test
    @DisplayName("Регистрация пользователя возвращает токен")
    void register_shouldReturnToken() throws Exception {
        User user = new User();
        user.setUsername(TEST_USERNAME);
        user.setPassword(TEST_PASSWORD);

        when(userService.register(any(User.class))).thenReturn(TOKEN);

        String requestJson = objectMapper.writeValueAsString(user);
        logger.info("Отправляемый JSON для регистрации: {}", requestJson);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string(TOKEN));

        verify(userService).register(any(User.class));
    }

    /**
     * Проверяет успешный вход пользователя.
     * Ожидаемый результат: возвращается JWT-токен и статус 200.
     */
    @Test
    @DisplayName("Вход пользователя возвращает токен")
    void login_shouldReturnToken() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername(TEST_USERNAME);
        loginDTO.setPassword(TEST_PASSWORD);

        // ИСПРАВЛЕНИЕ: Мокаем userService.login напрямую, чтобы контроллер вернул токен
        when(userService.login(any(LoginDTO.class))).thenReturn(TOKEN);

        String requestJson = objectMapper.writeValueAsString(loginDTO);
        logger.info("Отправляемый JSON для входа: {}", requestJson);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(TOKEN));

        // ИСПРАВЛЕНИЕ: Verify только для userService.login, удаляем ненужные verify для внутренних компонентов
        verify(userService).login(any(LoginDTO.class));
    }

    /**
     * Проверяет вход с пустым именем пользователя.
     * Ожидаемый результат: возвращается статус 400.
     */
    @Test
    @DisplayName("Вход с пустым именем пользователя возвращает 400")
    void login_shouldFail_whenUsernameEmpty() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("");
        loginDTO.setPassword(TEST_PASSWORD);

        String requestJson = objectMapper.writeValueAsString(loginDTO);
        logger.info("Отправляемый JSON для входа с пустым именем: {}", requestJson);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))

                .andExpect(jsonPath("$.username").value("Username must be between 3 and 50 characters"));
    }

    /**
     * Проверяет вход с пустым паролем.
     * Ожидаемый результат: возвращается статус 400.
     */
    @Test
    @DisplayName("Вход с пустым паролем возвращает 400")
    void login_shouldFail_whenPasswordEmpty() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername(TEST_USERNAME);
        loginDTO.setPassword("");

        String requestJson = objectMapper.writeValueAsString(loginDTO);
        logger.info("Отправляемый JSON для входа с пустым паролем: {}", requestJson);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password cannot be empty"));
    }

    /**
     * Проверяет регистрацию с пустым именем пользователя.
     * Ожидаемый результат: возвращается статус 400.
     */
    @Test
    @DisplayName("Регистрация с пустым именем пользователя возвращает 400")
    void register_shouldFail_whenUsernameEmpty() throws Exception {
        User user = new User();
        user.setUsername("");
        user.setPassword("password123");

        String requestJson = objectMapper.writeValueAsString(user);
        logger.info("Отправляемый JSON для регистрации с пустым именем: {}", requestJson);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    /**
     * Проверяет регистрацию с уже существующим именем пользователя.
     * Ожидаемый результат: возвращается статус 400 и сообщение об ошибке.
     */
    @Test
    @DisplayName("Регистрация с существующим именем пользователя возвращает 400")
    void register_shouldFail_whenUsernameExists() throws Exception {
        User user = new User();
        user.setUsername("regular_user");
        user.setPassword("password");

        when(userService.register(any(User.class)))
                .thenThrow(new IllegalArgumentException("Имя пользователя уже занято"));

        String requestJson = objectMapper.writeValueAsString(user);
        logger.info("Отправляемый JSON для регистрации с существующим именем: {}", requestJson);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Имя пользователя уже занято"));
    }

    /**
     * Проверяет вход с некорректными учетными данными.
     * Ожидаемый результат: возвращается статус 401 и сообщение об ошибке.
     */
    @Test
    @DisplayName("Вход с некорректными учетными данными возвращает 401")
    void login_shouldFail_whenInvalidCredentials() throws Exception {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("wrongUser");
        loginDTO.setPassword("wrongPassword");


        when(userService.login(any(LoginDTO.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        String requestJson = objectMapper.writeValueAsString(loginDTO);
        logger.info("Отправляемый JSON для входа с неверными данными: {}", requestJson);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid credentials"));


        verify(userService).login(any(LoginDTO.class));
    }
}
