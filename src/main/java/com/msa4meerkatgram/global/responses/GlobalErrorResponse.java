package com.msa4meerkatgram.global.responses;

public record GlobalErrorResponse(
    String code,
    String message
) {
    public static GlobalErrorResponse from(String code, String message){
        return new GlobalErrorResponse(code,message);
    }
}
