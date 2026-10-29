# HU23 — Filtro de status dos pedidos recebidos, com todos os status e contagem

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU23 |
| Perfil | Fornecedor |
| Prioridade MoSCoW | Should have. Pedido do autor em 2026-10-29, sobre `/minha-loja/pedidos`: "preciso do status, todos também". Formato escolhido: botões com todos os status e a contagem de cada um. |
| Dependências | HU06 (painel de pedidos recebidos), F8 (URL `minha-loja/pedidos`), F6 (OpenAPI) |
| Revisa | HU06, no seletor de status, que tinha só Em andamento, Pendentes, Confirmados, Em preparo e Em rota |

## 2. História de Usuário

Como fornecedor, quero filtrar os pedidos por qualquer status, inclusive
entregues, cancelados e todos, e ver quantos há em cada um, para achar um
pedido sem sair do painel.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Todos os status**: Dado o painel, Então o filtro tem Em
  andamento, Todos, Pendentes, Confirmados, Em preparo, Em rota, Entregues e
  Cancelados.
- **Cenário 2 — Contagem do banco**: Cada botão mostra quantos pedidos da
  loja há naquele status, contados no backend, sem depender de quantos
  pedidos a lista carregou. "Em andamento" é a soma dos que não estão
  entregues nem cancelados; "Todos" é o total.
- **Cenário 3 — Filtrar**: Quando toco num botão, Então a lista mostra só
  aquele status, e o botão fica destacado (`aria-pressed`). "Em andamento"
  continua sendo o padrão.
- **Cenário 4 — Atualização**: A contagem se atualiza junto com a lista: na
  atualização periódica e depois de mudar o status de um pedido.
- **Cenário 5 — Só a própria loja**: A contagem vem do fornecedor do token.

## 4. Estado atual (achados)

- `received-orders` usa um `<select>` com cinco opções e carrega 10
  pedidos por vez (`findBySupplierId` / `findByExcludeStatus`, `size=10`).
- Não há contagem por status no backend além de
  `OrderRepository.countBySupplierIdAndStatus`, uma consulta por status.

## 5. Contrato Técnico

- **`GET /api/orders/mine/status-counts`**, só `SUPPLIER`:
  - o fornecedor vem do token;
  - resposta `OrderStatusCountsDTO { counts: { PENDING, CONFIRMED,
    IN_PREPARATION, OUT_FOR_DELIVERY, DELIVERED, CANCELED }, active, total }`;
  - status sem pedido vem com 0;
  - uma consulta agrupada (`GROUP BY o.status`).
- OpenAPI com 200 e 403.
- Frontend (`received-orders`):
  - o `<select>` vira uma faixa de botões em pílula com rótulo e
    contagem, que rola para o lado no celular sem rolar a página;
  - a lista com um status escolhido ou com "Todos" carrega até 50 pedidos,
    os mais recentes primeiro;
  - a contagem é recarregada junto com a lista;
  - `OrderService.getMyStatusCounts()` no frontend.

## 6. Plano de Testes (TDD)

1. `OrderServiceTest`: `deveContarOsPedidosPorStatusDaLoja` (status sem
   pedido = 0, ativos e total certos, pedidos de outra loja fora).
2. `OrderControllerTest`: `deveContarOsPedidosDoFornecedorDoToken`.
3. `received-orders.component.spec.ts`:
   - `deveMostrarTodosOsStatusComContagem`;
   - `deveFiltrarPeloStatusEscolhido` (Entregues e Todos chamam a rota
     certa);
   - `deveRecarregarAContagemJuntoComALista`.

**E2E (registrado no DoD)**: como loja de teste com pedidos em vários
status, conferir as contagens com o banco e filtrar por Entregues e Todos.
Em 390 e 1280 px.

## 7. Fora de Escopo

- Paginação ou rolagem infinita da lista (o limite fica em 50).
- Mudar o Histórico (`/minha-loja/pedidos/historico`).
- Busca por número do pedido ou pelo nome do cliente.

## 8. Definição de Pronto

- [ ] Botões com os oito filtros e a contagem do backend.
- [ ] Filtro por qualquer status, inclusive Entregues, Cancelados e Todos.
- [ ] Rota documentada no Swagger.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` sem falha nova;
      `ng build` ok.
- [ ] E2E registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
