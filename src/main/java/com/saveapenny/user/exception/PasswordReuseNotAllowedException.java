package com.saveapenny.user.exception;

public class PasswordReuseNotAllowedException extends RuntimeException {

    public PasswordReuseNotAllowedException() {
        super("New password must be different from current password.");
    }
}
