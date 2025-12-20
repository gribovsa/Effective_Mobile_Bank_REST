package com.example.bankcards.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * DTO для обновления роли пользователя.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateDTO {

    /** Роль пользователя (ADMIN или USER). */
    @NotNull(message = "Role cannot be null")
    private String role;

    /**
     * Сравнивает этот DTO с другим объектом на равенство по полю роли.
     *
     * @param o объект для сравнения
     * @return true, если объекты равны, false в противном случае
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleUpdateDTO that = (RoleUpdateDTO) o;
        return Objects.equals(role, that.role);
    }

    /**
     * Генерирует хэш-код на основе поля роли.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        return Objects.hash(role);
    }

    /**
     * Возвращает строковое представление DTO для отладки.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "RoleUpdateDTO{role=" + role + '}';
    }
}
