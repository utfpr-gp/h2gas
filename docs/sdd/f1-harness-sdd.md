# F1 — Harness SDD (Skills, Rules, Workflows)

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F1 |
| Perfil | Infraestrutura (Fundação) — sem perfil de usuário final |
| Prioridade MoSCoW | Must have (pré-requisito de todos os demais scripts) |
| Dependências | Nenhuma — este é o primeiro script da fila |
| Referência no TCC | Cap. 4.3 "Desenvolvimento Agêntico e Specification-Driven Development (SDD)" |

## 2. Objetivo

Como desenvolvedor orquestrando agentes de IA, quero configurar o *Harness* do
projeto (Skills, Rules e Workflows) e as convenções de TDD/CI, para que a
codificação de cada história de usuário (HU01…HU18) e dos demais scripts de
fundação (F2…F5) siga um processo determinístico, padronizado e resistente a
alucinações — em vez de depender de instruções ad-hoc a cada sessão.

Este script não entrega uma funcionalidade de produto. Ele entrega o
**processo** que os scripts seguintes vão executar. Por isso as seções
"Critérios de Aceitação" e "Plano de Testes" abaixo validam o processo em si
(documentos existem, são coerentes, são verificáveis), não um comportamento
de sistema.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Skills documentadas**: Dado o repositório do H2Gás, Quando um
  novo agente de IA inicia uma sessão, Então ele encontra em
  `docs/sdd/f1-harness-sdd.md` a lista de capacidades disponíveis (terminal,
  Maven, npm/ng, leitura do repositório) e não precisa que o desenvolvedor
  as descreva verbalmente.

- **Cenário 2 — Rules aplicadas por padrão**: Dado que o agente vai gerar
  código para qualquer HU, Quando ele cria um endpoint, entidade, DTO ou
  componente Angular, Então ele segue as convenções de pacote/pasta e as
  regras de negócio globais (RN01–RN05) descritas na seção 5, sem que cada
  script individual precise repeti-las.

- **Cenário 3 — Workflow determinístico por história**: Dado o identificador
  de uma história (ex.: HU01), Quando o desenvolvedor pede para implementá-la,
  Então o agente segue os 7 passos do Workflow Micro (seção 6.2) na ordem
  definida, sem pular a etapa de teste-antes-do-código (Red) nem a de
  revisão humana antes do merge.

- **Cenário 4 — Rastreabilidade de escopo**: Dado um script SDD de uma HU
  específica, Quando o agente encontra uma seção "Fora de Escopo", Então ele
  não implementa esses itens mesmo que tecnicamente triviais, evitando
  scope creep (conforme §5.3 do TCC).

## 4. Skills (capacidades instaladas no ambiente)

| Skill | Descrição | Uso |
|---|---|---|
| Execução de terminal | Rodar `mvn` (backend) e `npm`/`ng` (frontend) | Rodar testes, build, lint |
| Leitura de repositório | Acesso de leitura a todo o monorepo (`backend/`, `frontend/`, `docs/sdd/`) | Recuperar contrato técnico e código existente antes de codificar |
| Execução de testes | `mvn -f backend/pom.xml test` / `ng test` | Validar ciclo Red-Green-Refactor |
| Análise estática | Compilação Java (falha em warnings de tipo) e `ng build` (falha em erro de template/tipo) | Autocorreção antes do PR |
| Controle de versão | `git` (commit, branch, PR via `gh`) | Governança e histórico por HU |

Este projeto **não** usa MCP externo nem orquestração multi-agente
autônoma — diferente do texto do TCC (que descreve integração com IDE
Antigravity e MCP), a execução real destes scripts é feita via Claude Code
neste repositório. Isso é uma adaptação de implementação, não uma mudança de
metodologia: o padrão PRD → SDDoc → TDD → revisão → CI/CD é mantido.

## 5. Rules (diretrizes rígidas de arquitetura e comportamento)

### 5.1 Backend (Spring Boot / Java 17)

- Pacotes seguem a estrutura existente: `controller`, `services`,
  `model.entity`, `model.dto`, `model.mapper`, `model.repository`,
  `configuration`, `exceptions`, `enums`, `response`, `util`.
- Controllers nunca retornam entidades JPA diretamente — sempre DTOs
  (`model.dto`), mapeados via `model.mapper`.
- **RN01 (Cálculo Financeiro Seguro)**: todo cálculo de subtotal/total de
  pedido é feito no backend. O frontend nunca envia um valor de preço que o
  backend confia sem recalcular a partir do catálogo.
- **RN02 (Bloqueio Automático de Estoque)**: qualquer serviço que decremente
  `stock_quantity` deve, na mesma transação, ocultar/bloquear o item quando
  chegar a zero.
- **RN03 (Status Inicial do Pedido)**: todo `Order` nasce com status
  `PENDING`; não existe caminho de criação que já nasça `CONFIRMED` ou além.
- **RN04 (Avaliação Restrita)**: endpoints de `reviews` validam que o pedido
  referenciado está com status `DELIVERED` antes de aceitar a gravação.
- **RN05 (Restrição Geográfica)**: listagens de fornecedores/produtos para
  cliente sempre filtram por `city_id` compatível com o endereço ativo do
  usuário — nunca client-side apenas.
- Migrações de schema (a partir do F3) são feitas via Flyway
  (`backend/src/main/resources/db/migration/V{n}__{descricao}.sql`), nunca
  via `ddl-auto=update` para mudanças estruturais.
- Banco de dados é **MySQL** (não PostgreSQL — ver nota de divergência no
  F3). Dialect `MySQLDialect`, driver `mysql-connector-j`.

### 5.2 Frontend (Angular 19 / Tailwind + DaisyUI)

- Componentes standalone (padrão já usado no projeto — `imports: []` no
  `@Component`, sem `NgModule`).
- Estilos usam classes Tailwind + componentes DaisyUI; cores e tipografia
  vêm exclusivamente dos Design Tokens definidos no F2 — nenhum HEX literal
  novo em template/CSS.
- Serviços HTTP ficam em `services/`, um por recurso, seguindo o padrão
  existente (`*.service.ts` + `*.service.spec.ts`).
- Nenhum recálculo de preço no frontend além de exibição — consumir sempre o
  valor vindo da API (reforça RN01).

### 5.3 Regras de escopo (todas as HUs)

- Um script SDD só é considerado "pronto para implementar" depois de passar
  pela seção "Definição de Pronto" do próprio script.
- A seção "Fora de Escopo" de cada script é vinculante: implementar algo lá
  listado é tratado como desvio, mesmo que pareça uma melhoria natural.
- Nenhuma história `Should have`/`Could have` (HU11+) é implementada antes de
  todas as `Must have` (HU01–HU10 + F1–F5) estarem em `Done`, salvo decisão
  explícita do orientador/Product Owner.

## 6. Workflows (roteiros determinísticos)

### 6.1 Workflow Macro (uma vez, já em execução com este documento)

1. PRD já existe (Cap. 5 + Apêndice A do TCC) — fonte das Histórias de
   Usuário e Critérios de Aceitação.
2. SDDoc já existe (Cap. 6 do TCC) — fonte da arquitetura macro.
3. Este Harness (F1) traduz PRD + SDDoc em Skills/Rules/Workflows acionáveis.
4. Cada HU/F vira um script individual em `docs/sdd/`, seguindo o template
   fixado nesta fase de brainstorming.

### 6.2 Workflow Micro (repetido a cada script F2…F5, HU01…HU18)

1. **Recuperação de contexto**: ler `docs/sdd/<id>-<slug>.md` por completo
   (Critérios de Aceitação + Contrato Técnico + Plano de Testes) e o estado
   atual do código relacionado.
2. **Quebra de tarefas**: separar em mudanças de schema/migration, backend
   (services/controllers), frontend (componentes/serviços) — nessa ordem de
   dependência.
3. **TDD — Red**: escrever os testes listados no "Plano de Testes" do script
   primeiro, confirmando que falham (funcionalidade ainda não existe).
4. **TDD — Green**: implementar o código mínimo para os testes passarem.
5. **Refatoração**: limpar duplicação/nomes mantendo os testes verdes.
6. **Autocorreção**: rodar build + testes localmente
   (`mvn -f backend/pom.xml test`, `ng test`, `ng build`); corrigir erros de
   análise estática antes de prosseguir.
7. **Commit e PR**: commit referenciando o ID do script (ex.: `HU01: filtro
   de catálogo por localização`); abrir PR. CI (`.github/workflows/workflow.yml`)
   roda os testes de backend automaticamente. Merge em `main` dispara o
   deploy existente (`deploy.yml`).

> Nota sobre o CI atual: hoje `.github/workflows/workflow.yml` só roda
> testes de **backend**. Não há step de testes de frontend nem de análise
> estática dedicada. Isso é uma lacuna real do repositório (não do TCC) —
> cada script de fundação/HU que alterar o frontend deve rodar `ng test`
> manualmente até que a CI seja estendida; essa extensão não faz parte do
> escopo Must-have definido pelo TCC e não será feita de forma implícita.

## 7. Convenções de Teste (TDD) já disponíveis no repositório

- **Backend**: JUnit 5 + Mockito + `spring-boot-starter-test` +
  `spring-security-test` (já em `pom.xml`). Testes de serviço em
  `backend/src/test/java/com/h2gas/services/*ServiceTest.java`, testes de
  controller em `backend/src/test/java/com/h2gas/controller/*ControllerTest.java`.
  Cada script novo segue esse mesmo padrão de nomenclatura e localização.
- **Frontend**: Karma + Jasmine via `ng test` (já configurado). Specs em
  `*.spec.ts` ao lado do arquivo testado — padrão já usado em
  `services/*.service.spec.ts`.
- Cada cenário BDD ("Dado/Quando/Então") de um script vira, no mínimo, um
  teste automatizado nomeado de forma rastreável ao cenário (ex.:
  `deveFiltrarCatalogoPorCidadeDoEndereco`).

## 8. Fora de Escopo

- Orquestração multi-agente autônoma ou integração MCP real (o TCC descreve
  isso na visão macro; a execução prática deste roteiro é conduzida
  diretamente por um único agente/desenvolvedor via Claude Code).
- Extensão da esteira de CI para rodar testes de frontend ou análise
  estática adicional — sinalizado como lacuna, não resolvido aqui.
- Qualquer regra de negócio específica de uma HU (essas ficam nos scripts
  individuais, este documento só fixa as regras **globais** RN01–RN05).

## 9. Artefato operacional

As Rules deste documento foram também materializadas em
[`CLAUDE.md`](../../CLAUDE.md) na raiz do repositório, para que qualquer
sessão futura de agente de IA neste projeto já nasça restrita a elas — sem
depender de reler este script manualmente a cada sessão. Esse arquivo é o
elo prático entre a "Visão Macro" (Harness) e a "Visão Micro"
(implementação de cada HU) descritas no Cap. 4.3 do TCC.

## 10. Definição de Pronto

- [x] Skills, Rules e Workflows documentados e coerentes com o código real
      do repositório (stack, pacotes, padrões de teste já existentes).
- [x] Divergências entre o texto do TCC e o código real (MySQL vs
      PostgreSQL, ausência de Flyway, CI só de backend) estão explicitadas,
      não escondidas.
- [x] `CLAUDE.md` criado na raiz do repositório operacionalizando as Rules.
- [ ] Revisado e aprovado pelo desenvolvedor/orientador antes de F2 começar.
