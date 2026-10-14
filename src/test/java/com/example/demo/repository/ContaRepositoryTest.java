package com.example.demo.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.example.demo.entity.ContaBancaria;

@DataJpaTest
class ContaRepositoryTest {

    @Autowired
    private ContaRepository repository;

    @Test
    @DisplayName("Salvar uma conta nova gera um id")
    void salvarGeraId() {
        ContaBancaria salva = repository.save(new ContaBancaria("Maria"));

        assertNotNull(salva.getId());
    }

    @Test
    @DisplayName("Contas diferentes recebem ids diferentes")
    void idsDiferentes() {
        ContaBancaria a = repository.save(new ContaBancaria("Maria"));
        ContaBancaria b = repository.save(new ContaBancaria("João"));

        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    @DisplayName("Encontra pelo id uma conta que foi salva")
    void encontraContaSalva() {
        ContaBancaria salva = repository.save(new ContaBancaria("Maria"));

        Optional<ContaBancaria> encontrada = repository.findById(salva.getId());

        assertTrue(encontrada.isPresent());
        assertEquals("Maria", encontrada.get().getTitular());
        assertEquals(0.0, encontrada.get().getSaldo(), 0.001);
    }

    @Test
    @DisplayName("Id que não existe devolve vazio")
    void idInexistenteDevolveVazio() {
        assertTrue(repository.findById(99999L).isEmpty());
    }

    @Test
    @DisplayName("Salvar de novo uma conta existente atualiza, não duplica")
    void salvarAtualiza() {
        ContaBancaria conta = repository.save(new ContaBancaria("Maria"));
        Long id = conta.getId();

        conta.depositar(50.0);
        repository.save(conta);

        assertEquals(id, conta.getId());
        assertEquals(50.0, repository.findById(id).get().getSaldo(), 0.001);
        assertEquals(1, repository.count());
    }
}
