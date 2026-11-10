# F12 — Segurança das rotas por id e horário de Brasília

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F12 |
| Tipo | Fundação (correção de segurança e de dado) |
| Prioridade MoSCoW | Must have. Revisão do orientador em 2026-11-09, no ambiente do Coolify: "Estava logado com Gás Guará e abri /minha-loja/produtos/1/editar … o formulário carregou um produto que não é da Gás Guará" e "o sistema está mostrando os horários no fuso UTC (Greenwich), não no horário de Brasília". |
| Dependências | HU18 (`Clock` de Brasília), F6 (OpenAPI) |
| Branch | `fix/seguranca-e-fuso` |

## 2. Objetivo

(a) Nenhum usuário lê ou altera, pelo id, um recurso que não é dele:
produto de outra loja, pedido de outra pessoa ou loja, endereço de outro
usuário. (b) Todo horário gravado e exibido pelo sistema é o de Brasília.

## 3. Critérios de Aceitação (BDD)

### Item (a) — dono nas rotas por id

- **Cenário 1 — Produto de outra loja**: Dado que estou logado como a loja
  A, Quando abro `/minha-loja/produtos/{id}/editar` de um produto da loja B,
  Então a API responde 403 e a tela avisa "Este produto não é da sua loja" e
  volta para a lista de produtos.
- **Cenário 2 — Pedido pelo id**: `GET /api/orders/{id}` só responde ao
  cliente dono, à loja do pedido, ao entregador do pedido ou ao admin;
  qualquer outro recebe 403.
- **Cenário 3 — Status de pedido de outra loja**: a loja A não muda o status
  de um pedido da loja B (403); o admin continua podendo.
- **Cenário 4 — Pedidos de outro cliente**: um cliente que chama
  `GET /api/orders/by-customer/{id}` com o id de outro cliente recebe 403.
- **Cenário 5 — Endereços**: `GET /api/addresses` (todos) é só do admin;
  `GET /api/addresses/by-user/{id}` e `PUT /api/addresses/{id}` só respondem
  ao dono do endereço ou ao admin (403 para os demais).
- **Cenário 6 — Dono continua funcionando**: em todas as rotas acima, o dono
  segue recebendo 200 como hoje.

### Item (b) — horário de Brasília

- **Cenário 7 — Pedido novo**: Dado um pedido feito às 15:07 em Brasília,
  Então a lista da loja, Meus pedidos e o histórico mostram 15:07.
- **Cenário 8 — Pedidos antigos**: os pedidos já gravados em UTC passam a
  aparecer no horário de Brasília (sem pedido "no futuro").
- **Cenário 9 — Datas de negócio**: "entregues hoje", o mês do painel e a
  validade do token usam o dia e a hora de Brasília.

## 4. Estado atual (achados)

- `ProductItemService.findById` não confere o dono; `update` e `delete` já
  conferem (`ForbiddenResourceException`).
- `OrderController`: `GET /{id}` sem papel nem dono; `update-order-status`
  sem dono (só papel); `by-customer/{id}` sem comparar com o cliente logado.
  `cancel` e `assign-deliverer` já conferem a loja; `requireOwnStore` é o
  padrão de checagem no controller.
- `AddressController`: `GET /` e `GET /by-user/{id}` sem papel nem dono;
  `PUT /{id}` sem dono. `GET /by-user/{id}/all` já confere.
- A JVM do container roda em UTC (`backend/Dockerfile` sem `TZ`; JDBC com
  `serverTimezone=UTC`). `@CreationTimestamp` e `LocalDateTime.now()` gravam
  UTC; o JSON sai sem fuso e o navegador mostra +3 h. O `Clock` de Brasília
  (`ClockConfig`) só é usado no horário da loja.

## 5. Contrato Técnico

### 5.1 Item (a)

- `ProductItemService.findOwnById(id, actor)`: 404 se não existe; 403 se o
  produto não é da loja logada. `GET /api/product-items/{id}` passa a usá-lo.
- `OrderService.findByIdOrder(id, actor)`: permite cliente dono, loja do
  pedido, entregador atribuído (`deliverer.user`) e admin. `GET
  /api/orders/{id}` com `@RolesAllowed` dos quatro papéis.
- `OrderService.updateOrderStatus(id, status, actor)`: loja do pedido ou
  admin.
- `GET /api/orders/by-customer/{customerId}`: com papel CUSTOMER, só o próprio
  id.
- `AddressService.updateAddress(id, dto, actor)` e
  `findByUserIdAddress(userId, actor)`: dono ou admin. `GET /api/addresses`
  só ADMIN.
- Erro: `ForbiddenResourceException` → 403 (handler existente). OpenAPI:
  `@Operation` e `@ApiResponses` com 403 nas rotas tocadas.
- Frontend: `product-item-form`, ao receber 403 ou 404 no carregamento,
  mostra o aviso e navega para `/minha-loja/produtos`.

### 5.2 Item (b)

- `backend/Dockerfile`: `ENV TZ=America/Sao_Paulo` e
  `-Duser.timezone=America/Sao_Paulo`; JDBC com
  `serverTimezone=America/Sao_Paulo`.
- `OrderService` e os painéis passam a usar o `Clock` injetado no lugar de
  `LocalDateTime.now()`/`LocalDate.now()`/`YearMonth.now()`; `TokenService`
  usa `Instant.now(clock)` no lugar do offset fixo `-03:00`.
- Migração `V26__horarios_em_brasilia.sql`: subtrai 3 h dos horários já
  gravados (pedidos, recusas de entrega, avaliações e produtos). Antes de
  aplicá-la, conferir no banco local que os registros recentes estão +3 h.

## 6. Plano de Testes (TDD)

Item (a):
1. `AcessoDoDonoTest` (serviço, banco de teste):
   - `deveNegarProdutoDeOutraLoja` e `deveAbrirOProdutoDaPropriaLoja`;
   - `deveNegarPedidoAQuemNaoParticipa`, `deveMostrarPedidoAoClienteALojaEAoEntregador`;
   - `deveNegarStatusDePedidoDeOutraLoja`;
   - `deveNegarEnderecoDeOutroUsuario` (busca e edição).
2. `OrderControllerTest.deveNegarPedidosDeOutroCliente`.
3. `AddressControllerTest.deveNegarListaGeralDeEnderecosParaCliente`.
4. `product-item-form.component.spec.ts`: `deveVoltarParaAListaComProdutoDeOutraLoja`.

Item (b):
5. `OrderServiceTest.deveGravarOHorarioDeBrasilia` (Clock fixo).
6. Painel e "entregues hoje" com o dia do Clock.

## 7. Fora de Escopo

- Mudar o formato das datas na tela (é da `fix/pedidos-e-painel`).
- `GET /api/product-items` (lista geral): devolve dados públicos do catálogo.
- Rate limit, auditoria de acesso e renomear rotas da API (achados da F8).

## 8. Definição de Pronto

- [ ] Cenários 1–6 com teste verde; o dono segue com 200.
- [ ] Rotas tocadas com 403 documentado no Swagger.
- [ ] Cenários 7–9: V26 aplicada no MySQL local, horários conferidos com a
      hora real.
- [ ] `mvn test` só com o `contextLoads`; `ng test` sem falha; `ng build` ok.
- [ ] Teste manual do Igor em cada item.
- [ ] Nada do "Fora de Escopo".
