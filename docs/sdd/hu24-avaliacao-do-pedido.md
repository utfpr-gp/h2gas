# HU24 — O cliente avalia a loja e o entregador depois da entrega

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU24 |
| Perfil | Cliente (avalia); Fornecedor (vê a nota) |
| Prioridade MoSCoW | Should have. É a "HU14: Sistema de Avaliações" do texto do TCC (A.2); aqui a HU14 já é o carrinho de uma loja só, por isso o número novo. Pedido do autor em 2026-10-29: "vamos finalizar as reviews, pro cliente avaliar". Decisão: **duas notas**, uma da loja e outra do entregador, quando houve entrega. |
| Dependências | HU06 e HU07 (pedido entregue e entregador), HU08 (status ao vivo em Meus pedidos), HU09 (painel do fornecedor), F7 §5.25 e §5.27 (média e lista de avaliações da loja), F6 (OpenAPI) |
| Regra global | RN04: a avaliação só é aceita se o pedido estiver `DELIVERED` |

## 2. História de Usuário

Como cliente, quero avaliar a loja e o entregador depois que o pedido chega,
para ajudar outros clientes e cobrar qualidade do serviço. Como fornecedor,
quero ver a nota média da minha loja e a de cada entregador da frota.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Convite para avaliar**: Dado um pedido meu que ficou
  "Entregue", Quando abro Meus pedidos (ou o status muda ao vivo, pela HU08),
  Então o card mostra "Como foi o pedido?" com um botão "Avaliar".
- **Cenário 2 — Duas notas**: Quando toco em "Avaliar", Então escolho de 1 a
  5 estrelas para a loja e, se o pedido foi entregue por um entregador, de 1
  a 5 estrelas para ele; o comentário é opcional (até 500 caracteres). Em
  pedido de retirada, ou sem entregador, só aparece a nota da loja.
- **Cenário 3 — Uma vez por pedido**: Depois de enviar, o card mostra as
  estrelas que dei, sem o botão. Uma segunda avaliação do mesmo pedido é
  recusada (409).
- **Cenário 4 — Só pedido entregue (RN04)**: Pedido em qualquer outro status
  não mostra o convite, e a API recusa a avaliação (400).
- **Cenário 5 — Só o dono**: Avaliar pedido de outro cliente é recusado
  (403); pedido que não existe, 404.
- **Cenário 6 — Nota da loja no painel**: No painel do fornecedor, um card
  mostra a nota média da loja (1 casa) e quantas avaliações a formam,
  considerando todo o histórico, não só o mês escolhido.
- **Cenário 7 — Nota do entregador**: Em Entregadores, cada card mostra a
  média do entregador e quantas avaliações ele tem; sem avaliações, "Sem
  avaliações".
- **Cenário 8 — Loja na vitrine**: A média da vitrine e a lista de
  avaliações da loja (F7) passam a incluir as avaliações novas, sem mudar
  de formato; a nota do entregador não entra na média da loja.

## 4. Estado atual (achados)

- A tabela `reviews` existe desde a V2: `order_id`, `supplier_id`,
  `deliverer_id` (nulo), `rating` 1–5 e `comment`. Não há índice único por
  pedido, nem coluna para a nota do entregador.
- Só há leitura: `ReviewRepository.summarizeBySupplierIds` (média na
  vitrine) e `findStoreReviews` (`GET /api/suppliers/{id}/reviews`). Não há
  rota nem tela para o cliente criar uma avaliação. As 20 avaliações do banco
  local vêm do seed, todas de pedidos entregues e sem pedido repetido.
- `OrderResponseDTO.status` vem com o rótulo em português ("Entregue").
- Respostas 409 do projeto usam `ResponseStatusException(CONFLICT)`; regra
  de negócio usa `BusinessRuleException` (400).

## 5. Contrato Técnico

### 5.1 Banco

`V22__review_deliverer_rating.sql`:
- `ADD COLUMN deliverer_rating INT NULL`, com
  `CHECK (deliverer_rating BETWEEN 1 AND 5)`;
- `ADD CONSTRAINT uk_reviews_order UNIQUE (order_id)`.

`rating` continua sendo a nota da loja (as leituras atuais não mudam).

### 5.2 API

**`POST /api/orders/{id}/review`**, só `CUSTOMER`:
- corpo `CreateReviewDTO { storeRating (1–5, obrigatório),
  delivererRating (1–5, opcional), comment (até 500, opcional) }`;
- o cliente vem do token; o pedido precisa ser dele (403) e existir (404);
- o pedido precisa estar `DELIVERED` (400, RN04);
- `delivererRating` só é aceito se o pedido tem entregador (400 caso
  contrário); com entregador, é opcional;
- comentário em branco vira nulo;
- segunda avaliação do mesmo pedido: 409;
- grava `supplier` e `deliverer` a partir do pedido, não do corpo;
- resposta 201 com `OrderReviewDTO { storeRating, delivererRating,
  comment }`.

**`OrderResponseDTO.review`** (`OrderReviewDTO`, nulo sem avaliação), preenchido
nas listagens do cliente (`GET /api/orders/by-customer/{id}`) com uma
consulta só para a página inteira (sem N+1).

**Painel**: `SupplierDashboardDTO` ganha `averageRating` (1 casa, nulo sem
avaliações) e `ratingCount`, de todo o histórico da loja.

**Entregadores**: `DelivererDTO` ganha `averageRating` (1 casa, nulo) e
`ratingCount`, da nota do entregador, numa consulta agrupada para a lista.

OpenAPI: rota nova com 201, 400, 403, 404 e 409; `@Schema` nos DTOs e nos
campos novos.

### 5.3 Frontend

- `OrderService.reviewOrder(orderId, body)`; modelos `OrderReview` e
  `review?` em `OrderResponse`; `averageRating`/`ratingCount` no painel e no
  entregador.
- `order-list`: pedido "Entregue" sem `review` mostra o convite
  (`data-testid="review-invite"`) e o botão `review-open`; com `review`,
  mostra as estrelas dadas (`review-given`).
- Componente `order-review-dialog` (standalone): estrelas como grupo de
  rádio acessível (`role="radiogroup"`, cada estrela com `aria-label="4 de
  5"`, alvo de 44 px), o grupo do entregador só quando o pedido tem
  entregador, comentário com contador, "Enviar" desabilitado sem nota da loja.
  Ao salvar, o card atualiza sem recarregar a lista.
- Painel do fornecedor: card "Nota da loja" (`dashboard-rating`) com a
  média e a contagem; sem avaliações, "Sem avaliações ainda".
- `manage-deliverers`: a nota no card do entregador (`deliverer-rating`).
- Design tokens (F2), sem HEX novo.

## 6. Plano de Testes (TDD)

1. `ReviewServiceTest`:
   - `deveGravarAsDuasNotasDoPedidoEntregue` (loja e entregador vindos do
     pedido);
   - `deveGravarSoANotaDaLojaNaRetirada`;
   - `deveRecusarPedidoNaoEntregue` (400);
   - `deveRecusarPedidoDeOutroCliente` (403);
   - `deveRecusarSegundaAvaliacao` (409);
   - `deveRecusarNotaDoEntregadorSemEntregador` (400);
   - `deveGuardarComentarioEmBrancoComoNulo`.
2. `OrderControllerTest`:
   - `deveAvaliarOPedidoDoClienteDoToken` (201);
   - `deveRecusarNotaForaDe1a5` (400);
   - `deveNegarAvaliacaoAoFornecedor` (403).
3. `OrderServiceTest`: `deveTrazerAAvaliacaoNosPedidosDoCliente`.
4. `SupplierDashboardServiceTest`: `deveMostrarANotaMediaDaLoja` (todo o
   histórico, sem avaliações = nulo e 0).
5. `DelivererServiceTest`: `deveMostrarANotaDeCadaEntregador`.
6. `order-review-dialog.component.spec.ts`:
   - `deveMostrarANotaDoEntregadorSoComEntregador`;
   - `deveDesabilitarEnviarSemANotaDaLoja`;
   - `deveEnviarAsNotasEOComentario`.
7. `order-list.component.spec.ts`:
   - `deveConvidarParaAvaliarPedidoEntregue`;
   - `naoDeveConvidarPedidoNaoEntregue`;
   - `deveMostrarAAvaliacaoFeita`.
8. `supplier-dashboard` e `manage-deliverers`: `deveMostrarANotaDaLoja`,
   `deveMostrarANotaDoEntregador`.

**E2E (registrado no DoD)**: como cliente de teste com pedido entregue,
avaliar loja e entregador; conferir o card, o banco, a segunda tentativa
(409), a nota no painel e em Entregadores. Em 390 e 1280 px.

## 7. Fora de Escopo

- Avisar por e-mail, push ou WhatsApp para avaliar (o convite fica em Meus
  pedidos, que já se atualiza ao vivo).
- Editar ou apagar uma avaliação depois de enviada.
- Resposta da loja à avaliação; denúncia ou moderação de comentário.
- Prazo para avaliar.
- Mostrar a nota do entregador ao cliente ou na vitrine.
- Avaliar produto.

## 8. Definição de Pronto

- [ ] V22 aplicada no MySQL local, sem perder as avaliações existentes.
- [ ] Cliente avalia loja e entregador; retirada só a loja.
- [ ] RN04, dono e uma vez por pedido garantidos no backend.
- [ ] Nota no painel e em Entregadores.
- [ ] Rota documentada no Swagger.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` sem falha nova;
      `ng build` ok.
- [ ] E2E registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
