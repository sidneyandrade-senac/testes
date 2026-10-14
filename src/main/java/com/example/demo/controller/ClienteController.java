package com.example.demo.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Cliente;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @GetMapping("/todos")
    public ResponseEntity<List<Cliente>> listarTodosClientes() {
        try {
            // Carrega o arquivo JSON de recursos, poderia ser dados do banco de dados.
            // em testes unitários, podemos "mokar" os dados para realizar os testes, com isso não depende da disponibilidade do banco
            InputStream inputStream = new ClassPathResource("clientes.json").getInputStream();

            // Mapeia o JSON para uma lista de objetos Cliente usando Jackson
            ObjectMapper objectMapper = new ObjectMapper();
            List<Cliente> clientes = objectMapper.readValue(inputStream, new TypeReference<List<Cliente>>() {});

            return new ResponseEntity<>(clientes, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
