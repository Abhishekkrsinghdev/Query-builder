package com.querybuilder.backend.shared.exception;

/**
 * Exception thrown when trying to create a resource that already exists
 */
public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }

    public ResourceAlreadyExistsException(String resource, String field, Object value) {
        super(String.format("%s already exists with %s: %s", resource, field, value));
    }
}