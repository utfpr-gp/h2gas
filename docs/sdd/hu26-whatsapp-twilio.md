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

### 5.5 Revisão: SMS como canal (2026-10-29)

No teste real, o WhatsApp do sandbox exige o `join`. O SMS da conta de teste
chega direto em número verificado. O autor pediu o SMS, com o remetente em
`TWILIO_SMS_FROM`. O que muda:

- **Nomes de canal neutro**, porque a mensagem pode ir por WhatsApp ou SMS:
  - `PhoneMessageSender` com `isConfigured(canal)` e `send(canal, phone,
    text)`;
  - canal `PhoneChannel { WHATSAPP, SMS }`;
  - implementação `TwilioPhoneMessageSender`;
  - no SMS, `From` = `TWILIO_SMS_FROM` e `To` = `+55{dígitos}`, sem o
    prefixo `whatsapp:`.
- **`V24__app_settings_phone_channel.sql`**:
  - renomeia as chaves para `PHONE_LOGIN_CODE` e
    `PHONE_ORDER_OUT_FOR_DELIVERY`;
  - cria `PHONE_USE_SMS` (desligado = WhatsApp, ligado = SMS).
- **Admin**:
  - `GET /api/admin/settings` → `{ whatsappConfigured, smsConfigured,
    settings }`;
  - ligar uma mensagem exige o canal escolhido configurado;
  - ligar `PHONE_USE_SMS` exige o SMS configurado;
  - na tela: "Enviar por SMS em vez do WhatsApp".
- **Login**:
  - o canal do pedido de código passa a ser `EMAIL` ou `PHONE`;
  - `GET /api/auth/login/channels` → `{ phone, phoneChannel }`;
  - o card da tela mostra "SMS" ou "WhatsApp" conforme o canal.
- As mensagens não mudam. O resto do contrato segue igual.

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

- [x] V23 aplicada, as duas mensagens desligadas.
- [x] Interruptores do admin funcionando e travados sem Twilio.
- [x] Código de login por WhatsApp, voltando para o e-mail quando não dá.
- [x] Aviso de saiu para entrega depois do commit, sem quebrar a rota.
- [x] Rotas documentadas no Swagger.
- [x] Testes da seção 6 verdes; `mvn test` e `ng test` sem falha nova;
      `ng build` ok.
- [ ] E2E registrado aqui com data.
- [x] Nada do "Fora de Escopo".

### Registro do E2E (2026-10-29), com tudo desligado

- **Variáveis**:
  - o container recebe `TWILIO_ACCOUNT_SID` (`AC…`, 34),
    `TWILIO_API_KEY_SID` (`SK…`, 34), `TWILIO_API_KEY_SECRET` (32) e
    `TWILIO_WHATSAPP_FROM` (`whatsapp:…`, 21);
  - `GET /Accounts/{AC}.json` com a API Key respondeu 200;
  - no `.env` há uma linha antiga `TWILIO_ACCOUNT_SID=KEY` antes da real.
    Hoje vence a última, mas o autor deve apagar a antiga.
- **Banco**: V23 aplicada no MySQL local, com `WHATSAPP_LOGIN_CODE` e
  `WHATSAPP_ORDER_OUT_FOR_DELIVERY` desligados.
- **Rotas**:
  - `GET /api/auth/login/channels` → `{"whatsapp":false}`;
  - Swagger com `/api/admin/settings`, `/api/admin/settings/{key}` e
    `/api/auth/login/channels`.
- **Pedido de código com `channel=WHATSAPP`**: 200, e o código foi pelo
  e-mail; nenhuma chamada ao Twilio no log.
- **Telas**:
  - admin em `/admin/configuracoes`: dois interruptores desligados e liberados
    (Twilio configurado), sem rolagem horizontal a 390 px;
  - login: WhatsApp desabilitado com "Em breve", E-mail escolhido.
- **Testes**: `mvn test` com 537 testes, só falha o `contextLoads`.
  `ng test` com 702 verdes, só as 10 falhas antigas. `ng build` ok.

**Pendente**: o envio real, uma mensagem de cada no sandbox. Depende do
celular do autor ter mandado o `join` e de uma conta de teste com esse
número. Por economia, as duas mensagens ficam desligadas até ele pedir.

### Tentativas de envio real (2026-10-29)

1. **WhatsApp** para 55 99158-3814: o número foi digitado sem o DDD 42.
   O Twilio recusou com o erro 572002 (destino não autorizado na conta de
   teste). O código foi por e-mail, como no Cenário 5.
2. **SMS** (revisão 5.5) para +55 42 99158-3814:
   - V24 aplicada;
   - `smsConfigured=true` e `whatsappConfigured=false` (o
     `TWILIO_WHATSAPP_FROM` saiu do `.env`);
   - com o canal em SMS e o código ligado, `/login/channels` →
     `{phone: true, phoneChannel: "SMS"}`;
   - o Twilio recusou com o erro 572006: "Trial accounts can only use
     predefined SMS templates". Na conta de teste, o SMS só aceita os
     modelos prontos do Twilio, não texto livre. O código foi por e-mail
     (Cenário 5).
3. No fim, as duas mensagens foram desligadas de novo e o celular da conta
   de teste voltou ao original. O canal ficou em SMS: voltar para o
   WhatsApp sem o `TWILIO_WHATSAPP_FROM` configurado é recusado (400), pela
   regra 5.5. Com as mensagens desligadas, nada é enviado.

**Pendente**: a entrega real depende de uma destas opções:
- tirar a conta do trial (upgrade), para o SMS aceitar texto livre;
- usar o WhatsApp do sandbox depois do `join` do celular, com o
  `TWILIO_WHATSAPP_FROM` de volta no `.env`.
