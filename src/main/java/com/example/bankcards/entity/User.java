package com.example.bankcards.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Сущность, представляющая пользователя в банковской системе.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    /** Уникальный идентификатор пользователя. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Имя пользователя, должно быть уникальным. */
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /** Хэшированный пароль пользователя. */
    @Column(nullable = false)
    @NotBlank(message = "Password cannot be empty")
    private String password;

    /** Роль пользователя (ADMIN или USER). */
    @Column(nullable = false)
    private String role; // ADMIN или USER

    /** Список карт, принадлежащих пользователю.
     *  @OneToMany указывает на связь "один ко многим" между сущностью, в которой она находится, и сущностью Card
     *  mappedBy указывает, что связь управляется полем owner в классе Card
     *  cascade = CascadeType.ALL указывает, что все операции каскадирования (например, сохранение, обновление, удаление) будут применяться к связанным объектам Card
     *  fetch = FetchType.LAZY указывает, что связанные объекты Card будут загружаться только по мере необходимости
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Card> cards = new ArrayList<>();

    /**
     * Добавляет карту в список карт пользователя и устанавливает владельца карты.
     *
     * @param card карта для добавления
     */
    public void addCard(Card card) {
        cards.add(card);
        card.setOwner(this);
    }

    /**
     * Переопределим метод toString базового класса
     * Возвращает строковое представление сущности для отладки.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString(){
        return "User{id=" + id + ", username='" + username + "', role='" + role + "'}";
    }

    /**
     * Переопределим метод equals базового класса
     * Сравнивает эту сущность с другим объектом на равенство по идентификатору.
     *
     * @param o объект для сравнения
     * @return true, если объекты равны, false в противном случае
     */
    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    /**
     * Генерирует хэш-код на основе идентификатора.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode(){
        return Objects.hash(id);
    }
}
