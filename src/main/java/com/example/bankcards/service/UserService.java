package com.example.bankcards.service;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.LoginDTO;
import com.example.bankcards.dto.PasswordUpdateDTO;
import com.example.bankcards.dto.RoleUpdateDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для управления пользователями и аутентификацией.
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final CardService cardService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository, CardService cardService,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.cardService = cardService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Регистрирует нового пользователя.
     *
     * @param user данные пользователя
     * @return JWT-токен для зарегистрированного пользователя
     * @throws IllegalArgumentException если имя пользователя уже существует
     */
    @Transactional
    public String register(User user) {
        logger.info("Регистрация пользователя: {}", user.getUsername());
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Имя пользователя уже занято");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getUsername());
        logger.info("Сгенерирован токен для пользователя {}: {}", user.getUsername(), token);
        return token;
    }

    /**
     * Выполняет вход пользователя.
     *
     * @param loginDTO данные для входа (имя и пароль)
     * @return JWT-токен
     * @throws BadCredentialsException если учетные данные неверны
     */
    @Transactional(readOnly = true)
    public String login(LoginDTO loginDTO) {
        logger.info("Попытка входа пользователя: {}", loginDTO.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword()));
            String token = jwtUtil.generateToken(authentication.getName());
            logger.info("Сгенерирован токен для пользователя {}: {}", authentication.getName(), token);
            return token;
        } catch (BadCredentialsException e) {
            logger.error("Неверные учетные данные для пользователя: {}", loginDTO.getUsername());
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    /**
     * Создаёт нового пользователя (для администратора).
     *
     * @param user данные пользователя
     * @return сообщение об успешном создании
     * @throws IllegalArgumentException если имя пользователя уже существует или роль недопустима
     * @throws AccessDeniedException если текущий пользователь не имеет прав назначить роль ADMIN
     */
    @Transactional
    public String createUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Имя пользователя уже существует");
        }
        String currentRole = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().substring(5)) // Удаляем "ROLE_"
                .orElse("USER");
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(currentRole)) {
            throw new AccessDeniedException("Только администратор может назначить роль ADMIN");
        }
        if (!"USER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("Недопустимая роль. Используйте 'USER' или 'ADMIN'");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return "Пользователь создан";
    }

    /**
     * Удаляет пользователя по идентификатору.
     *
     * @param userId идентификатор пользователя
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Пользователь не найден");
        }
        userRepository.deleteById(userId);
    }

    /**
     * Обновляет пароль пользователя.
     *
     * @param userId     идентификатор пользователя
     * @param passwordDTO DTO с новым паролем
     * @return сообщение об успешном обновлении
     * @throws ResourceNotFoundException если пользователь не найден
     */
    @Transactional
    public String updateUserPassword(Long userId, PasswordUpdateDTO passwordDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        user.setPassword(passwordEncoder.encode(passwordDTO.getPassword()));
        userRepository.save(user);
        return "Пароль обновлён";
    }

    /**
     * Обновляет роль пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param roleDTO DTO с новой ролью
     * @return сообщение об успешном обновлении
     * @throws ResourceNotFoundException если пользователь не найден
     * @throws AccessDeniedException если текущий пользователь не имеет прав назначить роль ADMIN
     * @throws IllegalArgumentException если роль недопустима
     */
    @Transactional
    public String updateUserRole(Long userId, RoleUpdateDTO roleDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        String currentRole = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().substring(5))
                .orElse("USER");
        if ("ADMIN".equals(roleDTO.getRole()) && !"ADMIN".equals(currentRole)) {
            throw new AccessDeniedException("Только администратор может назначить роль ADMIN");
        }
        if (!"USER".equals(roleDTO.getRole()) && !"ADMIN".equals(roleDTO.getRole())) {
            throw new IllegalArgumentException("Недопустимая роль. Используйте 'USER' или 'ADMIN'");
        }
        user.setRole(roleDTO.getRole());
        userRepository.save(user);
        return "Роль обновлена";
    }

    /**
     * Получает список пользователей с пагинацией и возможностью поиска по имени.
     *
     * @param username имя пользователя для поиска (опционально)
     * @param pageable параметры пагинации
     * @return страница с DTO пользователей
     */
    @Transactional(readOnly = true)
    public Page<UserDTO> getUsers(String username, Pageable pageable) {
        Page<User> usersPage;
        if (username != null && !username.isEmpty()) {
            usersPage = userRepository.findByUsernameContaining(username, pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }
        return usersPage.map(user -> {
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setRole(user.getRole());
            dto.setCards(user.getCards().stream().map(card -> {
                CardDTO cardDTO = new CardDTO();
                cardDTO.setId(card.getId());
                String decryptedNumber = cardService.decryptCardNumber(card.getCardNumber());
                cardDTO.setMaskedCardNumber(String.format("**** **** **** %s", decryptedNumber.substring(12)));
                cardDTO.setOwnerUsername(card.getOwner().getUsername());
                cardDTO.setExpiryDate(card.getExpiryDate());
                cardDTO.setStatus(card.getStatus());
                cardDTO.setBalance(card.getBalance());
                return cardDTO;
            }).collect(Collectors.toList()));
            return dto;
        });
    }
}
