package com.example.demo.service;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.example.demo.dto.ContaDTO;
import com.example.demo.dto.CriarContaDTO;
import com.example.demo.dto.OperacaoDTO;
import com.example.demo.entity.ContaBancaria;
import com.example.demo.repository.ContaRepository;

@Service
public class ContaService {

    private final ContaRepository repository;

    public ContaService(ContaRepository repository) {
        this.repository = repository;
    }

    public ContaDTO criarConta(CriarContaDTO dto) {
        ContaBancaria conta = new ContaBancaria(dto.titular());
        return ContaDTO.de(repository.save(conta));
    }

    public ContaDTO buscar(Long id) {
        return ContaDTO.de(buscarConta(id));
    }

    public ContaDTO depositar(Long id, OperacaoDTO dto) {
        ContaBancaria conta = buscarConta(id);
        conta.depositar(dto.valor());
        return ContaDTO.de(repository.save(conta));
    }

    public ContaDTO sacar(Long id, OperacaoDTO dto) {
        ContaBancaria conta = buscarConta(id);
        conta.sacar(dto.valor());
        return ContaDTO.de(repository.save(conta));
    }

    private ContaBancaria buscarConta(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Conta não encontrada"));
    }
}
