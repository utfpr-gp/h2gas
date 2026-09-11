# HU07 — Atribuição de Pedidos e Módulo do Entregador

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU07 |
| Perfil | Fornecedor (SUPPLIER) e Entregador (DELIVERER) |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (entidade `Deliverer`, `Order.deliverer`/`dispatchedAt`/`deliveredAt`), F5 (PWA — módulo do entregador é mobile-first) |
| Referência no TCC | Apêndice A.1 — HU07 |

## 2. História de Usuário

Como entregador, quero acessar uma interface móvel com os pedidos
atribuídos a mim e atualizar o status com um toque, para facilitar a
logística de campo.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Encaminhamento**: Dado um pedido pronto, Quando o
  fornecedor aciona o despacho, Então permite atribuir a entrega a um
  entregador específico da frota.
- **Cenário 2 — Rota em campo**: Dado o acesso do entregador no módulo
  móvel, Quando visualiza suas tarefas, Então exibe apenas suas rotas
  **e** disponibiliza botões de atalho para confirmar a entrega.

## 4. Estado atual (achados)

- F3 criou a entidade `Deliverer` e o repositório, mas **nenhuma camada
  de serviço/controller existe ainda** — nem cadastro de entregador, nem
  atribuição, nem consulta de rotas. É greenfield quase completo.
- **Como um `Deliverer` passa a existir?** O TCC não tem uma HU dedicada
  a "cadastro de entregador" — mas o Cenário 1 fala em "entregador
  específico da frota", implicando que o fornecedor gerencia sua própria
  frota. Modelo adotado: **o fornecedor cadastra seus entregadores**
  (análogo a cadastrar produtos), criando o par `User(DELIVERER)` +
  `Deliverer` vinculado ao seu `supplierId` — não é auto-registro público
  como cliente/fornecedor.
- **"Pedido pronto" (Cenário 1)**: interpretado como status
  `IN_PREPARATION` — é o estado que antecede logicamente o despacho no
  fluxo já existente (`PENDING → CONFIRMED → IN_PREPARATION →
  OUT_FOR_DELIVERY → DELIVERED`). "Aciona o despacho" = atribuir
  entregador, o que avança o status para `OUT_FOR_DELIVERY`.
- **Fecha duas pontas soltas do F3**: `Order.dispatchedAt` (nunca
  preenchido até agora) passa a ser setado no momento da atribuição;
  `Order.deliveredAt` passa a ser setado quando o entregador confirma a
  entrega. Isso é o que finalmente alimenta o "tempo médio de entrega" da
  HU09, que até aqui sempre retornava nulo.
- O guard de rotas (`authGuard`) já é genérico por `route.data.roles` +
  `user.userType` — adicionar `'DELIVERER'` como valor válido não exige
  mudança no guard, só nas rotas novas.
- Padrão de senha já estabelecido em `UserService`:
  `new BCryptPasswordEncoder().encode(...)` — reaproveitado aqui.

## 5. Contrato Técnico

### 5.1 Backend — Cadastro de entregador (pré-requisito dos 2 cenários)

**Novo DTO `DelivererCreateDTO { String name; String email; String phoneNumber; String password; String vehicleInfo; }`**.

**Novo DTO `DelivererDTO { Long id; String name; String vehicleInfo; boolean active; boolean busy; }`**
(`busy` é computado, não persistido — ver §5.2.)

**`DelivererService.java`** (novo):
```java
public DelivererDTO createDeliverer(DelivererCreateDTO dto, String supplierId) {
    User user = new User(dto.getName(), dto.getEmail(), dto.getPhoneNumber(),
            null, new BCryptPasswordEncoder().encode(dto.getPassword()), UserTypeEnum.DELIVERER);
    userRepository.save(user);

    Deliverer deliverer = new Deliverer();
    deliverer.setUser(user);
    deliverer.setSupplier(supplierService.findEntityById(supplierId));
    deliverer.setActive(true);
    deliverer.setVehicleInfo(dto.getVehicleInfo());
    delivererRepository.save(deliverer);

    return toDto(deliverer);
}
```

**`DelivererController.java`** (novo):
```java
@PostMapping
@RolesAllowed({"ADMIN", "SUPPLIER"})
public ResponseEntity<DelivererDTO> create(@RequestBody DelivererCreateDTO dto, Authentication auth) {
    String supplierId = supplierService.findEntityByName(auth.getName()).getId();
    return ResponseEntity.ok(delivererService.createDeliverer(dto, supplierId));
}

@GetMapping("/by-supplier/{supplierId}")
@RolesAllowed({"ADMIN", "SUPPLIER"})
public ResponseEntity<List<DelivererDTO>> listBySupplier(@PathVariable String supplierId) {
    return ResponseEntity.ok(delivererService.findBySupplier(supplierId));
}
```

### 5.2 Backend — Cenário 1 (atribuição pelo fornecedor)

**`DelivererRepository.java`** — nova query de disponibilidade:
```java
boolean existsByIdAndOrders_Status(Long delivererId, OrderStatusEnum status);
```
(ou, mais simples e explícito via `@Query` no `OrderRepository`:)
```java
boolean existsByDelivererIdAndStatus(Long delivererId, OrderStatusEnum status);
```
`busy = orderRepository.existsByDelivererIdAndStatus(deliverer.getId(), OrderStatusEnum.OUT_FOR_DELIVERY)`.

**`OrderService.java`** — novo método:
```java
public void assignDeliverer(String orderId, Long delivererId, String supplierId) {
    Order order = findEntityById(orderId);
    if (order.getStatus() != OrderStatusEnum.IN_PREPARATION) {
        throw new BusinessRuleException("Só é possível atribuir entregador a pedidos em preparação.");
    }

    Deliverer deliverer = delivererService.findEntityById(delivererId);
    if (!deliverer.getSupplier().getId().equals(supplierId)) {
        throw new BusinessRuleException("Entregador não pertence a este fornecedor.");
    }

    order.setDeliverer(deliverer);
    order.setStatus(OrderStatusEnum.OUT_FOR_DELIVERY);
    order.setDispatchedAt(LocalDateTime.now());
    orderRepository.save(order);
}
```

**`OrderController.java`** — novo endpoint:
```java
@PatchMapping("/{id}/assign-deliverer")
@RolesAllowed({"ADMIN", "SUPPLIER"})
public ResponseEntity<Map<String, String>> assignDeliverer(
        @PathVariable String id,
        @RequestBody Map<String, Long> body,
        Authentication auth) {
    String supplierId = supplierService.findEntityByName(auth.getName()).getId();
    orderService.assignDeliverer(id, body.get("delivererId"), supplierId);
    return ResponseEntity.ok(Map.of("status", "OUT_FOR_DELIVERY"));
}
```

### 5.3 Backend — Cenário 2 (rota do entregador em campo)

**`DelivererRepository.java`** — `Optional<Deliverer> findByUserId(String userId);`
(mapeia o usuário autenticado para seu perfil de entregador.)

**`OrderRepository.java`** — `List<Order> findByDelivererIdAndStatusOrderByDispatchedAtAsc(Long delivererId, OrderStatusEnum status);`

**`OrderController.java`** — novos endpoints, escopados ao próprio entregador autenticado:
```java
@GetMapping("/my-routes")
@RolesAllowed({"DELIVERER"})
public ResponseEntity<List<OrderResponseDTO>> myRoutes(Authentication auth) {
    Deliverer deliverer = delivererService.findByUserId(auth.getName());
    return ResponseEntity.ok(orderService.findActiveByDeliverer(deliverer.getId()));
}

@PatchMapping("/{id}/complete-delivery")
@RolesAllowed({"DELIVERER"})
public ResponseEntity<Map<String, String>> completeDelivery(@PathVariable String id, Authentication auth) {
    Deliverer deliverer = delivererService.findByUserId(auth.getName());
    orderService.completeDelivery(id, deliverer.getId());
    return ResponseEntity.ok(Map.of("status", "DELIVERED"));
}
```

**`OrderService.completeDelivery`**:
```java
public void completeDelivery(String orderId, Long delivererId) {
    Order order = findEntityById(orderId);
    if (!order.getDeliverer().getId().equals(delivererId)) {
        throw new BusinessRuleException("Pedido não está atribuído a este entregador.");
    }
    order.setStatus(OrderStatusEnum.DELIVERED);
    order.setDeliveredAt(LocalDateTime.now());
    orderRepository.save(order);
}
```
(`findActiveByDeliverer` reaproveita `findByDelivererIdAndStatusOrderByDispatchedAtAsc` com status `OUT_FOR_DELIVERY` — "exibe apenas suas rotas" do Cenário 2 é garantido pelo filtro `delivererId` vindo do próprio token autenticado, não de parâmetro livre — um entregador não pode consultar rotas de outro.)

### 5.4 Frontend

**`UserTypeEnum`/rotas** — `'DELIVERER'` passa a ser um valor válido de
`userType`; novas rotas protegidas com `data: { roles: ['ADMIN', 'DELIVERER'] }`.

**Supplier — atribuição (Cenário 1)**:
- Modal de atribuição (Figura 15 do TCC) em `received-orders` — ação
  "Despachar" visível só para pedidos `IN_PREPARATION`; lista
  entregadores via `GET /api/deliverers/by-supplier/{id}`, badge
  "Disponível"/"Em rota" a partir de `busy`. Confirmar chama
  `PATCH /orders/{id}/assign-deliverer`.
- Novo componente `manage-deliverers` (`components/pages/manage-deliverers/`)
  — cadastro simples de entregador (nome, e-mail, telefone, senha,
  veículo), lista os já cadastrados via `GET /by-supplier/{id}`.

**Entregador — módulo móvel (Cenário 2)**:
- Novo componente `deliverer-routes` (`components/pages/deliverer-routes/`)
  — tela principal pós-login do entregador (Figura 17a): lista
  `GET /api/orders/my-routes` em formato de cards, botão de atalho
  "Confirmar Entrega" por card chamando
  `PATCH /orders/{id}/complete-delivery`.
- Login existente (`login.component.ts`) já funciona para qualquer
  `userType` — sem mudança necessária ali; só o redirecionamento
  pós-login precisa reconhecer `DELIVERER` e mandar para
  `/deliverer-routes` em vez de `/painel-cliente`/`/dashboard-fornecedor`.

## 6. Plano de Testes (TDD)

**Backend:**
1. `DelivererServiceTest` — `deveCriarEntregadorVinculadoAoFornecedor`,
   `deveCalcularBusyComoTrueQuandoHaPedidoEmRotaAtribuido`.
2. `OrderServiceTest` —
   `deveAtribuirEntregadorEAvancarStatusParaOutForDelivery`,
   `deveSetarDispatchedAtAoAtribuir`,
   `deveRejeitarAtribuicaoQuandoPedidoNaoEstaEmPreparacao`,
   `deveRejeitarAtribuicaoDeEntregadorDeOutroFornecedor`,
   `deveConcluirEntregaESetarDeliveredAt`,
   `deveRejeitarConclusaoDePedidoNaoAtribuidoAoEntregador`.
3. `OrderControllerTest` —
   `deveRetornarApenasRotasDoEntregadorAutenticado` (garante isolamento —
   um entregador não vê pedidos de outro),
   `deveRetornar200AoDespacharComEntregadorValido`.

**Frontend:**
4. `deliverer.service.spec.ts` — `deveBuscarEntregadoresPorFornecedor`,
   `deveCriarNovoEntregador`.
5. `received-orders.component.spec.ts` — estender:
   `deveExibirAcaoDeDespacharApenasParaPedidosEmPreparacao`,
   `deveAtribuirEntregadorSelecionadoNoModal`.
6. `deliverer-routes.component.spec.ts` —
   `deveListarApenasRotasDoEntregadorLogado`,
   `deveConfirmarEntregaComUmToque`.

## 7. Fora de Escopo

- Notificação em tempo real (SSE) quando o pedido é despachado ou
  entregue — isso é HU08, cliente-side; aqui só a transição de status
  acontece, sem push.
- Tela de histórico de entregas do entregador (Figura 17c do TCC) — não
  é exigida pelos 2 cenários oficiais da HU07; pode ser incremento
  futuro.
- Mapa/navegação (Figura 17b mostra um mapa) — o TCC já lista
  "Rastreamento de GPS em Tempo Real" como Won't have explícito
  (§5.3.4); um link estático para app de mapas externo (ex.: `geo:` URI
  ou link do Google Maps com o endereço) é aceitável, mas não é parte do
  critério de aceitação.
- Edição/exclusão de entregador cadastrado — só criação e listagem.
- Recusa de entrega pelo entregador — os cenários só falam em confirmar.

## 8. Definição de Pronto

- [ ] Fornecedor consegue cadastrar entregadores da própria frota.
- [ ] Fornecedor consegue atribuir um entregador a um pedido
      `IN_PREPARATION`, avançando para `OUT_FOR_DELIVERY` e preenchendo
      `dispatchedAt`.
- [ ] Entregador vê apenas suas próprias rotas ativas, isoladas por
      autenticação (não por parâmetro manipulável).
- [ ] Entregador confirma entrega com um toque, preenchendo
      `deliveredAt` e avançando para `DELIVERED`.
- [ ] Todos os testes da seção 6 passando.
- [ ] Revisado e aprovado antes de HU08 começar.
