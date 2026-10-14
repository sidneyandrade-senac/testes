package com.example.demo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.example.demo.model.Produto;

public class CalcularProdutoTest extends CalculaTotal {

    @Test
    void calcularTotal() {
        Produto p = new Produto();
        p.setNome("Teclado");
        p.setPreco(29.99);
        int quantidade = 3;
        Double totalEsperado = quantidade * p.getPreco();

        // estamos testando esse metodo, para analisar se de fato esta realizando a
        // conta correta
        // se trocar a varivel quantidade por um numero diferente do valor que foi
        // atribuido a variavel, retorna erro.
        Double total = setCalcularTotal(quantidade, p.getPreco());

        // Verificação
        // para comparar valores de ponto flutuante, é uma prática comum incluir um
        // delta, que é a diferença máxima permitida entre os valores.
        assertEquals(totalEsperado, total, 0.01, "O total calculado não corresponde ao esperado.");
    }
}
