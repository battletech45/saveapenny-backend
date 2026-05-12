package com.saveapenny.auth.exception;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(String roleName) {
        super("Role not found: " + roleName);
    }
}
