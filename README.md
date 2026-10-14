# Testes com Java, JUnit 5 e Spring Boot

Projeto de estudo da unidade curricular de testes (SENAC, Tecnologia em Análise e Desenvolvimento de Sistemas). Ele reúne exemplos de **testes automatizados** em uma aplicação Spring Boot, indo dos testes unitários mais simples até testes de integração e da camada web.

## O que o projeto contém

O projeto tem duas partes.

**1. Exemplos do slide** (`Exemplos de testes.pptx`)

Reprodução do código mostrado na apresentação: um cadastro de clientes, usuários e produtos, com os respectivos testes.

| Classe testada | Teste | O que mostra |
|---|---|---|
| `ClienteController` (lê `clientes.json`) | `ClienteControllerTest` | Estrutura básica de um teste, `@Test` e asserts |
| `UserController` (valida e-mail e senha) | `UserControllerTest` | `assertTrue` e `assertFalse` em regras booleanas |
| `CalculaTotal` | `CalcularProdutoTest` | Comparação de `double` com `delta` |

**2. Exemplo didático da conta bancária**

Um exemplo construído para ensinar testes **por etapas**. A classe `ContaBancaria` carrega as próprias regras de negócio (modelo rico), e cada aula acrescenta uma camada e um tipo de teste novo, **sem alterar os testes das aulas anteriores**.

| Aula | Camada | Tipo de teste |
|---|---|---|
| **1** | `ContaBancaria` (entity com as regras) e DTOs | Unitário, só com JUnit 5 |
| **2** | `ContaRepository` (Spring Data JPA) | Integração com banco H2 (`@DataJpaTest`) |
| **3** | `ContaService` | Unitário com mock (Mockito) |
| **4** | `ContaController` e `ApiExceptionHandler` | Camada web (`@WebMvcTest` e `MockMvc`) |

A ideia que organiza as aulas: **cada teste cobre uma responsabilidade, e uma regra nunca é testada em dois lugares.**

## Documentação

| Documento | Conteúdo |
|---|---|
| [exemplo-didatico-testes.md](exemplo-didatico-testes.md) | **Aula 1.** O que é um teste unitário, modelo rico, a estrutura do projeto, o código completo, a explicação de cada conceito (AAA, `@BeforeEach`, `assertEquals` e `delta`, `assertThrows`, testes parametrizados e os outros asserts) e um roteiro de demonstração. |
| [proximas-aulas-testes.md](proximas-aulas-testes.md) | **Aulas 2, 3 e 4.** Repository e teste de integração, service com Mockito (o que é um mock e como usar), controller e `MockMvc`, e a visão geral dos tipos de teste. |
| [leiame.txt](leiame.txt) | Lista resumida das anotações e dos asserts do JUnit. |

Comece pelo **exemplo-didatico-testes.md**: os outros materiais partem dele.

## Tecnologias

- Java 17
- Spring Boot 3.2.3 (Web, Data JPA)
- H2 (banco em memória)
- JUnit 5 e Mockito (via `spring-boot-starter-test`)
- Maven, com o wrapper incluso (`mvnw`)

## Como rodar os testes

```
.\mvnw test
```

Para rodar uma classe específica:

```
.\mvnw test -Dtest=ContaBancariaTest
```

Resultado esperado: `Tests run: 51, Failures: 0, Errors: 0, Skipped: 1` e `BUILD SUCCESS`. O teste ignorado (`@Disabled`) é o exemplo de teste que falha de propósito, usado nas demonstrações em aula.

## Estrutura resumida

```
src/
├── main/java/com/example/demo/
│   ├── controller/   ClienteController, UserController, ContaController, ApiExceptionHandler
│   ├── dto/          CriarContaDTO, OperacaoDTO, ContaDTO
│   ├── entity/       ContaBancaria
│   ├── model/        Cliente, Produto, User
│   ├── repository/   ContaRepository
│   ├── service/      ContaService
│   └── util/         CalculaTotal
└── test/java/com/example/demo/
    └── (um teste para cada camada acima)
```

A árvore completa, arquivo por arquivo, está em [exemplo-didatico-testes.md](exemplo-didatico-testes.md#estrutura-do-projeto).
