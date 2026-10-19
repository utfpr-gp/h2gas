# HU07 — Atribuição de Pedidos e Módulo do Entregador

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU07 |
| Perfil | Fornecedor (SUPPLIER) e Entregador (DELIVERER) |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (entidade `Deliverer`, `Order.deliverer`/`dispatchedAt`/`deliveredAt`), F5 (PWA — módulo do entregador é mobile-first), HU05/HU06 (retirada sem endereço, painel do fornecedor, `ForbiddenResourceException`), F7 (Figuras 15 e 17) |
| Referência no TCC | Apêndice A.1 — HU07; Figura 17a/b/c (p. 42) |

## 2. História de Usuário

Como entregador, quero acessar uma interface móvel com os pedidos
atribuídos a mim e atualizar o status com um toque, para facilitar a
logística de campo.

## 3. Critérios de Aceitação (BDD)

> Revisado em 2026-10-18 sobre a `develop` com HU01–HU06 mergeadas.
> Decisões do autor: (a) entregador "Em rota" pode receber outro pedido
> (o badge só avisa); (b) o "Despachar" substitui o atalho "Enviar para
> entrega" nos pedidos de entrega e o backend recusa o atalho.
>
> **Segunda revisão (mesma data)**: a primeira entrega divergia da
> Figura 17 do TCC. Decisões do autor para alinhar: (c) seguir o fluxo do
> TCC — despachar só **atribui**; o entregador toca **Iniciar Entrega**
> para o pedido sair em rota; (d) **Cancelar Entrega** devolve o pedido ao
> fornecedor com motivo, sem cancelar o pedido do cliente; (e) o detalhe
> tem **mapa embutido** (iframe sem chave de API) e o link "Abrir no
> Maps"; (f) entram o **histórico** (17c) e a **barra de abas**
> Entregas/Histórico; ficam fora troco e aba Perfil.
>
> **Terceira revisão (2026-10-18)**: no teste manual o entregador
> confirmava a entrega pela lista sem nunca abrir o detalhe (mapa,
> contato). A Figura 17a só tem "Iniciar Entrega" nos cards; o confirmar
> é do detalhe (17b). Decisões do autor: (g) a lista **não confirma**
> entrega — o card EM ROTA só leva ao detalhe ("Continuar entrega");
> (h) ao **iniciar**, o entregador vai para o detalhe e **fica preso**
> nele até confirmar ou cancelar: lista, histórico e o detalhe de outro
> pedido redirecionam para a entrega em rota, e o detalhe em rota não tem
> "voltar"; (i) **uma entrega em rota por vez** — o TCC não proíbe
> simultâneas, mas trata "Em rota" como ocupado (Figura 15); o backend
> recusa iniciar outra enquanto houver uma em rota. Atribuir a quem está
> em rota continua permitido (decisão (a)). Se dados antigos tiverem
> várias em rota, a trava prende na de despacho mais antigo.

- **Cenário 1 — Encaminhamento**: Dado um pedido pronto, Quando o
  fornecedor aciona o despacho, Então permite atribuir a entrega a um
  entregador específico da frota (e trocar o entregador enquanto ele não
  iniciou a entrega).
- **Cenário 2 — Rota em campo**: Dado o acesso do entregador no módulo
  móvel, Quando visualiza suas tarefas, Então exibe apenas suas rotas
  (PENDENTE e EM ROTA), o detalhe com trajeto e contato do cliente, **e**
  disponibiliza botões para iniciar e confirmar a entrega — iniciar no
  card ou no detalhe, confirmar **só no detalhe**.
- **Cenário 2b — Entrega em andamento**: Dado um entregador com uma
  entrega em rota, Quando tenta abrir a lista, o histórico ou outro
  pedido, Então volta ao detalhe da entrega em rota até confirmá-la ou
  cancelá-la; E Quando tenta iniciar outra entrega, Então o backend
  recusa (400) pedindo que finalize a atual.
- **Cenário 3 — Recusa**: Dado um pedido atribuído a ele, Quando o
  entregador cancela a entrega informando o motivo, Então o pedido volta
  ao fornecedor (em preparo, sem entregador), que vê o motivo e pode
  despachar de novo; o cliente não é avisado de cancelamento.
- **Cenário 4 — Histórico**: Dado o entregador logado, Quando abre o
  Histórico, Então vê as entregas que concluiu e as que recusou, por dia,
  com a contagem de entregas de hoje.

## 4. Estado atual (achados)

- F3 criou a entidade `Deliverer` (`user`, `supplier`, `active`,
  `vehicleInfo`). `Order` já tem `deliverer`, `dispatchedAt` e
  `deliveredAt` (o "tempo médio de entrega" da HU09 depende de preenchê-los
  aqui; `dispatchedAt` passa a ser gravado no **início** da entrega).
- `UserTypeEnum.DELIVERER` existe e a V3 já libera o valor no banco. O
  login por senha serve para qualquer perfil (`ROLE_DELIVERER`); o
  `authGuard` é genérico por `route.data.roles`.
- **Como um entregador passa a existir**: o fornecedor cadastra a própria
  frota (par `User(DELIVERER)` + `Deliverer`). Não é auto-registro.
- O principal autenticado é o `User` (`SecurityContextHolder`); o id do
  fornecedor **é** o id do usuário (`Supplier` herda `User` com `JOINED`).
  Nunca receber `supplierId`/`delivererId` por URL/corpo quando o principal
  define o dono (CLAUDE.md).
- Atribuição checa o **dono do pedido** (outro fornecedor → 403; ADMIN
  pode) e só vale para `deliveryMode = DELIVERY`. Início, conclusão e
  recusa checam que o pedido está **atribuído ao entregador do token** e o
  **status** adequado.
- **"Pedido pronto" (Cenário 1)** = `IN_PREPARATION`; "aciona o despacho"
  = atribuir entregador (o pedido **continua** `IN_PREPARATION`).
- **Atalho legado**: o painel (HU06) tem "Enviar para entrega"
  (`PATCH /update-order-status` → `OUT_FOR_DELIVERY`) sem entregador. O
  backend recusa esse atalho para `DELIVERY` (400, "use o despacho com
  entregador"); retirada continua usando o atalho.
- **Divergências da primeira entrega em relação ao TCC (Figura 17)**:
  despacho já levava a `OUT_FOR_DELIVERY` (TCC: card PENDENTE + "Iniciar
  Entrega → ", e o botão "gatilho direto" do motoboy dispara as
  notificações ao cliente e ao fornecedor); não havia detalhe com mapa,
  "Cancelar Entrega", histórico nem abas.
- `OrderResponseDTO` hoje traz `delivererName`, `customerPhone` e o
  endereço sem a **cidade**; o detalhe precisa de `cityName` (mapa/Maps),
  `deliveredAt` e do último motivo de recusa para o painel do fornecedor.
- **Visual (F7 §5.8 → Figuras 15 e 17)**: modal "Atribuir a um
  Entregador" (15); módulo móvel com "Minhas Entregas" (17a), detalhe
  (17b: mapa, "Abrir no Maps", cliente com ligar/mensagem, resumo,
  "Confirmar Entrega" e "Cancelar Entrega") e histórico (17c: "Entregas
  hoje", grupos por dia, Entregue/Cancelado) com abas Entregas/Histórico.

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
    não persistido (pedido só atribuído, ainda não iniciado, não torna o
    entregador "em rota");
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
  - grava `deliverer` e **mantém `IN_PREPARATION`** (não grava
    `dispatchedAt`); chamar de novo com outro entregador **troca** o
    entregador (permitido enquanto `IN_PREPARATION`).
- `PATCH /api/orders/{id}/assign-deliverer` (`ADMIN`, `SUPPLIER`, `@Valid`)
  devolve o `OrderResponseDTO`. OpenAPI 200/400/403/404.
- `OrderService.updateOrderStatus`: `OUT_FOR_DELIVERY` em pedido
  `DELIVERY` → `BusinessRuleException` ("Use o despacho com entregador.").
- `OrderResponseDTO` ganha `delivererName`, `cityName`, `deliveredAt`,
  `lastRefusalReason` e `lastRefusalBy` (nome do entregador da recusa mais
  recente; só preenchidos enquanto o pedido está sem entregador após uma
  recusa), todos preenchidos no `OrderMapper`.

### 5.3 Backend — Cenário 2 (rota do entregador)

- O entregador **sempre** vem do principal (`findByUserId`), nunca de
  parâmetro.
- `GET /api/orders/my-routes` (`DELIVERER`): pedidos atribuídos a ele com
  status `IN_PREPARATION` (PENDENTE) ou `OUT_FOR_DELIVERY` (EM ROTA);
  em rota primeiro (do despacho mais antigo), depois pendentes (do mais
  antigo `createdAt`). Quem consome distingue pelo `status`.
- `PATCH /api/orders/{id}/start-delivery` (`DELIVERER`):
  - pedido inexistente → 404; não atribuído a este entregador → 403;
  - status ≠ `IN_PREPARATION` → 400;
  - entregador já tem outro pedido `OUT_FOR_DELIVERY` → 400 ("Finalize a
    entrega em rota antes de iniciar outra.") — terceira revisão (i);
  - grava `OUT_FOR_DELIVERY` e `dispatchedAt = now`.
- `PATCH /api/orders/{id}/complete-delivery` (`DELIVERER`):
  - 404; não atribuído a este entregador (ou sem entregador) → 403;
  - status ≠ `OUT_FOR_DELIVERY` → 400;
  - grava `DELIVERED` e `deliveredAt = now`.
- OpenAPI nas rotas (200/400/403/404, segurança Bearer).

### 5.4 Backend — Cenário 3 (recusa) e Cenário 4 (histórico)

- **Migração `V7__delivery_refusals.sql`**: tabela `delivery_refusals`
  (`id` BIGINT AI PK, `order_id` VARCHAR(36) FK `orders`, `deliverer_id`
  BIGINT FK `deliverers`, `reason` VARCHAR(500) NOT NULL, `created_at`
  DATETIME NOT NULL) e índice por `deliverer_id, created_at`. Entidade
  `DeliveryRefusal` + `DeliveryRefusalRepository`; `Order` ganha
  `@OneToMany refusals` (somente leitura, ordenada por `createdAt desc`).
- `PATCH /api/orders/{id}/cancel-delivery` (`DELIVERER`, `@Valid`
  `CancelDeliveryRequestDTO { @NotBlank @Size(max = 500) reason }`):
  - 404; não atribuído a este entregador → 403;
  - status fora de `IN_PREPARATION`/`OUT_FOR_DELIVERY` → 400;
  - grava um `DeliveryRefusal` (motivo sem espaços nas pontas), limpa
    `deliverer` e `dispatchedAt` e deixa o pedido em `IN_PREPARATION`;
  - **não** cancela o pedido nem mexe no estoque.
- `GET /api/orders/my-deliveries/history` (`DELIVERER`): devolve
  `DeliveryHistoryDTO { deliveredToday, items[] }`, itens do mais recente
  ao mais antigo, de dois tipos: `DELIVERED` (pedidos entregues por ele,
  data = `deliveredAt`) e `REFUSED` (recusas dele, data = `createdAt` da
  recusa). Cada item: `orderId`, `kind`, `occurredAt`, endereço (rua,
  número, bairro, cidade), resumo dos itens, `paymentMethod`,
  `totalPrice` (nulo em `REFUSED`). `deliveredToday` conta as `DELIVERED`
  do dia corrente.
- OpenAPI (200/400/403/404, Bearer) nas duas rotas.

### 5.5 Frontend

- `models/deliverer.ts`, `services/deliverer.service.ts` (`listMine`,
  `create`); `OrderService.assignDeliverer` / `myRoutes` /
  `startDelivery` / `completeDelivery` / `cancelDelivery` /
  `deliveryHistory`; `OrderResponse` com os campos novos do 5.2; model do
  histórico.
- **Fornecedor**
  - `manage-deliverers` (rota `/entregadores`, só `SUPPLIER`): formulário
    de cadastro e lista da frota com badge Disponível / Em rota.
  - `received-orders`: em pedido `IN_PREPARATION` de entrega **sem
    entregador** o botão é **"Despachar"** (modal da Figura 15). Com
    entregador atribuído e ainda em preparo, o detalhe mostra "Aguardando
    <nome> iniciar" e o botão vira **"Trocar entregador"** (mesmo modal).
    Se houve recusa, mostra "Recusado por <nome>: <motivo>" e volta a
    oferecer "Despachar". Retirada segue com "Pronto para retirada".
- **Entregador** (mobile-first, tokens F2, `rounded-xl`, alto contraste):
  - login redireciona `DELIVERER` para `/entregas`; o `menu` mostra só a
    marca e "Sair"; **barra de abas** fixa no rodapé: Entregas |
    Histórico;
  - `deliverer-routes` (`/entregas`, Figura 17a): contagem de pendentes e
    um card por pedido (bairro/endereço, itens, pagamento). Card
    PENDENTE tem **"Iniciar Entrega →"**, que inicia e **leva ao
    detalhe**; card EM ROTA tem **"Continuar entrega →"**, que só abre o
    detalhe (a lista não confirma); tocar no card abre o detalhe;
    atualização a cada 30 s;
  - `deliverer-route-detail` (`/entregas/:id`, Figura 17b): **mapa
    embutido** (iframe `maps.google.com/maps?q=<endereço>&output=embed`,
    sanitizado), endereço completo com **"Abrir no Maps"**
    (`google.com/maps/dir/?api=1&destination=`), cliente com Ligar (`tel:`)
    e WhatsApp (`wa.me`), itens, pagamento, total e os botões **Iniciar
    Entrega** ou **Confirmar Entrega** (conforme o status) e **Cancelar
    Entrega** (modal com motivo obrigatório, padrão do modal da HU06);
    com a entrega em rota o "voltar" some (a saída é confirmar ou
    cancelar);
  - `activeDeliveryGuard` (terceira revisão (h)) em `/entregas`,
    `/entregas/historico` e `/entregas/:id`: para `DELIVERER`, busca
    `myRoutes()`; havendo pedido "Saiu para entrega" (o backend ordena o
    de despacho mais antigo primeiro), redireciona para
    `/entregas/<id>` dele, exceto se já for esse o destino. `ADMIN` não é
    preso; erro ao buscar não bloqueia a navegação (a tela mostra o
    próprio erro);
  - `deliverer-history` (`/entregas/historico`, Figura 17c): "Entregas
    hoje N", grupos por dia, selo Entregue/Cancelado, endereço, itens,
    pagamento e valor ("—" quando recusada).
  - rotas com `data.roles: ['DELIVERER', 'ADMIN']`.

## 6. Plano de Testes (TDD)

**Backend:**
1. `DelivererServiceTest` — `deveCriarEntregadorVinculadoAoFornecedor`,
   `deveRejeitarEmailJaCadastrado`,
   `deveMarcarEntregadorComoOcupadoQuandoHaPedidoEmRota`,
   `deveListarApenasEntregadoresDoFornecedor`,
   `naoDeveMarcarOcupadoQuandoPedidoSoEstaAtribuido`.
2. `OrderServiceTest` — despacho: `deveAtribuirEntregadorSemAvancarStatus`,
   `devePermitirTrocarEntregadorEnquantoEmPreparacao`,
   `deveRejeitarDespachoForaDeEmPreparacao`,
   `deveRejeitarDespachoDePedidoDeRetirada`,
   `deveRejeitarDespachoDePedidoDeOutroFornecedor` (403),
   `deveRejeitarEntregadorDeOutroFornecedor`,
   `deveRecusarAtalhoDeEnvioParaPedidoDeEntrega`;
   início: `deveIniciarEntregaEGravarDispatchedAt`,
   `deveRejeitarInicioDePedidoDeOutroEntregador` (403),
   `deveRejeitarInicioForaDeEmPreparacao`,
   `deveRejeitarInicioComOutraEntregaEmRota`,
   `devePermitirIniciarNovaEntregaDepoisDeConcluirAAnterior`,
   `devePermitirIniciarNovaEntregaDepoisDeCancelarAAnterior`,
   `devePermitirOutroEntregadorIniciarComColegaEmRota`,
   `deveAtribuirPedidoAEntregadorEmRotaSemIniciar`; conclusão:
   `deveConcluirEntregaEGravarDeliveredAt`,
   `deveRejeitarConclusaoDePedidoDeOutroEntregador` (403),
   `deveRejeitarConclusaoForaDeOutForDelivery`; recusa:
   `deveCancelarEntregaDevolvendoPedidoAoFornecedor`,
   `deveRejeitarCancelamentoDeEntregaDeOutroEntregador` (403),
   `deveRejeitarCancelamentoDePedidoJaEntregue`,
   `deveExporUltimaRecusaNoPedido`; listas:
   `deveListarRotasPendentesEEmRotaApenasDoEntregador`,
   `deveMontarHistoricoComEntreguesERecusadas`,
   `deveContarEntregasDeHoje`.
3. `DelivererControllerTest` — `deveRetornar400ComDadosInvalidos`,
   `deveRetornar200AoCadastrarEntregador`.
4. `OrderControllerTest` — `deveRetornar200AoDespacharComEntregador`,
   `deveRetornarRotasDoEntregadorAutenticado`,
   `deveRetornar200AoIniciarEntrega`,
   `deveRetornar400AoIniciarComOutraEntregaEmRota`,
   `deveRetornar200AoConfirmarEntrega`,
   `deveRetornar400AoCancelarEntregaSemMotivo`,
   `deveRetornar200AoCancelarEntregaComMotivo`,
   `deveRetornarHistoricoDoEntregador`.

**Frontend:**
5. `deliverer.service.spec.ts` — `deveListarFrotaDoFornecedor`,
   `deveCadastrarEntregador`; `order.service.spec.ts` —
   `deveDespacharPedido`, `deveBuscarMinhasRotas`, `deveIniciarEntrega`,
   `deveConfirmarEntrega`, `deveCancelarEntregaComMotivo`,
   `deveBuscarHistoricoDeEntregas`.
6. `manage-deliverers.component.spec.ts` —
   `deveListarEntregadoresComStatus`,
   `deveCadastrarEntregadorEAtualizarLista`.
7. `received-orders.component.spec.ts` —
   `deveExibirDespacharApenasParaEntregaEmPreparo`,
   `deveManterAtalhoDeRetiradaSemEntregador`,
   `deveAtribuirEntregadorSelecionadoNoModal`,
   `deveAvisarQuandoNaoHaEntregadores`,
   `deveOferecerTrocarEntregadorQuandoJaAtribuido`,
   `deveExibirMotivoDaUltimaRecusa`.
8. `deliverer-routes.component.spec.ts` —
   `deveListarRotasDoEntregadorLogado`,
   `deveMostrarEstadoVazioSemRotas`,
   `deveAtualizarRotasPeriodicamente`,
   `deveIniciarEntregaDoCardPendenteEAbrirDetalhe`,
   `deveAbrirDetalheDoCardEmRotaSemConfirmar` (substitui
   `deveConfirmarEntregaDoCardEmRota`, terceira revisão (g)),
   `deveAvisarENaoAbrirDetalheQuandoBackendRecusarInicio`.
9. `deliverer-route-detail.component.spec.ts` —
   `deveExibirEnderecoEContatoDoCliente`,
   `deveMontarMapaELinkAbrirNoMaps`,
   `deveIniciarEPoisConfirmarEntrega`,
   `naoDeveCancelarEntregaSemMotivo`,
   `deveCancelarEntregaComMotivoEVoltarParaLista`,
   `naoDeveExibirVoltarComEntregaEmRota`,
   `deveManterPendenteComVoltarQuandoBackendRecusarInicio`,
   `deveCarregarOutraEntregaQuandoOParametroMudar` (o guard pode levar de
   uma entrega a outra reaproveitando o componente).
9b. `active-delivery.guard.spec.ts` —
   `deveRedirecionarParaEntregaEmRota`,
   `devePermitirDetalheDaEntregaEmRota`,
   `deveRedirecionarDetalheDeOutroPedidoParaEntregaEmRota`,
   `devePermitirNavegarSemEntregaEmRota`,
   `naoDevePrenderAdministrador`,
   `naoDeveBloquearQuandoFalharAoBuscarRotas`.
9c. `deliverer-flow.spec.ts` (rotas e guard reais) —
   `deveIrAoDetalheAoIniciarEFicarPresoNele`,
   `deveVoltarParaListaAoConfirmarSemIniciarAProximaSozinho`,
   `deveIrDiretoParaOutraEntregaJaEmRotaAoConfirmar`,
   `deveLiberarListaEHistoricoSemEntregaEmRota`.
10. `deliverer-history.component.spec.ts` —
    `deveExibirEntregasDeHojeEGruposPorDia`,
    `deveMarcarEntregaRecusadaComoCancelada`.
11. `menu.component.spec.ts` — `deveExibirAbasEntregasEHistoricoParaEntregador`;
    `login.component.spec.ts` — `deveRedirecionarEntregadorParaEntregas`.

Regra herdada: specs antigos falhando no frontend não aumentam (22).

## 7. Fora de Escopo

- Notificação em tempo real (SSE) ao cliente/fornecedor — HU08; aqui só
  as transições de status e os horários (o fornecedor vê a recusa no
  painel por polling de 30 s).
- Aba **Perfil** da barra inferior.
- GPS em tempo real e traçado de rota calculado por nós; o "trajeto" é o
  do Google Maps (iframe/link), sem chave de API.
- Troco: `Order.changeFor` existe, mas o checkout não coleta o valor; a
  linha "Receber R$ x e dar R$ y de troco" fica para quando o checkout
  capturar.
- Selo "Cortesia / sem taxa extra" e "Agendado: hh:mm" da Figura 17.
- Cancelamento do pedido do cliente pelo entregador (a recusa devolve ao
  fornecedor; cancelar o pedido é do fornecedor, HU06).
- Busca por nome no modal da Figura 15 (frota pequena).
- Edição, desativação e exclusão de entregador; cadastro de entregador
  por ADMIN.
- Checagem de dono no `update-order-status` genérico para os demais
  status (dívida das rotas antigas) e o `AccessDenied` devolvendo 500 em
  vez de 403 no `GlobalExceptionHandler` (pré-existente).

## 8. Definição de Pronto

- [x] Fornecedor cadastra entregadores da própria frota
      (`POST /api/deliverers`) e vê quem está disponível ou em rota
      (`GET /api/deliverers/mine`).
- [x] Despacho atribui (e permite trocar) entregador em pedido de entrega
      `IN_PREPARATION` sem mudar o status; 403 para pedido de outro
      fornecedor; retirada não tem entregador; atalho "Enviar para
      entrega" recusado para pedidos de entrega.
- [x] Entregador vê apenas as próprias rotas (identificado pelo token),
      inicia a entrega (grava `dispatchedAt`, vai a `OUT_FOR_DELIVERY`) e
      a confirma (grava `deliveredAt`, vai a `DELIVERED`).
- [x] Entregador cancela a entrega com motivo; o pedido volta ao
      fornecedor, que vê o motivo e redespacha; nada é cancelado ao
      cliente.
- [x] Histórico do entregador lista entregues e recusadas, por dia, com
      "Entregas hoje".
- [x] Rotas novas documentadas no Swagger; migração `V7` aplicada.
- [x] Terceira revisão: lista sem confirmar (card EM ROTA abre o
      detalhe); iniciar leva ao detalhe; entregador preso ao detalhe da
      entrega em rota (`activeDeliveryGuard`, sem "voltar"); backend
      recusa iniciar uma segunda entrega em rota (400, Swagger
      atualizado); testes da seção 6 verdes.
- [x] Telas no padrão F7 (Figuras 15 e 17), mobile-first, com mapa
      embutido e "Abrir no Maps" no detalhe e barra de abas.
- [x] Testes da seção 6 verdes; `mvn test` e `ng test` (≤ 22 falhas
      antigas).
- [x] Commits pequenos por fatia (CLAUDE.md, seção Git).
- [x] Testado de ponta a ponta (API e interface, com entregador de teste
      no banco local).

> Verificado em 2026-10-18: backend 166 testes (o único erro é `BackendApplicationTests.contextLoads`,
> que precisa do MySQL local e já falhava fora do Docker); frontend 192 testes com as 22 falhas antigas.
>
> Terceira revisão verificada em 2026-10-18: backend 172 testes (só o
> `contextLoads` antigo falha fora do Docker); frontend 207 testes com as
> 22 falhas antigas; aprovada pelo autor após teste manual com o
> entregador Carlos (5 entregas pendentes no banco local).
