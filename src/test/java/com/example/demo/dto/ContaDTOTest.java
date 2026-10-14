package com.example.demo.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.entity.ContaBancaria;

class ContaDTOTest {

    @Test
    @DisplayName("Converte a conta (entity) no DTO de saída")
    void converteContaParaDTO() {
        ContaBancaria conta = new ContaBancaria("Maria");
        conta.depositar(80.0);

        ContaDTO dto = ContaDTO.de(conta);

        // assertNotNull: confere que o resultado existe e que o campo veio preenchido
        assertNotNull(dto);
        assertNotNull(dto.titular());
        assertEquals("Maria", dto.titular());
        assertEquals(80.0, dto.saldo(), 0.001);
    }

    // O record gera o equals automaticamente: dois DTOs com os mesmos dados são iguais
    @Test
    @DisplayName("Contas com os mesmos dados geram DTOs iguais")
    void contasIguaisGeramDTOsIguais() {
        ContaBancaria a = new ContaBancaria("Maria");
        ContaBancaria b = new ContaBancaria("Maria");
        a.depositar(80.0);
        b.depositar(80.0);

        assertEquals(ContaDTO.de(a), ContaDTO.de(b));
    }

    // assertNotNull... e o contrário: assertNotEquals confere que dois valores
    // são DIFERENTES. Aqui, uma diferença de saldo basta para os DTOs diferirem.
    @Test
    @DisplayName("Contas com saldos diferentes geram DTOs diferentes")
    void contasDiferentesGeramDTOsDiferentes() {
        ContaBancaria a = new ContaBancaria("Maria");
        ContaBancaria b = new ContaBancaria("Maria");
        a.depositar(80.0);
        b.depositar(80.5);

        assertNotEquals(ContaDTO.de(a), ContaDTO.de(b));
    }
}
