# HU26 — WhatsApp pelo Twilio: código de login e aviso de saída para entrega, ligados pelo admin

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU26 |
| Perfil | Usuário (login), Cliente (aviso) e Administrador (configuração) |
| Prioridade MoSCoW | Should have. É a "HU12: Autenticação via Token por WhatsApp" do texto do TCC (A.2); aqui a HU12 já é o perfil do admin, por isso o número novo. Pedido do autor em 2026-10-29: só duas mensagens, para economizar (o código de login e "saiu para entrega"), tudo pronto mas **desligado**, com a escolha numa configuração do admin. |
| Dependências | HU11 (código de login por e-mail), HU07 (início da rota), HU08 (`OrderStatusChangedEvent`), HU25 (regra do número com DDI), F6 (OpenAPI) |
| Decisões | O aviso vai ao cliente quando o pedido sai para entrega. No login, o usuário digita o e-mail e escolhe o canal; o código vai para o celular cadastrado. |

## 2. História de Usuário

- Como usuário, quero receber o código de login no WhatsApp, um canal que uso
  todo dia.
- Como cliente, quero saber pelo WhatsApp que o meu pedido saiu para entrega.
- Como administrador, quero ligar e desligar cada uma dessas mensagens,
  porque cada envio tem custo.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Nasce desligado**: Dado o sistema recém-instalado, Então as
  duas mensagens ficam desligadas, e nada é enviado pelo Twilio.
- **Cenário 2 — Configuração do admin**: Dado o admin em
  `/admin/configuracoes`, Quando liga ou desliga "Código de login por
  WhatsApp" ou "Aviso de saiu para entrega", Então a mudança vale na hora.
  Sem as credenciais do Twilio, os interruptores ficam travados, com o
  aviso "Twilio não configurado", e a API recusa ligar (400).
- **Cenário 3 — Escolher o canal**: Com o login por WhatsApp ligado, a tela
  do código mostra "E-mail" e "WhatsApp". Com ele desligado, a tela fica
  como hoje.
- **Cenário 4 — Código no WhatsApp**: Quando escolho WhatsApp e informo o
  e-mail de uma conta com celular, Então recebo "Seu código de acesso H2Gás
  é 123456. Válido por 5 minutos." no celular cadastrado. A validação do
  código não muda.
- **Cenário 5 — Volta para o e-mail**: Se o login por WhatsApp está
  desligado, se a conta não tem celular válido ou se o envio falha, o código
  vai por e-mail.
- **Cenário 6 — Sem revelar conta**: A resposta do pedido de código é igual
  para conta existente ou não, em qualquer canal.
- **Cenário 7 — Saiu para entrega**: Com o aviso ligado, quando o entregador
  inicia a rota, o cliente recebe "Seu pedido #{código} da {loja} saiu para
  entrega com {primeiro nome do entregador}. Acompanhe no H2Gás.".
  A mensagem sai só depois de o status ser gravado.
- **Cenário 8 — Falha não quebra a entrega**: Se o Twilio falhar ou demorar,
  a rota começa do mesmo jeito; a falha fica registrada no log.

## 4. Estado atual (achados)

- O `LoginTokenService.sendCode` manda o código só por e-mail (HU11).
- O `OrderStatusChangedEvent(orderId, customerId, status)` é publicado pelo
  `OrderService`; o `OrderStatusSseListener` o escuta em `AFTER_COMMIT`.
- O `application.properties` já tem `twilio.account.sid`, `twilio.auth.token`
  e `twilio.verify.service.sid`, nenhum deles usado no código. O
  docker-compose passa o `.env` inteiro ao backend.
- Não existe configuração do sistema guardada no banco.
- Variáveis conferidas em 2026-10-29: o Account SID e a API Key (SK + secret)
  respondem 200 na API do Twilio; o remetente é o número do sandbox.

## 5. Contrato Técnico

### 5.1 Configuração

- **Variáveis de ambiente**:
  - `TWILIO_ACCOUNT_SID`;
  - `TWILIO_API_KEY_SID`;
  - `TWILIO_API_KEY_SECRET`;
  - `TWILIO_WHATSAPP_FROM` (ex.: `whatsapp:+14155238886`).
  - A autenticação é pela API Key. Falta de variável, ou o valor `KEY` /
    `fake`, deixa o Twilio "não configurado".
- **Migração `V23__app_settings.sql`**:
  - tabela `app_settings` (`setting_key` PK, `enabled` e `updated_at`);
  - linhas `WHATSAPP_LOGIN_CODE` e `WHATSAPP_ORDER_OUT_FOR_DELIVERY`, as duas
    com `false`.

### 5.2 API

- `GET /api/admin/settings` (ADMIN) → `{ whatsappConfigured, settings: [{ key,
  enabled }] }`.
- `PUT /api/admin/settings/{key}` (ADMIN) com o corpo `{ enabled }`:
  - 200 com a configuração;
  - 400 ao ligar sem Twilio;
  - 404 com chave desconhecida.
- `GET /api/auth/login/channels` (público) → `{ whatsapp }`: verdadeiro só com
  o interruptor ligado e o Twilio configurado.
- `POST /api/auth/login/token/request` ganha `channel` (`EMAIL`, o padrão, ou
  `WHATSAPP`). A resposta não muda.
- OpenAPI com `@Tag("Configurações")`, `@Operation`, `@ApiResponses` e
  `@Schema` nos DTOs.

### 5.3 Envio

- Interface `WhatsAppSender` com `isConfigured()` e `send(phone, text)`.
- `TwilioWhatsAppSender`:
  - POST form-urlencoded em `/2010-04-01/Accounts/{AC}/Messages.json`, com
    Basic `SK:secret`;
  - `To` = `whatsapp:+55{dígitos}`, sem repetir o 55 quando há mais de 11
    dígitos;
  - timeout de 3 s;
  - sem SDK nem dependência nova;
  - não loga secret, número completo nem o texto com o código.
- `WhatsAppOrderListener`: `AFTER_COMMIT` (com `fallbackExecution`), só para
  "Saiu para entrega" e só com o aviso ligado.

### 5.4 Frontend

- Tela `admin/configuracoes` com os dois interruptores (salvam ao trocar),
  o aviso de Twilio não configurado e a nota de custo por mensagem; link
  "Configurações" no menu do admin.
- `login-token`: escolha do canal só quando `GET /login/channels` diz
  `whatsapp: true`; o canal vai no pedido do código; o passo do código diz
  por onde ele foi enviado.

## 6. Plano de Testes (TDD)

1. `TwilioWhatsAppSenderTest` (`MockRestServiceServer`):
   - `deveMandarFormularioComAutenticacao`;
   - `deveMontarONumeroComDdi`;
   - `naoDeveEstarConfiguradoComPlaceholder`.
2. `AppSettingServiceTest`:
   - `deveNascerDesligado`;
   - `deveLigarEDesligar`;
   - `deveRecusarLigarSemTwilio`.
3. `AdminSettingsControllerTest`:
   - `deveListarParaAdmin`;
   - `deveNegarParaCliente`;
   - `deveAtualizar`.
4. `LoginTokenServiceTest`:
   - `deveMandarPeloWhatsappQuandoLigado`;
   - `deveUsarEmailQuandoDesligado`;
   - `deveUsarEmailSemCelular`;
   - `deveUsarEmailSeOWhatsappFalhar`.
5. `WhatsAppOrderListenerTest`:
   - `deveAvisarQuandoSaiParaEntrega`;
   - `naoDeveAvisarOutroStatus`;
   - `naoDeveAvisarDesligado`;
   - `naoDeveQuebrarSeOEnvioFalhar`.
6. `AuthenticationControllerTest`:
   - `deveInformarOsCanais`;
   - `deveAceitarCanalNoPedidoDeCodigo`.
7. Frontend:
   - `admin-settings.component.spec.ts`: lista, liga e desliga, travado sem
     Twilio;
   - `login-token.component.spec.ts`: a opção só aparece com WhatsApp ligado,
     e o canal vai no pedido;
   - `admin-settings.service.spec.ts`.

**E2E (registrado no DoD)**:
- com tudo desligado, nada é enviado;
- uma mensagem real de cada, no sandbox, com o celular do autor;
- os dois interruptores desligados de novo no fim.

## 7. Fora de Escopo

- Login digitando o celular em vez do e-mail.
- Outras mensagens: pedido novo para a loja, entregue, cancelado,
  avaliação.
- Templates aprovados pela Meta e remetente próprio (produção); aqui é o
  sandbox.
- Twilio Verify, SMS e a fila ou repetição de envio que falhou.
- Configurações do admin além desses dois interruptores.

## 8. Definição de Pronto

- [ ] V23 aplicada, as duas mensagens desligadas.
- [ ] Interruptores do admin funcionando e travados sem Twilio.
- [ ] Código de login por WhatsApp, voltando para o e-mail quando não dá.
- [ ] Aviso de saiu para entrega depois do commit, sem quebrar a rota.
- [ ] Rotas documentadas no Swagger.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` sem falha nova;
      `ng build` ok.
- [ ] E2E registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
