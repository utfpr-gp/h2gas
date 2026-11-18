# HU17 — Frete por distância

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU17 |
| Perfil | Cliente, Visitante e Fornecedor |
| Prioridade MoSCoW | Must have — decisão do autor em 2026-10-26: "frete por produto não é válido; podemos calcular frete por km" |
| Dependências | F3 (Flyway), F6 (OpenAPI), HU04 (endereços do cliente), HU13 (CEP do visitante), HU16 (ordenação da vitrine), HU18 (horário), HU20 (preview do carrinho), **HU21 (preço único do produto, entra antes)** |
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
- **Cenário 8 — Produto com um preço só**: saiu para a **HU21** (revisão
  de 2026-10-27). Aqui o frete é somado ao preço único que ela criou.
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

> **Revisão de 2026-10-28** (substitui o contrato original e a revisão de
> 2026-10-27). Mudanças:
> - **HU21 entrou antes**: o produto já tem preço único (`V18`). As
>   migrações daqui são **V19 a V21**.
> - **F9 entrou antes**: a configuração de entrega fica na seção
>   "Funcionamento e entrega" da tela da loja e é salva pelo mesmo
>   `PUT /suppliers/mine/settings`. Não existe rota separada
>   `/delivery-settings`.
> - **Decisão do autor**: sem coordenada (CEP desconhecido ou API fora),
>   a entrega continua possível e o frete vira **só a taxa fixa**, com o
>   raio não checado e o aviso "distância não calculada". Isso troca o
>   comportamento do Cenário 11.
> - **Decisão do autor**: os endereços já cadastrados ganham coordenada
>   por uma **rotina na subida do backend**. Isso revoga o "backfill fora
>   de escopo".
> - **Visitante**: o CEP é geocodificado na listagem, com cache em
>   memória por CEP. É a única chamada fora da gravação de endereço.
> - Nomes reais: `catalog-sort-menu` (HU16), `pages/configuration` (F9),
>   `pages/order-resume`, `POST /api/cart/preview` (HU20).

## 5. Contrato Técnico

### 5.1 Dados (migrações)

- **`V19__supplier_delivery_settings.sql`**:
  ```sql
  ALTER TABLE suppliers
    ADD COLUMN offers_delivery     BIT(1) NOT NULL DEFAULT 1,
    ADD COLUMN offers_pickup       BIT(1) NOT NULL DEFAULT 1,
    ADD COLUMN delivery_radius_km  DECIMAL(6,2) NULL,
    ADD COLUMN delivery_base_fee   DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN delivery_fee_per_km DECIMAL(10,2) NOT NULL DEFAULT 0;
  ```
  Os padrões mantêm o comportamento de hoje: entrega e retirada, sem
  raio e sem frete. Raio nulo significa sem limite.
- **`V20__address_coordinates.sql`**: `latitude` e `longitude`
  `DECIMAL(10,7) NULL` em `addresses`.
- **`V21__order_delivery_fee.sql`**: `delivery_fee DECIMAL(10,2) NOT NULL
  DEFAULT 0` e `delivery_distance_km DECIMAL(6,2) NULL` em `orders`.
  Pedidos antigos ficam com frete 0.

### 5.2 Geocodificação

- `GeocodingService` (pacote `services`), devolvendo
  `Optional<Coordinates>`:
  - fonte principal: **AwesomeAPI** (`https://cep.awesomeapi.com.br/json/{cep}`,
    `lat`/`lng`);
  - alternativa: **BrasilAPI v2**
    (`GET https://brasilapi.com.br/api/cep/v2/{cep}`,
    `location.coordinates.latitude/longitude`), quando a primeira falha ou
    vem sem coordenada;
  - a ordem foi invertida no teste manual de 2026-10-28: a BrasilAPI
    devolvia o centro de Curitiba (-25,42778, -49,27306) para CEPs de
    bairros diferentes, o que zerava as distâncias;
  - URLs configuráveis por property, timeout de 3 s, sem chave de API;
  - falha, timeout ou CEP sem coordenada viram `Optional.empty()` com log
    `warn`, nunca exceção;
  - cache em memória por CEP, para não repetir a consulta do visitante.
- **Ao gravar um endereço** (`AddressService.createAddress` nas duas
  formas e `updateAddress`), as coordenadas são buscadas e gravadas.
  Quando o CEP muda na edição, são buscadas de novo. Se falhar, o
  endereço é salvo sem coordenada.
- **Rotina de subida** (`AddressCoordinatesBackfill`, `ApplicationRunner`,
  fora do profile `test`): em uma thread separada, busca as coordenadas
  dos endereços sem latitude, um por vez com uma pausa curta, sem
  bloquear o boot. Falha em um endereço não interrompe os outros.

### 5.3 Cálculo

- `DeliveryFeeCalculator` (classe de domínio em `model`, sem Spring):
  - `distanceKm(origem, destino)`: Haversine (raio 6371 km),
    arredondado a 1 casa;
  - `quote(loja, origem, destino)` devolve `DeliveryQuote { available,
    distanceKm, fee }`:
    - loja que não faz entrega: indisponível;
    - falta coordenada de um dos lados: disponível, `distanceKm = null`,
      `fee = taxa fixa`;
    - distância acima do raio, quando o raio existe: indisponível;
    - caso contrário: `fee = taxa fixa + valor por km × distância`,
      `HALF_UP` a 2 casas.
- Tudo no backend (RN01). O frontend exibe e não recalcula.

### 5.4 API

- **Configurações** (`GET`/`PUT /api/suppliers/mine/settings`,
  `SupplierSettingsDTO`): passa a ter também `offersDelivery`,
  `offersPickup`, `deliveryRadiusKm` (nulo ou de 0,1 a 100),
  `deliveryBaseFee` e `deliveryFeePerKm` (≥ 0). As duas modalidades
  desmarcadas → **400** "A loja precisa oferecer entrega ou retirada.";
  valor negativo → 400.
- **Vitrine** (`GET /api/suppliers/actives/open`):
  - parâmetros novos: `zipCode` (do endereço ativo do cliente ou do CEP
    do visitante), que diz de onde medir. Isso troca o `addressId` previsto
    antes: assim a rota pública não precisa checar o dono do endereço, e a
    conta bate com a do pedido, que usa as coordenadas do mesmo CEP;
    `deliveryMode=DELIVERY|PICKUP`; `freeDelivery=true`;
  - `sort=distance` entra junto dos valores da HU16;
  - sem `addressId` nem `zipCode`, ou sem coordenada de origem: a lista
    vem sem distância, e a ordem "distance" cai para o nome;
  - `StorefrontDTO` ganha `offersDelivery`, `offersPickup`, `distanceKm`
    (nulo sem coordenada), `deliveryFee` (nulo quando não entrega no
    endereço), `deliversHere` e `freeDelivery` (taxa fixa e por km
    zeradas);
  - filtros e ordem vão na consulta (JPQL com a fórmula de Haversine
    parametrizada pela origem), para a paginação continuar certa. Os
    valores do card saem do `DeliveryFeeCalculator`.
- **Pedido** (`POST /api/orders`): em modo entrega, o frete é calculado
  no fechamento com o endereço de entrega e gravado em `deliveryFee` e
  `deliveryDistanceKm`; o total passa a ser itens + frete. Loja que não
  entrega naquele endereço → **400** "Esta loja não entrega no endereço
  escolhido.". Em retirada, o frete é 0. `OrderResponseDTO` ganha
  `deliveryFee`, `deliveryDistanceKm` e `subtotal`.
- **Preview do carrinho** (`POST /api/cart/preview`): o corpo ganha
  `zipCode`, opcional. A resposta ganha:
  - `deliveryFee`: 0 em retirada; nulo sem CEP ou quando a loja não
    entrega ali;
  - `distanceKm`, `deliversHere` e `total`.
- OpenAPI atualizado em todas as rotas tocadas.

### 5.5 Frontend

- **Configurações** (seção `#funcionamento`):
  - interruptores "Faço entrega" e "Aceito retirada";
  - raio (km), taxa fixa e valor por km;
  - prévia do frete a 3 km;
  - aviso "distância em linha reta";
  - as duas modalidades desmarcadas mostram erro e bloqueiam o salvar.
- **`LocationContext`** guarda também o `zipCode`, do visitante e do
  cliente. A vitrine e o carrinho mandam esse CEP.
- **`supplier-card`**: a linha "2,3 km · Entrega R$ 7,90", "Entrega
  grátis", "Só retirada" (fora do raio ou sem entrega) ou "Taxa de
  entrega R$ 5,00 · distância não calculada".
- **Vitrine**: chips "Entregam aqui", "Retirada" e "Entrega grátis" ao
  lado da ordenação, que ganha "Mais perto".
- **Drawer do carrinho e resumo do pedido**: subtotal, frete e total como
  linhas separadas; em retirada, "Frete: grátis (retirada)".

## 6. Plano de Testes (TDD)

**Backend**
1. `DeliveryFeeCalculatorTest`:
   - `deveCalcularADistanciaEntreDoisPontos` (Curitiba centro → Batel,
     tolerância de 0,2 km);
   - `deveSomarTaxaFixaEValorPorKm`, `deveArredondarParaDuasCasas`;
   - `naoDeveEntregarForaDoRaio`, `deveTratarRaioNuloComoSemLimite`;
   - `deveCobrarSoATaxaFixaSemCoordenada`,
     `naoDeveEntregarQuandoALojaSoFazRetirada`.
2. `GeocodingServiceTest` (MockRestServiceServer):
   - `deveObterCoordenadasPeloCep`,
     `deveUsarAFonteAlternativaQuandoAPrimeiraFalha`;
   - `naoDeveQuebrarQuandoAsDuasFalham`, `deveUsarOCacheNoMesmoCep`.
3. `AddressServiceTest`:
   - `deveGravarAsCoordenadasAoSalvarEndereco`,
     `deveSalvarEnderecoSemCoordenadaQuandoAApiFalha`;
   - `deveBuscarDeNovoQuandoOCepMuda`.
4. `AddressCoordinatesBackfillTest`: `devePreencherSoOsEnderecosSemCoordenada`,
   `deveSeguirQuandoUmEnderecoFalha`.
5. `SupplierServiceTest` (vitrine):
   - `deveCalcularFreteParaOEnderecoDoCliente`,
     `deveMarcarSoRetiradaForaDoRaio`;
   - `deveFiltrarLojasQueEntregamNoEndereco`, `deveFiltrarLojasComRetirada`,
     `deveFiltrarEntregaGratis`;
   - `deveOrdenarPelaMaisProxima`, `deveColocarLojaSemCoordenadaNoFim`;
   - `deveListarSemFreteQuandoNaoHaEnderecoNemCep`,
     `deveUsarOCepDoVisitante`.
6. `SupplierServiceTest` (configurações):
   - `deveSalvarAsConfiguracoesDeEntrega`;
   - `naoDevePermitirDesmarcarAsDuasModalidades` (400),
     `naoDeveAceitarValoresNegativos` (400).
7. `OrderServiceTest`:
   - `deveSomarOFreteAoTotalDoPedido`, `deveGravarOFreteEADistanciaNoPedido`;
   - `naoDeveCobrarFreteNaRetirada`, `deveRecusarEnderecoForaDoRaio`;
   - `naoDeveRecalcularOFreteDePedidoJaFeito`.
8. `CartPreviewServiceTest`: `deveSomarOFreteNoTotal`,
   `naoDeveCobrarFreteNaRetirada`, `deveAvisarQueNaoEntregaNoEndereco`.
9. Migrações V19–V21: conferidas no MySQL local e registradas no DoD.

**Frontend**
10. `configuration.component.spec.ts`: `deveSalvarAEntregaNaSecaoDeFuncionamento`,
    `deveExigirAoMenosUmaModalidade`, `deveMostrarAPreviaDoFrete`.
11. `supplier-card.component.spec.ts`:
    - `deveMostrarDistanciaEFrete`, `deveMostrarSoRetiradaForaDoRaio`;
    - `deveMostrarSeloEntregaGratis`, `deveAvisarDistanciaNaoCalculada`.
12. `supplier-list.component.spec.ts`:
    - `deveFiltrarPorModalidade`, `deveFiltrarEntregaGratis`;
    - `deveOrdenarPorDistancia`, `deveMandarOEnderecoOuOCep`.
13. `location-context` e `visitor-cep-panel`: guardam o CEP.
14. `order-resume` e `menu` (drawer): `deveMostrarSubtotalFreteETotal`,
    `deveMostrarFreteZeroNaRetirada`, `deveMandarOEnderecoNoPreview`.

**E2E (registrado no DoD)**, em 390 e 1280 px:
- configurar a entrega de uma loja;
- vitrine com distância e frete;
- filtros de modalidade e entrega grátis;
- ordenar por "Mais perto";
- pedido em entrega com o frete somado e gravado;
- pedido em retirada sem frete;
- endereço sem coordenada cobrando só a taxa fixa.

## 7. Fora de Escopo

- Rota por ruas e tempo com trânsito. O cálculo é em linha reta, e a
  tela do fornecedor avisa isso.
- Faixas de frete por km, frete por peso ou volume, frete grátis acima de
  um valor de compra e cupom de frete.
- Pedido mínimo por loja.
- Geolocalização do navegador.
- Repasse do frete ao entregador e acerto financeiro.
- Histórico de mudança de tarifa.
- Converter pedidos antigos: eles ficam com `delivery_fee = 0`.
- Mostrar o frete no histórico e no painel do fornecedor.

## 8. Definição de Pronto

- [x] V19–V21 aplicadas no MySQL local; endereços antigos com coordenada
      depois da rotina de subida.
- [x] Fornecedor configura modalidades, raio e tarifa; configuração
      inválida recusada com a mensagem do contrato.
- [x] Coordenadas gravadas ao salvar endereço, com as duas fontes, o
      cache e a falha tratada sem quebrar o cadastro.
- [x] Vitrine com distância e frete do backend, filtros de modalidade e
      entrega grátis, e ordenação por distância.
- [x] Frete somado e **gravado** no pedido; retirada sem frete; preview do
      carrinho com subtotal, frete e total.
- [x] Rotas novas e alteradas documentadas no Swagger com os status reais.
- [x] Testes da seção 6 verdes; `mvn test` (491, só o `contextLoads` de
      ambiente) e `ng test` (só as 10 falhas antigas de provider) sem falha
      nova; `ng build` ok.
- [x] E2E em 390 e 1280 px registrado aqui com data.
- [x] Nada do "Fora de Escopo".

### Registro do teste manual (2026-10-28/29)

- **Migrações**: V19, V20 e V21 aplicadas no boot, depois de um dump do
  banco local.
- **Rotina de subida**: preencheu 18 endereços. O CEP genérico
  `80000-000` ficou sem coordenada, como esperado.
- **Achado**: a BrasilAPI devolvia o centro de Curitiba para CEPs de
  bairros diferentes. A AwesomeAPI passou a ser a fonte principal (5.2), as
  coordenadas foram zeradas e preenchidas de novo, e cada endereço ficou
  com a do próprio bairro.
- **Cadastro de cliente pela API** (Bigorrilho, CEP 80730-000): o endereço
  já nasce com coordenada.
- **Loja de teste "Plantão Gás e Água"** (R$ 4,00 + R$ 1,50/km, raio de 5
  km, a 4,1 km do cliente):
  - preview do carrinho e pedido de entrega com frete **R$ 10,15**, gravado
    em `orders.delivery_fee` e `delivery_distance_km = 4.1`, e total
    R$ 109,15;
  - retirada com frete 0;
  - com raio de 3 km o pedido foi recusado com 400 "Esta loja não entrega
    no endereço escolhido.";
  - as duas modalidades desmarcadas deram 400.
- **Vitrine** em 390 e 1280 px, como cliente:
  - linha "4,1 km · Entrega R$ 10,15" no card, e "Entrega grátis" nas
    lojas sem tarifa;
  - chips "Entregam aqui", "Retirada" e "Entrega grátis";
  - "Mais perto" com as abertas primeiro (HU18) e, dentro de cada grupo,
    pela distância.
- **Configurações** em 390 px, como fornecedor: interruptores, raio,
  tarifas e a prévia "A 3 km, o frete fica R$ 8,50".
- **Checkout** em 390 px:
  - etapa 2 com "Frete até este endereço: R$ 10,15 · 4,1 km em linha
    reta";
  - etapa 3 com subtotal R$ 99,00, frete R$ 10,15 e total R$ 109,15.
