package com.cosmocats.cosmo_cats.exception;

// Кидаємо, коли page або size мають неприпустиме значення
public class InvalidPageRequestException extends RuntimeException {
    public InvalidPageRequestException(String message) {
        super(message);
    }
}