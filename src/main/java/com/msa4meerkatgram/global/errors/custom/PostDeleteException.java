package com.msa4meerkatgram.global.errors.custom;

public class PostDeleteException extends RuntimeException {
    public PostDeleteException(String message) {
        super(message);
    }
}
