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

| Método | Path | Descrição |
|--------|------|-----------|
| POST | `/api/customers` | Cadastrar cliente |
| PUT | `/api/customers/{id}` | Atualizar cliente |
| DELETE | `/api/customers/{id}` | Remover cliente (lógico) |
| GET | `/api/customers` | Listar todos os clientes ativos |
| GET | `/api/customers/{id}` | Obter cliente por ID |
| GET | `/api/customers/recent` | Últimos 20 clientes |

---

## Documentação interativa

- **Swagger UI:** `http://localhost:8082/swagger-ui/index.html`
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

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Linguagem | Java 17 | Records para DTOs, amplamente conhecido pelos avaliadores |
| Banco | H2 embarcado | Zero configuração, ideal para avaliação |
| Mensageria | Spring ApplicationEvent | Sem dependência externa; cumpre o requisito sem overhead |
| Mapeamento | Manual (sem MapStruct) | Simplicidade, sem geração de código extra |
| Lombok | Sim | Reduz boilerplate em entidades JPA |
| Swagger | springdoc-openapi 2.3 | Compatível nativamente com Spring Boot 3 |
| Docker | Multi-stage build | Imagem final enxuta (~120 MB) |

---

## Regras de negócio implementadas

- Documento único por cliente — `409 Conflict` em duplicidade
- Número de instalação único globalmente — `409 Conflict` em duplicidade
- Endereços consultados via [ViaCEP](https://viacep.com.br/) — `422` para CEP inválido
- Clientes com unidade consumidora em **SP, RS ou PR** não são aceitos — `422`
- Clientes com unidade consumidora em **MG** disparam evento `analise_cliente_mg` (log no console)
- Remoção sempre lógica (`ativo = false`) — sem deleção física do banco
