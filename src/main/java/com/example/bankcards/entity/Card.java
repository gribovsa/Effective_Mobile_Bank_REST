package com.example.bankcards.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Сущность, представляющая банковскую карту в системе.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cards", indexes = @Index(columnList = "cardNumber"))

public class Card {
    /** Уникальный идентификатор карты. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Номер карты, должен быть уникальным. */
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Card number cannot be empty") //проверяет, что строка не пуста
    private String cardNumber;

    /** Владелец карты. */
    @ManyToOne(fetch = FetchType.LAZY) //несколько экземпляров текущего класса могут ссылаться на один и тот же экземпляр класса User
    @JoinColumn(name = "owner_id", nullable = false) //в базе данных связь будет установлена через столбец owner_id
    private User owner;

    /** Дата истечения срока действия карты. */
    @Column(nullable = false)
    private LocalDate expiryDate;

    /** Статус карты (ACTIVE, BLOCKED, EXPIRED). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    /** Текущий баланс карты, должен быть неотрицательным. */
    @Column(nullable = false)
    private Double balance;

    /**
     * Переопределим метод toString базового класса
     * Возвращает строковое представление сущности для отладки.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "Card{id=" + id + ", cardNumber='" + cardNumber + "', expiryDate=" + expiryDate + ", status=" + status + ", balance=" + balance + "}";
    }

    /**
     * Переопределим метод equals базового класса
     * Сравнивает эту сущность с другим объектом на равенство по идентификатору.
     *
     * @param o объект для сравнения
     * @return true, если объекты равны, false в противном случае
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return id != null && id.equals(card.id);
    }

    /**
     * Переопределим метод hashCode базового класса
     * Генерирует хэш-код на основе идентификатора.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
