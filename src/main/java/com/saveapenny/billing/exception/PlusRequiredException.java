package com.saveapenny.billing.exception;

public class PlusRequiredException extends RuntimeException {

    private final String feature;

    public PlusRequiredException(String feature) {
        super("This feature requires a Plus subscription: " + feature);
        this.feature = feature;
    }

    public String getFeature() {
        return feature;
    }
}
