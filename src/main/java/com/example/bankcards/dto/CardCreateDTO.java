package com.example.bankcards.dto;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO для создания новой банковской карты.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardCreateDTO {

    /** Имя пользователя, владельца карты. */
    @NotBlank(message = "Owner username cannot be empty")
    @Size(min = 3, max = 50, message = "Owner username must be between 3 and 50 characters")
    private String ownerUsername;

    /** Дата истечения срока действия карты, должна быть в будущем. */
    @NotNull(message = "Expiry date cannot be null")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    /** Начальный баланс карты, должен быть неотрицательным. */
    @NotNull(message = "Initial balance cannot be null")
    @PositiveOrZero(message = "Initial balance must be non-negative")
    private Double initialBalance;

    /**
     * Сравнивает этот DTO с другим объектом на равенство по всем полям.
     *
     * @param o объект для сравнения
     * @return true, если объекты равны, false в противном случае
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardCreateDTO that = (CardCreateDTO) o;
        return Objects.equals(ownerUsername, that.ownerUsername) &&
                Objects.equals(expiryDate, that.expiryDate) &&
                Objects.equals(initialBalance, that.initialBalance);
    }

    /**
     * Генерирует хэш-код на основе всех полей.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        return Objects.hash(ownerUsername, expiryDate, initialBalance);
    }

    /**
     * Возвращает строковое представление DTO для отладки.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "CardCreateDTO{" +
                "ownerUsername='" + ownerUsername + '\'' +
                ", expiryDate=" + expiryDate +
                ", initialBalance=" + initialBalance +
                '}';
    }
}
