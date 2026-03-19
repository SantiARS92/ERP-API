package es.pedro.pymes_backend.infrastructure.web.exception;

public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}