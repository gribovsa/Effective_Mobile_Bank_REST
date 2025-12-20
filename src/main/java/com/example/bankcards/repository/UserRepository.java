package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Находит пользователя по имени пользователя.
     *
     * @param username имя пользователя
     * @return Optional с пользователем, если он найден, или пустой Optional
     */
    Optional<User> findByUsername(String username);

    /**
     * Находит пользователей, чьи имена содержат указанную подстроку, с пагинацией.
     *
     * @param username подстрока для поиска в имени пользователя
     * @param pageable параметры пагинации
     * @return страница с пользователями, соответствующими запросу
     */
    Page<User> findByUsernameContaining(String username, Pageable pageable);

    /**
     * Проверяет, существует ли пользователь с указанным именем.
     *
     * @param username имя пользователя
     * @return true, если пользователь существует, false в противном случае
     */
    boolean existsByUsername(String username);
}

