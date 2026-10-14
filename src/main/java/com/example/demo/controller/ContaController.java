package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ContaDTO;
import com.example.demo.dto.CriarContaDTO;
import com.example.demo.dto.OperacaoDTO;
import com.example.demo.service.ContaService;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ContaDTO> criar(@RequestBody CriarContaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarConta(dto));
    }

    @GetMapping("/{id}")
    public ContaDTO buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/depositar")
    public ContaDTO depositar(@PathVariable Long id, @RequestBody OperacaoDTO dto) {
        return service.depositar(id, dto);
    }

    @PostMapping("/{id}/sacar")
    public ContaDTO sacar(@PathVariable Long id, @RequestBody OperacaoDTO dto) {
        return service.sacar(id, dto);
    }
}
