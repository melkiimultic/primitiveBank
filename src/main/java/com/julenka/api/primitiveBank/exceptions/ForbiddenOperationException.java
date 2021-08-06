package com.julenka.api.primitiveBank.exceptions;

public class ForbiddenOperationException extends RuntimeException{
    public ForbiddenOperationException(String message){
        super(message);
    }
}
