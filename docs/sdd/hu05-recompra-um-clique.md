# HU05 — Recompra em Um Clique

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU05 |
| Perfil | Cliente (CLIENT) |
| Prioridade MoSCoW | Must have |
| Dependências | HU03 (preços `priceDelivery`/`pricePickup`, RN01), HU04 (checkout com `addressId` explícito) |
| Referência no TCC | Apêndice A.1 — HU05 |

> **Nota de correção**: a versão anterior deste script foi escrita antes
> de eu ter lido o Apêndice A completo do TCC (só tinha lido o resumo do
> capítulo 5.3). Ela inventava um segundo cenário de aceitação
> ("validação de disponibilidade") que não existe no documento, e mandava
> o usuário para o carrinho em vez do checkout. Corrigido abaixo com o
> texto real (pág. 51).

## 2. História de Usuário

Como cliente, quero utilizar um botão de "Pedir Novamente" no meu
histórico, para refazer a compra de rotina de forma instantânea.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Refação de pedido**: Dado o histórico em "Meus Pedidos",
  Quando aciona "Pedir Novamente" em um item, Então os produtos vão ao
  carrinho **e** o usuário é direcionado ao **checkout**, pulando o
  catálogo.

Este é o único cenário oficial da HU05 no Apêndice A. O cenário abaixo é
uma adição minha, não do TCC — ver §4 para a justificativa técnica.

- **Cenário 2 (adicional, não coberto pelo TCC) — Validação de
  disponibilidade**: Dado que um item do pedido antigo não está mais
  disponível (removido ou sem estoque), Quando a recompra é acionada,
  Então o sistema notifica o cliente e prossegue apenas com os itens
  válidos, sem impedir a recompra dos demais.

- **Cenário 3 (adicional, decisão do autor em 2026-10-14) — Edição no
  checkout**: Dado o checkout aberto (vindo da recompra ou do carrinho),
  Quando o cliente altera a quantidade ou remove um item, Então o
  carrinho é atualizado e persistido, e o subtotal é recalculado pelo
  backend (preview da HU03, RN01).
- **Cenário 4 (adicional, decisão do autor em 2026-10-14, revisto em
  2026-10-15) — Checkout em etapas**: Dado o checkout aberto, Quando o
  cliente avança, Então passa por 1) Itens (só a lista editável e o
  subtotal), 2) Entrega ou retirada (escolha da modalidade e, conforme
  ela, endereço do cliente ou endereço da loja), 3) Resumo e pagamento;
  não avança com o carrinho vazio nem, na Entrega, sem endereço
  selecionado; pode voltar a qualquer etapa anterior; o pedido só é
  confirmado na etapa 3.
- **Cenário 5 (adicional, decisão do autor em 2026-10-15) — Retirada sem
  endereço do cliente**: Dado o checkout, Quando o cliente escolhe
  Retirada na etapa 2, Então vê o endereço e o horário da loja de cada
  fornecedor do carrinho, avança sem escolher endereço próprio, e o
  pedido é criado sem endereço de entrega. Na Entrega, o endereço
  continua obrigatório e com checagem de dono (HU04).

**Decisões do autor (2026-10-14):** o Cenário 2 é mantido. A recompra
**substitui** o carrinho atual (o checkout mostra exatamente o pedido
refeito); se o carrinho tinha itens, o cliente é avisado por toast.

## 4. Estado atual (achados)

- `order-list.component.ts` é a tela "Meus Pedidos" real do cliente
  (usa `findByCustomerId`, corresponde à Figura 12b do TCC) — **não
  existe nenhum botão ou lógica de "Pedir Novamente" hoje**.
  (`order-history.component.ts`, apesar do nome parecido, é o painel do
  **fornecedor** para avançar status de pedido — não confundir os dois.)
- O checkout já existe como `OrderResumeComponent`, na rota
  `/resumo-pedido` — é para lá que o Cenário 1 exige que o cliente seja
  levado, não para uma tela de carrinho intermediária.
- `OrderItemResponseDTO.productId` já existe, suficiente para reconsultar
  o produto atual.
- **Por que adicionei o Cenário 2 mesmo não estando no TCC**: o Cenário 1
  manda copiar os itens do pedido antigo "ao carrinho" sem qualificar o
  que fazer se um item não existir mais ou estiver sem estoque. Sem
  alguma validação, a recompra levaria o cliente direto ao checkout
  (conforme exigido) com um item impossível de comprar, e a criação do
  pedido falharia só na hora de confirmar — uma experiência pior do que
  avisar antes. Trato isso como um requisito técnico implícito, não como
  invenção de escopo de produto; **se você preferir que eu remova essa
  validação e deixe o comportamento falhar simplesmente no checkout,
  posso simplificar**.
- **Revisão de 2026-10-14** (sobre a `develop` com HU03 e HU04 mergeadas):
  - O checkout agrupa o carrinho por `supplierId` para criar um pedido
    por fornecedor. O item de recompra **precisa** trazer `supplierId`
    (a versão anterior do contrato não trazia, e o pedido sairia com
    `supplierRef` indefinido). Também precisa de `imageUrl`, `type` e
    `category`, que o checkout exibe.
  - `OrderItem.product` é FK para `product_items`: um produto que já foi
    pedido não chega a ser apagado (o `DELETE` falha pela FK). O caso
    "produto removido" fica tratado defensivamente (produto nulo), mas o
    caso real é **sem estoque**.
  - Posse do pedido: CLAUDE.md proíbe confiar em `customerId` vindo da
    requisição quando o principal autenticado já define o dono. O
    endpoint usa o usuário logado, sem `@RequestParam customerId`.
  - `OrderController` já tem o padrão OpenAPI da HU03; a rota nova segue.
  - `order-list` é uma tabela desktop com cores `blue-600` literais; a
    Figura 12b pede cards por pedido com "Detalhes" e "Pedir Novamente".
- **Divergência da Figura 12a (decisão do autor)**: o TCC mostra o
  checkout em página única. Os Cenários 3 e 4 trocam isso por um fluxo em
  etapas no estilo iFood, com itens editáveis na primeira etapa. A
  Figura 12a continua sendo a referência visual de cada bloco (cards,
  tokens), não da organização em página única.
- O drawer do carrinho (`menu`) já tem +/− e remover, mas o +/− muta o
  objeto em memória sem persistir no `localStorage` — o checkout em
  etapas não reaproveita esse código.
- **Decisão de design mantida**: a recompra não deve reutilizar
  `unitPrice` do pedido antigo (pode estar desatualizado) — usa preço
  atual do produto, mesmo padrão RN01 já estabelecido na HU03.

## 5. Contrato Técnico

### 5.1 Backend

**Novos DTOs** (`model.dto`, com `@Schema`):
```java
public class ReorderItemDTO {
    private Long productId;
    private String productBrand;
    private String category;          // nome da categoria
    private String type;              // nome do tipo de produto
    private String imageUrl;
    private String supplierId;        // checkout agrupa por fornecedor
    private int quantity;             // limitada ao estoque atual
    private Double priceDelivery;     // preço ATUAL do produto (RN01)
    private Double pricePickup;
    private String unavailableReason; // null | PRODUTO_REMOVIDO | SEM_ESTOQUE
}

public class ReorderPreviewDTO {
    private List<ReorderItemDTO> availableItems;
    private List<ReorderItemDTO> unavailableItems;
}
```

**`OrderService.buildReorderPreview(String orderId, User customer)`**:
- pedido inexistente → `ResourceNotFoundException` (404);
- pedido de outro cliente → nova exceção de domínio
  `ForbiddenResourceException` (403, mapeada no `GlobalExceptionHandler`)
  — não revela dados de pedido alheio. Não usar `AccessDeniedException`
  do Spring no service: o handler genérico de `Exception` a transformaria
  em 500;
- para cada `OrderItem`: produto nulo → indisponível `PRODUTO_REMOVIDO`;
  `stockQuantity <= 0` → indisponível `SEM_ESTOQUE`; senão disponível
  com `quantity = min(quantidade antiga, estoque atual)` e preços atuais
  do produto (nunca o `unitPrice` histórico);
- só leitura: não persiste e não mexe no estoque.

**`OrderController`**:
```java
@Operation(summary = "Monta a recompra de um pedido", description = "...")
@ApiResponses({200, 403 (sem token, papel sem permissão ou pedido de outro cliente), 404})
@GetMapping("/{id}/reorder-preview")
@RolesAllowed({"ADMIN", "CUSTOMER"})
public ResponseEntity<ReorderPreviewDTO> getReorderPreview(@PathVariable String id) {
    User usuarioLogado = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return ResponseEntity.ok(orderService.buildReorderPreview(id, usuarioLogado));
}
```

### 5.2 Frontend

**`models/order.model.ts`** — `ReorderItem` e `ReorderPreview`.

**`order.service.ts`** — `getReorderPreview(orderId)` →
`GET /api/orders/{id}/reorder-preview`.

**`order-list.component.ts`** — `reorder(order)`:
1. chama `getReorderPreview(order.id)`;
2. sem nenhum item disponível → `toastr.warning` e **não** navega;
3. senão: se o carrinho tinha itens, avisa que foi substituído;
   `cartService.setCart(itens disponíveis mapeados para ProductItem,
   com quantitySelect = quantity)`; se houver indisponíveis, avisa quais
   (por marca); navega para `/resumo-pedido`;
4. erro HTTP → `toastr.error`, sem mexer no carrinho.

### 5.3 Visual (F7 §5.8 — Figura 12b)

`order-list` vira lista de cards `rounded-xl` (um por pedido): fornecedor,
data, itens resumidos, badge de status, total e botões "Detalhes"
(expande os itens, comportamento atual do acordeão) e "Pedir Novamente"
(`btn-primary`). Chips de status com `bg-primary` no ativo. Só tokens,
sem HEX novo, sem `blue-600` literal; `pb-24` no mobile. O FAB "+" atual
(sem ação) sai; "Fazer novo pedido" vira botão do cabeçalho em todas as
larguras.

### 5.3.1 Retirada sem endereço (Cenário 5)

**Reverte o Fora de Escopo da HU03** ("tornar `deliveryAddress` opcional
para pedidos PICKUP") — decisão do autor em 2026-10-15: pedir o endereço
do cliente numa retirada não faz sentido no fluxo em etapas.

- Migração `V5__order_address_nullable_for_pickup.sql`:
  `ALTER TABLE orders MODIFY address_id BIGINT NULL;`
- `Order.deliveryAddress` deixa de ser `nullable = false`.
- `OrderService.createOrder` resolve a modalidade antes do endereço:
  `PICKUP` → `deliveryAddress = null` (um `addressId` recebido é
  ignorado); `DELIVERY` → regra da HU04 (obrigatório e do cliente).
- `OrderResponseDTO.deliveryMode` (`"DELIVERY"`/`"PICKUP"`), para as
  listas mostrarem "Retirada na loja" no lugar do endereço vazio
  (`order-list` do cliente e `orders-widget` do fornecedor).
- Swagger: descrição do `POST /api/orders` e `@Schema` do `addressId`
  ("obrigatório só para DELIVERY").
- Frontend: na Retirada, a etapa 2 mostra um card por fornecedor do
  carrinho (nome, endereço da loja, horário) via
  `GET /api/suppliers/{id}`, e o pedido sai sem `addressId`.

### 5.3.2 Achado do teste de ponta a ponta — criação de pedido atômica (RN02)

No teste E2E de 2026-10-18, um carrinho com um item sem estoque mostrou
que `OrderService.createOrder` **não era transacional**: o pedido é
salvo antes dos itens, então quando um item falha por estoque
(`BusinessRuleException`, 400) ficam gravados um pedido órfão
(`PENDING`, sem itens, total nulo) e o decremento de estoque dos itens
já processados — violando a RN02 ("decremento de estoque na mesma
transação"). Defeito anterior à HU05, corrigido aqui por decisão do
autor, porque o checkout em etapas e a recompra dependem dele:
`createOrder` passa a ser `@Transactional` (pedido, itens e estoque
entram ou saem juntos). O toast de erro do checkout também passa a
mostrar a mensagem do backend (`{ error }`), que antes aparecia como
"undefined".

### 5.4 Checkout em etapas (Cenários 3 e 4)

**`cart.service.ts`** — `updateQuantity(productId, quantity)` (mínimo 1)
e `removeProduct(productId)`, ambos persistindo no `localStorage`.

**`order-resume.component.ts`**:
- `step: 1 | 2 | 3` (começa em 1), `nextStep()`, `previousStep()`;
- 1→2 exige carrinho não vazio; 2→3 exige `selectedAddressId`; trava
  explica o motivo por toast;
- `increaseQuantity`, `decreaseQuantity`, `removeItem` → `CartService`
  e nova chamada de `loadPreview()`; carrinho vazio limpa o preview;
- `createOrder()` só a partir da etapa 3.

**`order-resume.component.html`** — indicador `steps` (DaisyUI): Itens ·
Endereço · Pagamento.
1. Itens: lista com +/− e remover, toggle de modalidade, subtotal do
   preview, "Continuar"; vazio → "Seu carrinho está vazio" + "Ver
   produtos" (`/inicio`).
2. Endereço: seleção de endereço e observações, "Voltar"/"Continuar".
3. Resumo e pagamento: itens, endereço, modalidade e subtotal (só
   leitura), métodos de pagamento, "Voltar"/"Confirmar Pedido".

### 5.5 Movimento e feedback (checkout e Meus Pedidos)

Pedido do autor após o primeiro teste manual: fluxo mais fluido, perto
do iFood. Só frontend; animações via `@angular/animations` (já provido
em `main.ts`), cores só por tokens.

- Troca de etapa desliza com fade (da direita ao avançar, da esquerda ao
  voltar); etapas concluídas do indicador são clicáveis para voltar
  (`goToStep(n)` só aceita `n < step`).
- Toggle de modalidade como controle segmentado com pílula deslizante.
- Itens entram com fade e, ao remover, colapsam; quantidade dá um
  "bump" curto ao mudar.
- `previewLoading`: subtotal vira skeleton enquanto o preview carrega,
  nunca "R$ –" vazio.
- Barra de ação fixa no mobile (subtotal + Continuar/Confirmar) acima da
  bottom tab bar.
- Opção de pagamento selecionada com destaque e ícone de check.
- Confirmar mostra spinner no botão; no sucesso, `orderPlaced` exibe a
  tela "Pedido realizado!" e só então navega para `/meus-pedidos`.
- Meus Pedidos: cards entram com stagger, "Detalhes" expande com
  animação de altura, "Pedir Novamente" mostra spinner e fica desabilitado
  enquanto busca o preview (`reorderingId`).
- Acessibilidade: `prefers-reduced-motion: reduce` desliga animações e
  transições.
- Achado: o tema F2 (`h2gas`) não definia `--color-base-300`, do qual o
  `steps` e o `skeleton` do DaisyUI dependem (sem ele, a linha das etapas
  saía com borda preta e o skeleton ficava invisível). Token adicionado
  ao tema em `styles.css`. Pelo mesmo motivo faltavam `info`, `success`,
  `warning`, `error` (e `*-content`, `neutral-content`): o contador do
  carrinho (`badge-info`) saía sem fundo, e a tela de sucesso
  (`text-success`) e o hover da lixeira (`text-error`) não tinham cor.
  Tema completado com os tokens semânticos do DaisyUI.
- Achado: o `badge` do DaisyUI 5 também depende de variáveis estruturais
  do tema (`--size-selector`, `--border`, `--radius-selector`), que o
  `h2gas` não define — o contador do carrinho colapsava num traço. O
  contador do menu passou a ser um círculo em Tailwind puro (tokens
  `info`/`primary`), oculto com carrinho vazio.
- **Dívida registrada**: o tema `h2gas` não define as variáveis
  estruturais do DaisyUI 5 (`--radius-*`, `--size-*`, `--border`,
  `--depth`, `--noise`). Completá-las afeta botões, inputs e cards do app
  inteiro — fica para uma fatia F2/F7 com revisão visual geral.

## 6. Plano de Testes (TDD)

**Backend:**
1. `OrderServiceTest` —
   `deveMontarPreviewComTodosItensDisponiveis`,
   `deveMarcarItemComoIndisponivelQuandoProdutoForRemovido`,
   `deveMarcarItemComoIndisponivelQuandoEstoqueZerado`,
   `deveLimitarQuantidadeAoEstoqueAtualQuandoInsuficiente`,
   `deveUsarPrecoAtualDoProdutoNaoOHistorico`,
   `deveRejeitarPreviewDePedidoQueNaoPertenceAoCliente`,
   `deveTrazerSupplierIdNosItensDisponiveis`,
   `naoDeveAlterarEstoqueNoReorderPreview`.
2. `OrderControllerTest` — `deveRetornar200ComPreviewValido`
   (principal autenticado repassado ao service).

**Frontend:**
3. `order.service.spec.ts` — `deveBuscarReorderPreviewDoPedido`.
4. `order-list.component.spec.ts` —
   `deveSubstituirCarrinhoComItensDisponiveisAoClicarPedirNovamente`,
   `deveNotificarItensIndisponiveisSemBloquearOsDisponiveis`,
   `deveNavegarParaResumoPedidoAposRecompra`,
   `naoDeveNavegarQuandoNenhumItemEstiverDisponivel`.

5. `cart.service.spec.ts` — `deveAtualizarQuantidadeEPersistir`,
   `deveRemoverItemEPersistir`.
6. `order-resume.component.spec.ts` — `deveIniciarNaEtapaDeItens`,
   `deveRecalcularPreviewAoAlterarQuantidade`,
   `deveRemoverItemERecalcularPreview`, `naoDeveAvancarComCarrinhoVazio`,
   `naoDeveAvancarDoEnderecoSemSelecionar`, `deveVoltarParaEtapaAnterior`,
   `deveConfirmarPedidoSoNaEtapaDeResumo`.

7. `order-resume.component.spec.ts` (§5.5) —
   `deveExibirCarregandoEnquantoPreviewNaoChega`,
   `deveVoltarParaEtapaConcluidaAoClicarNoIndicador`,
   `deveMostrarConfirmacaoAntesDeNavegar`.
8. `order-list.component.spec.ts` (§5.5) —
   `deveDesabilitarPedirNovamenteEnquantoCarrega`.
9. Cenário 5 — `OrderServiceTest`: `deveCriarPedidoDeRetiradaSemEndereco`,
   `deveIgnorarEnderecoEmPedidoDeRetirada`,
   `deveExporModalidadeNaRespostaDoPedido`;
   `order-resume.component.spec.ts`: `naoDeveExibirModalidadeNaEtapaDeItens`,
   `deveExibirModalidadeNaEtapaDeEntrega`,
   `deveAvancarSemEnderecoQuandoRetirada`,
   `deveExibirEnderecoDaLojaNaRetirada`,
   `deveEnviarPedidoDeRetiradaSemAddressId`;
   `order-list.component.spec.ts`:
   `deveExibirRetiradaNaLojaQuandoPedidoForPickup`.

10. §5.3.2 — `OrderServiceTransactionTest` (sem transação de teste,
    para enxergar o rollback):
    `deveDesfazerPedidoEEstoqueQuandoUmItemFalhaPorEstoque`;
    `order-resume.component.spec.ts`:
    `deveExibirMensagemDoBackendQuandoPedidoFalhar`.

Regra herdada: specs antigos falhando no frontend não aumentam (23 após
a primeira parte desta HU).

## 7. Fora de Escopo

- Recompra agendada/automática (fora do critério de aceitação).
- **Dívida registrada (teste E2E de 2026-10-18)**: negação de papel via
  `@RolesAllowed` (ex.: fornecedor chamando `PATCH
  /api/suppliers/{id}/update-active`) responde **500 "Access Denied"**
  em vez de 403 — o handler genérico de `Exception` do
  `GlobalExceptionHandler` captura a `AuthorizationDeniedException`. O
  acesso continua bloqueado; só o status está errado. Vale para o app
  inteiro, não é desta HU.
- **Dívida registrada (mesmo teste)**: produto com estoque 0 aparece no
  catálogo e pode ir ao carrinho (o backend recusa ao confirmar) — RN02,
  fica para a HU09.
- Limitar a quantidade ao estoque no client — o backend já rejeita
  estoque insuficiente ao criar o pedido.
- Corrigir o +/− do drawer do carrinho (`menu`), que não persiste — dívida
  registrada.
- Alterar a modalidade de entrega/retirada do pedido antigo — usa o
  estado atual do carrinho (HU03).
- Checar se o fornecedor do pedido antigo atende a cidade do endereço
  ativo (RN05) ou está aberto — o checkout atual também não checa; fica
  registrado como dívida.
- Busca por texto em "Meus Pedidos" (aparece na Figura 12b, mas não é
  critério da HU05).

## 8. Definição de Pronto

- [ ] `GET /api/orders/{id}/reorder-preview` implementado, posse checada
      pelo usuário autenticado, documentado no Swagger.
- [ ] "Pedir Novamente" em `order-list` substitui o carrinho com
      dados/preços atuais **e navega direto para `/resumo-pedido`**.
- [ ] Itens indisponíveis são notificados e não bloqueiam os demais.
- [ ] `order-list` no padrão F7 (Figura 12b): cards, tokens, `pb-24`.
- [ ] Checkout em etapas (Itens → Endereço → Pagamento) com itens
      editáveis e subtotal recalculado pelo backend.
- [ ] Retirada sem endereço do cliente: V5 aplicada, loja exibida na
      etapa 2, pedido criado com `address_id` NULL, listas mostram
      "Retirada na loja".
- [ ] Criação de pedido atômica: item sem estoque não deixa pedido
      órfão nem estoque decrementado (§5.3.2).
- [ ] Transições e estados de carregamento no checkout e em Meus Pedidos
      (§5.5), respeitando `prefers-reduced-motion`.
- [ ] Todos os testes da seção 6 passando; specs antigos falhando ≤ 25.
- [ ] Testado manualmente no navegador pelo desenvolvedor.
