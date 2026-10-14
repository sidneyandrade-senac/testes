package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.demo.model.User;

public class UserControllerTest {

    private UserController uc;
    private UserControllerTest() {
        uc = new UserController();
    }
    @Test
    public void testValidEmailAndPassword() {
        // verificar se uma determinada condição é verdadeira (assertTrue) ou falsa (assertFalse) - testa sempre o boolean
        assertTrue(uc.isValidEmail("test@example.com"));
        assertTrue(uc.isValidEmail("user123@test.com"));
        assertFalse(uc.isValidEmail(null));
        assertFalse(uc.isValidEmail("invalidEmail.com"));
    }
    @Test
    public void testValidPasswordLength() {
        assertTrue(uc.registerUser(new User("test@example.com", "password123")).getStatusCode()
                .is2xxSuccessful());
        assertFalse(
                uc.registerUser(new User("test@example.com", "pass")).getStatusCode().is2xxSuccessful());

    }
    @Test
    public void testPasswordsMatch() {
        assertTrue(uc.passwordsMatch("password123", "password123"));
        assertFalse(uc.passwordsMatch("password123", "differentpassword"));
    }
}
