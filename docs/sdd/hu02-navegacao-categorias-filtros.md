# HU02 — Navegação por Categorias e Filtros

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU02 |
| Perfil | Cliente (CLIENT) |
| Prioridade MoSCoW | Must have |
| Dependências | HU01 (`LocationContextService`, filtro de cidade) |
| Referência no TCC | Apêndice A.1 — HU02 |

## 2. História de Usuário

Como cliente, quero navegar por diferentes categorias de produtos e
filtrar a listagem, para encontrar e comparar rapidamente os itens que
preciso.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Filtro de categoria**: Dado a visão do catálogo, Quando
  seleciona uma categoria (ex.: gás, água), Então a listagem exibe
  exclusivamente os itens correspondentes.
- **Cenário 2 — Ordenação**: Dado múltiplos produtos exibidos, Quando
  aplica a ordenação por "menor preço", Então a lista é reorganizada de
  forma crescente.
- **Cenário 3 (adicional) — Consistência com RN05**: Dado que o cliente já
  tem uma cidade definida (HU01), Quando alterna para a visão de produtos
  (`viewMode = 'products'`), Então a listagem global de produtos também
  respeita o filtro de cidade — não só a listagem de fornecedores.

## 4. Estado atual (achados)

- `supplier-list.component.ts` já tem um toggle `viewMode: 'suppliers' |
  'products'` que chama `getProductsFromActiveSuppliers()` — é a "visão
  global de produtos da plataforma" (Figura 24 do TCC). **Esse endpoint
  hoje não filtra por categoria, nem por cidade, nem ordena** — só
  pagina. Se não for corrigido junto, o cliente pode ver produtos de
  cidades diferentes só por trocar de aba, contornando o que a HU01
  acabou de implementar.
- `ProductItemService.findProductsFiltered` (backend) já filtra por
  categoria, mas está escopado a **um único fornecedor**
  (`by-supplier/{supplierId}`) — não serve para a visão global.
- `GET /api/product-types` (lista de categorias: "Gás", "Água"...) está
  restrito a `ADMIN`/`SUPPLIER` — o cliente não consegue montar as abas de
  categoria hoje.
- `PaginationUtil.getPageableRequest` não aceita `Sort` — nenhum endpoint
  do sistema ordena por preço hoje.
- Preço já está segregado em `priceDelivery`/`pricePickup` desde F3.
  **Decisão**: "menor preço" (HU02) ordena por `priceDelivery`, que é o
  valor mostrado em destaque nos cards antes de o cliente entrar no
  detalhe/carrinho (onde HU03 mostra as duas modalidades lado a lado).

## 5. Contrato Técnico

### 5.1 Backend

**`PaginationUtil.java`** — nova sobrecarga (não quebra chamadas
existentes de 2 argumentos):
```java
public static Pageable getPageableRequest(Integer page, Integer size, String sort) {
    Pageable base = getPageableRequest(page, size);
    if (sort == null || sort.isBlank()) return base;

    String[] parts = sort.split(",");
    Sort.Direction direction = parts.length > 1
        ? Sort.Direction.fromString(parts[1])
        : Sort.Direction.ASC;
    return PageRequest.of(base.getPageNumber(), base.getPageSize(), Sort.by(direction, parts[0]));
}
```

**`ProductItemRepository.java`** — nova query (substitui a derivação
simples de `findBySupplierActiveAndSupplierOpen`):
```java
@Query("""
    SELECT p FROM ProductItem p
    WHERE p.supplier.active = true AND p.supplier.open = true
      AND (:cityId IS NULL OR p.supplier.city.id = :cityId)
      AND (:category IS NULL OR LOWER(p.categoryProduct.productType.name) = LOWER(:category))
    """)
Page<ProductItem> findAvailableProducts(@Param("cityId") Long cityId,
                                         @Param("category") String category,
                                         Pageable pageable);
```

**`ProductItemService.java`** — `findBySupplierActiveAndSupplierOpen`
passa a aceitar `cityId`/`category` e delega para a nova query.

**`ProductItemController.java`** (`GET /by-supplier-open`) — adiciona
`@RequestParam(required = false) Long cityId`,
`@RequestParam(required = false) String category`,
`@RequestParam(required = false) String sort`, repassando para
`PaginationUtil.getPageableRequest(page, size, sort)`.

**`ProductTypeController.java`** — endpoint `GET /api/product-types`
ganha `CUSTOMER` em `@RolesAllowed` (leitura pública de nomes de
categoria, sem dado sensível).

### 5.2 Frontend

**`product-item.service.ts`** — `getProductsFromActiveSuppliers` ganha
parâmetros `cityId?`, `category?`, `sort?`, todos opcionais, anexados à
query string quando presentes.

**`supplier-list.component.ts`**:
- `getProducts()` passa a enviar
  `cityId: this.locationContext.current()?.cityId` (via
  `LocationContextService` da HU01).
- Novo estado `selectedCategory: string | null` e `sortByPrice: boolean`;
  ao mudar qualquer um, rechama `getProducts()` com os parâmetros
  atualizados.
- Novo `ngOnInit` (ou método dedicado) busca `ProductTypeService.findAll()`
  para montar as abas de categoria (ícones "Água", "Gás"... — Figura 11).

**`product-type.service.ts`** — se não existir um método de listagem
simples (sem paginação) consumível pelo cliente, adicionar
`findAll(): Observable<ProductType[]>` apontando para `GET /api/product-types`.

## 6. Plano de Testes (TDD)

**Backend:**
1. `ProductItemRepositoryTest` (`@DataJpaTest`) —
   `deveFiltrarProdutosPorCidadeECategoria`,
   `deveRetornarTodosQuandoCidadeECategoriaNulos`.
2. `ProductItemServiceTest` — `deveOrdenarPorPrecoDeEntregaCrescente`
   (verifica que o `Pageable` passado ao repositório tem `Sort` por
   `priceDelivery` ASC quando `sort=priceDelivery,asc`).
3. `ProductItemControllerTest` — `deveAceitarParametrosCityIdCategoriaESort`.
4. `ProductTypeControllerTest` — `deveExigirApenasAutenticacaoParaListarTipos`
   (CUSTOMER não recebe 403).

**Frontend:**
5. `supplier-list.component.spec.ts` —
   `deveFiltrarProdutosPelaCategoriaSelecionada`,
   `deveReenviarCityIdAoBuscarProdutosGlobais` (regressão da RN05 no
   viewMode 'products'), `deveOrdenarPorMenorPrecoAoAtivarToggle`.
6. `product-item.service.spec.ts` — `deveMontarQueryStringComCategoriaESort`.

## 7. Fora de Escopo

- Filtro por sub-categoria (`CategoryProduct`, ex.: "Gás 13kg" vs "Gás
  45kg") — o TCC especifica categoria no nível de `ProductType` ("ex:
  gás, água"); granularidade maior fica para trabalho futuro.
- Ordenação por outros critérios (avaliação, distância) — só "menor
  preço" está no critério de aceitação.
- Exibição simultânea de preço de entrega/retirada no card — isso é
  HU03.

## 8. Definição de Pronto

- [x] `PaginationUtil` aceita `sort` sem quebrar chamadas existentes.
- [x] Endpoint `by-supplier-open` filtra por `cityId` e `category`, ordena
      por `sort`.
- [x] `GET /api/product-types` acessível a `CUSTOMER`.
- [x] Abas de categoria e toggle de ordenação funcionais na visão de
      produtos de `supplier-list` (reimplementadas como chips circulares
      pelo F7, mesma lógica de filtro/ordenação).
- [x] Trocar para `viewMode = 'products'` mantém o filtro de cidade da
      HU01 (verificado manualmente).
- [x] Todos os testes da seção 6 passando.
- [x] Revisado e aprovado antes de HU03 começar.
