# HU28 — Pedidos e painel da loja: receita, frete, telefone, pedido parado e imagens

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU28 |
| Perfil | Fornecedor (principal); Cliente (frete em Meus pedidos) |
| Prioridade MoSCoW | Must have. Revisão do orientador em 2026-11-09, no ambiente do Coolify. |
| Dependências | HU09 (painel), HU17 (frete por km), HU23 (contagem por status), F12 (horário de Brasília) |
| Branch | `fix/pedidos-e-painel` |

Itens, na ordem de execução (um por vez, validado pelo autor antes do commit):
(a) receita só do que foi entregue; (b) frete no pedido; (c) telefone
formatado; (d) data e pedido parado; (e) imagens em produção.

## 2. História de Usuário

Como fornecedor, quero ver no painel só o dinheiro que de fato entrou, o
frete de cada pedido, o telefone legível e os pedidos esquecidos em
destaque, para não perder venda nem me enganar com números.

## 3. Critérios de Aceitação (BDD)

### (a) Receita

- **Cenário 1 — Receita só de entregues**: Dado um mês com 2 pedidos
  pendentes, 1 em rota e 2 entregues, Então a "Receita" do painel soma só os 2
  entregues (o pagamento é na entrega), e a variação, o ticket médio e o
  gráfico de receita por dia seguem a mesma regra.
- **Cenário 2 — A receber**: "A receber" soma os pedidos em andamento (nem
  entregues nem cancelados). Em Pedidos recebidos, o card "A receber" vem do
  backend e não muda com o filtro de status (hoje, com "Todos", soma até
  cancelados).
- **Cenário 3 — Hub**: o card do Painel no hub mostra a receita do mês com o
  texto "recebido no mês".

### (b) Frete

- **Cenário 4**: o detalhe do pedido em Pedidos recebidos, o Histórico e Meus
  pedidos mostram Subtotal, Frete (com a distância) e Total. Entrega sem custo
  mostra "Grátis"; retirada não mostra a linha de frete.

### (c) Telefone

- **Cenário 5**: telefones aparecem como `(42) 99000-0011` (celular) ou
  `(42) 3622-0000` (fixo); os links `tel:` e WhatsApp seguem com os dígitos.

### (d) Data e pedido parado

- **Cenário 6 — Data**: na lista de Pedidos recebidos, pedido de outro dia
  mostra a data (`03/11 19:32`), não só a hora.
- **Cenário 7 — Parado por etapa**: pendente há mais de 15 min, confirmado ou
  em preparo há mais de 1 h, em rota há mais de 2 h, ou qualquer pedido em
  aberto de outro dia fica em destaque vermelho com "parado há 6 dias"; uma
  faixa no topo avisa "N pedidos parados".

### (e) Imagens

- **Cenário 8**: as fotos dos produtos de teste aparecem em produção (sobem
  para o Cloudinary) e o cadastro de produto grava a URL `https`.

## 4. Estado atual (achados)

- `SupplierDashboardService.buildDashboard`: `totalRevenue` soma todos os
  pedidos não cancelados do mês; `receivable` já é "não entregue";
  `averageTicket` = receita / pedidos contados; `revenueByDay` usa os mesmos
  pedidos.
- `received-orders.component.ts:getTotalRevenue()` soma no front os pedidos
  carregados que não estão "Entregue" (com o filtro "Todos" entram os
  cancelados). `GET /api/orders/mine/status-counts` já existe (HU23).
- `OrderResponseDTO` já traz `subtotal`, `deliveryFee`,
  `deliveryDistanceKm`; o modelo do front não tem os campos.
- Telefone é exibido cru em ~5 telas; não há pipe.
- Não há nada de pedido parado; a lista mostra só `HH:mm`.
- 40 produtos apontam para `/dev-fixtures/*.jpg`, que só existem no PC do
  autor; o cadastro grava `url` (http) do Cloudinary.

## 5. Contrato Técnico

### 5.1 (a) Receita

- `SupplierDashboardService`: `totalRevenue`, `revenueChangePercent`,
  `averageTicket` (receita / entregues) e `revenueByDay` passam a usar só os
  pedidos `DELIVERED` do mês. `totalOrders`, produtos, pagamento e categorias
  continuam com todos os não cancelados (são vendas, não receita).
  `receivable` sem mudança.
- `OrderStatusCountsDTO.receivable` (`BigDecimal`, `@Schema`): soma do
  `totalPrice` dos pedidos em andamento da loja, de todo o histórico.
- Front: "A receber" de Pedidos recebidos usa `statusCounts.receivable`; sai o
  `getTotalRevenue()`. Hub: "recebido no mês". Painel: o card "Receita"
  ganha "pedidos entregues" e o gráfico vira "Receita por dia".

### 5.2 a 5.5 — (b) a (e)

Detalhados no início de cada item, a partir do plano aprovado: campos de
frete no modelo do front; pipe `phone` em `utils/`; `OrderResponseDTO.stalled`
calculado no backend com o `Clock`; `secure_url` no cadastro e envio das 7
fotos ao Cloudinary com `UPDATE` no banco.

## 6. Plano de Testes (TDD)

(a)
1. `SupplierDashboardServiceTest.deveContarReceitaSoDeEntregues` (receita,
   variação, ticket e receita por dia).
2. `OrderServiceTest.deveSomarOAReceberDosPedidosEmAndamento`.
3. `received-orders.component.spec.ts`: `deveMostrarAReceberDoBackend`.
4. `supplier-home.component.spec.ts`: texto "recebido no mês".

(b) a (e): listados no início de cada item.

## 7. Fora de Escopo

- Mudar o que conta como "venda" nos gráficos de produtos, pagamento e
  categorias.
- Notificação de pedido parado (push/e-mail) — só o destaque na tela.
- Upload de imagem novo para logo/banner (já usam `secure_url`).

## 8. Definição de Pronto

- [ ] Itens (a)–(e) com testes verdes e validados pelo autor.
- [ ] Rotas/DTOs alterados documentados no Swagger.
- [ ] `mvn test` só com o `contextLoads`; `ng test` sem falha; `ng build` ok.
- [ ] Nada do "Fora de Escopo".
