package com.example.demo.entity;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cada teste segue o padrão AAA:
 *   Arrange (preparar)  -> monta o cenário
 *   Act     (agir)      -> executa o que está sendo testado
 *   Assert  (verificar) -> confere se o resultado é o esperado
 *
 * Um teste bom responde: "se alguém quebrar essa regra, eu aviso?"
 *
 * Repare que a conta é criada com "new": é um teste unitário puro,
 * sem subir o Spring e sem banco de dados, por isso roda em milissegundos.
 * As anotações JPA da entity são ignoradas aqui.
 */
class ContaBancariaTest {

    private ContaBancaria conta;

    // Roda ANTES de cada teste: cada teste começa com uma conta nova,
    // assim um teste nunca depende do resultado de outro.
    @BeforeEach
    void preparar() {
        conta = new ContaBancaria("Maria");
    }

    @Test
    @DisplayName("Conta nova começa com o titular informado e saldo zero")
    void contaNovaComecaComSaldoZero() {
        assertEquals("Maria", conta.getTitular());
        assertEquals(0.0, conta.getSaldo(), 0.001);
    }

    // assertNull: confere que algo é nulo. O id é gerado pelo banco ao salvar;
    // como esta conta ainda não foi salva, ele precisa ser null.
    @Test
    @DisplayName("Conta nova ainda não tem id (o id é gerado pelo banco ao salvar)")
    void contaNovaAindaNaoTemId() {
        assertNull(conta.getId());
    }

    // Um mesmo teste executado com vários valores de entrada
    // (null, "" e "   " são todos titulares inválidos)
    @ParameterizedTest(name = "titular [{0}] deve ser recusado")
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    @DisplayName("Não permite criar conta sem titular")
    void naoPermiteContaSemTitular(String titularInvalido) {
        assertThrows(IllegalArgumentException.class,
                () -> new ContaBancaria(titularInvalido));
    }

    @Test
    @DisplayName("Depositar aumenta o saldo")
    void depositarAumentaSaldo() {
        // Act
        conta.depositar(100.0);

        // Assert
        assertEquals(100.0, conta.getSaldo(), 0.001);
    }

    @Test
    @DisplayName("Vários depósitos são somados")
    void variosDepositosSaoSomados() {
        conta.depositar(100.0);
        conta.depositar(50.5);

        assertEquals(150.5, conta.getSaldo(), 0.001);
    }

    @Test
    @DisplayName("Sacar diminui o saldo")
    void sacarDiminuiSaldo() {
        // Arrange
        conta.depositar(100.0);

        // Act
        conta.sacar(30.0);

        // Assert
        assertEquals(70.0, conta.getSaldo(), 0.001);
    }

    // Caso de LIMITE: sacar exatamente o saldo é permitido (a regra só
    // proíbe sacar MAIS do que o saldo). É nos limites que os bugs aparecem.
    @Test
    @DisplayName("Permite sacar exatamente o saldo, deixando a conta zerada")
    void permiteSacarOSaldoTotal() {
        conta.depositar(100.0);

        conta.sacar(100.0);

        assertEquals(0.0, conta.getSaldo(), 0.001);
    }

    // assertTrue / assertFalse: usados quando o método devolve um boolean.
    // Testamos os dois lados e o valor exatamente no limite.
    @Test
    @DisplayName("Informa se o saldo cobre o valor (inclusive no limite)")
    void informaSeTemSaldoPara() {
        conta.depositar(100.0);

        assertTrue(conta.temSaldoPara(50.0));    // sobra saldo
        assertTrue(conta.temSaldoPara(100.0));   // exatamente o saldo
        assertFalse(conta.temSaldoPara(100.01)); // um centavo a mais
    }

    @Test
    @DisplayName("Conta sem saldo não cobre nenhum valor positivo")
    void contaSemSaldoNaoCobreValor() {
        assertFalse(conta.temSaldoPara(0.01));
    }

    // assertArrayEquals: compara dois arrays posição por posição. Aqui
    // registramos o saldo depois de cada operação e conferimos a sequência
    // inteira de uma vez (com delta, porque são double).
    @Test
    @DisplayName("O saldo evolui na ordem correta a cada operação")
    void saldoEvoluiACadaOperacao() {
        double[] esperado = { 100.0, 150.5, 120.5, 0.0 };
        double[] real = new double[4];

        conta.depositar(100.0);
        real[0] = conta.getSaldo();
        conta.depositar(50.5);
        real[1] = conta.getSaldo();
        conta.sacar(30.0);
        real[2] = conta.getSaldo();
        conta.sacar(120.5);
        real[3] = conta.getSaldo();

        assertArrayEquals(esperado, real, 0.001);
    }

    // Testar o "caminho feliz" não basta: o valor de um teste está em
    // garantir que as REGRAS DE PROTEÇÃO funcionam. assertThrows verifica
    // que o código lança o erro esperado.
    @Test
    @DisplayName("Não permite sacar mais do que o saldo")
    void naoPermiteSacarMaisQueOSaldo() {
        conta.depositar(50.0);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> conta.sacar(80.0));

        assertEquals("Saldo insuficiente", erro.getMessage());
        // E o saldo não pode ter mudado depois da tentativa que falhou
        assertEquals(50.0, conta.getSaldo(), 0.001);
    }

    @ParameterizedTest(name = "depositar {0} deve ser recusado")
    @ValueSource(doubles = { 0, -1, -100.5 })
    @DisplayName("Não permite depositar zero ou valor negativo")
    void naoPermiteDepositoInvalido(double valorInvalido) {
        assertThrows(IllegalArgumentException.class,
                () -> conta.depositar(valorInvalido));
    }

    @ParameterizedTest(name = "sacar {0} deve ser recusado")
    @ValueSource(doubles = { 0, -10 })
    @DisplayName("Não permite sacar zero ou valor negativo")
    void naoPermiteSaqueInvalido(double valorInvalido) {
        conta.depositar(100.0);

        assertThrows(IllegalArgumentException.class,
                () -> conta.sacar(valorInvalido));
    }

    // ---------------------------------------------------------------
    // DEMONSTRAÇÃO: COMO UM TESTE FALHA
    // Tire o @Disabled, rode `.\mvnw test` e veja o erro.
    // Depois, em ContaBancaria.temSaldoPara, troque "valor <= this.saldo"
    // por "valor <= this.saldo + 1000" e rode de novo: o teste
    // "naoPermiteSacarMaisQueOSaldo" vai FALHAR, avisando que a regra
    // foi quebrada. É para isso que os testes servem.
    // ---------------------------------------------------------------
    @Disabled("Remova esta linha para ver um teste falhando")
    @Test
    @DisplayName("Exemplo de teste que falha (valor esperado errado de propósito)")
    void exemploDeTesteQueFalha() {
        conta.depositar(100.0);

        // Esperado 90, mas o código desconta 30 => 70. O teste falha.
        conta.sacar(30.0);
        assertEquals(90.0, conta.getSaldo(), 0.001);
    }
}
