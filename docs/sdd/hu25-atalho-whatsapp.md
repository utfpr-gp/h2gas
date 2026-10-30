# HU25 — Atalho de WhatsApp com mensagem pronta durante o pedido

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU25 |
| Perfil | Cliente, Fornecedor e Entregador |
| Prioridade MoSCoW | Should have. É o Cenário 2 da "HU13: Pagamento via Pix e Atalho de Comunicação" do texto do TCC (A.2). Aqui a HU13 já é a vitrine pública, por isso o número novo. Pedido do autor em 2026-10-29: "pode começar o atalho do whatsapp". O Pix (Cenário 1) fica para outro script. |
| Dependências | HU06 (painel de pedidos recebidos), HU07 (tela da rota do entregador), HU08 (status ao vivo em Meus pedidos), F7 (padrão visual) |

## 2. História de Usuário

Como cliente, loja ou entregador, quero um botão que abra o WhatsApp da outra
parte do pedido com uma mensagem já escrita, para falar sobre o pedido sem
copiar número nem explicar de qual pedido se trata.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Cliente fala com a loja**: Dado um pedido meu em andamento
  (Pendente, Confirmado, Em preparo ou Saiu para entrega), Então o card em
  Meus pedidos tem "Falar com a loja". O botão abre o WhatsApp da loja com:
  "Olá, {loja}! Sobre o meu pedido #{código} no H2Gás: ".
- **Cenário 2 — Cliente fala com o entregador**: Dado um pedido meu "Saiu
  para entrega" com entregador, Então o card também tem "Falar com o
  entregador", com: "Olá, {primeiro nome do entregador}! Sou o cliente do
  pedido #{código} da {loja}.".
- **Cenário 3 — Loja fala com o cliente**: Dado um pedido em andamento no
  painel de pedidos recebidos, Então o bloco do cliente tem um botão de
  WhatsApp com: "Olá, {primeiro nome do cliente}! Aqui é a {loja}, sobre o
  seu pedido #{código} no H2Gás.".
- **Cenário 4 — Entregador fala com o cliente**: O botão de WhatsApp que já
  existe na tela da rota passa a levar a mensagem: "Olá, {primeiro nome do
  cliente}! Sou o entregador da {loja} e estou a caminho com o seu pedido
  #{código}.".
- **Cenário 5 — Pedido encerrado**: Pedido entregue ou cancelado não mostra
  os atalhos da loja e do cliente.
- **Cenário 6 — Telefone inválido**: Sem telefone, ou com menos de 10
  dígitos, o botão não aparece.
- **Cenário 7 — Privacidade do entregador**: O telefone do entregador só vem
  na resposta enquanto o pedido está "Saiu para entrega".

O código do pedido é o mesmo que a loja vê no painel: os 8 primeiros
caracteres do id.

## 4. Estado atual (achados)

- `OrderResponseDTO` traz `customerPhone` e `delivererName`, mas não traz o
  telefone da loja nem o do entregador.
- O telefone da loja é o `phoneNumber` do usuário fornecedor, editável em
  Configurações → Dados.
- `deliverer-route-detail` já tem `wa.me/55…`, sem mensagem.
- No painel da loja, o telefone do cliente é só um link `tel:`.
- O card em Meus pedidos mostra "Pedido #1, #2…", que é a posição na página,
  e não o código.

## 5. Contrato Técnico

### 5.1 Backend

- `OrderResponseDTO` ganha dois campos, com `@Schema`, preenchidos no
  `OrderMapper`:
  - `supplierPhone`: telefone da loja;
  - `delivererPhone`: telefone do entregador, só quando o status é
    `OUT_FOR_DELIVERY` e há entregador (nulo nos outros casos).
- Nenhuma rota nova; as rotas que devolvem pedido passam a trazer os campos
  na documentação do schema.

### 5.2 Frontend

- `utils/whatsapp-link.ts`:
  - `whatsappLink(phone, message)` devolve
    `https://wa.me/55{dígitos}?text={mensagem codificada}`, sem repetir o 55
    quando o número já começa com ele;
  - devolve `null` quando há menos de 10 dígitos;
  - `orderCode(id)` dá os 8 primeiros caracteres;
  - `firstName(nome)` dá o primeiro nome.
- `order-list` (cliente): os links `whatsapp-store` e `whatsapp-deliverer`
  (`target="_blank" rel="noopener"`, alvo de 44 px), com rótulos visíveis.
- `received-orders` (loja): `whatsapp-customer` ao lado do telefone, nos
  pedidos em andamento.
- `deliverer-route-detail`: o link existente passa a usar o utilitário, com a
  mensagem.
- Design tokens (F2), sem HEX novo, inclusive para o verde do WhatsApp: usar
  `btn-success`/`text-success`.

## 6. Plano de Testes (TDD)

1. `OrderMapperTest`:
   - `deveLevarOTelefoneDaLoja`;
   - `deveLevarOTelefoneDoEntregadorSoEmRota`.
2. `whatsapp-link.spec.ts`:
   - `deveMontarOLinkComDdiEMensagem`;
   - `naoDeveRepetirO55`;
   - `deveDevolverNuloComTelefoneInvalido`;
   - `deveDarOCodigoEOPrimeiroNome`.
3. `order-list.component.spec.ts`:
   - `deveMostrarFalarComALojaNoPedidoEmAndamento` (com a mensagem no href);
   - `deveMostrarFalarComOEntregadorSoEmRota`;
   - `naoDeveMostrarAtalhosNoPedidoEncerrado`.
4. `received-orders.component.spec.ts`: `deveAbrirOWhatsappDoClienteComMensagem`.
5. `deliverer-route-detail.component.spec.ts`: o teste do link passa a
   conferir a mensagem.

**E2E (registrado no DoD)**: conferir os hrefs nas três telas, com um pedido
em rota e outro entregue. Em 390 e 1280 px.

## 7. Fora de Escopo

- Envio automático de mensagens pela API do WhatsApp (Twilio etc.): o atalho
  só abre o app ou o WhatsApp Web.
- Chat dentro do H2Gás.
- Mudar o "Pedido #1" do card do cliente para o código.
- Pix com QR Code (Cenário 1 da HU13 do TCC).
- Telefone de WhatsApp separado do telefone de cadastro.

## 8. Definição de Pronto

- [x] Os campos novos estão no `OrderResponseDTO` e documentados no Swagger.
- [x] O telefone do entregador só vem com o pedido em rota.
- [x] Os atalhos com mensagem estão nas três telas.
- [x] Testes da seção 6 verdes; `mvn test` e `ng test` sem falha nova;
      `ng build` ok.
- [x] E2E registrado aqui com data.
- [x] Nada do "Fora de Escopo".

### Registro do E2E (2026-10-29)

Feito com a loja `gpv-centro@seed-teste.h2gas.local`, o cliente
`gpv-cliente-centro@…` e o entregador `gpv-entregador@…`, com um pedido
levado pela API até "Saiu para entrega" (#94c103db). Em Chrome headless a 390
e 1280 px.
- Pela API: o pedido em rota trouxe `supplierPhone` 42990000001 e
  `delivererPhone` 42990000021; o pedido entregue trouxe `delivererPhone`
  nulo.
- Cliente, em Meus pedidos:
  - "Falar com a loja" →
    `wa.me/5542990000001?text=Olá, Gás Guará Centro! Sobre o meu pedido
    #94c103db no H2Gás: `;
  - "Falar com o entregador" →
    `wa.me/5542990000021?text=Olá, João! Sou o cliente do pedido #94c103db
    da Gás Guará Centro.`;
  - o pedido entregue não mostrou atalhos.
- Loja, no painel: o botão WhatsApp do cliente →
  `wa.me/5542990000011?text=Olá, Cliente! Aqui é a Gás Guará Centro, sobre o
  seu pedido #94c103db no H2Gás.` O "Cliente" é o primeiro nome da conta de
  teste.
- Entregador, na rota: `wa.me/5542990000011?text=Olá, Cliente! Sou o
  entregador da Gás Guará Centro e estou a caminho com o seu pedido
  #94c103db.`
- Ajuste do teste manual: os botões saíam cinza com `btn-outline`; passaram
  a usar o token `success` (borda, texto e fundo claro).
- A 390 px não há rolagem horizontal (`scrollWidth` 390).
- `mvn test`: 515 testes, só falha o `contextLoads`. `ng test`: 703 verdes,
  só as 10 falhas antigas. `ng build` ok.
