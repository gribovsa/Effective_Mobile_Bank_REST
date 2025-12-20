package com.example.bankcards.service;


import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 * Сервис для управления банковскими картами пользователей.
 */
@Service
public class CardService {
    /** Репозиторий для работы с картами. */
    private final CardRepository cardRepository;

    /** Репозиторий для работы с пользователями. */
    private final UserRepository userRepository;

    /** Ключ для шифрования номеров карт. */
    private final String encryptionKey;

    /**
     * Создаёт экземпляр сервиса с указанными зависимостями.
     *
     * @param cardRepository репозиторий для работы с картами
     * @param userRepository репозиторий для работы с пользователями
     * @param encryptionKey  ключ для шифрования номеров карт
     */
    public CardService(CardRepository cardRepository, UserRepository userRepository, @Value("${encryption.key}") String encryptionKey) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.encryptionKey = encryptionKey;
    }

    /**
     * Создаёт новую карту на основе предоставленного DTO.
     *
     * @param createDTO DTO с данными для создания карты
     * @return DTO созданной карты
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional
    public CardDTO createCard(CardCreateDTO createDTO) {
        User owner = userRepository.findByUsername(createDTO.getOwnerUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        Card card = new Card();
        card.setCardNumber(encryptCardNumber(generateCardNumber()));
        card.setOwner(owner);
        card.setExpiryDate(createDTO.getExpiryDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(createDTO.getInitialBalance());
        card = cardRepository.save(card);
        return mapToDTO(card);
    }

    /**
     * Возвращает список карт текущего пользователя с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница с DTO карт
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional(readOnly = true)
    public Page<CardDTO> getUserCards(Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return cardRepository.findByOwner(user, pageable).map(this::mapToDTO);
    }

    /**
     * Возвращает список карт текущего пользователя, соответствующих поисковому запросу, с пагинацией.
     *
     * @param query    поисковый запрос (часть номера карты)
     * @param pageable параметры пагинации
     * @return страница с DTO карт
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional(readOnly = true)
    public Page<CardDTO> getUserCardsBySearch(String query, Pageable pageable) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return cardRepository.findByOwnerAndCardNumberContaining(user, query, pageable).map(this::mapToDTO);
    }

    /**
     * Возвращает список активных карт текущего пользователя с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница с DTO карт
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional(readOnly = true)
    public Page<CardDTO> getUserCardsBySearch(Pageable pageable) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        return cardRepository.findByOwnerAndStatus(user, CardStatus.ACTIVE, pageable).map(this::mapToDTO);
    }

    /**
     * Блокирует карту по её идентификатору.
     *
     * @param cardId идентификатор карты
     * @throws ResourceNotFoundException если карта не найдена
     * @throws AccessDeniedException если пользователь не имеет прав
     */
    @Transactional
    public void blockCard(Long cardId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));
        if (!card.getOwner().getId().equals(currentUser.getId()) && !isAdmin()) {
            throw new AccessDeniedException("Нет прав для блокировки карты");
        }
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    /**
     * Активирует карту по её идентификатору (только для администратора).
     *
     * @param cardId идентификатор карты
     * @throws ResourceNotFoundException если карта не найдена
     * @throws AccessDeniedException если пользователь не администратор
     */
    @Transactional
    public void activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));
        if (!isAdmin()) {
            throw new AccessDeniedException("Доступ запрещён");
        }
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    /**
     * Удаляет карту по её идентификатору (только для администратора).
     *
     * @param cardId идентификатор карты
     * @throws ResourceNotFoundException если карта не найдена
     * @throws AccessDeniedException если пользователь не администратор
     */
    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));
        if (!isAdmin()) {
            throw new AccessDeniedException("Доступ запрещён");
        }
        cardRepository.delete(card);
    }

    /**
     * Выполняет перевод средств между картами.
     *
     * @param fromCardId идентификатор карты-отправителя
     * @param toCardId   идентификатор карты-получателя
     * @param amount     сумма перевода
     * @throws ResourceNotFoundException если одна из карт не найдена
     * @throws AccessDeniedException если пользователь не владеет обеими картами
     * @throws IllegalArgumentException если недостаточно средств
     */
    @Transactional
    public void transfer(Long fromCardId, Long toCardId, Double amount) {
        Card fromCard = cardRepository.findById(fromCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта-отправитель не найдена"));
        Card toCard = cardRepository.findById(toCardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта-получатель не найдена"));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!fromCard.getOwner().getUsername().equals(username) || !toCard.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Доступ запрещён");
        }
        if (fromCard.getBalance() < amount) {
            throw new IllegalArgumentException("Недостаточно средств");
        }
        fromCard.setBalance(fromCard.getBalance() - amount);
        toCard.setBalance(toCard.getBalance() + amount);
        cardRepository.saveAll(Arrays.asList(fromCard, toCard));
    }

    /**
     * Возвращает список всех карт в системе с пагинацией (только для администратора).
     *
     * @param pageable параметры пагинации
     * @return страница с DTO карт
     * @throws AccessDeniedException если пользователь не администратор
     */
    @Transactional(readOnly = true)
    public Page<CardDTO> getAllCards(Pageable pageable) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Доступ запрещён");
        }
        return cardRepository.findAll(pageable).map(this::mapToDTO);
    }

    /**
     * Возвращает баланс карты по её идентификатору.
     *
     * @param cardId идентификатор карты
     * @return баланс карты
     * @throws ResourceNotFoundException если карта не найдена
     * @throws AccessDeniedException если пользователь не владеет картой
     */
    @Transactional(readOnly = true)
    public Double getCardBalance(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Карта не найдена"));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!card.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Доступ запрещён");
        }
        return card.getBalance();
    }

    /**
     * Генерирует уникальный номер карты с использованием алгоритма Луна.
     *
     * @return сгенерированный номер карты
     */
    private String generateCardNumber() {
        Random random = new Random();
        String cardNumber;
        boolean isUnique;

        do {
            StringBuilder tempNumber = new StringBuilder("4");
            for (int i = 0; i < 14; i++) {
                tempNumber.append(random.nextInt(10));
            }
            int checkDigit = calculateLuhnCheckDigit(tempNumber.toString());
            cardNumber = tempNumber.append(checkDigit).toString();
            isUnique = !cardRepository.existsByCardNumber(cardNumber);
        } while (!isUnique);

        return cardNumber;
    }

    /**
     * Вычисляет контрольную цифру номера карты по алгоритму Луна.
     *
     * @param number номер карты без контрольной цифры
     * @return контрольная цифра
     */
    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean isEven = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (isEven) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            isEven = !isEven;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Шифрует номер карты с использованием AES.
     *
     * @param cardNumber номер карты для шифрования
     * @return зашифрованный номер карты
     * @throws IllegalArgumentException если номер карты не содержит 16 цифр
     * @throws RuntimeException если произошла ошибка шифрования
     */
    String encryptCardNumber(String cardNumber) {
        try {
            if (cardNumber.length() != 16) {
                throw new IllegalArgumentException("Номер карты должен содержать 16 цифр");
            }
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            System.err.println("Ошибка шифрования: " + e.getMessage());
            throw new RuntimeException("Ошибка шифрования", e);
        }
    }

    /**
     * Дешифрует номер карты, зашифрованный с использованием AES.
     *
     * @param encryptedCardNumber зашифрованный номер карты
     * @return расшифрованный номер карты
     * @throws RuntimeException если произошла ошибка дешифрования
     */
    public String decryptCardNumber(String encryptedCardNumber) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedCardNumber);
            byte[] iv = Arrays.copyOfRange(combined, 0, 16);
            byte[] encrypted = Arrays.copyOfRange(combined, 16, combined.length);
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка декодирования Base64: " + e.getMessage());
            throw new RuntimeException("Ошибка дешифрования: неверный формат Base64", e);
        } catch (BadPaddingException e) {
            System.err.println("Ошибка дешифрования: неверный padding - " + e.getMessage());
            throw new RuntimeException("Ошибка дешифрования: неверный padding", e);
        } catch (IllegalBlockSizeException e) {
            System.err.println("Ошибка дешифрования: неверный размер блока - " + e.getMessage());
            throw new RuntimeException("Ошибка дешифрования: неверный размер блока", e);
        } catch (InvalidKeyException e) {
            System.err.println("Ошибка дешифрования: неверный ключ - " + e.getMessage());
            throw new RuntimeException("Ошибка дешифрования: неверный ключ", e);
        } catch (Exception e) {
            System.err.println("Общая ошибка дешифрования: " + e.getMessage());
            throw new RuntimeException("Ошибка дешифрования", e);
        }
    }

    /**
     * Преобразует сущность карты в DTO.
     *
     * @param card сущность карты
     * @return DTO карты
     */
    private CardDTO mapToDTO(Card card) {
        CardDTO dto = new CardDTO();
        dto.setId(card.getId());
        String decrypted = decryptCardNumber(card.getCardNumber());
        dto.setMaskedCardNumber(String.format("**** **** **** %s", decrypted.substring(12)));
        dto.setOwnerUsername(card.getOwner().getUsername());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setStatus(card.getStatus());
        dto.setBalance(card.getBalance());
        return dto;
    }

    /**
     * Проверяет, является ли текущий пользователь администратором.
     *
     * @return true, если пользователь имеет роль ADMIN, false в противном случае
     */
    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
