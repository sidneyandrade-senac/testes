package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dto.ContaDTO;
import com.example.demo.dto.CriarContaDTO;
import com.example.demo.dto.OperacaoDTO;
import com.example.demo.entity.ContaBancaria;
import com.example.demo.repository.ContaRepository;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    @Mock
    private ContaRepository repository;

    private ContaService service;

    @BeforeEach
    void preparar() {
        service = new ContaService(repository);
    }

    private ContaBancaria contaComSaldo(double saldo) {
        ContaBancaria conta = new ContaBancaria("Maria");
        if (saldo > 0) {
            conta.depositar(saldo);
        }
        return conta;
    }

    private void repositoryTemConta(ContaBancaria conta) {
        when(repository.findById(1L)).thenReturn(Optional.of(conta));
    }

    private void repositorySalvaERetornaOMesmo() {
        when(repository.save(any(ContaBancaria.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    @DisplayName("Cria a conta com o titular informado e salva no repository")
    void criaContaESalva() {
        repositorySalvaERetornaOMesmo();

        ContaDTO resposta = service.criarConta(new CriarContaDTO("Maria"));

        assertEquals("Maria", resposta.titular());
        assertEquals(0.0, resposta.saldo(), 0.001);

        ArgumentCaptor<ContaBancaria> captor = ArgumentCaptor.forClass(ContaBancaria.class);
        verify(repository).save(captor.capture());
        assertEquals("Maria", captor.getValue().getTitular());
    }

    @Test
    @DisplayName("Titular inválido lança erro e não salva nada")
    void titularInvalidoNaoSalva() {
        assertThrows(IllegalArgumentException.class,
                () -> service.criarConta(new CriarContaDTO("  ")));

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("Busca uma conta existente e devolve o DTO")
    void buscaContaExistente() {
        repositoryTemConta(contaComSaldo(80.0));

        ContaDTO resposta = service.buscar(1L);

        assertEquals("Maria", resposta.titular());
        assertEquals(80.0, resposta.saldo(), 0.001);
    }

    @Test
    @DisplayName("Conta inexistente lança erro e não salva nada")
    void contaInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        NoSuchElementException erro = assertThrows(NoSuchElementException.class,
                () -> service.depositar(99L, new OperacaoDTO(10.0)));

        assertEquals("Conta não encontrada", erro.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Depositar atualiza o saldo e salva a conta")
    void depositarSalva() {
        ContaBancaria conta = contaComSaldo(0.0);
        repositoryTemConta(conta);
        repositorySalvaERetornaOMesmo();

        ContaDTO resposta = service.depositar(1L, new OperacaoDTO(100.0));

        assertEquals(100.0, resposta.saldo(), 0.001);
        verify(repository).save(conta);
    }

    @Test
    @DisplayName("Depósito inválido lança erro e não salva nada")
    void depositoInvalidoNaoSalva() {
        repositoryTemConta(contaComSaldo(0.0));

        assertThrows(IllegalArgumentException.class,
                () -> service.depositar(1L, new OperacaoDTO(-5.0)));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Sacar atualiza o saldo e salva a conta")
    void sacarSalva() {
        ContaBancaria conta = contaComSaldo(100.0);
        repositoryTemConta(conta);
        repositorySalvaERetornaOMesmo();

        ContaDTO resposta = service.sacar(1L, new OperacaoDTO(30.0));

        assertEquals(70.0, resposta.saldo(), 0.001);
        verify(repository).save(conta);
    }

    @Test
    @DisplayName("Saque acima do saldo lança erro e não salva nada")
    void saqueAcimaDoSaldoNaoSalva() {
        ContaBancaria conta = contaComSaldo(50.0);
        repositoryTemConta(conta);

        assertThrows(IllegalStateException.class,
                () -> service.sacar(1L, new OperacaoDTO(80.0)));

        assertEquals(50.0, conta.getSaldo(), 0.001);
        verify(repository, never()).save(any());
    }
}
