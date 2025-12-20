package com.example.bankcards.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * DTO для обновления пароля пользователя.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUpdateDTO {
    /** Новый пароль пользователя, должен содержать от 4 до 100 символов. */
    @NotNull(message = "Password cannot be null")
    @Size(min = 4, max = 100, message = "Password must be between 4 and 100 characters")
    private String password;

    /**
     * Сравнивает этот DTO с другим объектом на равенство по полю пароля.
     *
     * @param o объект для сравнения
     * @return true, если объекты равны, false в противном случае
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordUpdateDTO that = (PasswordUpdateDTO) o;
        return Objects.equals(password, that.password);
    }

    /**
     * Генерирует хэш-код на основе поля пароля.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        return Objects.hash(password);
    }

    /**
     * Возвращает строковое представление DTO, скрывая пароль для безопасности.
     * Предотвращение случайной утечки пароля.
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "PasswordUpdateDTO{password='[REDACTED]'}";
    }
}
