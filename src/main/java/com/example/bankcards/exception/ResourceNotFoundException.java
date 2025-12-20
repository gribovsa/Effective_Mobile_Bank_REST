package com.example.bankcards.exception;

/**
 * Исключение, выбрасываемое при отсутствии запрашиваемого ресурса.
 */
public class ResourceNotFoundException extends RuntimeException{
    /**
     * Создаёт исключение с указанным сообщением.
     *
     * @param message сообщение об ошибке
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
