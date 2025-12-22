package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тесты для сервиса управления картами.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CardServiceTest {

    /** Репозиторий для работы с картами (мок). */
    @Mock
    private CardRepository cardRepository;

    /** Репозиторий для работы с пользователями (мок). */
    @Mock
    private UserRepository userRepository;

    /** Сервис для загрузки данных пользователя (мок). */
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    /** Сервис для работы с картами. */
    @InjectMocks
    private CardService cardService;

    private static final String ENCRYPTION_KEY = "1234567890123456";
    private static final String USERNAME = "user";
    private static final String ADMIN_USERNAME = "admin";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * Настройка перед каждым тестом: устанавливает ключ шифрования.
     */
    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(cardService, "encryptionKey", ENCRYPTION_KEY);
    }

    /**
     * Проверяет создание новой карты.
     * Ожидаемый результат: возвращается DTO созданной карты.
     */
    @Test
    void createCard_shouldReturnCardDTO() {
        CardCreateDTO dto = new CardCreateDTO();
        dto.setOwnerUsername(USERNAME);
        dto.setExpiryDate(LocalDate.now().plusYears(3));
        dto.setInitialBalance(100.0);

        User user = new User();
        user.setUsername(USERNAME);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(cardRepository.existsByCardNumber(any(String.class))).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card savedCard = invocation.getArgument(0);
            savedCard.setId(1L); // Симулируем присвоение ID
            return savedCard;
        });

        CardDTO result = cardService.createCard(dto);

        assertNotNull(result);
        assertEquals(USERNAME, result.getOwnerUsername());
        verify(cardRepository).save(any(Card.class));
    }

    /**
     * Проверяет получение карт пользователя.
     * Ожидаемый результат: возвращается страница карт пользователя.
     */
    @Test
    void getUserCards_shouldReturnUserCards() {
        User user = new User();
        user.setUsername(USERNAME);

        Card card = new Card();
        card.setId(1L);
        card.setCardNumber(cardService.encryptCardNumber("4111111111111111"));
        card.setOwner(user);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(100.0);

        mockAuthentication(USERNAME, ROLE_USER);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(cardRepository.findByOwner(eq(user), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(card)));

        Page<CardDTO> result = cardService.getUserCards(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals(USERNAME, result.getContent().get(0).getOwnerUsername());
    }

    /**
     * Проверяет создание карты для несуществующего пользователя.
     * Ожидаемый результат: выбрасывается ResourceNotFoundException.
     */
    @Test
    void createCard_shouldThrowIfUserNotFound() {
        CardCreateDTO dto = new CardCreateDTO();
        dto.setOwnerUsername("missing");

        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> cardService.createCard(dto));
        assertEquals("Пользователь не найден", exception.getMessage());
    }

    /**
     * Проверяет блокировку карты не владельцем и не администратором.
     * Ожидаемый результат: выбрасывается AccessDeniedException.
     */
    @Test
    void blockCard_shouldThrowIfNotOwnerOrAdmin() {
        User owner = new User();
        owner.setId(2L); // Different ID to ensure the current user is not the owner
        owner.setUsername("someone");

        User currentUser = new User();
        currentUser.setId(1L); // Different ID from owner
        currentUser.setUsername(USERNAME);

        Card card = new Card();
        card.setId(1L);
        card.setOwner(owner);
        card.setStatus(CardStatus.ACTIVE);

        mockAuthentication(USERNAME, ROLE_USER);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(currentUser));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> cardService.blockCard(1L));
        assertEquals("Нет прав для блокировки карты", exception.getMessage());
    }

    /**
     * Проверяет активацию карты не администратором.
     * Ожидаемый результат: выбрасывается AccessDeniedException.
     */
    @Test
    void activateCard_shouldThrowIfNotAdmin() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.BLOCKED);

        mockAuthentication(USERNAME, ROLE_USER);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> cardService.activateCard(1L));
        assertEquals("Доступ запрещён", exception.getMessage());
    }

    /**
     * Проверяет удаление карты не администратором.
     * Ожидаемый результат: выбрасывается AccessDeniedException.
     */
    @Test
    void deleteCard_shouldThrowIfNotAdmin() {
        Card card = new Card();
        card.setId(1L);

        mockAuthentication(USERNAME, ROLE_USER);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> cardService.deleteCard(1L));
        assertEquals("Доступ запрещён", exception.getMessage());
    }

    /**
     * Проверяет перевод с несуществующей карты.
     * Ожидаемый результат: выбрасывается ResourceNotFoundException.
     */
    @Test
    void transfer_shouldThrowIfCardNotFound() {
        mockAuthentication(USERNAME, ROLE_USER);
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> cardService.transfer(1L, 2L, 10.0));
        assertEquals("Карта-отправитель не найдена", exception.getMessage());
    }

    /**
     * Проверяет перевод с карты, не принадлежащей пользователю.
     * Ожидаемый результат: выбрасывается AccessDeniedException.
     */
    @Test
    void transfer_shouldThrowIfNotOwner() {
        User another = new User();
        another.setUsername("another");

        Card from = new Card();
        from.setId(1L);
        from.setOwner(another);

        Card to = new Card();
        to.setId(2L);
        to.setOwner(another);

        mockAuthentication(USERNAME, ROLE_USER);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> cardService.transfer(1L, 2L, 10.0));
        assertEquals("Доступ запрещён", exception.getMessage());
    }

    /**
     * Проверяет перевод при недостаточном балансе.
     * Ожидаемый результат: выбрасывается IllegalArgumentException.
     */
    @Test
    void transfer_shouldThrowIfInsufficientBalance() {
        User user = new User();
        user.setUsername(USERNAME);

        Card from = new Card();
        from.setId(1L);
        from.setBalance(5.0);
        from.setOwner(user);

        Card to = new Card();
        to.setId(2L);
        to.setOwner(user);

        mockAuthentication(USERNAME, ROLE_USER);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> cardService.transfer(1L, 2L, 10.0));
        assertEquals("Недостаточно средств", exception.getMessage());
    }

    /**
     * Проверяет получение всех карт не администратором.
     * Ожидаемый результат: выбрасывается AccessDeniedException.
     */
    @Test
    void getAllCards_shouldThrowIfNotAdmin() {
        mockAuthentication(USERNAME, ROLE_USER);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> cardService.getAllCards(Pageable.unpaged()));
        assertEquals("Доступ запрещён", exception.getMessage());
    }

    /**
     * Проверяет получение карт несуществующего пользователя.
     * Ожидаемый результат: выбрасывается ResourceNotFoundException.
     */
    @Test
    void getUserCards_shouldThrowIfUserNotFound() {
        mockAuthentication(USERNAME, ROLE_USER);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> cardService.getUserCards(Pageable.unpaged()));
        assertEquals("Пользователь не найден", exception.getMessage());
    }

    /**
     * Проверяет получение карт с поиском для несуществующего пользователя.
     * Ожидаемый результат: выбрасывается ResourceNotFoundException.
     */
    @Test
    void getUserCardsBySearch_shouldThrowIfUserNotFound() {
        mockAuthentication(USERNAME, ROLE_USER);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> cardService.getUserCardsBySearch("1234", Pageable.unpaged()));
        assertEquals("Пользователь не найден", exception.getMessage());
    }

    /**
     * Проверяет дешифрование некорректного номера карты.
     * Ожидаемый результат: выбрасывается IllegalArgumentException.
     */
    @Test
    void decryptCardNumber_shouldThrowIfBase64Invalid() {
        String invalid = "%%%notbase64%%%";

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cardService.decryptCardNumber(invalid));
        assertEquals("Ошибка дешифрования: неверный формат Base64", exception.getMessage());
    }

    /**
     * Проверяет получение карт пользователя с фильтрацией по номеру.
     * Ожидаемый результат: возвращается страница отфильтрованных карт.
     */
    @Test
    void getUserCardsBySearch_shouldReturnFilteredCards() {
        User user = new User();
        user.setUsername(USERNAME);

        Card card = new Card();
        card.setId(1L);
        card.setCardNumber(cardService.encryptCardNumber("4111111111115678"));
        card.setOwner(user);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(50.0);

        mockAuthentication(USERNAME, ROLE_USER);

        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(cardRepository.findByOwnerAndCardNumberContaining(eq(user), eq("5678"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(card)));

        Page<CardDTO> result = cardService.getUserCardsBySearch("5678", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals(USERNAME, result.getContent().get(0).getOwnerUsername());
    }

    /**
     * Проверяет удаление карты администратором.
     * Ожидаемый результат: карта удаляется.
     */
    @Test
    void deleteCard_shouldDeleteIfAdmin() {
        Card card = new Card();
        card.setId(1L);

        mockAuthentication(ADMIN_USERNAME, ROLE_ADMIN);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.deleteCard(1L);

        verify(cardRepository).delete(card);
    }

    /**
     * Проверяет получение всех карт администратором.
     * Ожидаемый результат: возвращается страница всех карт.
     */
    @Test
    void getAllCards_shouldReturnAllCardsIfAdmin() {
        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);

        Card card = new Card();
        card.setId(1L);
        card.setCardNumber(cardService.encryptCardNumber("4111111111111111"));
        card.setOwner(admin);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(99.0);

        mockAuthentication(ADMIN_USERNAME, ROLE_ADMIN);
        when(cardRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(card)));

        Page<CardDTO> result = cardService.getAllCards(Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals(ADMIN_USERNAME, result.getContent().get(0).getOwnerUsername());
    }

    /**
     * Проверяет получение баланса карты владельцем.
     * Ожидаемый результат: возвращается баланс карты.
     */
    @Test
    void getCardBalance_shouldReturnBalanceIfOwner() {
        User user = new User();
        user.setUsername(USERNAME);

        Card card = new Card();
        card.setId(1L);
        card.setOwner(user);
        card.setBalance(123.0);

        mockAuthentication(USERNAME, ROLE_USER);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        Double balance = cardService.getCardBalance(1L);

        assertEquals(123.0, balance);
    }

    /**
     * Проверяет получение баланса карты не владельцем.
     * Ожидаемый результат: выбрасывается AccessDeniedException.
     */
    @Test
    void getCardBalance_shouldThrowIfNotOwner() {
        User owner = new User();
        owner.setUsername("someone");

        Card card = new Card();
        card.setId(1L);
        card.setOwner(owner);

        mockAuthentication(USERNAME, ROLE_USER);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> cardService.getCardBalance(1L));
        assertEquals("Доступ запрещён", exception.getMessage());
    }

    /**
     * Проверяет шифрование и дешифрование номера карты.
     * Ожидаемый результат: возвращается исходный номер карты.
     */
    @Test
    void encryptAndDecryptCardNumber_shouldReturnOriginal() {
        String cardNumber = "4111111111111111";
        String encrypted = cardService.encryptCardNumber(cardNumber);
        String decrypted = cardService.decryptCardNumber(encrypted);

        assertNotEquals(cardNumber, encrypted, "Зашифрованный номер должен отличаться от исходного");
        assertEquals(cardNumber, decrypted, "Дешифрованный номер должен совпадать с исходным");
    }

    /**
     * Проверяет перевод средств между картами.
     * Ожидаемый результат: балансы карт обновляются.
     */
    @Test
    void transfer_shouldTransferBalance() {
        User user = new User();
        user.setId(1L); // Set ID to avoid potential issues with ID comparison
        user.setUsername(USERNAME);

        Card from = new Card();
        from.setId(1L);
        from.setBalance(100.0);
        from.setOwner(user);

        Card to = new Card();
        to.setId(2L);
        to.setBalance(50.0);
        to.setOwner(user);

        mockAuthentication(USERNAME, ROLE_USER);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user)); // Mock user lookup
        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));
        when(cardRepository.saveAll(Arrays.asList(from, to))).thenReturn(Arrays.asList(from, to));

        cardService.transfer(1L, 2L, 30.0);

        assertEquals(70.0, from.getBalance());
        assertEquals(80.0, to.getBalance());
        verify(cardRepository, times(1)).saveAll(Arrays.asList(from, to));
    }

    /**
     * Проверяет блокировку карты владельцем или администратором.
     * Ожидаемый результат: карта блокируется.
     */
    @Test
    void blockCard_shouldBlockIfOwnerOrAdmin() {
        User user = new User();
        user.setId(1L); // Set an ID for the user
        user.setUsername(USERNAME);

        Card card = new Card();
        card.setId(1L);
        card.setOwner(user);
        card.setStatus(CardStatus.ACTIVE);

        mockAuthentication(USERNAME, ROLE_USER);
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.blockCard(1L);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).save(card);
    }

    /**
     * Проверяет активацию карты администратором.
     * Ожидаемый результат: карта активируется.
     */
    @Test
    void activateCard_shouldActivateIfAdmin() {
        Card card = new Card();
        card.setId(1L);
        card.setStatus(CardStatus.BLOCKED);

        mockAuthentication(ADMIN_USERNAME, ROLE_ADMIN);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.activateCard(1L);

        assertEquals(CardStatus.ACTIVE, card.getStatus());
        verify(cardRepository).save(card);
    }

    /**
     * Настраивает мок-аутентификацию для пользователя с указанными ролями.
     *
     * @param username имя пользователя
     * @param roles    роли пользователя
     */
    private void mockAuthentication(String username, String... roles) {
        var authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();

        Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
