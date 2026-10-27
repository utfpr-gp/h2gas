# HU18 — Loja aberta só no horário de funcionamento

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU18 |
| Perfil | Cliente, Visitante e Fornecedor |
| Prioridade MoSCoW | Must have — decisão do autor em 2026-10-27 ("as lojas estão abertas mesmo fora do horário; remover o interruptor") |
| Dependências | F3 (Flyway), F6 (OpenAPI), HU06 (pedidos recebidos), HU13 (vitrine pública), HU16 (ordenação "abertas primeiro") |
| Referência no TCC | RN05 (vitrine); revisão do status aberto/fechado do MVP |

## 2. História de Usuário

Como cliente, quero ver como aberta só a loja que está de fato dentro do
horário de funcionamento, para não fazer pedido numa loja que não vai
atender; e como fornecedor, quero que a loja abra e feche sozinha pelo
horário que cadastrei, sem depender de lembrar de apertar um botão.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Aberta dentro do horário**: Dado uma loja com horário
  08:00–18:00, Quando são 10:00 em Brasília, Então ela aparece na vitrine,
  no catálogo de produtos e no cabeçalho da loja como "Aberta".
- **Cenário 2 — Fechada fora do horário**: Dado a mesma loja, Quando são
  19:00 em Brasília, Então ela não aparece na vitrine (nem os produtos
  dela), exceto na ordenação "Abertas primeiro", onde vem depois das
  abertas com o selo "Fechada".
- **Cenário 3 — Limites do horário**: Dado o horário 08:00–18:00, Então às
  08:00 a loja está aberta e às 18:00 está fechada (a abertura conta, o
  fechamento não).
- **Cenário 4 — Horário que vira a meia-noite**: Dado o horário 18:00–02:00
  (que a tela de configurações já aceita hoje), Então a loja está aberta às
  23:00 e à 01:00, e fechada às 10:00.
- **Cenário 5 — Sem horário cadastrado**: Dado uma loja sem abertura ou sem
  fechamento cadastrado, Então ela é tratada como fechada.
- **Cenário 6 — Sem interruptor**: Dado o fornecedor em "Pedidos recebidos"
  ou "Histórico de pedidos", Então vê o estado da loja só para leitura
  ("Aberta agora · fecha às 18:00" / "Fechada · abre às 08:00"), com atalho
  para Configurações; não existe mais botão de abrir ou fechar.
- **Cenário 7 — Rota antiga removida**: Dado uma chamada a
  `PATCH /api/suppliers/{id}/status`, Então a rota não existe mais.
- **Cenário 8 — Fuso fixo**: Dado o servidor rodando em UTC, Então a regra
  usa sempre a hora de Brasília (`America/Sao_Paulo`).

## 4. Estado atual (achados)

- `Supplier.open` (coluna `suppliers.open`, criada pelo antigo
  `ddl-auto=update`, sem migração) só muda por
  `PATCH /api/suppliers/{id}/status` (`SupplierController.java:402-419` →
  `SupplierService.updateStatusOpenClosed`, `:431-437`). A loja nasce
  fechada (padrão da entidade) e só abre quando o fornecedor aciona.
- `openingTime`/`closingTime` (LocalTime) são gravados em
  `PUT /suppliers/mine/settings` e exibidos, mas nada os compara com a hora
  atual.
- Leem o flag: `SupplierRepository.CATALOG_WHERE` e o `CASE ... s.open` do
  `findForCatalog` (HU16); `ProductItemRepository.AVAILABLE_WHERE`
  (`p.supplier.open = true`); `StorefrontDTO.open`;
  `GET /api/suppliers/{id}/open`. Derivados mortos:
  `findByActiveAndOpen`, `findByCityIdAndActiveAndOpen`.
- Interruptores no front: `received-orders` e `order-history`
  (`data-testid="store-toggle"`, via `SupplierService.openDistribuidor`);
  `distributor-management` também tem um, mas não está roteado.
- Virar a meia-noite **já é permitido**: `updateMySettings` só recusa
  abertura igual ao fechamento, há teste gravando 18:00→02:00
  (`SupplierServiceTest`), e a tela de configurações mostra "do dia
  seguinte". A regra nova mantém isso para não regredir.
- Não há `Clock` nem fuso configurado: todo o backend usa `now()` direto,
  então não há como fixar a hora num teste.
- `OrderService.createOrder` não confere se a loja está aberta (a única
  barreira é a listagem). Fica registrado como risco (ver §7).

## 5. Contrato Técnico

### 5.1 Hora e regra
- Bean `Clock` em `configuration/ClockConfig`:
  `Clock.system(ZoneId.of("America/Sao_Paulo"))`. Nos testes, um `Clock`
  fixo (`@Primary`, profile `test`, 10:00 de Brasília), para que nenhum
  teste dependa da hora em que a suíte roda.
- Regra única, `model/OpeningHours.isOpenAt(opening, closing, now)`:
  horários nulos → fechada; `opening < closing` →
  `now >= opening && now < closing`; senão (vira a meia-noite) →
  `now >= opening || now < closing`.

### 5.2 Consultas
- A mesma regra em JPQL, numa constante reaproveitada (`OPEN_NOW`), com o
  parâmetro `:now` (LocalTime do `Clock`):
  ```
  (s.openingTime IS NOT NULL AND s.closingTime IS NOT NULL AND (
     (s.openingTime < s.closingTime AND :now >= s.openingTime AND :now < s.closingTime)
     OR (s.openingTime > s.closingTime AND (:now >= s.openingTime OR :now < s.closingTime))))
  ```
- Usada em `CATALOG_WHERE` (no lugar de `s.open = true`), no `CASE` de
  "abertas primeiro" do `findForCatalog` e em `AVAILABLE_WHERE` (no lugar de
  `p.supplier.open = true`, com `p.supplier` no lugar de `s`).

### 5.3 API
- `StorefrontDTO.open` e `GET /api/suppliers/{id}/open` continuam, agora
  **calculados** por `OpeningHours`; OpenAPI diz "aberta agora pelo horário
  de funcionamento (Brasília)".
- **Sai** `PATCH /api/suppliers/{id}/status`, `updateStatusOpenClosed`, o
  campo `Supplier.open` e os métodos `findByActiveAndOpen` /
  `findByCityIdAndActiveAndOpen`.
- Migração `V16__supplier_open_by_hours.sql`:
  ``ALTER TABLE suppliers DROP COLUMN `open`;``

### 5.4 Frontend
- `received-orders` e `order-history`: o `store-toggle` vira selo somente
  leitura (`data-testid="store-status"`) com o estado e o próximo horário
  ("fecha às" / "abre às"), e link para `/configuracao`.
- Sai `SupplierService.openDistribuidor`. `getOpen` continua.
- Tokens F2, sem HEX literal novo.

## 6. Plano de Testes (TDD)

**Backend**
1. `OpeningHoursTest`: `deveEstarAbertaDentroDoHorario`,
   `deveEstarFechadaForaDoHorario`, `deveAbrirNaHoraDaAberturaEFecharNaHoraDoFechamento`,
   `deveTratarHorarioQueViraAMeiaNoite`, `deveTratarLojaSemHorarioComoFechada`.
2. `SupplierServiceTest` (com `Clock` fixo):
   `deveListarSoLojasDentroDoHorario`,
   `deveIncluirLojaDaMadrugadaNaJanelaCertaDoHorario`,
   `deveTrazerAsForaDoHorarioDepoisEmAbertasPrimeiro`,
   `deveCalcularOpenNaVitrine`.
3. `ProductItemRepositoryTest`: `naoDeveListarProdutoDeLojaForaDoHorario`.
4. `SupplierControllerTest`: `naoDeveExistirRotaDeStatus` (o teste de 403
   no `/status` sai); `deveCalcularOOpenDaLoja`.
5. Os helpers de teste deixam de receber `open` e passam a controlar o
   estado pelo horário.

**Frontend**
6. `received-orders.component.spec.ts` e `order-history.component.spec.ts`:
   `deveMostrarOEstadoDaLojaSemInterruptor`, `deveMostrarOProximoHorario`,
   `naoDeveChamarARotaDeStatus`.
7. `supplier.service.spec.ts`: sai o teste de `openDistribuidor`.

**E2E (registrado no DoD)**: loja 08:00–18:00 visível de dia e ausente à
noite; loja 18:00–02:00 visível de madrugada; "Abertas primeiro" com o selo;
pedidos recebidos sem interruptor. Em 390 e 1280 px.

## 7. Fora de Escopo

- Horário por dia da semana (domingo fechado, sábado até 13h) — hoje há um
  par único de horários; exigiria tabela e tela novas.
- Bloquear pedido fora do horário no backend (`OrderService.createOrder`).
  **Risco conhecido**: um carrinho montado antes do fechamento ainda pode
  ser enviado. A HU20 mostra o aviso de loja fechada no carrinho.
- Fechamento extraordinário (feriado, imprevisto) — sem o interruptor, o
  fornecedor ajusta o horário em Configurações.
- Fuso por cidade.
- Apagar o componente morto `distributor-management`.

## 8. Definição de Pronto

- [ ] Loja aberta só dentro do horário, com a regra da meia-noite e loja
      sem horário fechada.
- [ ] `Clock` em Brasília no app e fixo nos testes; nenhum teste depende da
      hora da suíte.
- [ ] Interruptor e `PATCH /suppliers/{id}/status` removidos; migração
      `V16` aplicada no MySQL local.
- [ ] Swagger sem a rota de status e com `open` descrito como calculado.
- [ ] Testes da seção 6 verdes; `mvn -f backend/pom.xml test` e `ng test`
      sem falha nova; `ng build` ok.
- [ ] E2E em 390 e 1280 px registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
