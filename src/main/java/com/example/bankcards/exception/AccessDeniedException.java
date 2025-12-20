package com.example.bankcards.exception;

/**
 * Исключение, выбрасываемое при отсутствии прав доступа к ресурсу.
 */
public class AccessDeniedException extends RuntimeException{
    /**
     * Создаёт исключение с указанным сообщением.
     *
     * @param message сообщение об ошибке
     */
    public AccessDeniedException(String message) {
        super(message);
    }
}
