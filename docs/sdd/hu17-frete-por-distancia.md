# HU17 — Frete por distância e preço único do produto

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU17 |
| Perfil | Cliente, Visitante e Fornecedor |
| Prioridade MoSCoW | Must have — decisão do autor em 2026-10-26: "frete por produto não é válido; podemos calcular frete por km" |
| Dependências | F3 (Flyway), F6 (OpenAPI), HU03 (transparência de preços), HU04 (endereços do cliente), HU10 (precificação logística), HU13 (CEP do visitante), HU16 (barra de filtros) |
| Referência no TCC | RN01 (cálculo no backend); revisão do modelo de preço da HU03/HU10 |

## 2. História de Usuário

Como cliente, quero ver quanto custa a entrega até o meu endereço antes
de escolher a loja, e como fornecedor quero cobrar o frete pela distância
real até o cliente (e não embutido no preço de cada produto), para que o
preço seja honesto para os dois lados.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — A loja configura a entrega**: Dado o fornecedor em
  "Minha loja", Quando define se faz entrega, se aceita retirada, o raio
  de entrega em km, a taxa fixa e o valor por km, Então os dados são
  salvos e passam a valer para o cálculo do frete. Desmarcar as duas
  modalidades é recusado ("A loja precisa oferecer entrega ou retirada.").
- **Cenário 2 — Coordenadas pelo CEP**: Dado um endereço de loja ou de
  cliente salvo com CEP, Quando o registro é gravado, Então o sistema
  busca a latitude e a longitude do CEP e as guarda junto do endereço,
  sem consultar a API de novo a cada listagem.
- **Cenário 3 — Frete na vitrine**: Dado um cliente com endereço ativo,
  Quando a vitrine carrega, Então cada card mostra a distância até a loja
  e o valor da entrega calculado pela distância — ambos vindos do
  backend (RN01), nunca calculados na tela.
- **Cenário 4 — Fora do raio**: Dado uma loja cujo raio não alcança o
  endereço do cliente, Quando a vitrine carrega, Então o card mostra
  "Só retirada" no lugar do frete, e a loja não aparece no filtro
  "Entregam aqui".
- **Cenário 5 — Filtrar por modalidade**: Dado a barra de filtros,
  Quando o cliente escolhe "Entregam aqui", Então vê só as lojas que
  fazem entrega e alcançam o endereço dele; Quando escolhe "Retirada",
  Então vê só as que aceitam retirada no local.
- **Cenário 6 — Entrega grátis**: Dado uma loja com taxa fixa e valor por
  km iguais a zero, Então o card mostra "Entrega grátis" e ela aparece no
  filtro de mesmo nome.
- **Cenário 7 — Ordenar por distância**: Quando o cliente escolhe "Mais
  perto", Então as lojas vêm da mais próxima para a mais distante; lojas
  sem coordenada ficam no fim.
- **Cenário 8 — Produto com um preço só**: Dado o fornecedor cadastrando
  ou editando um produto, Então informa **um** preço; não existem mais
  "preço de entrega" e "preço de retirada". No catálogo, o cliente vê
  esse preço e o frete separado.
- **Cenário 9 — Frete no checkout**: Dado um pedido em modo entrega,
  Quando o cliente confirma, Então o resumo mostra subtotal, frete e
  total; o frete é calculado no backend no fechamento e **gravado no
  pedido**, de modo que mudar a tarifa da loja depois não altera pedidos
  já feitos.
- **Cenário 10 — Retirada não cobra frete**: Dado um pedido em modo
  retirada, Então o frete é zero no resumo e no pedido gravado.
- **Cenário 11 — API de CEP indisponível**: Dado o serviço de
  coordenadas fora do ar, Quando um endereço é salvo, Então o cadastro é
  concluído normalmente e o endereço fica sem coordenada; a loja sem
  coordenada não aparece nos filtros de distância nem na ordenação por
  distância, e seu card não mostra frete — sem erro na tela.
- **Cenário 12 — Visitante sem endereço**: Dado um visitante que só
  informou o CEP (HU13), Então a distância e o frete são calculados a
  partir desse CEP; se não houver coordenada para ele, a vitrine carrega
  sem frete, como no Cenário 11.

## 4. Estado atual (achados)

- **O frete hoje é por produto e implícito**: `product_items` tem
  `price_delivery` e `price_pickup`, e o frete é a diferença entre os
  dois (`ProductItemMapper:25-30`, que também deriva `freeDelivery`
  quando são iguais). `OrderItemService:76-77` escolhe o preço unitário
  pelo modo do pedido. Não existe linha de frete em lugar nenhum.
- `Order` não tem campo de frete: só `totalPrice`. `OrderItem` tem
  `unitPrice` e `totalPrice`.
- `Supplier` não tem modalidade, raio, tarifa nem coordenadas.
  `DeliveryModeEnum (DELIVERY, PICKUP)` existe apenas em
  `Order.deliveryMode` — na prática, **toda loja aceita as duas
  modalidades** e nada registra isso.
- `Address` (`model/entity/Address.java`) tem `zipCode`, `city`,
  bairro e rua, mas **não tem latitude/longitude**.
- O projeto já consome ViaCEP no frontend (`AddressService.buscaViaCep`)
  e já casa cidade por código IBGE (`utils/city-match.ts`), mas o ViaCEP
  **não devolve coordenadas**.
- Última migração: `V15__supplier_banner.sql` → esta HU usa V16 em diante.
  Atenção: `suppliers` e `product_items` não têm `CREATE TABLE`
  versionado (vieram de `ddl-auto=update`) e o ambiente roda
  `ddl-auto=validate` — campo novo na entidade **exige** a migração.
  Os testes rodam em H2 com `create-drop` e Flyway desligado, então a
  migração não é exercitada por `mvn test`: conferir no MySQL local.
- Telas e DTOs que falam dos dois preços e precisarão acompanhar:
  `ProductItemDTO`, `CreateProductItemMapper:16-18`, `OrderService:172-173`,
  `product-card` (linhas `price-delivery` / `price-pickup`), o formulário
  de produto do fornecedor e o resumo do pedido.

## 5. Contrato Técnico

### 5.1 Dados (migrações)

- **`V16__supplier_delivery_settings.sql`**
  ```sql
  ALTER TABLE suppliers
    ADD COLUMN offers_delivery   BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN offers_pickup     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN delivery_radius_km   DECIMAL(6,2) NULL,
    ADD COLUMN delivery_base_fee    DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN delivery_fee_per_km  DECIMAL(10,2) NOT NULL DEFAULT 0;
  ```
  Os padrões preservam o comportamento atual (toda loja faz as duas
  coisas). `delivery_radius_km` nulo = sem limite de raio.
- **`V17__address_coordinates.sql`**: `latitude` e `longitude`
  `DECIMAL(10,7) NULL` em `addresses`.
- **`V18__order_delivery_fee.sql`**: `delivery_fee DECIMAL(10,2) NOT NULL
  DEFAULT 0` e `delivery_distance_km DECIMAL(6,2) NULL` em `orders`, para
  o pedido guardar o que foi de fato cobrado.
- **`V19__product_single_price.sql`**: `ALTER TABLE product_items ADD
  COLUMN price DECIMAL(10,2) NULL;` + `UPDATE product_items SET price =
  price_pickup;` + `MODIFY price ... NOT NULL`. As colunas antigas
  **continuam** nesta versão.
- **`V20__drop_product_double_price.sql`** (depois de o código novo estar
  no ar): remove `price_delivery` e `price_pickup`. Separar em duas
  migrações evita que o `validate` derrube o boot durante o deploy.

### 5.2 Geocodificação

- `GeocodingService` (pacote `services`), com interface e uma
  implementação HTTP:
  - fonte principal **BrasilAPI v2** —
    `GET https://brasilapi.com.br/api/cep/v2/{cep}`, que devolve
    `location.coordinates.latitude/longitude`;
  - alternativa **AwesomeAPI** — `https://cep.awesomeapi.com.br/json/{cep}`
    (campos `lat`/`lng`) — quando a primeira falha ou vem sem
    coordenada;
  - nenhuma das duas exige chave de API.
- Chamado ao **gravar** endereço de cliente e de loja; o resultado é
  persistido em `addresses.latitude/longitude`. Falha ou timeout (3 s)
  não propaga exceção: registra log em nível `warn` e segue sem
  coordenada (Cenário 11). Nunca é chamado durante a listagem.
- Endereços já existentes ficam sem coordenada até serem salvos de novo;
  o script não prevê backfill em massa (ver Fora de Escopo).

### 5.3 Cálculo

- `DeliveryFeeCalculator` (classe de domínio, sem Spring):
  - `distanceKm(origem, destino)` por **Haversine** (raio 6371 km),
    arredondado a 1 casa;
  - `fee(distanciaKm, loja)` = `delivery_base_fee + delivery_fee_per_km ×
    distanciaKm`, `RoundingMode.HALF_UP` a 2 casas;
  - fora de `delivery_radius_km` → `Optional.empty()` (a loja não entrega
    naquele endereço).
- Tudo no backend (RN01). O frontend exibe e não recalcula.

### 5.4 API

- `StorefrontDTO` ganha: `offersDelivery`, `offersPickup`, `distanceKm`
  (nulo quando falta coordenada), `deliveryFee` (nulo quando não entrega
  ali), `freeDelivery` (derivado: taxa fixa e por km zeradas).
- `GET /api/suppliers/actives/open` ganha:
  - `addressId` (cliente logado) **ou** `zipCode` (visitante) para saber
    de onde medir — sem nenhum dos dois, a resposta vem sem distância e
    sem frete;
  - `deliveryMode=DELIVERY|PICKUP` (filtro do Cenário 5);
  - `freeDelivery=true` (Cenário 6);
  - `sort=distance` somado aos valores da HU16 (Cenário 7).
- `PUT /api/suppliers/mine/delivery-settings` → `200`, corpo
  `DeliverySettingsDTO` (`offersDelivery`, `offersPickup`,
  `deliveryRadiusKm`, `deliveryBaseFee`, `deliveryFeePerKm`), só
  `SUPPLIER`. Nenhuma modalidade marcada → **400** "A loja precisa
  oferecer entrega ou retirada."; valores negativos → 400.
- `ProductItemDTO`: `price` passa a ser o preço único; saem
  `priceDelivery`, `pricePickup` e `shippingFee`. `freeDelivery` deixa de
  ser derivado do produto (passa a ser atributo da loja).
- Pedido: `OrderService` calcula o frete no fechamento, grava
  `delivery_fee` e `delivery_distance_km` no `Order`, e o total passa a
  ser `soma dos itens + frete`. Modo retirada → frete zero. O resumo
  (`OrderPreviewDTO`) ganha `deliveryFee` e `subtotal`.
- OpenAPI atualizado em todas as rotas tocadas, com os status reais.

### 5.5 Frontend

- "Minha loja": seção "Entrega" com os dois interruptores, raio, taxa
  fixa e valor por km, com a validação do Cenário 1.
- Formulário de produto: um único campo de preço.
- `supplier-card`: linha de distância + frete, selo "Entrega grátis" e
  selo "Só retirada" (Cenário 4), com os tokens F2.
- `catalog-filter-bar` (HU16) ganha os chips "Entregam aqui", "Retirada",
  "Entrega grátis" e a ordenação "Mais perto".
- `product-card`: um preço só; some a linha dupla entrega/retirada.
- Resumo do pedido: subtotal, frete e total como linhas distintas.

## 6. Plano de Testes (TDD)

**Backend**
1. `DeliveryFeeCalculatorTest`: `deveCalcularADistanciaEntreDoisPontos`
   (caso conhecido, tolerância de 0,2 km),
   `deveSomarTaxaFixaEValorPorKm`, `deveArredondarParaDuasCasas`,
   `naoDeveEntregarForaDoRaio`, `deveTratarRaioNuloComoSemLimite`.
2. `GeocodingServiceTest`: `deveObterCoordenadasPeloCep`,
   `deveUsarAFonteAlternativaQuandoAPrimeiraFalha`,
   `naoDeveQuebrarQuandoAsDuasFalham` (devolve vazio, não lança).
3. `AddressServiceTest`: `deveGravarAsCoordenadasAoSalvarEndereco`,
   `deveSalvarEnderecoSemCoordenadaQuandoAApiFalha`.
4. `SupplierServiceTest`: `deveCalcularFreteParaOEnderecoDoCliente`,
   `deveMarcarSoRetiradaForaDoRaio`,
   `deveFiltrarLojasQueEntregamNoEndereco`,
   `deveFiltrarLojasComRetirada`, `deveFiltrarEntregaGratis`,
   `deveOrdenarPelaMaisProxima`, `deveColocarLojaSemCoordenadaNoFim`,
   `deveListarSemFreteQuandoNaoHaEnderecoNemCep`.
5. `SupplierDeliverySettingsTest`: `deveSalvarAsConfiguracoesDeEntrega`,
   `naoDevePermitirDesmarcarAsDuasModalidades` (400),
   `naoDeveAceitarValoresNegativos` (400).
6. `OrderServiceTest`: `deveSomarOFreteAoTotalDoPedido`,
   `deveGravarOFreteEADistanciaNoPedido`,
   `naoDeveCobrarFreteNaRetirada`,
   `naoDeveRecalcularOFreteDePedidoJaFeito`.
7. `ProductItemServiceTest`: `deveUsarPrecoUnico`,
   `naoDeveAceitarPrecoDeEntregaSeparado`.
8. Migração: `FlywayMigrationTest` (ou verificação manual registrada no
   DoD) — `V19` preenche `price` a partir de `price_pickup` sem perder
   linha.

**Frontend**
9. `supplier-card.component.spec.ts`: `deveMostrarDistanciaEFrete`,
   `deveMostrarSoRetiradaForaDoRaio`, `deveMostrarSeloEntregaGratis`,
   `naoDeveMostrarFreteSemCoordenada`.
10. `catalog-filter-bar.component.spec.ts`: `deveEmitirOFiltroDeModalidade`,
    `deveEmitirEntregaGratis`, `deveEmitirOrdenacaoPorDistancia`.
11. `supplier-settings.component.spec.ts`: `deveSalvarAEntrega`,
    `deveExigirAoMenosUmaModalidade`, `deveMostrarOErroDoBackend`.
12. `product-item-form.component.spec.ts`: `deveTerUmUnicoCampoDePreco`.
13. `product-card.component.spec.ts`: `deveMostrarUmPrecoSo` (substitui
    os asserts de `price-delivery`/`price-pickup`).
14. `order-summary.component.spec.ts`: `deveMostrarSubtotalFreteETotal`,
    `deveMostrarFreteZeroNaRetirada`.

**E2E (registrado no DoD)**: em 390 e 1280 px — configurar a entrega de
uma loja; cadastrar produto com um preço; vitrine mostrando distância e
frete; filtros de modalidade e entrega grátis; ordenar por "Mais perto";
pedido em entrega com frete somado e gravado; pedido em retirada sem
frete; endereço sem coordenada não quebrando a tela.

## 7. Fora de Escopo

- Rota real por ruas e tempo com trânsito — o cálculo é **em linha reta**
  (Haversine), e essa limitação fica registrada na tela do fornecedor
  ("distância em linha reta").
- Backfill em massa das coordenadas dos endereços já cadastrados.
- Faixas de frete por km (ex.: até 3 km R$ 5, até 6 km R$ 8), frete por
  peso ou volume, frete grátis acima de um valor de compra, cupom de
  frete.
- Pedido mínimo por loja.
- Geolocalização do navegador (segue fora desde a HU13).
- Repasse do frete ao entregador e acerto financeiro.
- Histórico ou auditoria de mudança de tarifa.
- Converter pedidos antigos: eles ficam com `delivery_fee = 0`.

## 8. Definição de Pronto

- [ ] Migrações `V16`–`V20` aplicadas na ordem, com o produto migrado
      para preço único sem perda de dados.
- [ ] Fornecedor configura modalidade, raio e tarifa; recusa de
      configuração inválida com a mensagem do contrato.
- [ ] Coordenadas gravadas ao salvar endereço, com as duas fontes de CEP
      e falha tratada sem quebrar o cadastro.
- [ ] Vitrine mostrando distância e frete calculados no backend, com os
      filtros de modalidade e entrega grátis e a ordenação por distância.
- [ ] Produto com preço único em toda a cadeia (formulário, catálogo,
      carrinho, resumo).
- [ ] Frete somado ao total e **gravado** no pedido; retirada sem frete.
- [ ] Rotas novas e alteradas documentadas no Swagger com os status reais.
- [ ] Testes da seção 6 verdes; `mvn -f backend/pom.xml test` e
      `ng test` sem falha nova; `ng build` ok.
- [ ] E2E em 390 e 1280 px registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
