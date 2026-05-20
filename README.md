# Customer API

API REST de cadastro, atualização, remoção e pesquisa de clientes.
**Desafio Técnico — Bolt | Java 17 + Spring Boot 3.2**

---

## Pré-requisitos

- Java 17+
- Maven 3.9+
- Docker e Docker Compose (opcional)

---

## Como executar

### Via Maven (sem Docker)

```bash
mvn clean package -DskipTests
java -jar target/customer-api-1.0.0.jar
```

### Via Docker Compose

```bash
docker compose up --build
```

A aplicação estará disponível em `http://localhost:8082`.

---

## Endpoints

| Método | Path                      | Descrição                     |
| ------- | ------------------------- | ------------------------------- |
| POST    | `/api/customers`        | Cadastrar cliente               |
| PUT     | `/api/customers/{id}`   | Atualizar cliente               |
| DELETE  | `/api/customers/{id}`   | Remover cliente (lógico)       |
| GET     | `/api/customers`        | Listar todos os clientes ativos |
| GET     | `/api/customers/{id}`   | Obter cliente por ID            |
| GET     | `/api/customers/recent` | Últimos 20 clientes            |

---

## Documentação interativa

- **Swagger UI:** `http://localhost:8082/swagger-ui/index.html`
  > ⚠️ Usar o caminho `/swagger-ui/index.html` — o atalho `/swagger-ui.html` retorna página em branco com springdoc 2.x.
  >
- **H2 Console:** `http://localhost:8082/h2-console`
  - JDBC URL: `jdbc:h2:mem:customerdb`
  - Usuário: `sa` | Senha: *(vazio)*

---

## Coleção Postman

Importe `postman/customer-api.postman_collection.json` no Postman.
Configure a variável de ambiente: `baseUrl = http://localhost:8082`

---

## Executar testes

```bash
mvn test
```

---

## Decisões técnicas

| Decisão   | Escolha                 | Justificativa                                             |
| ---------- | ----------------------- | --------------------------------------------------------- |
| Linguagem  | Java 17                 | Records para DTOs, amplamente conhecido pelos avaliadores |
| Banco      | H2 embarcado            | Zero configuração, ideal para avaliação               |
| Mensageria | Spring ApplicationEvent | Sem dependência externa; cumpre o requisito sem overhead |
| Mapeamento | Manual (sem MapStruct)  | Simplicidade, sem geração de código extra              |
| Lombok     | Sim                     | Reduz boilerplate em entidades JPA                        |
| Swagger    | springdoc-openapi 2.3   | Compatível nativamente com Spring Boot 3                 |
| Docker     | Multi-stage build       | Imagem final enxuta (~120 MB)                             |

---

## Regras de negócio implementadas

- Documento único por cliente — `409 Conflict` em duplicidade
- Número de instalação único globalmente — `409 Conflict` em duplicidade
- Endereços consultados via [ViaCEP](https://viacep.com.br/) — `422` para CEP inválido
- Clientes com unidade consumidora em **SP, RS ou PR** não são aceitos — `422`
- Clientes com unidade consumidora em **MG** disparam evento `analise_cliente_mg` (log no console)
- Remoção sempre lógica (`ativo = false`) — sem deleção física do banco



## 1. Overview técnico

REST API para cadastro, manutenção e consulta de clientes, com regras de negócio regionais e notificação assíncrona para clientes em Minas Gerais.

**Stack:**

- Java 17+
- Spring Boot 3.x
- JPA / Hibernate
- H2 embarcado (runtime e testes)
- Maven
- Porta: `8082`

**Diferenciais incluídos:** Docker + Docker Compose, Swagger/OpenAPI, testes unitários, testes de integração.

---

## 2. Estrutura de Pacotes

```
com.bolt.customerapi
├── config/
│   ├── OpenApiConfig.java         # Configuração Swagger/OpenAPI
│   └── AppConfig.java             # Beans: RestTemplate
├── controller/
│   └── CustomerController.java    # Endpoints REST
├── dto/
│   ├── request/
│   │   ├── CustomerRequest.java   # Record: criar/atualizar cliente
│   │   └── ConsumerUnitRequest.java
│   ├── response/
│   │   ├── CustomerResponse.java  # Record: resposta cliente
│   │   └── ErrorResponse.java     # Record: resposta de erro padronizada
│   └── viacep/
│       └── ViaCepResponse.java    # Record: resposta ViaCEP
├── entity/
│   ├── Customer.java              # Entidade principal
│   ├── ConsumerUnit.java          # Unidade consumidora
│   └── Address.java               # @Embeddable endereço
├── event/
│   └── ClienteAnaliseMGEvent.java # ApplicationEvent para clientes MG
├── exception/
│   ├── DocumentAlreadyExistsException.java  # 409
│   ├── CustomerNotFoundException.java       # 404
│   ├── RegionNotAllowedException.java       # 422
│   ├── InvalidCepException.java             # 422
│   └── GlobalExceptionHandler.java          # @ControllerAdvice
├── integration/
│   └── ViaCepClient.java          # Integração REST com ViaCEP
├── repository/
│   └── CustomerRepository.java    # JpaRepository + queries customizadas
└── service/
    └── CustomerService.java       # Toda a lógica de negócio
```

---

## 3. Modelo de Dados

### Customer

| Campo         | Tipo                 | Restrições               |
| ------------- | -------------------- | -------------------------- |
| id            | Long                 | PK, auto-gerado            |
| nome          | String               | NOT NULL                   |
| documento     | String               | NOT NULL, UNIQUE           |
| ativo         | boolean              | default true               |
| createdAt     | LocalDateTime        | preenchido automaticamente |
| updatedAt     | LocalDateTime        | atualizado automaticamente |
| endereco      | Address              | @Embedded, NOT NULL        |
| consumerUnits | List\<ConsumerUnit\> | @OneToMany, cascade ALL    |

### ConsumerUnit

| Campo            | Tipo     | Restrições                 |
| ---------------- | -------- | ---------------------------- |
| id               | Long     | PK, auto-gerado              |
| nome             | String   | NOT NULL (ex.: "Minha casa") |
| numeroInstalacao | String   | NOT NULL, UNIQUE global      |
| endereco         | Address  | @Embedded, NOT NULL          |
| customer         | Customer | @ManyToOne, FK               |

### Address (@Embeddable)

| Campo       | Tipo   | Notas                           |
| ----------- | ------ | ------------------------------- |
| cep         | String | Fornecido na request            |
| logradouro  | String | Preenchido via ViaCEP           |
| bairro      | String | Preenchido via ViaCEP           |
| localidade  | String | Cidade — preenchido via ViaCEP |
| uf          | String | Estado — preenchido via ViaCEP |
| numero      | String | Fornecido na request (opcional) |
| complemento | String | Fornecido na request (opcional) |

> **Nota:** `Address` é `@Embeddable`. Em `Customer`, colunas têm prefixo `cliente_`. Em `ConsumerUnit`, prefixo `unidade_`. Isso evita colisão de nomes na tabela do H2.

---

## 4. Endpoints REST

Base path: `/api/customers`

| Método | Path                      | Status sucesso | Descrição                                    |
| ------- | ------------------------- | -------------- | ---------------------------------------------- |
| POST    | `/api/customers`        | 201            | Cadastrar cliente                              |
| PUT     | `/api/customers/{id}`   | 200            | Atualizar cliente                              |
| DELETE  | `/api/customers/{id}`   | 204            | Remoção lógica                              |
| GET     | `/api/customers`        | 200            | Listar todos os ativos                         |
| GET     | `/api/customers/{id}`   | 200            | Obter por ID                                   |
| GET     | `/api/customers/recent` | 200            | Últimos 20 em ordem decrescente por createdAt |

### Request Body — POST/PUT

```json
{
  "nome": "João Silva",
  "documento": "123.456.789-00",
  "endereco": {
    "cep": "01310-100",
    "numero": "123",
    "complemento": "Apto 4"
  },
  "consumerUnits": [
    {
      "nome": "Minha casa",
      "numeroInstalacao": "9001234",
      "endereco": {
        "cep": "30130-110",
        "numero": "45"
      }
    }
  ]
}
```

### Response Body — Customer

```json
{
  "id": 1,
  "nome": "João Silva",
  "documento": "123.456.789-00",
  "ativo": true,
  "createdAt": "2026-05-20T10:30:00",
  "updatedAt": "2026-05-20T10:30:00",
  "endereco": {
    "cep": "01310-100",
    "logradouro": "Avenida Paulista",
    "bairro": "Bela Vista",
    "localidade": "São Paulo",
    "uf": "SP",
    "numero": "123",
    "complemento": "Apto 4"
  },
  "consumerUnits": [...]
}
```

### Response Body — Erro padronizado

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Documento já cadastrado: 123.456.789-00",
  "timestamp": "2026-05-20T10:30:00"
}
```

---

## 5. Regras de Negócio

Todas implementadas em `CustomerService`. Ordem de validação no cadastro:

1. **Documento único** — `customerRepository.existsByDocumento(documento)` → `DocumentAlreadyExistsException` (409)
2. **CEP do cliente** — chamar `viaCepClient.buscar(cep)`. Se 404 ou erro → `InvalidCepException` (422). Preencher `Address` com dados retornados.
3. **CEP de cada unidade consumidora** — mesmo processo do item 2.
4. **Estados bloqueados** — para cada unidade consumidora, se `address.uf` ∈ `{SP, RS, PR}` → `RegionNotAllowedException` (422) com mensagem indicando o estado.
5. **Número de instalação único** — `consumerUnitRepository.existsByNumeroInstalacao(n)` → `DocumentAlreadyExistsException` com mensagem específica (409).
6. **Persistência** — salvar cliente com `customerRepository.save(customer)`.
7. **Evento MG** — após salvar, verificar se alguma unidade tem `uf = "MG"`. Se sim, publicar `ClienteAnaliseMGEvent` via `applicationEventPublisher.publishEvent(...)`.

**Atualização (PUT):**

- Revalida documentos e CEPs
- Permite alterar documento somente se o novo não pertencer a outro cliente
- Substitui lista de unidades consumidoras completa

**Remoção (DELETE):**

- Busca cliente por ID (404 se não encontrado ou já inativo)
- Define `ativo = false` e `updatedAt = now()`
- Salva

---

## 6. Integração ViaCEP

`ViaCepClient` usa `RestTemplate` (bean configurado em `AppConfig`).

```
GET https://viacep.com.br/ws/{cep}/json/
```

- CEP fornecido pode ter máscara (`01310-100`) ou não (`01310100`) — normalizar removendo `-` antes da chamada
- Resposta com campo `"erro": true` indica CEP inválido → `InvalidCepException`
- `HttpClientErrorException` (404) → `InvalidCepException`
- Timeout configurado: connect 3s, read 5s

---

## 7. Mensageria — ApplicationEvent

```java
public class ClienteAnaliseMGEvent extends ApplicationEvent {
    private final Long customerId;
    private final String documento;
    // construtor, getters
}
```

Publicado pelo `CustomerService` após persistência bem-sucedida de cliente com unidade em MG.

Um `@EventListener` (log-only) registra o evento para demonstrar o funcionamento — o consumer real não é requisito.

---

## 8. Testes

### Unitários — `CustomerServiceTest`

- Cadastro com documento duplicado → `DocumentAlreadyExistsException`
- Cadastro com unidade em SP → `RegionNotAllowedException`
- Cadastro com unidade em RS → `RegionNotAllowedException`
- Cadastro com unidade em PR → `RegionNotAllowedException`
- Cadastro com unidade em MG → publica evento
- CEP inválido → `InvalidCepException`
- Deleção lógica → `ativo = false`
- Deleção de cliente não encontrado → `CustomerNotFoundException`

Mock do `ViaCepClient` e `ApplicationEventPublisher`.

### Unitários — `ViaCepClientTest`

- CEP válido → retorna `ViaCepResponse` preenchido
- CEP com campo `erro: true` → lança `InvalidCepException`
- HttpClientError (404) → lança `InvalidCepException`

### Integração — `CustomerControllerIT`

- `POST /api/customers` → 201, verifica body
- `POST /api/customers` com documento duplicado → 409
- `POST /api/customers` com UF bloqueada → 422
- `GET /api/customers/{id}` → 200
- `GET /api/customers/{id}` não encontrado → 404
- `GET /api/customers/recent` → 200, lista ≤ 20 itens
- `DELETE /api/customers/{id}` → 204, verifica `ativo=false`

WireMock stubado para respostas do ViaCEP.

---

## 9. Docker

### `Dockerfile` (multi-stage)

```dockerfile
# Stage 1: build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `docker-compose.yml`

```yaml
version: "3.9"
services:
  customer-api:
    build: .
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8082/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
```

---

## 10. Configurações da Aplicação

### `application.yml`

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:h2:mem:customerdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false
  h2:
    console:
      enabled: true
      path: /h2-console

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

---

## 11. README — Pontos-chave

- Como compilar: `mvn clean package`
- Como executar: `java -jar target/*.jar` ou `docker compose up --build`
- URL Swagger: `http://localhost:8082/swagger-ui.html`
- URL H2 Console: `http://localhost:8082/h2-console`
- Coleção Postman exportada em `/postman/customer-api.postman_collection.json`
- Justificativas: H2 para simplicidade de avaliação; SpringDoc para documentação interativa; ApplicationEvent para mensageria sem dependência externa; multi-stage Docker para imagem enxuta

---

## 12. Coleção Postman

Arquivo: `postman/customer-api.postman_collection.json`

Requisições:

1. Cadastrar cliente (POST)
2. Atualizar cliente (PUT)
3. Deletar cliente (DELETE)
4. Listar todos (GET)
5. Obter por ID (GET)
6. Listar últimos 20 (GET)

Variável de ambiente: `{{baseUrl}} = http://localhost:8082`

---

## Decisões de Design

| Decisão          | Escolha            | Justificativa                                     |
| ----------------- | ------------------ | ------------------------------------------------- |
| Linguagem         | Java 17+           | Familiaridade dos avaliadores, Records para DTOs  |
| Arquitetura       | Layered por camada | Simplicidade, clareza, padrão Spring reconhecido |
| Banco             | H2 embarcado       | Zero setup, ideal para avaliação                |
| Mensageria        | ApplicationEvent   | Sem dependência externa, cumpre o requisito      |
| Address           | @Embeddable        | Evita tabela extra desnecessária para o escopo   |
| Remoção lógica | campo `ativo`    | Requisito explícito, sem DELETE físico          |
