# HU27 — Notificação push no PWA (Android e desktop) e pedidos da loja em tempo real

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU27 |
| Perfil | Cliente, Fornecedor e Entregador |
| Prioridade MoSCoW | Should have. Pedido do autor em 2026-11-03: notificações para quem usa o PWA, com foco no Android, testáveis no Windows. É a alternativa gratuita ao SMS/WhatsApp da HU26 (sem custo por mensagem). |
| Dependências | F5 (PWA e service worker do Angular), HU08 (`OrderStatusChangedEvent`), HU06 (pedidos recebidos), HU07 (despacho para o entregador), F6 (OpenAPI) |
| Plataformas | Chrome no Android; Chrome ou Edge no Windows. **iOS fora** (decisão do autor). |

## 2. História de Usuário

Como cliente, loja ou entregador que usa o H2Gás no celular ou no PC, quero
receber uma notificação do sistema mesmo com o app fechado, para não
precisar ficar olhando a tela para saber do meu pedido.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Ativar**: Dado um usuário logado, num navegador com
  suporte a push e sem decisão sobre a permissão, Então a tela principal
  do perfil dele mostra o aviso "Ative as notificações", com o motivo:
  - cliente: em Meus pedidos;
  - loja: em Pedidos recebidos;
  - entregador: em Entregas.

  Quando ele toca em "Ativar" e permite, Então a assinatura é gravada no
  backend para ele, e o aviso some.
- **Cenário 2 — Não insistir**: Se ele negou a permissão, se tocou em
  "Agora não" ou se o navegador não tem suporte, o aviso não aparece (o
  "Agora não" fica guardado no aparelho).
- **Cenário 3 — Cliente**: Quando o pedido dele fica Confirmado, Saiu para
  entrega, Entregue ou Cancelado, Então ele recebe uma notificação, por
  exemplo "Pedido #94c103db saiu para entrega" / "João está a caminho com
  o seu pedido da Gás Guará Centro". Tocar abre Meus pedidos.
- **Cenário 4 — Loja**: Quando entra um pedido novo, Então a loja recebe
  "Novo pedido #94c103db · R$ 111,96". Tocar abre Pedidos recebidos.
- **Cenário 5 — Entregador**: Quando a loja atribui um pedido a ele, Então
  ele recebe "Nova entrega #94c103db da {loja}". Tocar abre a rota do
  pedido.
- **Cenário 6 — Só depois do commit**: A notificação sai só depois de o
  pedido ser gravado. Uma falha no envio nunca quebra o pedido.
- **Cenário 7 — Assinatura vencida**: Se o serviço de push responde que a
  assinatura não existe mais (404 ou 410), ela é apagada.
- **Cenário 8 — Sair da conta**: Ao sair, a assinatura deste aparelho é
  apagada; o próximo usuário do aparelho não recebe as notificações do
  anterior.
- **Cenário 9 — Só o dono**: Cada usuário só grava e apaga as próprias
  assinaturas (o dono vem do token, nunca do corpo).

- **Cenário 10 — Loja vê o pedido novo na hora (tempo real)**: Dado que a
  loja está em qualquer tela da área da loja, Quando um cliente faz um pedido,
  Então aparece o aviso "Novo pedido #c6423ce8" uma vez, e Pedidos recebidos
  (lista, contagens e "A receber"), o início da loja e o Painel se atualizam
  sem recarregar. Acrescentado em 2026-11-10, no teste do autor: "pro cliente
  funciona bem, atualiza em tempo real [...], mas pro fornecedor não atualiza
  no seu dashboard quando um pedido chega, nem em outras rotas".
- **Cenário 11 — Mudança de status na loja**: Quando o entregador inicia ou
  confirma a entrega (ou outra mudança passa pelo backend), as mesmas telas da
  loja se atualizam.
- **Cenário 12 — Canal só da própria loja**: o canal SSE `supplier:{id}` só
  aceita a loja do token (403 para os demais); o do cliente continua só dele.
- **Cenário 13 — Reserva**: se o SSE cair, a atualização periódica de 30 s de
  Pedidos recebidos continua.

## 4. Estado atual (achados)

- O service worker do Angular (`ngsw-worker.js`) só funciona no build de
  produção (`enabled: !isDevMode()`) e hoje só faz cache.
- Não existe Web Push: nem `SwPush`, nem permissão de notificação, nem
  chave VAPID.
- O SSE (HU08) só atualiza a tela Meus pedidos aberta, só para o cliente.
- `OrderService.publishStatusChange` publica `OrderStatusChangedEvent` nas
  mudanças de status. A criação do pedido e o despacho não publicam nada.
- O `apiUrl` de produção é relativo (`/h2gas-api/api`).

## 5. Contrato Técnico

### 5.1 Backend

- **Dependência** `nl.martijndwars:web-push` (e a `bcprov` que ela pede):
  - motivo: a criptografia do payload (aes128gcm) e a assinatura VAPID não
    valem ser escritas à mão;
  - variáveis `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY` e `VAPID_SUBJECT`
    (`mailto:`);
  - sem elas, o push fica "não configurado" e nada é enviado.
- **Migração `V27__push_subscriptions.sql`** (era V25; renumerada no rebase sobre a F12, porque a V26 já estava em produção e o Flyway recusa versão fora de ordem): tabela `push_subscriptions` com
  - `id`;
  - `user_id` (FK `users`);
  - `endpoint` (`VARCHAR(500)`, único);
  - `p256dh` e `auth`;
  - `created_at`.
- **Rotas** (`@Tag("Notificações")`), todas autenticadas e com OpenAPI:
  - `GET /api/push/public-key` → `{ publicKey, configured }`;
  - `POST /api/push/subscriptions` com o corpo `{ endpoint, keys: { p256dh,
    auth } }` → 204:
    - grava para o usuário do token;
    - o mesmo `endpoint` passa a ser do usuário atual (aparelho trocou de
      conta);
  - `DELETE /api/push/subscriptions` com o corpo `{ endpoint }` → 204:
    apaga só se for do usuário do token.
- **Envio**:
  - `WebPushSender` com `isConfigured()` e `send(subscription, payload)`;
  - `PushNotificationService.notifyUser(userId, title, body, url)`:
    - manda para todas as assinaturas do usuário;
    - 404 ou 410 apaga a assinatura;
    - outras falhas vão só para o log, sem endpoint nem chaves.
- **Eventos novos**:
  - `OrderPlacedEvent(orderId)`, no `createOrder`;
  - `DelivererAssignedEvent(orderId)`, no `assignDeliverer`.
- **`PushOrderListener`**, em `AFTER_COMMIT` com `fallbackExecution`:
  - `OrderStatusChangedEvent` (Confirmado, Saiu para entrega, Entregue ou
    Cancelado) → cliente;
  - `OrderPlacedEvent` → loja;
  - `DelivererAssignedEvent` → entregador.
- **Payload** no formato do `ngsw`: `{ notification: { title, body, icon,
  data: { onActionClick: { default: { operation:
  "navigateLastFocusedOrOpen", url } } } } }`.

### 5.2 Frontend

- `services/push.service.ts`, com o `SwPush`:
  - `canAsk()`: suportado, permissão `default` e sem "Agora não";
  - `enable()`: chave pública → `requestSubscription` → `POST`;
  - `disable()`: `DELETE` → `unsubscribe`;
  - `dismiss()`.
- Componente `push-opt-in` (o aviso), colocado em Meus pedidos, Pedidos
  recebidos e Entregas, com o texto do perfil.
- No logout (`auth.service`), `disable()` sem bloquear a saída.
- O clique na notificação fica com o próprio `ngsw`, pelo
  `onActionClick`.
- Design tokens (F2), sem HEX novo.

### 5.3 Teste local

- O push exige um contexto seguro; `localhost` serve.
- No PC, o build de produção é servido localmente, com o `/h2gas-api`
  repassado para o backend.
- No Android, o `localhost` chega por USB (port forwarding do
  `chrome://inspect`) ou por um túnel HTTPS.

### 5.4 Tempo real da loja (SSE, cenários 10–13)

- Achado: o SSE da HU08 só tinha o canal `customer:{id}`; Pedidos recebidos
  dependia do polling de 30 s e o início da loja e o Painel carregavam uma vez.
- `SseChannels.supplier(id)` e `isOwnChannel(channel, userId)` (aceita
  `customer:{id}` e `supplier:{id}` do próprio usuário); `SseController` usa o
  novo método, com o OpenAPI atualizado.
- `OrderStatusChangedEvent` ganha `supplierId` (construtor antigo mantido
  para quem só usa o cliente); `OrderStatusSseListener` publica
  `order-status-changed` também em `supplier:{id}`.
- `OrderPlacedSseListener` (`AFTER_COMMIT`): `order-placed` `{ orderId }` no
  canal da loja, a partir do `OrderPlacedEvent` deste script.
- Front: `SupplierLiveService` (um `EventSource` em `supplier:{id}` com o JWT
  em `?token=`, `changes$` e o aviso "Novo pedido #código"), conectado pelo
  `app.component` quando o usuário é SUPPLIER e desconectado no logout;
  `received-orders`, `supplier-home` e `supplier-dashboard` recarregam em
  `changes$`.

## 6. Plano de Testes (TDD)

1. `PushSubscriptionServiceTest`:
   - `deveGravarAAssinaturaDoUsuario`;
   - `devePassarOEndpointParaOUsuarioAtual`;
   - `deveApagarSoAPropria`.
2. `PushSubscriptionControllerTest`:
   - `deveGravarParaOUsuarioDoToken` (204);
   - `deveRecusarCorpoInvalido` (400);
   - `deveNegarSemToken`;
   - `deveDevolverAChavePublica`.
3. `PushNotificationServiceTest`:
   - `deveMandarParaTodasAsAssinaturas`;
   - `deveApagarAssinaturaVencida` (410);
   - `naoDeveMandarSemConfiguracao`;
   - `naoDeveQuebrarComFalha`.
4. `PushOrderListenerTest`:
   - `deveAvisarOClienteNosStatus`;
   - `naoDeveAvisarEmPreparo`;
   - `deveAvisarALojaDoPedidoNovo`;
   - `deveAvisarOEntregadorAtribuido`.
5. `OrderServiceTest`:
   - `devePublicarPedidoNovo`;
   - `devePublicarEntregadorAtribuido`.
6. `push.service.spec.ts`:
   - `deveAtivarEGravarAAssinatura`;
   - `naoDevePerguntarSemSuporteOuNegado`;
   - `deveDesativarNoLogout`;
   - `deveLembrarOAgoraNao`.
7. `push-opt-in.component.spec.ts`: mostra o texto do perfil, ativa e
   dispensa.

**E2E (registrado no DoD)**:
- no Windows (Chrome ou Edge) com o build de produção servido localmente:
  ativar, criar um pedido e ver a notificação da loja, levar o pedido até
  "saiu para entrega" e ver a do cliente, com o app fechado;
- no Android, se der, pelo port forwarding.

Tempo real da loja:
- `SseChannelsTest` (canal da própria loja, de outra loja, do cliente);
  `SseControllerTest` (loja assina o próprio canal; 403 no de outra);
  `OrderStatusSseListenerTest.devePublicarParaOClienteEParaALoja`;
  `OrderPlacedSseListenerTest` (avisa a loja; pedido sumido não quebra).
- `supplier-live.service.spec.ts` (canal e token, `changes$`, aviso uma vez,
  `disconnect`); `app.component.spec.ts` (conecta só para SUPPLIER);
  `received-orders`, `supplier-home`, `supplier-dashboard` recarregam em
  `changes$`.

## 7. Fora de Escopo

- iOS.
- Notificação para admin.
- Escolher por tipo quais notificações receber.
- Push em lote ou agendado.
- Repetir um envio que falhou.
- Trocar o SSE da HU08 (ele continua para a tela aberta).
- Ícone com contador (badge).

- Tempo real: som ou contador de pedidos novos no menu; SSE para o
  entregador; avisar a loja da recusa do entregador por SSE (o polling cobre).

## 8. Definição de Pronto

- [x] V27 aplicada no MySQL local (2026-11-10); chaves VAPID no ambiente.
- [ ] Notificação vista no celular Android (teste do autor pendente).
- [ ] Ativar, desativar e sair funcionando, só com as próprias assinaturas.
- [ ] Notificações do cliente, da loja e do entregador depois do commit.
- [ ] Assinatura vencida apagada.
- [ ] Loja vê pedido novo e mudanças de status na hora (cenários 10–13).
- [ ] Rotas documentadas no Swagger.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` sem falha nova;
      `ng build` ok.
- [ ] E2E no Windows registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
