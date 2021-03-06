package com.julenka.api.primitiveBank.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST)
public class UncertainAccountException extends RuntimeException {

    public UncertainAccountException(String message) {
        super(message);
    }
}
