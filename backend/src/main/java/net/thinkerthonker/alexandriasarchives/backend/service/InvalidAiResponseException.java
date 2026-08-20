package net.thinkerthonker.alexandriasarchives.backend.service;

public class InvalidAiResponseException extends RuntimeException {
    public InvalidAiResponseException(String message) { super(message); }
}
