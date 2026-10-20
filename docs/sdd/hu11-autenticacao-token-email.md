# HU11 — Autenticação via Token por E-mail

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU11 |
| Perfil | Todos os perfis autenticáveis (CUSTOMER, SUPPLIER, DELIVERER, ADMIN) |
| Prioridade MoSCoW | Should have — primeiro script depois dos 10 Must-have |
| Dependências | `MailService` (já existe), `TokenService`/JWT, F6 (OpenAPI), F7 (tokens e padrão visual), HU07/HU09 (destino do login por perfil) |
| Referência no TCC | Apêndice A.2 — HU11; cap. 6.2/6.3; Figura 10 (p. 37) |

## 2. História de Usuário

Como usuário, quero realizar login via código temporário (token)
enviado por e-mail, para evitar a necessidade de criação e memorização
de senhas fixas.

## 3. Critérios de Aceitação (BDD)

> Revisado em 2026-10-20 sobre a `develop` com F1–F7 e HU01–HU10
> mergeadas. A versão anterior era de 2026-10-15 e previa a migração
> `V6__login_tokens.sql`; **V6 (cancelamento) e V7 (recusas de entrega)
> já existem**, então esta é a **`V8__login_tokens.sql`**. Decisões do
> autor nesta revisão: (a) a tela de entrada passa a ser a do **código**
> (Figura 10a), com o login por senha em `/login/senha` ("Entrar com
> login padrão"); (b) falha de SMTP **não derruba** a solicitação — o
> código é gravado, a resposta é 200 e, **fora de produção**, o código
> aparece no log para teste local; (c) entram as **três telas** da Figura
> 10: código (10a), login por senha (10b) e cadastro (10c).

- **Cenário 1 — Solicitação de acesso**: Dado a tela de autenticação,
  Quando o usuário insere seu e-mail, Então o back-end gera um código
  numérico temporário (4 a 6 dígitos) e o envia para o endereço
  fornecido.
- **Cenário 2 — Validação da sessão**: Dado a tela de inserção de
  código, Quando o usuário informa o token recebido, Então o sistema
  valida a numeração e concede o acesso caso esteja correto e dentro do
  prazo de expiração.

Detalhamento operacional (não cria cenário novo):

- **1a — Sem enumeração de contas**: a solicitação responde **200
  sempre**, e só envia e-mail se o usuário existir. A mensagem de erro
  na validação é a mesma ("Código inválido.") para e-mail inexistente e
  para código errado.
- **1b — Um código por usuário**: solicitar de novo **substitui** o
  código anterior, zerando tentativas e prazo.
- **2a — Prazo e tentativas**: 6 dígitos, 5 minutos, no máximo 5
  tentativas; código usado não vale de novo.
- **2b — Mesma sessão do login tradicional**: a validação emite o mesmo
  JWT (`TokenService.generateToken`) e o frontend reaproveita o fluxo de
  sucesso (salvar `access_token` → `/me` → destino por perfil).

## 4. Estado atual (achados)

- **Migração**: a última aplicada é a `V7__delivery_refusals.sql` (HU07).
  Esta história usa **`V8__login_tokens.sql`**.
- `PasswordResetToken` é o modelo a seguir (token + `@OneToOne User` +
  `expiryDate` + `refreshToken`), mas a HU11 usa entidade e tabela
  próprias (`LoginToken`), porque o requisito é um **código numérico
  curto** para digitar, não um token longo de link.
- `SecurityConfigurations` libera rota por rota (`/api/auth/login`,
  `/api/auth/register`, `/api/users/reset-password`…), sem curinga
  `/api/auth/**`: as duas rotas novas entram explicitamente.
- `AuthenticationController` já tem `@Tag("Autenticação")` e `@Operation`
  no `/login` (F7 documentou o 401). As rotas novas já nascem
  documentadas e públicas no spec.
- `MailService.sendMail(to, subject, text)` existe e é reaproveitado. O
  SMTP local é falso (`spring.mail.host=localhost`), então **o envio
  lança exceção** no ambiente de desenvolvimento — daí a decisão (b).
- `login.component.ts` já tem o fluxo de sucesso pronto para reaproveitar:
  limpa `access_token`/`user`/carrinho/contexto, salva o token e chama
  `getAndSaveMe()`, que decide o destino por perfil (`/entregas` para
  DELIVERER, `/dashboard` para SUPPLIER, `/inicio` para os demais).
- **Visual atual x Figura 10**: `login.component.html` e
  `register-user.component.html` ainda têm `<!DOCTYPE html>`/`<body>`
  dentro do template (vêm do MVP) e **gradiente com HEX literal**
  (`from-[#0E2068] to-[#3B82F6]`), o que contraria os tokens do F2.
  A Figura 10 pede topo `bg-primary` com logo e slogan e card branco
  `rounded-xl` sobreposto.
- `register.component` só repassa `userType: 'CUSTOMER'` para
  `register-user`; o cadastro de fornecedor é outra rota
  (`/register-supplier`). A Figura 10c pede **seletor de perfil no topo**
  da própria tela de cadastro.
- **ADMIN**: `POST /api/auth/register` bloqueia cadastro de ADMIN; o
  login por código **não** muda isso (um ADMIN existente pode entrar por
  código).

## 5. Contrato Técnico

### 5.1 Migração `V8__login_tokens.sql`

```sql
CREATE TABLE login_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(6) NOT NULL,
  user_id VARCHAR(255) NOT NULL UNIQUE,
  expiry_date DATETIME(6) NOT NULL,
  used BIT NOT NULL DEFAULT 0,
  attempts INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_login_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 5.2 Backend

- **`LoginToken`** (`model/entity`): `code`, `@OneToOne User`,
  `expiryDate`, `used`, `attempts`; constantes `EXPIRATION_SECONDS = 300`
  e `MAX_ATTEMPTS = 5`; `refresh(code)` (novo código, prazo novo, `used`
  e `attempts` zerados) e `isExpiredOrExhausted()`.
- **`LoginTokenRepository`**: `findByUserId(String)`.
- **DTOs** (com `@Schema`): `RequestLoginTokenDTO { @NotBlank @Email
  String email; }` e `ValidateLoginTokenDTO { @NotBlank @Email String
  email; @NotBlank @Pattern("\\d{6}") String code; }`.
- **`LoginTokenService`** (injeção por construtor, `@Transactional`):
  - `requestToken(email)`: usuário inexistente → **retorna sem erro**
    (1a); gera 6 dígitos com `SecureRandom`; reaproveita o registro do
    usuário (`refresh`) ou cria um novo; envia o e-mail; **falha de envio
    é registrada em log e não propaga** (decisão (b)), e o código é
    logado só quando o perfil não é de produção;
  - `validateAndLogin(email, code)`: sem usuário, sem token ou código
    errado → `BusinessRuleException("Código inválido.")` (incrementando
    `attempts` quando há token); expirado, usado ou tentativas esgotadas
    → `BusinessRuleException("Código expirado ou esgotado. Solicite um
    novo.")`; sucesso → marca `used` e devolve
    `tokenService.generateToken(user)`.
- **`AuthenticationController`**: `POST /api/auth/login/token/request`
  (200 sempre, 400 em e-mail inválido) e `POST
  /api/auth/login/token/validate` (200 com `LoginDTO`, 400 em código
  inválido/expirado), ambos `@Valid`, com `@Operation`/`@ApiResponses` e
  públicos no spec.
- **`SecurityConfigurations`**: as duas rotas em `permitAll()`.

### 5.3 Frontend

- `auth.service.ts`: `requestLoginToken(email)` e
  `validateLoginToken(email, code)`.
- **Rotas**: `/login` passa a ser a tela de **código** (Figura 10a) e
  `/login/senha` recebe o login por senha (Figura 10b). Quem chega em
  `/login` vê primeiro o código.
- **`login-token` (novo componente, Figura 10a)**: topo `bg-primary` com
  logo e slogan, card branco `rounded-xl` sobreposto, dois passos:
  1. escolha do canal — **E-mail** ativo e **Celular (via SMS)**
     desabilitado com a etiqueta "Em breve" (HU12) — e campo de e-mail;
     envia `request` e **avança sempre** (não revela se o e-mail existe);
  2. campo de código de 6 dígitos (`inputmode="numeric"`), botão
     "Validar", link "Reenviar código" e "Trocar e-mail"; sucesso
     reaproveita o fluxo existente (token → `/me` → destino por perfil);
     erro mostra a mensagem do backend pelo helper `backendMessage`.
  - links "Não tem uma conta? Cadastre-se" e "Entrar com login padrão".
- **`login` (Figura 10b)**: mesmo invólucro visual ("Bem-vindo de
  volta"), ícones nos campos, "Esqueceu a senha?" e "Entrar com código";
  **sem `<!DOCTYPE html>`/`<body>` no template e sem HEX literal** (usa
  `bg-primary`); a lógica de login não muda.
- **`register-user` (Figura 10c)**: mesmo invólucro, card único e
  **seletor de perfil no topo** (Cliente | Fornecedor) quando a tela é a
  de cadastro público, trocando o `userType` e os campos (CPF/CNPJ) sem
  recarregar; validações e payload atuais mantidos.

## 6. Plano de Testes (TDD)

**Backend:**
1. `LoginTokenServiceTest` —
   `deveGerarCodigoDeSeisDigitosEEnviarPorEmail`,
   `naoDeveRevelarQuandoEmailNaoExiste`,
   `deveSubstituirOCodigoAnteriorAoSolicitarNovamente`,
   `naoDeveFalharQuandoOEnvioDeEmailDerErro`,
   `deveValidarCodigoCorretoDentroDoPrazoEEmitirJwt`,
   `deveRejeitarCodigoExpirado`,
   `deveRejeitarCodigoJaUtilizado`,
   `deveContarTentativasEBloquearAposOMaximo`,
   `deveRejeitarComMensagemGenericaQuandoEmailNaoExiste`.
2. `AuthenticationControllerTest` —
   `deveRetornar200AoSolicitarCodigoMesmoParaEmailInexistente`,
   `deveRetornar400AoSolicitarComEmailInvalido`,
   `deveRetornarJwtAoValidarCodigoCorreto`,
   `deveRetornar400AoValidarCodigoComFormatoInvalido`.

**Frontend:**
3. `auth.service.spec.ts` — `deveSolicitarCodigoDeAcesso`,
   `deveValidarCodigoEReceberToken`.
4. `login-token.component.spec.ts` —
   `deveAvancarParaEtapaDeCodigoAposEnviarEmail`,
   `deveAvancarMesmoQuandoOEmailNaoExiste`,
   `deveExigirSeisDigitosParaValidar`,
   `deveLogarEIrParaODestinoDoPerfilAoValidarCodigo`,
   `deveExibirMensagemDoBackendParaCodigoInvalido`,
   `devePermitirReenviarOCodigoETrocarDeEmail`,
   `deveManterOCanalCelularDesabilitado`.
5. `login.component.spec.ts` — `deveLevarParaOLoginPorCodigo` (mantendo
   os specs atuais de login por senha verdes).
6. `register-user.component.spec.ts` —
   `deveAlternarEntreClienteEFornecedorNoSeletor`.

Regra herdada: specs antigos falhando no frontend não aumentam (19).

## 7. Fora de Escopo

- **Canal por SMS/WhatsApp** (HU12): a opção aparece desabilitada.
- Rate limiting por IP ou dispositivo (só o contador de tentativas do
  próprio código).
- Desabilitar o login por senha — as duas formas coexistem.
- "Lembrar de mim" da Figura 10b (não há sessão longa no backend).
- Expirar/limpar tokens antigos por rotina agendada.
- Cadastro de ADMIN e mudança nas regras de registro.
- "Meus Dados" e menu do usuário — fatia própria da F7.

## 8. Definição de Pronto

- [ ] Solicitação de código responde 200 mesmo para e-mail inexistente e
      não revela a existência da conta.
- [ ] Código de 6 dígitos expira em 5 minutos, aceita no máximo 5
      tentativas e não vale duas vezes; pedir outro substitui o anterior.
- [ ] Validação correta emite o mesmo JWT do login tradicional e leva ao
      destino do perfil.
- [ ] Falha de SMTP não derruba a solicitação; fora de produção o código
      é logado para teste local.
- [ ] Migração `V8` aplicada e rotas novas documentadas no Swagger como
      públicas.
- [ ] Telas de código, login por senha e cadastro no padrão da Figura 10,
      sem HEX literal novo e sem `<body>` nos templates.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` (≤ 19 falhas
      antigas).
- [ ] Commits pequenos por fatia (CLAUDE.md, seção Git).
- [ ] Testado de ponta a ponta (API e interface, com o código lido do log
      local).
- [ ] Revisado e aprovado. Encerra a fila de scripts planejada.
