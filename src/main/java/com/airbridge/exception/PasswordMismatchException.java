package com.airbridge.exception;

public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException() {
        super("Password and confirm password do not match");
    }
}
