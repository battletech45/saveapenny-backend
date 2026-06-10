package com.saveapenny.auth.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StrongPasswordValidatorTest {

    private StrongPasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StrongPasswordValidator();
    }

    @Test
    void validPassword_returnsTrue() {
        assertTrue(validator.isValid("Strong@123", null));
    }

    @Test
    void nullPassword_returnsFalse() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void tooShort_returnsFalse() {
        assertFalse(validator.isValid("Ab@1", null));
    }

    @Test
    void noUppercase_returnsFalse() {
        assertFalse(validator.isValid("weak@1234", null));
    }

    @Test
    void noDigit_returnsFalse() {
        assertFalse(validator.isValid("Weak@pass", null));
    }

    @Test
    void noSpecialCharacter_returnsFalse() {
        assertFalse(validator.isValid("Weakpass1", null));
    }

    @Test
    void exactlyEightCharsWithAllRequirements_returnsTrue() {
        assertTrue(validator.isValid("Abcd@123", null));
    }

    @Test
    void uppercaseOnlyAtEnd_returnsTrue() {
        assertTrue(validator.isValid("abcd123@Z", null));
    }

    @Test
    void digitOnlyAtStart_returnsTrue() {
        assertTrue(validator.isValid("1Strong@pass", null));
    }

    @Test
    void specialCharacterOnlyInMiddle_returnsTrue() {
        assertTrue(validator.isValid("Strong@123", null));
    }

    @Test
    void multipleSpecialCharacters_returnsTrue() {
        assertTrue(validator.isValid("Str@ng#123", null));
    }

    @Test
    void allUppercase_returnsFalse_whenNoDigit() {
        assertFalse(validator.isValid("STRONG@PASS", null));
    }

    @Test
    void allLowercaseWithDigitAndSpecial_returnsFalse() {
        assertFalse(validator.isValid("weakpass1@", null));
    }
}
