package com.msa4meerkatgram.global.responses;



public record GlobalResponse<T> (
    String code,
    String message,
    T data
) {
    public static <T> GlobalResponse<T> from(String code, String message, T data){
        return new GlobalResponse<>(code,message,data);
    }
}
