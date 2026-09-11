# HU11 — Autenticação via Token por E-mail

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU11 |
| Perfil | Todos os perfis autenticáveis (CLIENT, SUPPLIER, DELIVERER, ADMIN) |
| Prioridade MoSCoW | Should have — só entra se houver tempo após o Must-have completo |
| Dependências | Nenhuma dos scripts anteriores além da infraestrutura de e-mail já existente |
| Referência no TCC | Apêndice A.2 — HU11 |

## 2. História de Usuário

Como usuário, quero realizar login via código temporário (token)
enviado por e-mail, para evitar a necessidade de criação e memorização
de senhas fixas.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Solicitação de acesso**: Dado a tela de autenticação,
  Quando o usuário insere seu e-mail, Então o back-end gera um código
  numérico temporário (4 a 6 dígitos) e o envia para o endereço
  fornecido.
- **Cenário 2 — Validação da sessão**: Dado a tela de inserção de
  código, Quando o usuário informa o token recebido, Então o sistema
  valida a numeração e concede o acesso caso esteja correto e dentro do
  prazo de expiração.

## 4. Estado atual (achados)

- `PasswordResetToken` já é um bom modelo a seguir: token +
  `@OneToOne` com `User` + `expiryDate` + método `refreshToken`. A HU11
  usa uma entidade nova e paralela (`LoginToken`), não a mesma tabela,
  porque o requisito é um **código numérico de 4-6 dígitos** para digitar
  manualmente, diferente do token longo usado no link de redefinição de
  senha.
- `MailService.sendMail(to, subject, text)` já existe e é reaproveitado
  (mesmo padrão já usado na HU06 para notificar cancelamento).
- `AuthenticationController.login` usa `AuthenticationManager` +
  senha — a HU11 **não passa por aí**; emite o mesmo JWT
  (`tokenService.generateToken(user)`) mas sem checar senha, já que a
  prova de identidade é o código recebido por e-mail.
- `login.component.ts` já tem o padrão de sucesso de login pronto para
  reaproveitar: `localStorage.setItem('access_token', ...)` seguido de
  `getAndSaveMe()` (busca `/me`, salva o usuário, navega para
  `/inicio`).
- **Decisão de segurança (não explícita no TCC, mas necessária)**: o
  endpoint de solicitação de código **não deve revelar se o e-mail existe
  ou não** (evita enumeração de contas) — sempre responde 200, só envia
  o e-mail de fato se o usuário existir.
- **Decisão de segurança adicional**: limitar tentativas de validação por
  código (contador simples) para não deixar o código de 6 dígitos aberto
  a força bruta ilimitada dentro da janela de expiração.

## 5. Contrato Técnico

### 5.1 Migração `V5__login_tokens.sql`

```sql
CREATE TABLE login_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(6) NOT NULL,
  user_id VARCHAR(255) NOT NULL UNIQUE,
  expiry_date DATETIME NOT NULL,
  used BIT NOT NULL DEFAULT 0,
  attempts INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_login_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### 5.2 Backend

**Nova entidade `LoginToken.java`** (mesmo padrão de `PasswordResetToken`):
```java
@Entity
@Table(name = "login_tokens")
public class LoginToken {
    private static final int EXPIRATION_SECONDS = 5 * 60; // 5 minutos
    private static final int MAX_ATTEMPTS = 5;

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String code;

    @OneToOne @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private Date expiryDate;
    private boolean used = false;
    private int attempts = 0;

    public LoginToken(String code, User user) {
        this.code = code;
        this.user = user;
        this.expiryDate = new Date(System.currentTimeMillis() + EXPIRATION_SECONDS * 1000L);
    }

    public void refresh(String code) {
        this.code = code;
        this.expiryDate = new Date(System.currentTimeMillis() + EXPIRATION_SECONDS * 1000L);
        this.used = false;
        this.attempts = 0;
    }

    public boolean isExpiredOrExhausted() {
        return used || attempts >= MAX_ATTEMPTS || expiryDate.before(new Date());
    }
}
```

**Novos DTOs**: `RequestLoginTokenDTO { String email; }`,
`ValidateLoginTokenDTO { String email; String code; }`.

**`LoginTokenService.java`** (novo):
```java
public void requestToken(String email) {
    Optional<User> userOpt = userRepository.findByEmail(email);
    if (userOpt.isEmpty()) return; // não revela se o e-mail existe (§4)

    User user = userOpt.get();
    String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));

    LoginToken token = loginTokenRepository.findByUserId(user.getId())
        .map(existing -> { existing.refresh(code); return existing; })
        .orElse(new LoginToken(code, user));
    loginTokenRepository.save(token);

    mailService.sendMail(user.getEmail(), "Seu código de acesso H2Gás",
        "Seu código de acesso é: " + code + ". Válido por 5 minutos.");
}

public String validateAndLogin(String email, String code) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessRuleException("Código inválido."));
    LoginToken token = loginTokenRepository.findByUserId(user.getId())
        .orElseThrow(() -> new BusinessRuleException("Código inválido."));

    if (token.isExpiredOrExhausted()) {
        throw new BusinessRuleException("Código expirado ou esgotado. Solicite um novo.");
    }
    if (!token.getCode().equals(code)) {
        token.setAttempts(token.getAttempts() + 1);
        loginTokenRepository.save(token);
        throw new BusinessRuleException("Código inválido.");
    }

    token.setUsed(true);
    loginTokenRepository.save(token);
    return tokenService.generateToken(user);
}
```
(as duas mensagens de erro por e-mail inexistente e por código errado
são propositalmente iguais — "Código inválido." — para não vazar se o
e-mail está cadastrado.)

**`AuthenticationController.java`** — novos endpoints, `permitAll()`
(mesmo padrão de `/login`, `/register`):
```java
@PostMapping("/login/token/request")
public ResponseEntity<Void> requestLoginToken(@RequestBody RequestLoginTokenDTO dto) {
    loginTokenService.requestToken(dto.getEmail());
    return ResponseEntity.ok().build();
}

@PostMapping("/login/token/validate")
public ResponseEntity<LoginDTO> validateLoginToken(@RequestBody ValidateLoginTokenDTO dto) {
    String jwt = loginTokenService.validateAndLogin(dto.getEmail(), dto.getCode());
    return ResponseEntity.ok(new LoginDTO(jwt));
}
```

**`SecurityConfigurations.java`** — adicionar as duas novas rotas à lista
de `permitAll()`.

### 5.3 Frontend

**`login.component.ts`/`.html`** — nova aba "Entrar com código" (Figura
10a do TCC), com 2 passos:
1. Campo de e-mail → `POST /api/auth/login/token/request` → avança para
   o passo 2 independentemente da resposta (não revela existência do
   e-mail na UI também).
2. Campo de código (6 dígitos) → `POST /api/auth/login/token/validate` →
   em caso de sucesso, reaproveita exatamente o fluxo já existente:
   `localStorage.setItem('access_token', response.token)` seguido de
   `getAndSaveMe()`.

**`auth.service.ts`** — novos métodos `requestLoginToken(email)` e
`validateLoginToken(email, code)`.

## 6. Plano de Testes (TDD)

**Backend:**
1. `LoginTokenServiceTest` —
   `deveGerarCodigoDeSeisDigitosEEnviarPorEmail`,
   `naoDeveRevelarQuandoEmailNaoExiste` (verifica que não lança exceção e
   não chama `mailService` quando usuário não existe),
   `deveValidarCodigoCorretoDentroDoPrazoEEmitirJwt`,
   `deveRejeitarCodigoExpirado`,
   `deveRejeitarCodigoAposMaximoDeTentativas`,
   `deveRejeitarCodigoJaUtilizado`,
   `deveGerarNovoCodigoSubstituindoOAnteriorAoSolicitarNovamente`.
2. `AuthenticationControllerTest` —
   `deveRetornar200AoSolicitarCodigoMesmoParaEmailInexistente`,
   `deveRetornarJwtValidoAoValidarCodigoCorreto`.

**Frontend:**
3. `auth.service.spec.ts` — `deveSolicitarCodigoDeAcesso`,
   `deveValidarCodigoEReceberToken`.
4. `login.component.spec.ts` — `deveAvancarParaEtapaDeCodigoAposEnviarEmail`,
   `deveLogarComSucessoAoValidarCodigoCorreto`,
   `deveExibirErroGenericoParaCodigoInvalido`.

## 7. Fora de Escopo

- Autenticação via token por WhatsApp — HU12, canal diferente sobre a
  mesma ideia.
- Rate limiting por IP/dispositivo (só por tentativas no próprio token)
  — proteção adicional razoável, mas não exigida pelo critério de
  aceitação; pode ser um endurecimento futuro.
- Desabilitar login tradicional por senha — as duas formas coexistem.

## 8. Definição de Pronto

- [ ] Solicitação de código não revela se o e-mail existe.
- [ ] Código de 6 dígitos expira em 5 minutos e tem limite de tentativas.
- [ ] Validação bem-sucedida emite o mesmo JWT do login tradicional.
- [ ] Frontend reaproveita o fluxo de sucesso de login já existente.
- [ ] Todos os testes da seção 6 passando.
- [ ] Revisado e aprovado antes de HU12 começar.
