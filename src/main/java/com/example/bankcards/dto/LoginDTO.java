package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


/**
 * DTO используется для передачи данных, необходимых для аутентификации пользователя.
 */
@Getter
@Setter
public class LoginDTO {
    /** Имя пользователя. */
    @NotBlank(message = "Username must be between 3 and 50 characters")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    /** Хэшированный пароль пользователя. */
    @NotBlank(message = "Password cannot be empty")
    private String password;
}
