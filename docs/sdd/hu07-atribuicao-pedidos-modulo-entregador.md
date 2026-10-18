# HU07 — Atribuição de Pedidos e Módulo do Entregador

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU07 |
| Perfil | Fornecedor (SUPPLIER) e Entregador (DELIVERER) |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (entidade `Deliverer`, `Order.deliverer`/`dispatchedAt`/`deliveredAt`), F5 (PWA — módulo do entregador é mobile-first), HU05/HU06 (retirada sem endereço, painel do fornecedor, `ForbiddenResourceException`), F7 (Figuras 15 e 17) |
| Referência no TCC | Apêndice A.1 — HU07 |

## 2. História de Usuário

Como entregador, quero acessar uma interface móvel com os pedidos
atribuídos a mim e atualizar o status com um toque, para facilitar a
logística de campo.

## 3. Critérios de Aceitação (BDD)

> Revisado em 2026-10-18 sobre a `develop` com HU01–HU06 mergeadas.
> Decisões do autor nesta revisão: (a) entregador "Em rota" pode receber
> outro pedido (o badge só avisa); (b) o "Despachar" substitui o atalho
> "Enviar para entrega" nos pedidos de entrega e o backend recusa o
> atalho; (c) o módulo móvel entrega as Figuras 17a (lista) e 17b
> (detalhe).

- **Cenário 1 — Encaminhamento**: Dado um pedido pronto, Quando o
  fornecedor aciona o despacho, Então permite atribuir a entrega a um
  entregador específico da frota.
- **Cenário 2 — Rota em campo**: Dado o acesso do entregador no módulo
  móvel, Quando visualiza suas tarefas, Então exibe apenas suas rotas
  **e** disponibiliza botões de atalho para confirmar a entrega.

## 4. Estado atual (achados)

- F3 criou a entidade `Deliverer` (`user`, `supplier`, `active`,
  `vehicleInfo`) e `DelivererRepository` vazio. **Não há service,
  controller, DTO nem tela** — greenfield. `Order` já tem `deliverer`,
  `dispatchedAt` e `deliveredAt`, hoje nunca preenchidos (o "tempo médio
  de entrega" da HU09 depende de preenchê-los aqui).
- `UserTypeEnum.DELIVERER` existe e a V3 já libera o valor no banco. O
  login por senha serve para qualquer perfil (`ROLE_DELIVERER`); o
  `authGuard` é genérico por `route.data.roles`. O `login.component`
  navega sempre para `/inicio`, então o entregador precisa de redirecionamento
  próprio, e o `menu` só tem opções para ADMIN/SUPPLIER/CUSTOMER.
- **Como um entregador passa a existir**: o fornecedor cadastra a própria
  frota (par `User(DELIVERER)` + `Deliverer`). Não é auto-registro.
- **Correções ao rascunho anterior** (que usava `auth.getName()` e
  `findEntityByName`, e é de antes da HU06):
  - o principal autenticado já é o `User` (`SecurityContextHolder`); o id
    do fornecedor **é** o id do usuário (`Supplier` herda `User` com
    `JOINED`). Nunca receber `supplierId` por URL/corpo (CLAUDE.md): a
    listagem da frota é `GET /api/deliverers/mine`;
  - a atribuição precisa checar o **dono do pedido** (o rascunho só checava
    o dono do entregador): outro fornecedor → 403
    (`ForbiddenResourceException`); ADMIN pode;
  - a conclusão precisa checar **status** `OUT_FOR_DELIVERY` (o rascunho
    permitiria reconcluir pedido entregue) e tratar pedido sem entregador
    (o rascunho daria NPE);
  - atribuição só vale para `deliveryMode = DELIVERY`; pedido de retirada
    (HU05) não tem entregador e segue o fluxo "Pronto para retirada".
- **"Pedido pronto" (Cenário 1)** = `IN_PREPARATION`; "aciona o despacho"
  = atribuir entregador, o que leva a `OUT_FOR_DELIVERY` e grava
  `dispatchedAt`.
- **Atalho legado**: o painel (HU06) tem "Enviar para entrega"
  (`PATCH /update-order-status` → `OUT_FOR_DELIVERY`) sem entregador.
  Para pedido de entrega isso deixaria um pedido "em rota" que não
  aparece na rota de ninguém. Decisão do autor: o backend passa a recusar
  esse atalho para `DELIVERY` (400, "use o despacho com entregador") e o
  painel mostra "Despachar". Retirada continua usando o atalho.
- `OrderResponseDTO` não traz o entregador nem os horários; o detalhe do
  fornecedor e o do entregador precisam de `delivererName`, e o do
  entregador de `paymentMethod` (já existe) e `customerPhone` (HU06).
- **Visual (F7 §5.8 → Figuras 15 e 17)**: modal "Atribuir a um
  Entregador" (15) com lista de entregadores, badge DISPONÍVEL / EM ROTA,
  e "Confirmar Atribuição"; módulo móvel "Minhas Entregas" (17a) em cards
  e detalhe da entrega (17b) com endereço, resumo, ligar/WhatsApp, "Abrir
  no Maps" e "Confirmar Entrega".

## 5. Contrato Técnico

### 5.1 Backend — frota do fornecedor

- `DelivererRepository`: `Optional<Deliverer> findByUserId(String)`,
  `List<Deliverer> findBySupplierIdOrderByUserNameAsc(String)`.
- `DelivererCreateDTO` (`@NotBlank name`, `@NotBlank @Email email`,
  `@NotBlank phoneNumber`, `@NotBlank @Size(min = 6) password`,
  `vehicleInfo` opcional) e `DelivererDTO` (`id`, `name`, `email`,
  `phoneNumber`, `vehicleInfo`, `active`, `busy`), ambos com `@Schema`.
- `DelivererService` (injeção por construtor, `@Transactional`):
  - `createDeliverer(dto, supplier)`: e-mail já usado → `BusinessRuleException`
    (400); senha com `BCryptPasswordEncoder` (padrão do `UserService`);
    cria `User(DELIVERER)` + `Deliverer` ativo vinculado ao fornecedor;
  - `findMine(supplierId)`: lista com `busy` = existe pedido
    `OUT_FOR_DELIVERY` com esse entregador
    (`OrderRepository.existsByDelivererIdAndStatus`); `busy` é computado,
    não persistido;
  - `findEntityById(id)` (404) e `findByUserId(userId)` (404 se o usuário
    não for entregador).
- `DelivererController` (`/api/deliverers`, `@Tag("Entregadores")`):
  - `POST` `@RolesAllowed("SUPPLIER")`, `@Valid` — o fornecedor é o
    principal; OpenAPI 200/400/403;
  - `GET /mine` `@RolesAllowed("SUPPLIER")` — OpenAPI 200/403.

### 5.2 Backend — Cenário 1 (despacho pelo fornecedor)

- `AssignDelivererRequestDTO { @NotNull Long delivererId }`.
- `OrderService.assignDeliverer(orderId, delivererId, actor)`
  (`@Transactional`):
  - pedido inexistente → 404; fornecedor que não é o do pedido → 403
    (ADMIN pode);
  - status ≠ `IN_PREPARATION` → 400; `deliveryMode` ≠ `DELIVERY` → 400;
  - entregador inexistente → 404; de outro fornecedor ou inativo → 400;
  - grava `deliverer`, `OUT_FOR_DELIVERY` e `dispatchedAt = now`.
- `PATCH /api/orders/{id}/assign-deliverer` (`ADMIN`, `SUPPLIER`, `@Valid`)
  devolve o `OrderResponseDTO`. OpenAPI 200/400/403/404.
- `OrderService.updateOrderStatus`: `OUT_FOR_DELIVERY` em pedido
  `DELIVERY` → `BusinessRuleException` ("Use o despacho com entregador.").
- `OrderResponseDTO` ganha `delivererName` (preenchido no `OrderMapper`).

### 5.3 Backend — Cenário 2 (rota do entregador)

- `OrderRepository`: `existsByDelivererIdAndStatus`,
  `findByDelivererIdAndStatusOrderByDispatchedAtAsc`.
- `GET /api/orders/my-routes` (`DELIVERER`): o entregador vem do
  **principal** (`findByUserId`), nunca de parâmetro; devolve os pedidos
  `OUT_FOR_DELIVERY` dele, do despacho mais antigo ao mais novo.
- `PATCH /api/orders/{id}/complete-delivery` (`DELIVERER`):
  - pedido inexistente → 404; não atribuído a este entregador (ou sem
    entregador) → 403;
  - status ≠ `OUT_FOR_DELIVERY` → 400;
  - grava `DELIVERED` e `deliveredAt = now`.
- OpenAPI nas duas (200/400/403/404, segurança Bearer).

### 5.4 Frontend

- `models/deliverer.ts`, `services/deliverer.service.ts` (`listMine`,
  `create`) e `OrderService.assignDeliverer` / `myRoutes` /
  `completeDelivery`; `OrderResponse.delivererName?`.
- **Fornecedor**
  - `manage-deliverers` (rota `/entregadores`, só `SUPPLIER`): formulário
    de cadastro (nome, e-mail, telefone, senha, veículo) e lista da frota
    com badge Disponível / Em rota; link "Entregadores" no menu.
  - `received-orders`: em pedido `IN_PREPARATION` de entrega o botão
    vira **"Despachar"** e abre o modal da Figura 15 — lista com badge
    DISPONÍVEL / EM ROTA (em rota continua selecionável), "Confirmar
    atribuição"; frota vazia mostra atalho para cadastrar. Retirada segue
    com "Pronto para retirada". O detalhe mostra "Entregador: …".
- **Entregador** (mobile-first, tokens F2, `rounded-xl`, alto contraste):
  - login redireciona `DELIVERER` para `/entregas`; o `menu` do entregador
    mostra só a marca e "Sair";
  - `deliverer-routes` (`/entregas`, Figura 17a): "Minhas Entregas" com a
    contagem de pendentes, um card por pedido (bairro/endereço, itens,
    pagamento) e atualização a cada 30 s;
  - `deliverer-route-detail` (`/entregas/:id`, Figura 17b): endereço
    completo com link "Abrir no Maps" (URL de busca com o endereço, sem
    mapa embutido), cliente com Ligar (`tel:`) e WhatsApp (`wa.me`),
    itens, forma de pagamento, total e **Confirmar Entrega** (um toque,
    com confirmação visual e volta para a lista).
  - rotas com `data.roles: ['DELIVERER']` (e `ADMIN` para inspeção).

## 6. Plano de Testes (TDD)

**Backend:**
1. `DelivererServiceTest` — `deveCriarEntregadorVinculadoAoFornecedor`,
   `deveRejeitarEmailJaCadastrado`,
   `deveMarcarEntregadorComoOcupadoQuandoHaPedidoEmRota`,
   `deveListarApenasEntregadoresDoFornecedor`.
2. `OrderServiceTest` — `deveDespacharEntregadorEAvancarParaOutForDelivery`
   (grava `dispatchedAt`), `deveRejeitarDespachoForaDeEmPreparacao`,
   `deveRejeitarDespachoDePedidoDeRetirada`,
   `deveRejeitarDespachoDePedidoDeOutroFornecedor` (403),
   `deveRejeitarEntregadorDeOutroFornecedor`,
   `devePermitirDespacharEntregadorJaEmRota`,
   `deveRecusarAtalhoDeEnvioParaPedidoDeEntrega`,
   `deveConcluirEntregaEGravarDeliveredAt`,
   `deveRejeitarConclusaoDePedidoDeOutroEntregador` (403),
   `deveRejeitarConclusaoForaDeOutForDelivery`,
   `deveListarApenasRotasDoEntregador`.
3. `DelivererControllerTest` — `deveRetornar400ComDadosInvalidos`,
   `deveRetornar200AoCadastrarEntregador`.
4. `OrderControllerTest` — `deveRetornar200AoDespacharComEntregador`,
   `deveRetornarRotasDoEntregadorAutenticado`,
   `deveRetornar200AoConfirmarEntrega`.

**Frontend:**
5. `deliverer.service.spec.ts` — `deveListarFrotaDoFornecedor`,
   `deveCadastrarEntregador`; `order.service.spec.ts` —
   `deveDespacharPedido`, `deveBuscarMinhasRotas`,
   `deveConfirmarEntrega`.
6. `manage-deliverers.component.spec.ts` —
   `deveListarEntregadoresComStatus`,
   `deveCadastrarEntregadorEAtualizarLista`.
7. `received-orders.component.spec.ts` —
   `deveExibirDespacharApenasParaEntregaEmPreparo`,
   `deveManterAtalhoDeRetiradaSemEntregador`,
   `deveAtribuirEntregadorSelecionadoNoModal`,
   `deveAvisarQuandoNaoHaEntregadores`.
8. `deliverer-routes.component.spec.ts` —
   `deveListarRotasDoEntregadorLogado`,
   `deveAtualizarRotasPeriodicamente`;
   `deliverer-route-detail.component.spec.ts` —
   `deveExibirEnderecoEContatoDoCliente`,
   `deveConfirmarEntregaComUmToque`.
9. `login.component.spec.ts` — `deveRedirecionarEntregadorParaEntregas`.

Regra herdada: specs antigos falhando no frontend não aumentam (22).

## 7. Fora de Escopo

- Notificação em tempo real (SSE) ao cliente quando despachado/entregue —
  HU08; aqui só a transição de status e os horários.
- Histórico de entregas do entregador (Figura 17c) e aba Perfil.
- Mapa embutido e GPS em tempo real (Won't have no TCC); só link para o
  Maps.
- "Cancelar Entrega" pelo entregador (botão da Figura 17b) e "Iniciar
  Entrega" (o despacho já leva a `OUT_FOR_DELIVERY`).
- Troco: `Order.changeFor` existe, mas o checkout não coleta o valor; a
  linha "Receber R$ x e dar R$ y de troco" fica para quando o checkout
  capturar.
- Busca por nome no modal da Figura 15 (frota pequena).
- Edição, desativação e exclusão de entregador; cadastro de entregador
  por ADMIN.
- Checagem de dono no `update-order-status` genérico para os demais
  status (dívida das rotas antigas).

## 8. Definição de Pronto

- [ ] Fornecedor cadastra entregadores da própria frota
      (`POST /api/deliverers`) e vê quem está disponível ou em rota
      (`GET /api/deliverers/mine`).
- [ ] Despacho atribui entregador a pedido de entrega `IN_PREPARATION`,
      leva a `OUT_FOR_DELIVERY` e grava `dispatchedAt`; 403 para pedido de
      outro fornecedor; retirada não tem entregador; atalho "Enviar para
      entrega" recusado para pedidos de entrega.
- [ ] Entregador vê apenas as próprias rotas (identificado pelo token) e
      confirma a entrega com um toque, gravando `deliveredAt` e indo a
      `DELIVERED`.
- [ ] Rotas novas documentadas no Swagger.
- [ ] Telas no padrão F7 (Figuras 15 e 17), mobile-first no módulo do
      entregador.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` (≤ 22 falhas
      antigas).
- [ ] Commits pequenos por fatia (CLAUDE.md, seção Git).
- [ ] Testado de ponta a ponta (API e interface, com entregador de teste
      no banco local).
