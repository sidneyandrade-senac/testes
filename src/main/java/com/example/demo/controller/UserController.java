package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.User;

@RestController
public class UserController {
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        // Verifica se o e-mail é válido e a senha tem no mínimo 6 caracteres
        if (!isValidEmail(user.getEmail()) || user.getPassword().length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("E-mail ou senha inválidos");
        }

        // Lógica de registro do usuário aqui

        return ResponseEntity.ok("Usuário registrado com sucesso");
    }
    // esse metodo poderia estar em outro pacote, para ser utilizado por todo o sistema
    // foi feito aqui somente para facilitar o processo
    // Método para validar e-mail
    boolean isValidEmail(String email) {
        // Implementação básica de validação de e-mail
        // Você pode usar bibliotecas de validação de e-mail mais robustas
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public boolean passwordsMatch(String password1, String password2) {
        return password1.equals(password2);
    }
}
