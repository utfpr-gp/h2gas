# HU10 — Gestão de Catálogo e Precificação Logística

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU10 |
| Perfil | Fornecedor (SUPPLIER) |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (`priceDelivery`/`pricePickup`), HU03 (mesmo padrão já aplicado do lado do cliente) |
| Referência no TCC | Apêndice A.1 — HU10 |

## 2. História de Usuário

Como fornecedor, quero cadastrar e editar meus produtos definindo
valores distintos para retirada e entrega, para precificar minha
operação de acordo com o custo logístico individual de cada item.

## 3. Critérios de Aceitação (BDD)

*(correção de rotulagem: a versão anterior usava "Preço de Retirada"/
"Preço de Entrega" e "frete grátis" — o texto real do Apêndice A, pág.
53, usa "Preço para Retirada"/"Preço com Entrega" e "Entrega Grátis". O
conteúdo técnico não mudou, só os rótulos exatos exibidos ao usuário.)*

- **Cenário 1 — Precificação dividida**: Dado o formulário de novo
  produto, Quando o fornecedor preenche os dados financeiros, Então o
  sistema exige a inserção obrigatória e separada dos campos "Preço para
  Retirada" e "Preço com Entrega".
- **Cenário 2 — Regra de Entrega Grátis**: Dado a edição de um item,
  Quando o fornecedor informa valores idênticos para retirada e entrega,
  Então o sistema salva a configuração e o front-end passa a renderizar a
  etiqueta de "Entrega Grátis" para aquele produto.

## 4. Estado atual (achado crítico — última ponta solta do F3)

O F3 dividiu `ProductItem.price` em `priceDelivery`/`pricePickup` no
schema e na entidade, mas **o formulário de cadastro/edição de produto do
fornecedor nunca foi atualizado** — é a última ponta solta dessa
migração (a primeira foi `OrderItemService`, corrigida na HU03):

- `CreateProductItemDTO` ainda tem um único campo `private BigDecimal price;`.
- `ProductItemService.updateProductItem` ainda chama
  `productItemUpdate.setPrice(productItemDto.getPrice())` — **não
  compila** desde o F3.
- `ProductItemService.createProductItem` usa `CreateProductItemMapper`
  (baseado em `ModelMapper`, mapeamento por reflexão/nome de campo) — como
  `price` não bate mais com nenhum campo da entidade, o mapeamento
  **compila, mas silenciosamente deixa `priceDelivery`/`pricePickup`
  nulos**, o que falha só no `INSERT` (colunas `NOT NULL`) — um bug mais
  perigoso que os de compilação, porque não aparece até rodar.
- `ProductItemDTO` (resposta) também ainda tem `Double price` — o mesmo
  problema silencioso do `ModelMapper` acontece na leitura.
- `product-item-form.component.ts` (frontend) só tem um campo de preço
  (`price: [0.0, [Validators.required]]`).
- Nenhum endpoint de criação/edição de produto usa `@Valid` hoje — "o
  sistema exige" do Cenário 1 precisa ser reforçado, não só assumido.

## 5. Contrato Técnico

### 5.1 Backend

**`CreateProductItemDTO.java`**:
```java
private Long id;
private String brand;

@NotNull(message = "Preço de entrega é obrigatório")
@DecimalMin(value = "0.0", message = "Preço de entrega não pode ser negativo")
private BigDecimal priceDelivery;

@NotNull(message = "Preço de retirada é obrigatório")
@DecimalMin(value = "0.0", message = "Preço de retirada não pode ser negativo")
private BigDecimal pricePickup;

private Long categoryProductId;
private String imageUrl;
private Integer stockQuantity;
private String description;
```

**`ProductItemDTO.java`** — `Double price` vira:
```java
private Double priceDelivery;
private Double pricePickup;
private boolean freeDelivery; // computado, não persistido
```

**`ProductItemMapper.toDto`** — como `freeDelivery` não tem campo
correspondente na entidade, `ModelMapper` não o preenche sozinho; setar
explicitamente:
```java
productItemDTO.setFreeDelivery(
    data.getPriceDelivery() != null && data.getPricePickup() != null
        && data.getPriceDelivery().compareTo(data.getPricePickup()) == 0
);
```
**Decisão**: `freeDelivery` é sempre **computado na leitura**, nunca
persistido como coluna — evita o risco de ficar desatualizado se um dos
preços mudar depois (o TCC usa "pode sinalizar", não exige persistência).

**`ProductItemService.updateProductItem`** — corrigir a quebra do F3:
```java
productItemUpdate.setPriceDelivery(productItemDto.getPriceDelivery());
productItemUpdate.setPricePickup(productItemDto.getPricePickup());
```

**`ProductItemController.java`** — endpoints de criação e edição passam a
validar o DTO:
```java
@PostMapping
public ResponseEntity<ProductItemDTO> createCategoryProduct(
        @RequestPart("product") @Valid CreateProductItemDTO productItemDTO,
        @RequestPart("file") MultipartFile file) { ... }

@PutMapping("/{id}")
public ResponseEntity<ProductItemDTO> update(
        @PathVariable Long id, @Valid @RequestBody CreateProductItemDTO dto) { ... }
```
(confirmar o handler global de `MethodArgumentNotValidException` — se não
existir um `@ExceptionHandler` centralizado retornando 400 com a mensagem
de validação, adicionar um mínimo em `exceptions/` seguindo o padrão já
usado para `BusinessRuleException`/`ResourceNotFoundException`.)

### 5.2 Frontend

**`product-item-form.component.ts`**:
```ts
this.form = this.fb.group({
  // ...
  priceDelivery: [0.0, [Validators.required, Validators.min(0)]],
  pricePickup: [0.0, [Validators.required, Validators.min(0)]],
});
```

**`product-item-form.component.html`** — dois campos numéricos lado a
lado ("Preço de Entrega" / "Preço de Retirada"), ambos obrigatórios.

**Cards de produto** (já ajustados na HU03 para exibir os dois preços) —
adicionar badge condicional:
```html
<span *ngIf="product.freeDelivery" class="badge badge-success">Entrega Grátis</span>
```

## 6. Plano de Testes (TDD)

**Backend:**
1. `ProductItemServiceTest` — atualizar testes que usavam `.setPrice`;
   novos: `deveAtualizarAmbosPrecosAoEditarProduto`,
   `deveCriarProdutoComPrecoDeEntregaEDeRetirada`.
2. `ProductItemMapperTest` (ou dentro do service test) —
   `deveSinalizarFreteGratisQuandoPrecosForemIguais`,
   `naoDeveSinalizarFreteGratisQuandoPrecosDiferentes`,
   `naoDeveQuebrarQuandoAlgumPrecoForNulo`.
3. `ProductItemControllerTest` —
   `deveRejeitarCriacaoSemPrecoDeEntrega` (400),
   `deveRejeitarCriacaoComPrecoNegativo` (400),
   `deveAceitarCriacaoComAmbosOsPrecosValidos`.

**Frontend:**
4. `product-item-form.component.spec.ts` —
   `deveExigirAmbosOsCamposDePrecoParaSubmeter`,
   `deveEnviarPriceDeliveryEPricePickupAoSalvar`.
5. Teste de card — `deveExibirBadgeDeFreteGratisQuandoPrecosIguais`,
   `naoDeveExibirBadgeQuandoPrecosDiferentes`.

## 7. Fora de Escopo

- Persistir o flag de frete grátis como coluna — decisão explícita de
  mantê-lo computado (§5.1).
- Regras de desconto, precificação dinâmica ou promoções — a HU10 trata
  só do par fixo entrega/retirada.
- Retroativamente corrigir dados de produtos já existentes no banco além
  do que o F3 já migrou (`price` → ambas as colunas com o mesmo valor
  inicial) — isso já foi coberto na migração `V2` do F3.

## 8. Definição de Pronto

- [ ] Backend volta a compilar (`ProductItemService` corrigido).
- [ ] Criação/edição de produto exige e persiste `priceDelivery` e
      `pricePickup` separadamente, com validação (`@Valid`, 400 se
      ausente/negativo).
- [ ] `ProductItemDTO` deixa de expor `price`; expõe `freeDelivery`
      computado.
- [ ] Formulário do fornecedor tem dois campos de preço obrigatórios.
- [ ] Badge de frete grátis aparece quando os preços são iguais.
- [ ] Todos os testes da seção 6 passando.
- [ ] Revisado e aprovado. Isso fecha o sub-projeto Logística/Dashboard
      do Fornecedor (HU06, HU09, HU10). Próximo script é HU07, início de
      Entregador + Tempo Real.
