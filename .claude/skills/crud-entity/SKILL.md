---
name: crud-entity
description: Cria ou altera uma entidade de ponta a ponta no backend Spring Boot da Rota Segura (model, repository, dao, bo, dto, controller, exceções e testes), seguindo as camadas do projeto. Use quando o Kelwin pedir uma nova entidade, um CRUD ou um endpoint novo.
argument-hint: "<NomeDaEntidade> [requisito, ex.: RF06]"
---

# Criar ou alterar uma entidade de ponta a ponta

Código em `domain-driven-design-java/backend/`, pacote base `br.com.rotasegura`. Siga os passos em ordem e não pule o passo 1.

## 1. Confirmar antes de codar

- Leia `CLAUDE.md` e `docs/decisions.md`. Se precisar de algo que está **Em aberto** (padrão de nome das colunas, regra de login de um endpoint, versão do Spring Boot...), pare e pergunte com 2 ou 3 opções.
- Confirme com o Kelwin: campos e tipos, obrigatórios, relacionamentos (cardinalidade e lado dono), qual RF e quais RNs a entidade atende. Não invente campos.
- Mostre o plano com a lista de arquivos que vai criar ou alterar. Espere o "ok".

## 2. Gerar, nesta ordem

Fluxo obrigatório: `controller → bo → dao → repository`.

1. `model/<Entity>.java`: `@Entity`, `@Table(name = "T_RS_<NOME>")`, `@Id` com `@GeneratedValue(strategy = GenerationType.IDENTITY)`; colunas com nome, tamanho e nulabilidade explícitos; relacionamentos com `fetch = FetchType.LAZY`.
2. `repository/<Entity>Repository.java`: `interface ... extends JpaRepository<Entity, Long>`; consultas derivadas só se o DAO precisar.
3. `dao/<Entity>DAO.java`: `@Component` que recebe o repository por construtor e expõe as operações de acesso a dados (salvar, buscar por id, listar, excluir, consultas específicas). Sem regra de negócio.
4. `dto/<Entity>Request.java` e `dto/<Entity>Response.java`: records com Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`, `@Size`...) e um mapeamento simples (ex.: `Response.from(entity)`).
5. `bo/<Entity>BO.java`: `@Service` que recebe o DAO por construtor; métodos create, findAll, findById, update, delete; regras de negócio aqui; lança exceções de domínio (não encontrado, regra violada).
6. `controller/<Entity>Controller.java`: `@RestController` que recebe o BO por construtor; GET lista, GET por id, POST (201 + `Location`), PUT, DELETE (204); `@Valid` no corpo; anotações do springdoc (`@Tag`, `@Operation`) com descrição em português.
7. `exception/`: reutilize as exceções e o `@RestControllerAdvice` existentes; crie só o que faltar.
8. `security/`: confira se a regra de acesso cobre os novos endpoints (POST, PUT e DELETE com login; GET aberto; check-in e leituras do sensor liberados). Se o endpoint novo for algo que o cidadão responde durante a emergência, pergunte ao Kelwin se ele também fica liberado.
9. Testes:
   - Nos testes de controller com login, envie o token CSRF (ex.: `with(csrf())`).
   - `bo/<Entity>BOTest.java` com JUnit 5 + Mockito, mockando o DAO: caminho feliz, não encontrado e cada regra de negócio.
   - `controller/<Entity>ControllerTest.java` com `@WebMvcTest` + MockMvc: status de cada verbo, erro de validação (400) e 401 sem login nos verbos protegidos.

## 3. Verificar

- Rode `./mvnw -q test` em `domain-driven-design-java/backend/` e mostre o resultado.
- Avise o que ficou desatualizado fora do Java: `database-design/` (DDL e DML), diagrama de classes, DER e tabela de endpoints. Ofereça rodar `/sql-scripts`, `/diagrams` e `/endpoints-table`.

## 4. Explicar e entregar

- Explique linha a linha, por arquivo, em português. Para conceitos novos, pergunte antes "o que você acha que isso faz?".
- Sugira a mensagem de commit. Não faça commit.
