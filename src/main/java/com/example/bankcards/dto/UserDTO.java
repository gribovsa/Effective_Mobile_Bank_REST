package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DTO для представления информации о пользователе.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    /** Уникальный идентификатор пользователя. */
    @NotNull(message = "User ID cannot be null")
    private Long id;

    /** Имя пользователя. */
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /** Роль пользователя (ADMIN или USER). */
    @NotNull(message = "Role cannot be null")
    private String role;

    /** Список карт, принадлежащих пользователю. */
    private List<CardDTO> cards = new ArrayList<>();

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
        UserDTO userDTO = (UserDTO) o;
        return Objects.equals(id, userDTO.id);
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
        return "UserDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}
