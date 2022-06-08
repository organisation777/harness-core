package io.harness.exception.runtime;

import io.harness.eraro.ErrorCode;

public class AmazonS3ServerRuntimeException extends RuntimeException{
    private String message;
    Throwable cause;
    ErrorCode code = ErrorCode.INVALID_CREDENTIAL;

    public AmazonS3ServerRuntimeException(String message){
        this.message=message;
    }

    public AmazonS3ServerRuntimeException(String message,ErrorCode errorCode){
        this.message=message;
        this.code=errorCode;
    }

    public AmazonS3ServerRuntimeException(String message,Throwable cause){
        this.message=message;
        this.cause=cause;
    }

    public AmazonS3ServerRuntimeException(String message, Throwable cause, ErrorCode code) {
        this.message = message;
        this.cause = cause;
        this.code = code;
    }
}
