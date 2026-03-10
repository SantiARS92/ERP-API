package es.pedro.pymes_backend.infrastructure.web.exception;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}