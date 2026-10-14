# Próximas aulas: repository, service e controller

> Continuação de [exemplo-didatico-testes.md](exemplo-didatico-testes.md) (aula 1: testes unitários da `ContaBancaria`). Aquele arquivo é o ponto de partida, e **nada dele muda** aqui.

Na aula 1 a `ContaBancaria` ganhou as próprias regras e foi testada sozinha, só com JUnit. Agora a aplicação cresce em volta dela: primeiro a persistência, depois o service e por fim a camada web. Cada camada traz **um tipo de teste novo**, e todos entram em **arquivos novos**.

| Aula | Código novo | Teste novo | Tipo de teste |
|---|---|---|---|
| **1** ([ver](exemplo-didatico-testes.md)) | `ContaBancaria` com regras, DTOs | `ContaBancariaTest`, `ContaDTOTest` | Unitário (JUnit puro) |
| **2** | `ContaRepository` | `ContaRepositoryTest` | Integração com banco (`@DataJpaTest`) |
| **3** | `ContaService` | `ContaServiceTest` | Unitário com mock (Mockito) |
| **4** | `ContaController` e `ApiExceptionHandler` | `ContaControllerTest` | Camada web (`@WebMvcTest` e `MockMvc`) |

Para rodar tudo: `.\mvnw test`

---

## 0. A ideia que organiza as três aulas

> **Cada teste cobre uma responsabilidade, e uma regra nunca é testada em dois lugares.**

```
Cliente --> Controller --> Service --> Repository --> Banco
 (JSON)      HTTP          orquestra    guarda        H2
             DTOs          o fluxo      e busca

               ^              ^             ^
        ContaControllerTest   |     ContaRepositoryTest
         (service falso)      |     (banco real em memória)
                       ContaServiceTest
                      (repository falso)

                  As regras ficam na ContaBancaria
                    (ContaBancariaTest, aula 1)
```

| Camada | O que ela é responsável por | O que o teste dela confere |
|---|---|---|
| **Entity** | As regras de negócio | Saldo, valores válidos e inválidos |
| **Repository** | Guardar e buscar | Que salvar e buscar funcionam de verdade |
| **Service** | Orquestrar: buscar, mandar a conta agir, salvar | A sequência de chamadas, e que **nada é salvo** se a regra falhar |
| **Controller** | Receber e devolver HTTP | Rota, JSON e código de status |

Por isso o `ContaServiceTest` **não repete** os casos de "saldo insuficiente" com todos os valores: essa regra já é garantida pelo `ContaBancariaTest`. O service só precisa garantir que, quando a conta recusa, ele não salva.

---

## Aula 2: Repository e teste de integração

### 2.1 Objetivo

Guardar as contas em banco de dados e garantir que salvar e buscar funcionam de verdade.

### 2.2 Código novo

```java
package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.ContaBancaria;

@Repository
public interface ContaRepository extends JpaRepository<ContaBancaria, Long> {
}
```

São só três linhas, e **não há implementação**. Ao estender `JpaRepository<ContaBancaria, Long>` (entidade e tipo da chave primária), o Spring Data **gera a implementação em tempo de execução**, com os métodos prontos:

| Método | O que faz |
|---|---|
| `save(conta)` | Insere (se o `id` for `null`) ou atualiza (se já existir) |
| `findById(id)` | Busca pela chave primária e devolve um `Optional` |
| `findAll()` | Lista todos os registros |
| `deleteById(id)` | Remove pelo `id` |
| `count()` | Conta os registros |

`Optional` representa "pode existir ou não". Em vez de devolver `null` quando a conta não existe, o repository devolve `Optional.empty()`, e quem chama é obrigado a decidir o que fazer.

**Nenhum arquivo da aula 1 muda.** A entity já tem as anotações JPA e o `pom.xml` já tem o H2.

### 2.3 Teste novo: `ContaRepositoryTest`

```java
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
```

### 2.4 Explicação

**`@DataJpaTest`** sobe só a parte de persistência do Spring (entidades e repositories) com um banco H2 temporário. É muito mais leve que `@SpringBootTest`, que sobe a aplicação inteira.

**`@Autowired`** pede ao Spring o repository real, já gerado pelo Spring Data. Em teste unitário puro isso não existe: aqui o Spring está de fato funcionando.

**Rollback automático.** Cada teste roda dentro de uma transação que é **desfeita no fim**. Por isso um teste não "suja" o banco para o próximo, e o `assertEquals(1, repository.count())` funciona mesmo depois de vários testes terem salvado contas.

**Sem mock.** O que se quer testar é justamente a persistência, então usa-se um banco de verdade.

**Por que isso é teste de integração.** Ele envolve o JPA, o Hibernate e o banco juntos. É mais lento que um teste unitário (décimos de segundo, contra milissegundos), e esse é o preço de testar a persistência de verdade.

**Reaproveitando a aula 1.** O teste `salvarAtualiza` usa `conta.depositar(50.0)`, o método da entity, e não um setter (que não existe). É o modelo rico se encaixando na persistência.

**O `99999L` no teste `idInexistenteDevolveVazio`.** O `L` no final do número é um sufixo do Java que indica que o valor é do tipo `long`. O `id` da conta é `Long`, então é preciso escrever `99999L` (e não `99999`, que seria um `int` e não compilaria em `findById`). O número em si é só um id que **não existe** no banco. A explicação completa está na seção 3.2.

---

## Aula 3: Service e teste unitário com Mockito

### 3.1 Objetivo

Criar o fluxo completo de cada operação (buscar, aplicar a regra, salvar) e testá-lo **sem banco**.

### 3.2 Mockito: o que é e por que usar

#### O que é o Mockito

O **Mockito** é uma biblioteca Java que cria **objetos falsos** (chamados de *mocks*) para usar em testes. Ele já vem no `spring-boot-starter-test`, então nada foi adicionado ao `pom.xml`.

Um mock tem a mesma "cara" de um objeto real (os mesmos métodos), mas **por dentro não faz nada de verdade**: quem decide o que ele responde é o teste.

#### O problema que ele resolve

O `ContaService` precisa de um `ContaRepository` para funcionar. O repository real conversa com o banco de dados. Se o teste do service usasse o repository real:

| | Com o repository real | Com um mock |
|---|---|---|
| **Precisa de banco?** | Sim | Não |
| **Velocidade** | Décimos de segundo ou mais | Milissegundos |
| **O que o teste testa** | Service **e** banco juntos | **Só o service** |
| **Se o teste falhar** | Pode ser o service, o JPA ou o banco | Só pode ser o service |
| **Montar um cenário** ("conta com saldo 50") | Gravar os dados no banco antes (e limpar depois) | Uma linha: `when(...).thenReturn(...)` |
| **Simular "conta não existe"** | Depende do estado do banco: precisa de um id que ninguém tenha gravado | Uma linha: `thenReturn(Optional.empty())` |
| **Simular um erro raro** ("banco fora do ar") | Praticamente impossível de provocar | Uma linha: `thenThrow(...)` |

O teste do service quer responder **"o service orquestrou certo?"**, e não **"o banco funciona?"**. Essa segunda pergunta já é respondida pelo `ContaRepositoryTest`.

**Montar um cenário: "conta com saldo 50"**

Para testar `sacar`, o service precisa receber do repository uma conta com saldo 50.

Com o **repository real**, o teste teria que preparar o banco antes:

```java
ContaBancaria conta = new ContaBancaria("Maria");
conta.depositar(50.0);
conta = repositoryReal.save(conta);          // grava no banco de verdade
Long id = conta.getId();                     // pega o id que o banco gerou

service.sacar(id, new OperacaoDTO(30.0));    // só agora o teste começa
```

Isso exige um banco rodando, gravar os dados, limpar depois para não afetar outro teste, e descobrir o `id` que o banco gerou.

Com o **mock**, o teste só "ensina" o que o repository responde, sem gravar nada:

```java
when(repository.findById(1L)).thenReturn(Optional.of(conta));   // monta o cenário
```

Lê-se: "quando o service pedir a conta 1, devolva esta aqui". Não há banco, gravação nem limpeza. No projeto, essa linha está no método auxiliar `repositoryTemConta(conta)`, para os testes ficarem legíveis.

**O que quer dizer o `1L`?**

O `L` no final de um número é um **sufixo do Java** que indica que o valor é do tipo `long`, e não `int`:

```java
1L    // long, valor 1
99L   // long, valor 99
1     // int, valor 1
```

O `id` da conta é do tipo `Long` (a versão "objeto" do `long`), na entity e no repository:

```java
private Long id;                              // na ContaBancaria
Optional<ContaBancaria> findById(Long id);    // no repository
```

Por isso os testes escrevem `1L`. Sem o `L`, o Java entende `1` como `int`, e `findById(1)` **não compila**: o Java converte `int` em `long`, mas não em `Long`.

Os números em si são só **ids de exemplo**:

| Valor | Uso nos testes |
|---|---|
| `1L` | O id da conta que **existe** (a que o mock devolve) |
| `99L` | Um id de conta que **não existe** (o mock devolve `Optional.empty()`) |
| `99999L` | O mesmo, no `ContaRepositoryTest`, com o banco real |

Poderia ser `2L`, `500L` ou qualquer outro. Usar 99 e 99999 é só uma convenção para deixar claro, para quem lê, que é um id "que não deveria existir".

`long` e `Long` diferem em um ponto importante:

| | Tipo | Aceita `null`? |
|---|---|---|
| `long` | Primitivo | Não |
| `Long` | Objeto | Sim |

O id é `Long` porque, antes de salvar, ele é `null` (o banco ainda não gerou o valor). É isso que o `assertNull(conta.getId())` da aula 1 confere.

O mesmo tipo de sufixo existe para outros números: `10.0f` é `float`, enquanto `10.0` é `double`. No projeto, todos os valores de dinheiro são `double` e não precisam de sufixo.

**Simular "conta não existe"**

O service deve lançar `NoSuchElementException("Conta não encontrada")` quando o repository não acha a conta. Com o mock, basta mandar o repository responder "não achei":

```java
when(repository.findById(99L)).thenReturn(Optional.empty());
```

Com o repository real, isso **também é possível**: basta pedir um id que ninguém salvou, como o `ContaRepositoryTest` faz com `findById(99999L)`. O ponto fraco é que o resultado **depende do estado do banco**. Se outro teste gravar esse id por acaso, o comportamento muda. Com o mock, o resultado depende só do que o teste programou.

**Simular um erro raro: "banco fora do ar"**

Aqui o mock faz a maior diferença. Alguns erros quase não dá para provocar de verdade, e com o mock são uma linha:

```java
when(repository.findById(1L))
        .thenThrow(new DataAccessResourceFailureException("Banco fora do ar"));
```

Com o banco real, seria preciso derrubar o banco no meio do teste para ver como o service reage. Esse é um bom uso para testar o que acontece quando as coisas dão errado.

| | Banco real | Mock |
|---|---|---|
| **Cenário** | Montar e gravar dados, e depois limpar | Programar a resposta, em uma linha |
| **Resultado depende de...** | Do que já está gravado no banco | Só do que o teste programou |
| **Erros raros** (banco fora do ar, tempo esgotado) | Praticamente impossível de provocar | Uma linha, com `thenThrow(...)` |

#### A analogia do dublê

Em um filme de ação, o **dublê** ocupa o lugar do ator em uma cena perigosa. Ele parece o ator na cena, mas só faz o que o diretor mandou.

O mock é o dublê do `ContaRepository`:

- **O service** é o ator principal, que é quem queremos avaliar.
- **O repository real** é um colega difícil de levar para a cena (precisa de banco).
- **O mock** é o dublê: o service o trata como se fosse o repository de verdade, sem saber a diferença.
- **O teste** é o diretor: define o roteiro ("quando perguntarem pela conta 1, devolva esta conta") e depois confere o que o ator fez.

#### O que um mock faz sem ser programado

Um mock recém-criado **não tem comportamento nenhum**. Ele devolve o "vazio" de cada tipo:

| O método devolve | O mock devolve |
|---|---|
| Um objeto (`ContaBancaria`) | `null` |
| `Optional<...>` | `Optional.empty()` |
| `boolean` | `false` |
| Número | `0` |
| Lista | Lista vazia |

Por isso, se o teste esquecer de programar o `findById`, o service recebe `Optional.empty()` e lança "Conta não encontrada", mesmo a conta existindo no teste. Um mock só sabe o que o teste lhe ensinou.

#### Como funciona por dentro (resumo)

Ao ver `@Mock private ContaRepository repository`, o Mockito **gera, em tempo de execução, uma classe que implementa a interface** `ContaRepository`. Cada chamada a essa classe é:

1. **Registrada** (o Mockito anota qual método foi chamado e com quais argumentos);
2. **Respondida** conforme o que o teste programou (ou com o "vazio", se nada foi programado).

Por isso não existe nenhuma implementação escrita à mão. Fazer um repository falso manualmente seria impraticável: o `JpaRepository` tem dezenas de métodos (`save`, `findAll`, `deleteById`, `count`...) e uma classe falsa teria que implementar todos. O Mockito faz isso sozinho.

#### As três coisas que se faz com um mock

| Passo | O que é | Como no Mockito | Etapa do teste |
|---|---|---|---|
| **1. Criar** | Gerar o objeto falso | `@Mock` | Preparação |
| **2. Programar** | Dizer o que ele responde | `when(...).thenReturn(...)` | Arrange |
| **3. Verificar** | Conferir o que o código chamou | `verify(...)` | Assert |

> Em termos técnicos, programar respostas se chama *stubbing* (o mock funciona como um "stub") e verificar chamadas é o papel de um *mock* propriamente dito. O Mockito usa a palavra "mock" para as duas coisas, e na prática isso não atrapalha.

#### Um teste lido passo a passo

O código completo da classe de teste está na seção 3.4. Aqui vai um dos testes, só para ver como o mock participa:

```java
@Test
@DisplayName("Depositar atualiza o saldo e salva a conta")
void depositarSalva() {
    ContaBancaria conta = contaComSaldo(0.0);        // 1
    repositoryTemConta(conta);                       // 2
    repositorySalvaERetornaOMesmo();                 // 3

    ContaDTO resposta = service.depositar(1L, new OperacaoDTO(100.0));   // 4

    assertEquals(100.0, resposta.saldo(), 0.001);    // 5
    verify(repository).save(conta);                  // 6
}
```

1. **Cria uma conta real** (`ContaBancaria` não é simulada, porque é um objeto simples e sem dependências).
2. **Programa o mock**: "quando o service chamar `findById(1L)`, devolva esta conta".
3. **Programa o mock**: "quando o service chamar `save(...)`, devolva a própria conta recebida".
4. **Executa o service**. Internamente ele chama `repository.findById(1L)` e recebe a conta que o teste preparou, chama `conta.depositar(100.0)`, e chama `repository.save(conta)`. Tudo isso sem banco.
5. **Confere o resultado**: o DTO devolvido tem saldo 100.
6. **Confere a interação**: o service **de fato mandou salvar** exatamente aquela conta.

Os passos 2 e 3 são o "Arrange", o 4 é o "Act", e os passos 5 e 6 são o "Assert". É o mesmo padrão AAA da aula 1, com o mock fazendo o papel do banco.

#### O que simular e o que não simular

O Mockito serve para simular as **fronteiras**: o que é externo à classe testada ou difícil de usar em teste.

| Simule (mock) | Use o real |
|---|---|
| `ContaRepository` (banco) | `ContaBancaria` (objeto simples, e já testado) |
| Serviços externos (e-mail, API de terceiros) | DTOs (`ContaDTO`, `OperacaoDTO`) |
| Relógio, gerador de números aleatórios | A própria classe que está sendo testada |

Regra prática: **nunca simule a classe que você está testando**, pois assim o teste só verifica o próprio mock. E não simule objetos simples só por simular: criar uma `ContaBancaria` real é mais fácil e deixa o teste mais fiel.

#### Erros comuns com o Mockito

| Erro | O que acontece | Como evitar |
|---|---|---|
| Esquecer de programar o mock | O service recebe `null` ou `Optional.empty()` e o teste falha por um motivo que não é o esperado | Programe tudo que o service vai chamar |
| Programar algo que o teste não usa | `UnnecessaryStubbingException` (Mockito estrito) | Programe só o que cada teste precisa (veja "Mockito estrito" na seção 3.5) |
| Usar `verify` em tudo | O teste fica preso ao "como" do código e quebra a cada refatoração | Verifique só os efeitos que importam (salvou ou não salvou) |
| Conferir só a exceção | Um bug que salva antes de validar passa despercebido | Combine `assertThrows` com `verify(repository, never()).save(...)` |

### 3.3 Código novo

```java
package com.example.demo.service;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.example.demo.dto.ContaDTO;
import com.example.demo.dto.CriarContaDTO;
import com.example.demo.dto.OperacaoDTO;
import com.example.demo.entity.ContaBancaria;
import com.example.demo.repository.ContaRepository;

@Service
public class ContaService {

    private final ContaRepository repository;

    public ContaService(ContaRepository repository) {
        this.repository = repository;
    }

    public ContaDTO criarConta(CriarContaDTO dto) {
        ContaBancaria conta = new ContaBancaria(dto.titular());
        return ContaDTO.de(repository.save(conta));
    }

    public ContaDTO buscar(Long id) {
        return ContaDTO.de(buscarConta(id));
    }

    public ContaDTO depositar(Long id, OperacaoDTO dto) {
        ContaBancaria conta = buscarConta(id);
        conta.depositar(dto.valor());
        return ContaDTO.de(repository.save(conta));
    }

    public ContaDTO sacar(Long id, OperacaoDTO dto) {
        ContaBancaria conta = buscarConta(id);
        conta.sacar(dto.valor());
        return ContaDTO.de(repository.save(conta));
    }

    private ContaBancaria buscarConta(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Conta não encontrada"));
    }
}
```

Observe como o service é **curto**: ele não tem nenhum `if` de regra. Quem decide se o saque é válido é a `conta.sacar(...)`. O service só orquestra.

```
ContaService.sacar(id, dto):
    1. busca a conta no repository          (novo)
    2. conta.sacar(dto.valor())             (já existe e já está testado na aula 1)
    3. salva a conta no repository          (novo)
```

**O `@Service`** marca a classe como um bean do Spring. Na inicialização, o Spring encontra a classe, cria uma instância e a entrega a quem precisar (aqui, o futuro controller). É a **injeção de dependência**. O service recebe o repository pelo construtor, e é isso que permite, nos testes, entregar um repository falso no lugar do real.

Aqui entram também os `DTO`s `CriarContaDTO` e `OperacaoDTO`, criados na aula 1 e ainda sem uso: o service recebe esses objetos e devolve um `ContaDTO`.

### 3.4 Teste novo: `ContaServiceTest`

```java
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
```

### 3.5 Os recursos do Mockito, um a um

**Criar o mock**

```java
@ExtendWith(MockitoExtension.class)   // liga o Mockito ao JUnit
class ContaServiceTest {

    @Mock
    private ContaRepository repository;   // o falso, criado automaticamente
```

O service é criado com `new ContaService(repository)`, sem subir o Spring. É por isso que a **injeção por construtor** ajuda: basta entregar o falso no lugar do real.

**Ensinar o mock a responder: `when(...).thenReturn(...)`**

```java
when(repository.findById(1L)).thenReturn(Optional.of(conta));
```

Lê-se: "quando alguém chamar `findById(1L)`, responda com esta conta". É o *Arrange* do teste: monta o cenário sem precisar de um banco.

**Responder com lógica: `thenAnswer`**

```java
when(repository.save(any(ContaBancaria.class)))
        .thenAnswer(invocacao -> invocacao.getArgument(0));
```

"Ao salvar qualquer conta, devolva a própria conta recebida." É o comportamento de um `save` real, e o teste não precisa repetir isso em cada cenário. `any(ContaBancaria.class)` significa "qualquer objeto desse tipo".

**Conferir o que o service fez: `verify`**

Os testes verificam dois tipos de coisa:

| Tipo | Pergunta | Exemplo |
|---|---|---|
| **Resultado** | O que o service devolveu? | `assertEquals(70.0, resposta.saldo(), 0.001)` |
| **Interação** | O que o service fez com o repository? | `verify(repository).save(conta)` |

```java
verify(repository).save(conta);                // salvou exatamente esta conta, 1 vez
verify(repository, never()).save(any());       // nunca salvou nada
verifyNoInteractions(repository);              // nem encostou no repository
```

Essa segunda categoria é o que garante a regra do service: **se a conta recusar a operação, nada pode ser salvo**. Sem o `verify`, um bug que salvasse a conta antes de validar passaria despercebido, porque o teste só veria a exceção.

**Capturar o argumento: `ArgumentCaptor`**

```java
ArgumentCaptor<ContaBancaria> captor = ArgumentCaptor.forClass(ContaBancaria.class);
verify(repository).save(captor.capture());
assertEquals("Maria", captor.getValue().getTitular());
```

Quando o service **cria** o objeto internamente (como em `criarConta`), o teste não tem a referência dele. O captor "pega" o que foi passado ao `save` para o teste poder inspecionar.

**Atenção: Mockito estrito.** Com `MockitoExtension`, um `when(...)` que **não é usado** no teste faz o teste falhar com `UnnecessaryStubbingException`. Por isso os cenários são montados por teste, e não em um conjunto grande no `@BeforeEach`: cada teste ensina ao mock só o que vai usar. Isso também mantém o teste honesto, porque quem lê vê exatamente o que ele depende.

### 3.6 O que este teste **não** faz

Repare que não existe aqui um teste para cada valor inválido (zero, negativo...) ou para o limite do saldo. Isso já está em `ContaBancariaTest`. O `ContaServiceTest` só confere que, **quando a conta recusa**, o service não salva (`depositoInvalidoNaoSalva` e `saqueAcimaDoSaldoNaoSalva`). Cada regra é testada em um lugar só.

---

## Aula 4: Controller e teste da camada web

### 4.1 Objetivo

Expor a conta por HTTP, recebendo e devolvendo os DTOs em JSON, com os códigos de status corretos.

### 4.2 Código novo

```java
package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ContaDTO;
import com.example.demo.dto.CriarContaDTO;
import com.example.demo.dto.OperacaoDTO;
import com.example.demo.service.ContaService;

@RestController
@RequestMapping("/contas")
public class ContaController {

    private final ContaService service;

    public ContaController(ContaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ContaDTO> criar(@RequestBody CriarContaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarConta(dto));
    }

    @GetMapping("/{id}")
    public ContaDTO buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping("/{id}/depositar")
    public ContaDTO depositar(@PathVariable Long id, @RequestBody OperacaoDTO dto) {
        return service.depositar(id, dto);
    }

    @PostMapping("/{id}/sacar")
    public ContaDTO sacar(@PathVariable Long id, @RequestBody OperacaoDTO dto) {
        return service.sacar(id, dto);
    }
}
```

E a classe que traduz as exceções das regras em respostas HTTP:

```java
package com.example.demo.controller;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> valorInvalido(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> naoEncontrado(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> regraViolada(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getMessage());
    }
}
```

| Exceção (lançada pelas regras) | Código HTTP | Significado |
|---|---|---|
| `IllegalArgumentException` | **400** Bad Request | Dado inválido (valor ≤ 0, titular vazio) |
| `NoSuchElementException` | **404** Not Found | Conta não existe |
| `IllegalStateException` | **422** Unprocessable Entity | Dado válido, mas a regra recusa (saldo insuficiente) |

O **`@RestControllerAdvice`** é um "tratador global": quando qualquer controller deixa escapar uma dessas exceções, o Spring chama o método correspondente e monta a resposta. Assim o controller e o service ficam **sem `try/catch`**, e a conversão fica em um lugar só.

### 4.3 Teste novo: `ContaControllerTest`

```java
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
```

### 4.4 Explicação

**`@WebMvcTest(ContaController.class)`** sobe só a camada web (controllers, conversão de JSON e o `@RestControllerAdvice`), sem banco e sem service real. É mais leve que `@SpringBootTest`.

**`MockMvc`** simula requisições HTTP **sem abrir um servidor de verdade**. O teste monta a requisição (`post("/contas")...`) e confere a resposta.

**`@MockBean`** coloca no contexto do Spring um `ContaService` **falso**. É o "Mockito dentro do Spring": o controller recebe o falso no construtor, e o teste define o que ele responde com `when(...)`, como na aula 3.

**`jsonPath`** confere campos do JSON de resposta: `jsonPath("$.saldo").value(100.0)` significa "o campo `saldo` da raiz vale 100.0".

**O que este teste confere:** só o contrato HTTP.

| Conferência | Exemplo |
|---|---|
| Rota e método | `post("/contas/1/sacar")` |
| JSON de entrada vira DTO | `{"valor":500.0}` chega como `OperacaoDTO` |
| JSON de saída | `jsonPath("$.titular").value("Maria")` |
| Código de status | `isCreated()`, `isNotFound()`, `isUnprocessableEntity()` |

**O que ele não confere:** as regras (saldo, valores), porque o service é falso. O teste de saldo insuficiente **simula** o erro com `thenThrow` para ver se o controller o converte em **422**.

> **Nota sobre versões:** este projeto usa Spring Boot 3.2.3, em que se usa `@MockBean`. A partir do Spring Boot 3.4, essa anotação foi substituída por `@MockitoBean`, que funciona do mesmo jeito.

---

## 5. Resultado final

### Estrutura do projeto ao final das quatro aulas

Todos esses arquivos **já existem no projeto**, e o código dos blocos deste material é **idêntico** ao dos arquivos das aulas 2, 3 e 4. Na aula 1 os arquivos reais têm, além do que está no material, comentários didáticos extras.

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java
│   │   ├── controller/
│   │   │   ├── ApiExceptionHandler.java          [aula 4]  exceção -> status HTTP
│   │   │   ├── ClienteController.java
│   │   │   ├── ContaController.java              [aula 4]  endpoints /contas
│   │   │   └── UserController.java
│   │   ├── dto/
│   │   │   ├── ContaDTO.java                     [aula 1]
│   │   │   ├── CriarContaDTO.java                [aula 1]
│   │   │   └── OperacaoDTO.java                  [aula 1]
│   │   ├── entity/
│   │   │   └── ContaBancaria.java                [aula 1]  as regras de negócio
│   │   ├── model/
│   │   │   ├── Cliente.java
│   │   │   ├── Produto.java
│   │   │   └── User.java
│   │   ├── repository/
│   │   │   └── ContaRepository.java              [aula 2]  extends JpaRepository
│   │   ├── service/
│   │   │   └── ContaService.java                 [aula 3]  orquestra: busca, regra, salva
│   │   └── util/
│   │       └── CalculaTotal.java
│   └── resources/
│       ├── application.properties
│       └── clientes.json
└── test/java/com/example/demo/
    ├── DemoApplicationTests.java
    ├── controller/
    │   ├── ClienteControllerTest.java
    │   ├── ContaControllerTest.java              [aula 4]  @WebMvcTest, 6 testes
    │   └── UserControllerTest.java
    ├── dto/
    │   └── ContaDTOTest.java                     [aula 1]  3 testes
    ├── entity/
    │   └── ContaBancariaTest.java                [aula 1]  19 testes (1 ignorado)
    ├── repository/
    │   └── ContaRepositoryTest.java              [aula 2]  @DataJpaTest, 5 testes
    ├── service/
    │   └── ContaServiceTest.java                 [aula 3]  Mockito, 8 testes
    └── util/
        └── CalcularProdutoTest.java
```

Cada aula só **acrescenta** arquivos. Nenhum arquivo marcado como `[aula 1]` é alterado nas aulas seguintes.

### Resultado da execução

Rodando `.\mvnw test` com as quatro aulas:

```
[INFO]    Tests run: 6,  Failures: 0, Errors: 0, Skipped: 0 -- in ...controller.ContaControllerTest
[INFO]    Tests run: 3,  Failures: 0, Errors: 0, Skipped: 0 -- in ...dto.ContaDTOTest
[WARNING] Tests run: 19, Failures: 0, Errors: 0, Skipped: 1 -- in ...entity.ContaBancariaTest
[INFO]    Tests run: 5,  Failures: 0, Errors: 0, Skipped: 0 -- in ...repository.ContaRepositoryTest
[INFO]    Tests run: 8,  Failures: 0, Errors: 0, Skipped: 0 -- in ...service.ContaServiceTest
...
[WARNING] Tests run: 51, Failures: 0, Errors: 0, Skipped: 1
[INFO] BUILD SUCCESS
```

(Os 51 incluem os testes anteriores do projeto: `ClienteControllerTest`, `UserControllerTest`, `DemoApplicationTests` e `CalcularProdutoTest`.)

O `ContaBancariaTest` continua com os **mesmos 19 testes da aula 1**, sem nenhuma alteração.

### Visão geral dos tipos de teste

| | Unitário puro | Unitário com mock | Slice (parte do Spring) |
|---|---|---|---|
| **Exemplo** | `ContaBancariaTest` | `ContaServiceTest` | `ContaRepositoryTest`, `ContaControllerTest` |
| **Sobe o Spring?** | Não | Não | Só a parte necessária |
| **Banco de dados** | Não | Não (mock) | Sim, em memória (repository) / não (controller) |
| **Velocidade** | Milissegundos | Milissegundos | Décimos de segundo a alguns segundos |
| **Quando usar** | Regras de negócio | Orquestração entre camadas | Persistência e web |

Uma boa suíte tem **muitos** testes rápidos (regras e orquestração) e **poucos** testes lentos (persistência e web). É a chamada **pirâmide de testes**.

---

## 6. Roteiro de demonstração em aula

**Demonstração 1: o service não pode salvar quando a regra recusa**
1. Em `ContaService.sacar`, mova `repository.save(conta)` para **antes** de `conta.sacar(dto.valor())` (guarde o resultado em uma variável).
2. Rode `.\mvnw test -Dtest=ContaServiceTest`.
3. O teste "Saque acima do saldo lança erro e não salva nada" **falha**: o service salvou mesmo com a regra violada. Um teste que só conferisse a exceção **não perceberia** esse bug.
4. Desfaça a alteração.

**Demonstração 2: o rollback entre os testes**
1. Rode `.\mvnw test -Dtest=ContaRepositoryTest`: os 5 testes passam.
2. Observe que **todos** salvam contas, e mesmo assim `salvarAtualiza` confere `assertEquals(1, repository.count())`.
3. Discuta: isso só funciona porque cada teste começa com o banco **vazio**. O `@DataJpaTest` desfaz (rollback) tudo o que o teste anterior gravou.
4. Para ver a diferença, comente temporariamente o `@DataJpaTest` e coloque `@SpringBootTest` no lugar: sem o rollback automático, as contas dos outros testes ficam no banco e o `salvarAtualiza` **falha** (`expected: <1> but was: <3>`). Desfaça a alteração.

**Demonstração 3: o status HTTP**
1. Em `ApiExceptionHandler`, troque `HttpStatus.UNPROCESSABLE_ENTITY` por `HttpStatus.BAD_REQUEST`.
2. Rode `.\mvnw test -Dtest=ContaControllerTest`.
3. O teste "saldo insuficiente responde 422" **falha**, mostrando que o contrato HTTP está protegido.
4. Desfaça a alteração.

**Demonstração 4: cada camada falha no seu teste**
1. Quebre a regra na entity (por exemplo, `valor <= this.saldo` por `valor <= this.saldo + 1000`).
2. Rode todos os testes. Só falham os testes da **entity**: os do service e do controller continuam passando, porque usam mocks.
3. Discuta: isso é o isolamento. O erro aponta direto para a camada certa.
4. Desfaça a alteração.

---

## 7. Boas práticas desta etapa

- **Mocke as fronteiras, nunca a classe testada.** Simule o que é externo (repository, service), mas não simule a classe que está sendo testada.
- **Confira os efeitos, não só o retorno.** Em operações que gravam dados, use `verify` para confirmar que salvou, e que **não** salvou quando devia recusar.
- **Cada regra em um lugar só.** Se o teste do service começa a repetir casos da entity, ele está testando a camada errada.
- **Poucos testes lentos.** Use `@DataJpaTest` e `@WebMvcTest` só para o que depende do Spring.
- **Monte o cenário por teste.** Evite um `@BeforeEach` grande com respostas de mock que nem todo teste usa (o Mockito estrito reclama).
- **Rode os testes sempre**, antes de cada commit.

---

## 8. Próximos passos

1. **`@SpringBootTest` com `TestRestTemplate`:** um teste de ponta a ponta, subindo tudo (controller, service e banco) e fazendo uma requisição real. É o teste mais lento e deve ser o mais raro.
2. **Bean Validation:** mover validações simples de formato (titular não vazio, valor positivo) para anotações no DTO (`@NotBlank`, `@Positive`), mantendo na entity as regras que dependem do estado (saldo suficiente).
3. **Banco real:** trocar o H2 por PostgreSQL ou MySQL, configurando `spring.datasource.*` no `application.properties`. Entity, service e testes unitários continuam iguais.
4. **`@Nested`:** agrupar testes por tema dentro da mesma classe.
5. **Cobertura de código (JaCoCo):** medir quais linhas e desvios os testes executam. A cobertura mostra o que **não** foi testado, mas cobertura alta não garante testes bons.
