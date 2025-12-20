package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO для представления информации о банковской карте.
 * в отличии от сущности Card здесь нужно отобразить замаскированный номер карты
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardDTO {
    /** Уникальный идентификатор карты. */
    @NotNull(message = "Card ID cannot be null")
    private Long id;

    /** Замаскированный номер карты (например, **** **** **** 1234). */
    @NotBlank(message = "Masked card number cannot be empty")
    private String maskedCardNumber;

    /** Имя пользователя, владельца карты. */
    @NotBlank(message = "Owner username cannot be empty")
    @Size(min = 3, max = 50, message = "Owner username must be between 3 and 50 characters")
    private String ownerUsername;

    /** Дата истечения срока действия карты. */
    @NotNull(message = "Expiry date cannot be null")
    private LocalDate expiryDate;

    /** Статус карты (ACTIVE, BLOCKED, EXPIRED). */
    @NotNull(message = "Status cannot be null")
    private CardStatus status;

    /** Текущий баланс карты, должен быть неотрицательным. */
    @NotNull(message = "Balance cannot be null")
    @PositiveOrZero(message = "Balance must be non-negative")
    private Double balance;

    /**
     * Сравнивает этот DTO с другим объектом на равенство по идентификатору.
     *
     * @param o объект для сравнения
     * @return true, если объекты равны, false в противном случае
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardDTO cardDTO = (CardDTO) o;
        return Objects.equals(id, cardDTO.id);
    }

    /**
     * Генерирует хэш-код на основе идентификатора.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    /**
     * Возвращает строковое представление DTO для отладки.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "CardDTO{" +
                "id=" + id +
                ", maskedCardNumber='" + maskedCardNumber + '\'' +
                ", ownerUsername='" + ownerUsername + '\'' +
                ", expiryDate=" + expiryDate +
                ", status=" + status +
                ", balance=" + balance +
                '}';
    }
}
