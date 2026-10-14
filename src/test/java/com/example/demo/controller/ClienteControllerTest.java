package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.example.demo.model.Cliente;

class ClienteControllerTest {

    private ClienteController ct;

    private ClienteControllerTest() {
        ct = new ClienteController();
    }

    @Test
    void testListarClientesAtivos() {
        // Chama o método para listar clientes ativos
        ResponseEntity<List<Cliente>> responseEntity = ct.listarTodosClientes();

        List<Cliente> clientesAtivos = responseEntity.getBody();
        // Verifica se a resposta não é nula
        assertNotNull(responseEntity.getBody());

        if (clientesAtivos != null) {
            // Se estiver na faixa de sucesso, o método
            // is2xxSuccessful() retornará true
            assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
            // Verifica se o primeiro cliente na lista está ativo
            assertEquals("Ativo", clientesAtivos.get(0).getStatus());
            assertEquals("anativo", clientesAtivos.get(1).getStatus());
        }
    }

    @Test
    void testListarClientesNotNull() {
        ResponseEntity<List<Cliente>> responseEntity = ct.listarTodosClientes();
        // Verifica se a resposta não é nula
        assertNotNull(responseEntity.getBody());
    }

    @Test
    public void testAtributosPreenchidos() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("João");
        cliente.setSexo("Masculino");
        cliente.setDataNascimento("1990-01-01");
        cliente.setEndereco("Rua Teste, 123");
        cliente.setStatus("Ativo");

        assertNotNull(cliente.getId());
        assertNotNull(cliente.getNome());
        assertNotNull(cliente.getSexo());
        assertNotNull(cliente.getDataNascimento());
        assertNotNull(cliente.getEndereco());
        assertNotNull(cliente.getStatus());
    }

    @Test
    public void testCompararClientes() {
        // Criando dois objetos Cliente com os mesmos atributos
        Cliente cliente1 = new Cliente();
        cliente1.setId(1L);
        cliente1.setNome("João");
        cliente1.setSexo("Masculino");
        cliente1.setDataNascimento("1990-01-01");
        cliente1.setEndereco("Rua Teste, 123");
        cliente1.setStatus("Ativo");

        Cliente cliente2 = new Cliente();
        cliente2.setId(1L);
        cliente2.setNome("João");
        cliente2.setSexo("Masculino");
        cliente2.setDataNascimento("1990-01-01");
        cliente2.setEndereco("Rua Teste, 123");
        cliente2.setStatus("Ativo");

        // Verificando se todos os dados dos dois objetos são iguais
        assertEquals(cliente1.getId(), cliente2.getId());
        assertEquals(cliente1.getNome(), cliente2.getNome());
        assertEquals(cliente1.getSexo(), cliente2.getSexo());
        assertEquals(cliente1.getDataNascimento(), cliente2.getDataNascimento());
        assertEquals(cliente1.getEndereco(), cliente2.getEndereco());
        assertEquals(cliente1.getStatus(), cliente2.getStatus());
    }

    @Test
    public void testCompararClientesAtributoDiferente() {
        // Criando dois objetos Cliente com os mesmos atributos, exceto o nome
        Cliente cliente1 = new Cliente();
        cliente1.setId(1L);
        cliente1.setNome("João");
        cliente1.setSexo("Masculino");
        cliente1.setDataNascimento("1990-01-01");
        cliente1.setEndereco("Rua Teste, 123");
        cliente1.setStatus("Ativo");

        Cliente cliente2 = new Cliente();
        cliente2.setId(1L);
        cliente2.setNome("Maria"); // Diferente do nome de cliente1
        cliente2.setSexo("Masculino");
        cliente2.setDataNascimento("1990-01-01");
        cliente2.setEndereco("Rua Teste, 123");
        cliente2.setStatus("Ativo");

        // Verificando se pelo menos um dos valores dos atributos é diferente
        assertFalse(cliente1.getId().equals(cliente2.getId())
                && cliente1.getNome().equals(cliente2.getNome())
                && cliente1.getSexo().equals(cliente2.getSexo())
                && cliente1.getDataNascimento().equals(cliente2.getDataNascimento())
                && cliente1.getEndereco().equals(cliente2.getEndereco())
                && cliente1.getStatus().equals(cliente2.getStatus()));
    }
}
