package com.example.demo.dto;

import com.example.demo.entity.ContaBancaria;

/**
 * DTO de SAÍDA: o que é devolvido ao cliente.
 * Expõe só o necessário, sem entregar a entity.
 */
public record ContaDTO(Long id, String titular, double saldo) {

    // Converte a entity no DTO de saída
    public static ContaDTO de(ContaBancaria conta) {
        return new ContaDTO(conta.getId(), conta.getTitular(), conta.getSaldo());
    }
}
