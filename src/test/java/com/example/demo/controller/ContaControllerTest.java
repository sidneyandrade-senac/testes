package com.example.demo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.dto.ContaDTO;
import com.example.demo.dto.CriarContaDTO;
import com.example.demo.dto.OperacaoDTO;
import com.example.demo.service.ContaService;

@WebMvcTest(ContaController.class)
class ContaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContaService service;

    @Test
    @DisplayName("POST /contas cria a conta e responde 201 com o JSON")
    void criaConta() throws Exception {
        when(service.criarConta(any(CriarContaDTO.class)))
                .thenReturn(new ContaDTO(1L, "Maria", 0.0));

        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titular\":\"Maria\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.titular").value("Maria"))
                .andExpect(jsonPath("$.saldo").value(0.0));
    }

    @Test
    @DisplayName("POST /contas com titular inválido responde 400")
    void titularInvalido() throws Exception {
        when(service.criarConta(any(CriarContaDTO.class)))
                .thenThrow(new IllegalArgumentException("Titular é obrigatório"));

        mockMvc.perform(post("/contas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titular\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Titular é obrigatório"));
    }

    @Test
    @DisplayName("GET /contas/{id} devolve a conta")
    void buscaConta() throws Exception {
        when(service.buscar(1L)).thenReturn(new ContaDTO(1L, "Maria", 80.0));

        mockMvc.perform(get("/contas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(80.0));
    }

    @Test
    @DisplayName("GET /contas/{id} de conta inexistente responde 404")
    void contaInexistente() throws Exception {
        when(service.buscar(99L)).thenThrow(new NoSuchElementException("Conta não encontrada"));

        mockMvc.perform(get("/contas/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Conta não encontrada"));
    }

    @Test
    @DisplayName("POST /contas/{id}/depositar responde 200 com o novo saldo")
    void deposita() throws Exception {
        when(service.depositar(eq(1L), any(OperacaoDTO.class)))
                .thenReturn(new ContaDTO(1L, "Maria", 100.0));

        mockMvc.perform(post("/contas/1/depositar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":100.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(100.0));
    }

    @Test
    @DisplayName("POST /contas/{id}/sacar com saldo insuficiente responde 422")
    void saldoInsuficiente() throws Exception {
        when(service.sacar(eq(1L), any(OperacaoDTO.class)))
                .thenThrow(new IllegalStateException("Saldo insuficiente"));

        mockMvc.perform(post("/contas/1/sacar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valor\":500.0}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string("Saldo insuficiente"));
    }
}
