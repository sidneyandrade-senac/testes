package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidade JPA com MODELO RICO: além dos dados, a classe carrega as próprias
 * regras de negócio e não deixa a conta ficar em um estado inválido.
 *
 * Regras (é isso que os testes vão proteger):
 *  1. A conta precisa ter um titular.
 *  2. A conta começa com saldo 0.
 *  3. Só é possível depositar valores maiores que zero.
 *  4. Só é possível sacar valores maiores que zero.
 *  5. Não é possível sacar mais do que o saldo disponível.
 *
 * Repare que NÃO existe setSaldo: o saldo só muda por depositar() e sacar(),
 * que aplicam as regras. Assim ninguém consegue pular a validação.
 */
@Entity
@Table(name = "conta_bancaria")
public class ContaBancaria {

    // Chave primária, gerada automaticamente pelo banco
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titular;

    @Column(nullable = false)
    private double saldo;

    // Exigido pelo JPA. Protegido, para o código da aplicação não usá-lo
    // e criar uma conta sem titular.
    protected ContaBancaria() {
    }

    public ContaBancaria(String titular) {
        if (titular == null || titular.isBlank()) {
            throw new IllegalArgumentException("Titular é obrigatório");
        }
        this.titular = titular;
        this.saldo = 0;
    }

    public void depositar(double valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("Valor do depósito deve ser maior que zero");
        }
        this.saldo += valor;
    }

    public void sacar(double valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("Valor do saque deve ser maior que zero");
        }
        if (!temSaldoPara(valor)) {
            throw new IllegalStateException("Saldo insuficiente");
        }
        this.saldo -= valor;
    }

    // Consulta (não altera nada): informa se o saldo cobre o valor
    public boolean temSaldoPara(double valor) {
        return valor <= this.saldo;
    }

    public Long getId() {
        return id;
    }

    public String getTitular() {
        return titular;
    }

    public double getSaldo() {
        return saldo;
    }
}
