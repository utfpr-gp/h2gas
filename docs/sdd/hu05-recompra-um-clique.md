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
- **Decisão de design mantida**: a recompra não deve reutilizar
  `unitPrice` do pedido antigo (pode estar desatualizado) — usa preço
  atual do produto, mesmo padrão RN01 já estabelecido na HU03.

## 5. Contrato Técnico

### 5.1 Backend

**Novos DTOs**:
```java
public class ReorderItemDTO {
    private Long productId;
    private String productBrand;
    private int quantity;          // já limitada ao estoque atual, se necessário
    private BigDecimal priceDelivery;
    private BigDecimal pricePickup;
    private String unavailableReason; // null se disponível
}

public class ReorderPreviewDTO {
    private List<ReorderItemDTO> availableItems;
    private List<ReorderItemDTO> unavailableItems;
}
```

**`OrderService.java`** — novo método:
```java
public ReorderPreviewDTO buildReorderPreview(String orderId, String customerId) {
    Order order = findEntityById(orderId); // 404 se não existir
    if (!order.getCustomer().getId().equals(customerId)) {
        throw new BusinessRuleException("Pedido não pertence ao cliente informado.");
    }

    List<ReorderItemDTO> available = new ArrayList<>();
    List<ReorderItemDTO> unavailable = new ArrayList<>();

    for (OrderItem item : order.getItems()) {
        Optional<ProductItem> productOpt = productItemService.findByIdOptional(item.getProduct().getId());

        if (productOpt.isEmpty()) {
            unavailable.add(toReorderItem(item, "PRODUTO_REMOVIDO", 0));
            continue;
        }

        ProductItem product = productOpt.get();
        if (product.getStockQuantity() <= 0) {
            unavailable.add(toReorderItem(item, "SEM_ESTOQUE", 0));
            continue;
        }

        int quantity = Math.min(item.getQuantity(), product.getStockQuantity());
        available.add(toReorderItem(item, null, quantity)); // preço vem de `product`, não do item histórico
    }

    return new ReorderPreviewDTO(available, unavailable);
}
```

**`OrderController.java`** — novo endpoint:
```java
@GetMapping("/{id}/reorder-preview")
@ResponseBody
@RolesAllowed({"ADMIN", "CUSTOMER"})
public ResponseEntity<ReorderPreviewDTO> getReorderPreview(
        @PathVariable String id,
        @RequestParam String customerId) {
    return ResponseEntity.ok(orderService.buildReorderPreview(id, customerId));
}
```

### 5.2 Frontend

**`order.service.ts`** — novo método:
```ts
getReorderPreview(orderId: string, customerId: string) {
  return this.http.get<ReorderPreviewDTO>(
    `${this.baseUrl}/${orderId}/reorder-preview`,
    { params: { customerId } }
  );
}
```

**`order-list.component.ts`/`.html`** — botão "Pedir Novamente" em cada
card de pedido. **Direciona ao checkout (`/resumo-pedido`), não ao
carrinho** — cumprindo o Cenário 1 literalmente:
```ts
reorder(orderId: string) {
  this.orderService.getReorderPreview(orderId, this.userLogado!.id!).subscribe({
    next: (preview) => {
      preview.availableItems.forEach(item => this.cartService.addProduct({
        id: item.productId,
        brand: item.productBrand,
        priceDelivery: item.priceDelivery,
        pricePickup: item.pricePickup,
        quantitySelect: item.quantity,
      }));

      if (preview.unavailableItems.length > 0) {
        const nomes = preview.unavailableItems.map(i => i.productBrand).join(', ');
        this.toastr.warning(`Alguns itens não puderam ser adicionados: ${nomes}`);
      }

      this.router.navigate(['/resumo-pedido']);
    },
    error: () => this.toastr.error('Não foi possível refazer este pedido.')
  });
}
```

## 6. Plano de Testes (TDD)

**Backend:**
1. `OrderServiceTest` —
   `deveMontarPreviewComTodosItensDisponiveis`,
   `deveMarcarItemComoIndisponivelQuandoProdutoForRemovido`,
   `deveMarcarItemComoIndisponivelQuandoEstoqueZerado`,
   `deveLimitarQuantidadeAoEstoqueAtualQuandoInsuficiente`,
   `deveUsarPrecoAtualDoProdutoNaoOHistorico`,
   `deveRejeitarPreviewDePedidoQueNaoPertenceAoCliente`.
2. `OrderControllerTest` — `deveRetornar200ComPreviewValido`.

**Frontend:**
3. `order.service.spec.ts` — `deveBuscarReorderPreviewComCustomerId`.
4. `order-list.component.spec.ts` —
   `devePreencherCarrinhoComItensDisponiveisAoClicarPedirNovamente`,
   `deveNotificarItensIndisponiveisSemBloquearOsDisponiveis`,
   `deveNavegarParaResumoPedidoAposRecompra` (cobre o "pulando o
   catálogo" do Cenário 1 oficial).

## 7. Fora de Escopo

- Recompra agendada/automática (fora do critério de aceitação).
- Edição dos itens copiados antes de irem ao checkout — o cliente ajusta
  pelo próprio resumo do pedido, já existente.
- Alterar a modalidade de entrega/retirada do pedido antigo — usa o
  estado atual do carrinho (HU03).

## 8. Definição de Pronto

- [ ] Endpoint `/{id}/reorder-preview` implementado, valida posse do
      pedido.
- [ ] Botão "Pedir Novamente" em `order-list` popula o carrinho com
      dados/preços atuais **e navega direto para `/resumo-pedido`**
      (checkout), cumprindo o Cenário 1 oficial.
- [ ] Itens indisponíveis são notificados, não bloqueiam os demais
      (Cenário 2 adicional — confirmar com o orientador se deve ser
      mantido como está ou simplificado).
- [ ] Todos os testes da seção 6 passando.
- [ ] Revisado e aprovado — encerra o sub-projeto UX Cliente (HU01-HU05).
