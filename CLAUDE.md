# H2Gás — Guia para agentes de IA

Plataforma de gestão de pedidos e entregas de gás (GLP) e água mineral.
Este repositório está em evolução do MVP (H2Gás 1.0) para a v2.0, guiada por
**Specification-Driven Development (SDD)**: cada funcionalidade tem um script
em `docs/sdd/` (formato: Cabeçalho, Objetivo, Critérios de Aceitação BDD,
Contrato Técnico, Plano de Testes TDD, Fora de Escopo, Definição de Pronto)
que funciona como contrato vinculante antes de qualquer código ser escrito.

**Antes de implementar qualquer história (HU) ou tarefa de fundação (F),
leia o script correspondente em `docs/sdd/` por completo.** Não implemente
nada listado na seção "Fora de Escopo" desse script, mesmo que pareça uma
melhoria natural — isso é scope creep, não bug fix.

## Stack

- **Backend**: Java 17, Spring Boot, MySQL (driver `mysql-connector-j`,
  `MySQLDialect`) — **não PostgreSQL**, apesar do texto do TCC do projeto
  mencionar Postgres; o código real usa MySQL.
- **Frontend**: Angular 19, componentes standalone (sem NgModule), Tailwind
  CSS + DaisyUI.
- **Migrações de schema**: a partir do script F3, via Flyway
  (`backend/src/main/resources/db/migration/V{n}__{descricao}.sql`). Não usar
  `ddl-auto=update` do Hibernate para mudanças estruturais novas.
- **Testes**: JUnit 5 + Mockito + `spring-boot-starter-test` no backend
  (`backend/src/test/java/...`); Karma + Jasmine (`ng test`) no frontend
  (`*.spec.ts` ao lado do arquivo testado).
- **CI**: `.github/workflows/workflow.yml` roda `mvn test` do backend em
  todo PR. Não há step de testes de frontend nem análise estática dedicada
  hoje — isso é uma lacuna conhecida, não replicar silenciosamente como se
  estivesse resolvida.

## Regras de negócio globais (RN01–RN05)

- **RN01**: todo cálculo financeiro (subtotal, total) é feito no backend. O
  frontend só exibe; nunca envia um preço que o backend aceita sem recalcular.
- **RN02**: quando `stock_quantity` de um produto chega a zero, o sistema
  bloqueia/oculta a venda automaticamente, na mesma transação do decremento.
- **RN03**: todo pedido novo nasce com status `PENDING`.
- **RN04**: avaliação (`reviews`) só é aceita se o pedido estiver `DELIVERED`.
- **RN05**: listagens de fornecedores/produtos para o cliente filtram por
  cidade compatível com o endereço ativo — sempre no backend, nunca só no
  client.

## Convenções de código

- Backend: `controller` → `services` → `model.repository`; entidades em
  `model.entity`, DTOs em `model.dto`, mapeamento em `model.mapper`.
  Controllers nunca retornam entidades JPA diretamente.
- Frontend: um serviço HTTP por recurso em `services/`; cores/tipografia
  vêm dos Design Tokens (script F2) — sem HEX literal novo em template/CSS.

## Git

- **Push só com permissão explícita.** Não executar `git push`, `--force` /
  `--force-with-lease`, nem abrir PR, a menos que o desenvolvedor peça isso
  nesta conversa. Preparar commits e texto de PR localmente é permitido;
  enviar ao remoto não é.
- Commits locais (sem push) podem ser feitos quando o desenvolvedor pedir
  para commitar ou preparar a branch.

## Fluxo de trabalho por script (TDD)

1. Ler o script completo em `docs/sdd/`.
2. Escrever os testes do "Plano de Testes" primeiro (devem falhar — Red).
3. Implementar o mínimo para os testes passarem (Green).
4. Refatorar mantendo os testes verdes.
5. Rodar `mvn -f backend/pom.xml test` e/ou `ng test` antes de abrir PR.
6. Commit referenciando o ID do script (ex.: `HU01: filtro de catálogo por localização`).

Detalhes completos do processo (Skills/Rules/Workflows) em
[`docs/sdd/f1-harness-sdd.md`](docs/sdd/f1-harness-sdd.md).
