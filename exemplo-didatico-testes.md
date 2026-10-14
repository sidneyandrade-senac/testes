# Exemplo didático de testes unitários com Java e JUnit 5

Exemplo completo usando uma **conta bancária simples**, com **modelo rico**: a própria classe `ContaBancaria` carrega as regras de negócio. A classe é pequena, mas tem regras reais, que é o que justifica ter testes.

Esta é a **primeira aula**: só testes unitários, sem banco de dados, sem Spring e sem Mockito. A seção 7 mostra como as próximas aulas acrescentam outros tipos de teste **sem mexer nestes**.

| Camada | Arquivo |
|---|---|
| Entity (com as regras) | `src/main/java/com/example/demo/entity/ContaBancaria.java` |
| DTO | `src/main/java/com/example/demo/dto/CriarContaDTO.java`, `OperacaoDTO.java`, `ContaDTO.java` |
| Teste da entity | `src/test/java/com/example/demo/entity/ContaBancariaTest.java` |
| Teste do DTO | `src/test/java/com/example/demo/dto/ContaDTOTest.java` |

Para rodar: `.\mvnw test`

### Estrutura do projeto

```
demo/
├── pom.xml                                       + spring-boot-starter-data-jpa e h2
├── exemplo-didatico-testes.md                    material desta aula
├── proximas-aulas-testes.md                      material das aulas 2, 3 e 4
├── leiame.txt
├── HELP.md
├── mvnw
├── mvnw.cmd
├── Exemplos de testes.pptx
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.jar
│       └── maven-wrapper.properties              recriado (o mvnw não rodava sem ele)
└── src/
    ├── main/
    │   ├── java/com/example/demo/
    │   │   ├── DemoApplication.java              [slides]
    │   │   ├── controller/
    │   │   │   ├── ApiExceptionHandler.java      [aula 4]
    │   │   │   ├── ClienteController.java        [slides]
    │   │   │   ├── ContaController.java          [aula 4]
    │   │   │   └── UserController.java           [slides]
    │   │   ├── dto/
    │   │   │   ├── ContaDTO.java                 [aula 1]  saída, com ContaDTO.de(conta)
    │   │   │   ├── CriarContaDTO.java            [aula 1]  entrada (usado a partir da aula 3)
    │   │   │   └── OperacaoDTO.java              [aula 1]  entrada (usado a partir da aula 3)
    │   │   ├── entity/
    │   │   │   └── ContaBancaria.java            [aula 1]  entity JPA com as regras
    │   │   ├── model/
    │   │   │   ├── Cliente.java                  [slides]
    │   │   │   ├── Produto.java                  [slides]
    │   │   │   └── User.java                     [slides]
    │   │   ├── repository/
    │   │   │   └── ContaRepository.java          [aula 2]
    │   │   ├── service/
    │   │   │   └── ContaService.java             [aula 3]
    │   │   └── util/
    │   │       └── CalculaTotal.java             [slides]
    │   └── resources/
    │       ├── application.properties            [slides]
    │       └── clientes.json                     [slides]
    └── test/java/com/example/demo/
        ├── DemoApplicationTests.java             [slides]
        ├── controller/
        │   ├── ClienteControllerTest.java        [slides]
        │   ├── ContaControllerTest.java          [aula 4]  6 testes
        │   └── UserControllerTest.java           [slides]
        ├── dto/
        │   └── ContaDTOTest.java                 [aula 1]  3 testes
        ├── entity/
        │   └── ContaBancariaTest.java            [aula 1]  19 testes (1 ignorado)
        ├── repository/
        │   └── ContaRepositoryTest.java          [aula 2]  5 testes
        ├── service/
        │   └── ContaServiceTest.java             [aula 3]  8 testes
        └── util/
            └── CalcularProdutoTest.java          [slides]
```

- **[slides]**: arquivos reproduzidos dos prints do `Exemplos de testes.pptx` (inclui os arquivos padrão de um projeto Spring Initializr).
- **[aula 1]**: arquivos desta aula, o exemplo da conta bancária.
- **[aula 2]**, **[aula 3]** e **[aula 4]**: arquivos das próximas aulas (repository, service e controller). **Já estão no projeto**, mas só são explicados em [proximas-aulas-testes.md](proximas-aulas-testes.md). Eles só **acrescentam** código: nenhum arquivo `[aula 1]` depende deles ou foi alterado por eles.
- As pastas `.github`, `.theia`, `.vscode` e `target` e o arquivo `.gitignore` foram omitidos da árvore por serem do ambiente ou gerados.

---

## 1. O que é um teste unitário e para que serve

Um teste unitário é um **código que executa outro código e confere se o resultado é o esperado**, automaticamente.

Sem testes, para saber se uma alteração quebrou algo, alguém precisa abrir o sistema e conferir na mão. Com testes, basta rodar um comando e, em segundos, saber se as regras continuam funcionando.

A pergunta que todo teste deve responder é:

> **"Se alguém quebrar essa regra, eu fico sabendo?"**

Por isso um teste que só chama métodos e confere se o setter guardou o valor (`setNome("João")` e depois `getNome()`) tem pouco valor: ele não protege nenhuma regra. Bons testes protegem **regras de negócio**.

Um teste unitário também é **isolado e rápido**: testa uma peça pequena do código, sem depender de banco, rede ou arquivos, e roda em milissegundos.

---

## 2. Modelo rico: a classe protege as próprias regras

### 2.1 Modelo anêmico x modelo rico

Existem dois jeitos de organizar uma classe como a conta bancária.

**Modelo anêmico:** a classe só guarda dados, com getters e setters. As regras ficam em outra classe (um service).

```java
// A conta é só uma "caixa de dados"
public class ContaBancaria {
    private double saldo;
    public double getSaldo() { return saldo; }
    public void setSaldo(double saldo) { this.saldo = saldo; }
}

// A regra mora em outro lugar
public class ContaService {
    public void sacar(ContaBancaria conta, double valor) {
        if (valor > conta.getSaldo()) throw new IllegalStateException("Saldo insuficiente");
        conta.setSaldo(conta.getSaldo() - valor);
    }
}
```

**Modelo rico:** a classe guarda os dados **e** as regras que dizem respeito a eles.

```java
public class ContaBancaria {
    private double saldo;

    public void sacar(double valor) {
        if (valor > saldo) throw new IllegalStateException("Saldo insuficiente");
        saldo -= valor;
    }
    public double getSaldo() { return saldo; }
}
```

### 2.2 O problema do modelo anêmico

Com `setSaldo` público, **qualquer código** pode fazer isto, sem passar por nenhuma regra:

```java
conta.setSaldo(1_000_000);   // ninguém impede
```

A regra "não sacar mais que o saldo" existe, mas só vale para quem lembrar de chamar o service. Com o tempo, alguém esquece, e a conta fica em um estado inválido.

### 2.3 Como o modelo rico resolve

1. **Sem `setSaldo`.** O saldo só muda por `depositar()` e `sacar()`, que aplicam as regras. Não existe caminho para pular a validação.
2. **O construtor valida.** Uma `ContaBancaria` **nunca existe** sem titular, porque `new ContaBancaria(null)` lança erro. Se o objeto existe, ele é válido.
3. **Quem usa não precisa conhecer as regras.** Basta chamar `conta.sacar(30)`.

Essa ideia se chama **encapsulamento**: os dados ficam protegidos, e o acesso é feito só por operações que garantem as regras. Outra forma de enxergar é o princípio **"diga, não pergunte"** (*tell, don't ask*): em vez de perguntar o saldo para decidir de fora se pode sacar, diga à conta para sacar, e ela decide.

### 2.4 Comparação

| | Modelo anêmico | Modelo rico |
|---|---|---|
| **Onde ficam as regras** | Em uma classe de serviço | Dentro da própria entity |
| **Setters públicos** | Sim, em todos os atributos | Não: só as operações com regra |
| **Estado inválido possível?** | Sim (`setSaldo(-500)`) | Não, por construção |
| **Teste unitário** | Precisa de um service (e, mais tarde, mock do repository) | `new ContaBancaria(...)`, sem mais nada |
| **Risco** | Regra duplicada ou esquecida em vários lugares | Entity cresce se acumular regras que envolvem várias entidades |
| **Costuma aparecer em** | Projetos Spring mais tradicionais | Projetos que seguem DDD (*Domain-Driven Design*) |

### 2.5 Nenhum dos dois é errado, mas há um limite

O modelo rico serve muito bem para regras que dizem respeito **a uma entidade só** (depositar e sacar na própria conta). Quando a regra envolve **várias entidades** ou recursos externos (uma transferência entre duas contas, uma notificação por e-mail), ela costuma ir para uma classe de serviço, que **orquestra** as entidades.

Na prática os dois convivem: a entity protege as suas regras e o service coordena o fluxo.

### 2.6 O que isso muda nos testes

- O teste unitário fica **o mais simples possível**: `new ContaBancaria("Maria")`, chama `depositar` e `sacar`, confere o resultado. Sem service, sem repository, sem mock.
- As regras ficam em **um lugar só**, e é ali que os testes se concentram.
- Esses testes **não mudam** nas próximas aulas: a persistência e o service entram em volta da entity, sem alterar as regras.

### 2.7 DTOs: o que entra e o que sai

O DTO (*Data Transfer Object*) define os dados que **entram e saem** da aplicação, sem expor a entity.

- **Controle do que entra:** `OperacaoDTO` só tem um `valor`. O cliente não consegue enviar um `saldo` direto, porque esse campo nem existe no DTO.
- **Controle do que sai:** `ContaDTO` devolve só `id`, `titular` e `saldo`. Se a entity ganhar um campo sensível amanhã (senha, documento), ele não vaza para a resposta.
- **Independência:** o formato da API pode mudar sem mexer na entity (e na tabela do banco), e vice-versa.

Nesta aula, só o `ContaDTO` é usado (na conversão `ContaDTO.de(conta)`). `CriarContaDTO` e `OperacaoDTO` entram em ação quando o service e o controller forem criados.

---

## 3. As regras de negócio

Antes de escrever testes, é preciso saber o que o código deve fazer:

1. A conta precisa ter um titular.
2. A conta começa com saldo 0.
3. Só é possível depositar valores **maiores que zero**.
4. Só é possível sacar valores **maiores que zero**.
5. Não é possível sacar **mais do que o saldo** disponível.

Cada regra vira, no mínimo, um teste.

---

## 4. O código

### 4.1 Entity com as regras

```java
package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conta_bancaria")
public class ContaBancaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titular;

    @Column(nullable = false)
    private double saldo;

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
```

Pontos para observar:

- **Sem setters.** Só existem getters. O saldo muda apenas por `depositar` e `sacar`.
- **Construtor `protected` sem argumentos.** O JPA exige um construtor vazio para montar o objeto a partir do banco. Ele é `protected` para o código da aplicação **não** conseguir criar uma conta sem titular.
- **Os `if` que lançam exceção** são as regras de proteção. São eles que os testes mais precisam cobrir.

| Anotação JPA | O que faz |
|---|---|
| `@Entity` | Diz ao JPA que a classe representa uma tabela do banco. |
| `@Table(name = "conta_bancaria")` | Define o nome da tabela. |
| `@Id` | Marca o atributo que é a **chave primária**. |
| `@GeneratedValue(strategy = IDENTITY)` | O banco gera o valor sozinho (auto incremento). Por isso não existe `setId`. |
| `@Column(nullable = false)` | Mapeia o atributo para uma coluna e proíbe valor nulo no banco. |

O Spring Boot 3 usa o pacote `jakarta.persistence`.

> Os blocos de código deste material mostram o essencial. Os arquivos reais (`ContaBancaria.java`, os DTOs e os testes) têm **comentários didáticos extras**, e o código é o mesmo.

> Nesta aula **nada é gravado em banco**. As anotações já estão na entity para que ela **não precise mudar** quando a persistência for adicionada. Os testes unitários ignoram essas anotações: `new ContaBancaria("Maria")` funciona como qualquer classe Java.
>
> O JPA acessa os atributos diretamente (`@Id` está no campo), por isso não precisa de setters.

### 4.2 DTOs

São `record`s (Java 16+): classes imutáveis, em uma linha, com construtor, getters (`titular()`, `valor()`...), `equals`, `hashCode` e `toString` gerados automaticamente.

```java
package com.example.demo.dto;

// DTO de ENTRADA: dados para criar uma conta
public record CriarContaDTO(String titular) {
}
```

```java
package com.example.demo.dto;

// DTO de ENTRADA: valor de um depósito ou de um saque
public record OperacaoDTO(double valor) {
}
```

```java
package com.example.demo.dto;

import com.example.demo.entity.ContaBancaria;

// DTO de SAÍDA: o que é devolvido ao cliente
public record ContaDTO(Long id, String titular, double saldo) {

    // Converte a entity no DTO de saída
    public static ContaDTO de(ContaBancaria conta) {
        return new ContaDTO(conta.getId(), conta.getTitular(), conta.getSaldo());
    }
}
```

### 4.3 A classe de teste da entity

```java
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

class ContaBancariaTest {

    private ContaBancaria conta;

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

    @Test
    @DisplayName("Conta nova ainda não tem id (o id é gerado pelo banco ao salvar)")
    void contaNovaAindaNaoTemId() {
        assertNull(conta.getId());
    }

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
        conta.depositar(100.0);

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
        conta.depositar(100.0);

        conta.sacar(30.0);

        assertEquals(70.0, conta.getSaldo(), 0.001);
    }

    @Test
    @DisplayName("Permite sacar exatamente o saldo, deixando a conta zerada")
    void permiteSacarOSaldoTotal() {
        conta.depositar(100.0);

        conta.sacar(100.0);

        assertEquals(0.0, conta.getSaldo(), 0.001);
    }

    @Test
    @DisplayName("Informa se o saldo cobre o valor (inclusive no limite)")
    void informaSeTemSaldoPara() {
        conta.depositar(100.0);

        assertTrue(conta.temSaldoPara(50.0));
        assertTrue(conta.temSaldoPara(100.0));
        assertFalse(conta.temSaldoPara(100.01));
    }

    @Test
    @DisplayName("Conta sem saldo não cobre nenhum valor positivo")
    void contaSemSaldoNaoCobreValor() {
        assertFalse(conta.temSaldoPara(0.01));
    }

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

    @Test
    @DisplayName("Não permite sacar mais do que o saldo")
    void naoPermiteSacarMaisQueOSaldo() {
        conta.depositar(50.0);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> conta.sacar(80.0));

        assertEquals("Saldo insuficiente", erro.getMessage());
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

    @Disabled("Remova esta linha para ver um teste falhando")
    @Test
    @DisplayName("Exemplo de teste que falha (valor esperado errado de propósito)")
    void exemploDeTesteQueFalha() {
        conta.depositar(100.0);

        conta.sacar(30.0);
        assertEquals(90.0, conta.getSaldo(), 0.001);
    }
}
```

> No arquivo real há comentários didáticos adicionais em cada teste.

### 4.4 O teste do DTO

```java
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

        assertNotNull(dto);
        assertNotNull(dto.titular());
        assertEquals("Maria", dto.titular());
        assertEquals(80.0, dto.saldo(), 0.001);
    }

    @Test
    @DisplayName("Contas com os mesmos dados geram DTOs iguais")
    void contasIguaisGeramDTOsIguais() {
        ContaBancaria a = new ContaBancaria("Maria");
        ContaBancaria b = new ContaBancaria("Maria");
        a.depositar(80.0);
        b.depositar(80.0);

        assertEquals(ContaDTO.de(a), ContaDTO.de(b));
    }

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
```

Conversões entre entity e DTO são um ponto comum de erro (campo esquecido, campo trocado), e o teste é curto e barato.

---

## 5. Explicação detalhada, conceito por conceito

### 5.1 Convenções de nome e estrutura

- A classe de teste tem o nome da classe testada mais `Test`: `ContaBancaria` → `ContaBancariaTest`.
- Fica em `src/test/java`, **no mesmo pacote** da classe testada, mas em outra pasta. O código de produção em `src/main` não leva o código de teste junto.
- O Maven só executa classes cujo nome termina (ou começa) com `Test`.

### 5.2 Teste unitário puro: `new ContaBancaria(...)`

A conta é criada com `new`, sem subir o Spring. Isso torna o teste **rápido** (milissegundos) e **isolado**: ele só depende do código que está sendo testado. Graças ao modelo rico, **não é preciso nenhuma outra classe** para testar as regras.

Compare com `DemoApplicationTests`, que usa `@SpringBootTest` e sobe a aplicação inteira, levando alguns segundos. Esse tipo de teste serve para outra finalidade.

### 5.3 `@Test`

Marca um método como teste. O JUnit executa cada método anotado, de forma independente, e registra se passou ou falhou. Um teste **passa** se termina sem lançar erro e **falha** se algum `assert` não for verdadeiro ou se ocorrer uma exceção inesperada.

### 5.4 O padrão AAA: Arrange, Act, Assert

Todo teste legível tem três momentos:

| Etapa | Pergunta | Exemplo (teste "Sacar diminui o saldo") |
|---|---|---|
| **Arrange** (preparar) | Qual é o cenário? | `conta.depositar(100.0);` |
| **Act** (agir) | O que estou testando? | `conta.sacar(30.0);` |
| **Assert** (verificar) | O que deve acontecer? | `assertEquals(70.0, conta.getSaldo(), 0.001);` |

Quando um teste tem mais de um "Act" ou vários `assert` sobre assuntos diferentes, geralmente é sinal de que ele deveria ser dividido em dois.

### 5.5 `@BeforeEach`

O método anotado roda **antes de cada teste**. Aqui ele cria uma conta nova. Isso garante o **isolamento**: o depósito feito em um teste não aparece no teste seguinte. Testes que dependem da ordem ou do resultado de outros são frágeis e difíceis de diagnosticar.

Para ações pontuais há também `@AfterEach` (depois de cada teste), `@BeforeAll` e `@AfterAll` (uma vez por classe).

### 5.6 `@DisplayName`

Dá ao teste um nome legível no relatório. Em vez de `naoPermiteSacarMaisQueOSaldo`, o relatório mostra "Não permite sacar mais do que o saldo". O nome do teste funciona como **documentação das regras**: quem lê a lista de testes entende o que o sistema faz.

### 5.7 `assertEquals` e o `delta`

```java
assertEquals(esperado, atual, delta);
```

- **O primeiro argumento é o valor esperado**, o segundo é o valor real. Inverter a ordem funciona, mas a mensagem de erro fica confusa.
- Com `double`, o terceiro argumento é o `delta` (aqui `0.001`), explicado abaixo.

#### O que é o `0.001` (o delta)

O `0.001` é o **delta**: a diferença máxima aceita entre o valor esperado e o valor real. O `assertEquals` passa se a diferença entre os dois for menor ou igual a esse valor.

```java
assertEquals(70.0, conta.getSaldo(), 0.001);
```

Isso significa: "o saldo deve ser 70.0, aceitando uma diferença de até 0,001". Valores como `70.0`, `70.0004` ou `69.9996` passam. `70.01` falha.

**Por que o delta é necessário.** Números `double` são guardados em binário, e muitos decimais não são representados com exatidão. O exemplo clássico:

```java
System.out.println(0.1 + 0.2);   // imprime 0.30000000000000004
```

Sem delta, `assertEquals(0.3, 0.1 + 0.2)` falharia, mesmo a conta estando "certa" para quem lê. O delta absorve essa imprecisão de arredondamento.

**Quando usar:**

| Tipo comparado | Usa delta? | Exemplo |
|---|---|---|
| `double` ou `float` | Sim, é obrigatório | `assertEquals(70.0, saldo, 0.001)` |
| `int`, `long`, `String` | Não, esses tipos são exatos | `assertEquals("Maria", conta.getTitular())` |

Se o terceiro argumento for esquecido com `double`, o JUnit compara com igualdade exata, e isso causa falhas inesperadas.

**Qual valor escolher.** O delta deve ser pequeno o bastante para pegar erros reais e grande o bastante para ignorar o arredondamento. Para dinheiro, `0.01` (um centavo) é comum. No `CalcularProdutoTest` do slide foi usado `0.01`, e neste exemplo `0.001`. Ambos funcionam para esses valores.

> Em sistemas financeiros de verdade, o ideal é nem usar `double`, e sim `BigDecimal`, que é exato. Aí o delta deixa de ser necessário. Neste exemplo `double` foi mantido para ficar simples e para poder explicar o delta.

#### Outros asserts úteis

Além do `assertEquals` e do `assertThrows`, o JUnit tem outros asserts para situações específicas. Cada um já aparece em um teste deste exemplo:

| Assert | Quando usar | Onde aparece |
|---|---|---|
| `assertTrue(condição)` | O método devolve `boolean` e o resultado esperado é verdadeiro | `ContaBancariaTest.informaSeTemSaldoPara` |
| `assertFalse(condição)` | O mesmo, quando o resultado esperado é falso | `ContaBancariaTest.informaSeTemSaldoPara` e `contaSemSaldoNaoCobreValor` |
| `assertNull(objeto)` | O valor deve ser `null` | `ContaBancariaTest.contaNovaAindaNaoTemId` |
| `assertNotNull(objeto)` | O valor deve existir (não ser `null`) | `ContaDTOTest.converteContaParaDTO` |
| `assertNotEquals(a, b)` | Dois valores devem ser **diferentes** | `ContaDTOTest.contasDiferentesGeramDTOsDiferentes` |
| `assertArrayEquals(esperado, real)` | Comparar dois arrays, posição por posição | `ContaBancariaTest.saldoEvoluiACadaOperacao` |

Exemplos do projeto:

```java
// assertTrue / assertFalse: testa os dois lados e o limite
conta.depositar(100.0);
assertTrue(conta.temSaldoPara(50.0));     // sobra saldo
assertTrue(conta.temSaldoPara(100.0));    // exatamente o saldo
assertFalse(conta.temSaldoPara(100.01));  // um centavo a mais

// assertNull: o id só existe depois que o banco salva
assertNull(conta.getId());

// assertNotEquals: o record gera o equals, e saldos diferentes dão DTOs diferentes
assertNotEquals(ContaDTO.de(a), ContaDTO.de(b));

// assertArrayEquals: confere a sequência inteira de uma vez (com delta, por serem double)
assertArrayEquals(new double[] { 100.0, 150.5, 120.5, 0.0 }, real, 0.001);
```

Os valores reais usados em cada teste:

| Assert | Teste | O que protege |
|---|---|---|
| `assertTrue` | `informaSeTemSaldoPara` | Que o saldo cobre 50 e cobre exatamente 100 |
| `assertFalse` | `informaSeTemSaldoPara` e `contaSemSaldoNaoCobreValor` | Que 100,01 não é coberto e que a conta zerada não cobre nada |
| `assertNull` | `contaNovaAindaNaoTemId` | Que o id só existe depois que o banco salva |
| `assertNotNull` | `ContaDTOTest.converteContaParaDTO` | Que o DTO e o titular vieram preenchidos, antes de conferir os valores |
| `assertNotEquals` | `ContaDTOTest.contasDiferentesGeramDTOsDiferentes` | Que saldos diferentes (80,0 e 80,5) geram DTOs diferentes |
| `assertArrayEquals` | `saldoEvoluiACadaOperacao` | A sequência de saldos `{100, 150.5, 120.5, 0}` depois de cada operação |

Dicas:

- **`assertTrue(x == y)` é pior que `assertEquals(x, y)`.** Quando falha, o `assertEquals` mostra o valor esperado e o recebido. O `assertTrue` só diz "expected true". Use `assertTrue` e `assertFalse` quando o resultado **já é** um `boolean`.
- **`assertNotNull` sozinho prova pouco.** Saber que o valor existe não diz se está certo. Normalmente vem **antes** de um `assertEquals` (como no teste do DTO), e não no lugar dele.
- **`assertEquals` em arrays não funciona como se espera**: ele compara a referência do array, e não o conteúdo. Para arrays, use sempre `assertArrayEquals`.

### 5.8 `assertThrows`: testar o que **não** pode acontecer

Esta é a parte mais importante do exemplo. Testar só o "caminho feliz" (depósito válido, saque válido) deixa as regras de proteção sem cobertura.

```java
IllegalStateException erro = assertThrows(IllegalStateException.class,
        () -> conta.sacar(80.0));
```

Isso significa: "executar este código **e esperar** que lance `IllegalStateException`". Se o código não lançar, ou lançar outra exceção, o teste falha. O objeto retornado permite conferir a mensagem (`erro.getMessage()`).

Observe também a última linha do teste:

```java
assertEquals(50.0, conta.getSaldo(), 0.001);
```

Ela verifica que, **depois da tentativa que falhou, o saldo não mudou**. Esse tipo de verificação pega bugs em que a operação altera o estado e só depois descobre o erro.

Com o modelo rico, o `assertThrows` também testa a **criação**: `new ContaBancaria(null)` deve lançar erro, porque o objeto nunca pode nascer inválido.

### 5.9 `@ParameterizedTest`, `@ValueSource` e `@NullAndEmptySource`

Para testar vários valores inválidos sem copiar o mesmo teste várias vezes:

```java
@ParameterizedTest(name = "depositar {0} deve ser recusado")
@ValueSource(doubles = { 0, -1, -100.5 })
void naoPermiteDepositoInvalido(double valorInvalido) { ... }
```

O JUnit executa o método **uma vez para cada valor**, e cada execução aparece separada no relatório (`depositar 0.0 deve ser recusado`, `depositar -1.0 deve ser recusado`...). O valor `0` merece atenção especial: ele fica exatamente no **limite** da regra (`valor <= 0`), e é nos limites que os bugs mais aparecem.

Para o titular, `@NullAndEmptySource` acrescenta automaticamente `null` e `""` à lista, e o `@ValueSource(strings = { "   " })` acrescenta o texto só com espaços. Assim, os três casos de "titular inválido" são cobertos em um único método.

### 5.10 Testes de limite

O teste `permiteSacarOSaldoTotal` cobre o **limite do outro lado** da regra de saque. A regra proíbe sacar **mais** que o saldo (`valor > saldo`), então sacar **exatamente** o saldo deve ser permitido. Se alguém trocar `>` por `>=`, esse teste falha e avisa.

Regra prática: para cada comparação no código (`>`, `<=`...), teste o valor **de um lado**, o valor **do outro** e o valor **exatamente no limite**.

### 5.11 `@Disabled` e o teste que falha

O último teste da entity está desativado de propósito. Ele espera 90 como saldo, mas o certo é 70, então **falharia**. Ele existe para mostrar a utilidade do teste.

---

## 6. Como rodar e demonstrar

No terminal, na raiz do projeto:

```
.\mvnw test
```

Saída esperada (resumo):

```
[WARNING] Tests run: 19, Failures: 0, Errors: 0, Skipped: 1 -- in com.example.demo.entity.ContaBancariaTest
[INFO]    Tests run: 3,  Failures: 0, Errors: 0, Skipped: 0 -- in com.example.demo.dto.ContaDTOTest
...
[INFO] BUILD SUCCESS
```

`Skipped: 1` é o teste com `@Disabled`. Para rodar só uma classe:

```
.\mvnw test -Dtest=ContaBancariaTest
```

### Roteiro de demonstração em aula

**Demonstração 1: ver um teste falhar por erro no teste**
1. Em `ContaBancariaTest`, apague a linha `@Disabled(...)` do último teste.
2. Rode `.\mvnw test`.
3. O Maven mostra algo como `expected: <90.0> but was: <70.0>` e `BUILD FAILURE`.
4. Discuta: o código estava certo e o teste, errado. Teste também precisa de revisão.
5. Volte a colocar o `@Disabled`.

**Demonstração 2: ver um teste proteger o código**
1. Em `ContaBancaria.temSaldoPara`, troque `return valor <= this.saldo;` por `return valor <= this.saldo + 1000;`.
2. Rode `.\mvnw test`.
3. Os testes "Não permite sacar mais do que o saldo" e "Informa se o saldo cobre o valor" **falham**, apontando que a regra foi quebrada.
4. Desfaça a alteração e rode de novo: `BUILD SUCCESS`.

**Demonstração 3: o limite**
1. Troque `valor <= 0` por `valor < 0` em `depositar`.
2. Rode os testes: só o caso `depositar 0.0` falha. É por isso que testamos o valor de limite.
3. Desfaça a alteração.

**Demonstração 4: o modelo rico protegendo a conta**
1. Em `ContaBancariaTest`, tente escrever `conta.setSaldo(1000000);`.
2. O código **nem compila**: o método não existe. Esse é o ponto do modelo rico, porque o saldo só muda pelas operações que aplicam as regras.
3. Remova a linha.

---

## 7. Próximas aulas: acrescentar testes, sem mexer nestes

O `ContaBancariaTest` **não muda** nas próximas aulas. Ele continua testando as regras, e novos tipos de teste entram em **arquivos novos**.

> **Material completo das aulas 2, 3 e 4** (código, testes, explicações e roteiro de demonstração): [proximas-aulas-testes.md](proximas-aulas-testes.md). O resumo abaixo é só uma visão geral.

Quando a persistência chegar, entra uma classe `ContaService` que **orquestra**: busca a conta, manda ela executar a regra e salva.

```
ContaService.sacar(id, dto):
    1. busca a conta no repository          (novo)
    2. conta.sacar(dto.valor())             (já existe e já está testado)
    3. salva a conta no repository          (novo)
```

| Aula | Código novo | Teste novo | Tipo |
|---|---|---|---|
| **1 (esta)** | `ContaBancaria` com regras, DTOs | `ContaBancariaTest`, `ContaDTOTest` | Unitário (JUnit puro) |
| **2** | `ContaRepository` (`extends JpaRepository`) | `ContaRepositoryTest` com `@DataJpaTest` | Integração com banco H2 |
| **3** | `ContaService` (`@Service`, usa repository) | `ContaServiceTest` com **Mockito** | Unitário com mock |
| **4** | `ContaController` | Teste com `MockMvc` | Web |

O que cada nova camada testa:

- **Aula 2:** só se salvar e buscar funciona, no banco real em memória.
- **Aula 3:** só a **orquestração** (buscou, mandou a conta agir, salvou). O Mockito simula o repository. Como as regras já estão testadas na aula 1, o teste do service **não repete** os casos de saldo e valor inválido: ele confere, por exemplo, que **nada é salvo** quando a conta lança erro.
- **Aula 4:** só se o endpoint recebe e devolve os DTOs certos.

Cada tipo de teste cobre **uma responsabilidade**, e uma regra nunca precisa ser testada em dois lugares.

> As anotações JPA já estão na `ContaBancaria` e o H2 já está no `pom.xml`. Nada disso muda nas próximas aulas. O `CriarContaDTO` e o `OperacaoDTO` passam a ser usados no service e no controller.

Outros temas que podem entrar depois:

1. **`@Nested`**: agrupar testes por tema (criação, depósito, saque) dentro da mesma classe.
2. **Bean Validation**: mover validações simples de formato (titular não vazio, valor positivo) para anotações no DTO (`@NotBlank`, `@Positive`).
3. **Cobertura de código (JaCoCo)**: medir quais linhas e desvios os testes executam. A cobertura mostra o que **não** foi testado, mas cobertura alta não garante testes bons.

---

## 8. Boas práticas resumidas

- **Um teste, uma regra.** Se o nome do teste precisa de "e" para descrever, divida-o.
- **Nomes que descrevem o comportamento**, não o método (`naoPermiteSacarMaisQueOSaldo`, e não `testSacar2`).
- **Testes independentes.** Nenhum depende de outro nem da ordem de execução.
- **Teste o caminho feliz e os casos de erro**, principalmente os **limites** (0, -1, igual ao saldo).
- **Evite testes triviais**, como conferir se o getter devolve o que o setter guardou. Eles dão falsa sensação de segurança.
- **Teste rápido e sem dependências externas** (banco, internet, arquivos). Aqui tudo roda em memória, em milissegundos.
- **Deixe a classe proteger as próprias regras** (sem setters que pulem a validação). Isso reduz os bugs e deixa o teste simples.
- **Rode os testes sempre**, antes de cada commit. Um teste que ninguém roda não protege nada.
