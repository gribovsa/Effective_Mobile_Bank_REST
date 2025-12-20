package com.example.bankcards.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * DTO для запроса перевода между картами.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDTO {

    /** Идентификатор карты-отправителя. */
    @NotNull(message = "Source card ID cannot be null")
    private Long fromCardId;

    /** Идентификатор карты-получателя. */
    @NotNull(message = "Destination card ID cannot be null")
    private Long toCardId;

    /** Сумма перевода, должна быть положительной. */
    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private Double amount;

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
        TransferRequestDTO that = (TransferRequestDTO) o;
        return Objects.equals(fromCardId, that.fromCardId) &&
                Objects.equals(toCardId, that.toCardId) &&
                Objects.equals(amount, that.amount);
    }

    /**
     * Генерирует хэш-код на основе всех полей.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + Objects.hashCode(fromCardId);
        result = 37 * result + Objects.hashCode(toCardId);
        result = 43 * result + Objects.hashCode(amount);
        return result;
    }

    /**
     * Возвращает строковое представление DTO для отладки.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "TransferRequestDTO{" +
                "fromCardId=" + fromCardId +
                ", toCardId=" + toCardId +
                ", amount=" + amount +
                '}';
    }
}
