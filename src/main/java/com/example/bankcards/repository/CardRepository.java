package com.example.bankcards.repository;


import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для работы с сущностями банковских карт.
 */
public interface CardRepository extends JpaRepository<Card, Long> {


    /**
     * Проверяет, существует ли карта с указанным номером.
     *
     * @param cardNumber номер карты
     * @return true, если карта с таким номером существует, false в противном случае
     */
    boolean existsByCardNumber(String cardNumber);


    /**
     * Находит все карты, принадлежащие указанному пользователю, с пагинацией.
     *
     * @param owner    пользователь-владелец карт
     * @param pageable параметры пагинации
     * @return страница с картами пользователя
     */
    Page<Card> findByOwner(User owner, Pageable pageable);


    /**
     * Находит карты пользователя, содержащие указанную подстроку в номере карты, с пагинацией.
     *
     * @param owner    пользователь-владелец карт
     * @param query    подстрока для поиска в номере карты
     * @param pageable параметры пагинации
     * @return страница с картами, соответствующими запросу
     */
    Page<Card> findByOwnerAndCardNumberContaining(User owner, String query, Pageable pageable);


    /**
     * Находит карты пользователя с указанным статусом, с пагинацией.
     *
     * @param owner    пользователь-владелец карт
     * @param status   статус карты (ACTIVE, BLOCKED, EXPIRED)
     * @param pageable параметры пагинации
     * @return страница с картами, соответствующими статусу
     */
    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);
}
