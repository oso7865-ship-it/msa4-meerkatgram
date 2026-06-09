package com.msa4meerkatgram.global.errors.custom;

public class InvalidPostCreateException extends RuntimeException {
    public InvalidPostCreateException(String message) {
        super(message);
    }
}
