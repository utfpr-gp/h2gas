# F6 — Documentação da API (Swagger/OpenAPI)

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F6 |
| Perfil | Infraestrutura (Fundação) — sem perfil de usuário final; consumido por desenvolvedores/QA/integradores |
| Prioridade MoSCoW | Should have (adição pontual autorizada fora da fila F1–F5, ver §3) |
| Dependências | F1 (Harness) |
| Referência no TCC | Nenhuma — o TCC não descreve documentação Swagger/OpenAPI como artefato macro; ver §3 |

## 2. Objetivo

Como desenvolvedor/integrador consumindo a API REST do H2Gás, quero uma
documentação interativa (Swagger UI) e uma especificação OpenAPI
(`/v3/api-docs`) geradas automaticamente a partir das 12 rotas já
existentes, para que eu possa explorar, testar e integrar os endpoints sem
depender de documentação manual desatualizada ou de leitura direta do
código-fonte dos controllers.

## 3. Divergência/decisão técnica não coberta pelo TCC

O TCC não menciona Swagger/OpenAPI na arquitetura macro nem nas HUs. Este
script existe porque o desenvolvedor solicitou explicitamente, nesta sessão,
a adição de documentação Swagger para as rotas **já existentes** — não é uma
HU do PRD nem estava na fila Must-have (F1–F5/HU01–HU10). Conforme F1 §5.3
("nenhuma história Should/Could é implementada antes de todas as Must-have,
salvo decisão explícita do orientador/Product Owner"), esta é exatamente
essa decisão explícita, registrada aqui para rastreabilidade. Não altera
nenhuma regra de negócio (RN01–RN05) nem cria rotas de domínio novas — é
puramente infraestrutura de documentação sobre rotas que já existem e já têm
comportamento definido.

## 4. Critérios de Aceitação (BDD)

- **Cenário 1 — Especificação OpenAPI acessível**: Dado o backend em
  execução, Quando qualquer cliente (autenticado ou não) faz
  `GET /v3/api-docs`, Então recebe `200 OK` com um JSON válido no formato
  OpenAPI 3, contendo entradas em `paths` para os 12 controllers existentes
  (`/api/addresses`, `/api/auth`, `/api/category-products`, `/api/cities`,
  `/api/orders`, `/api/order-items`, `/api/product-items`,
  `/api/product-types`, `/api/suppliers`, `/api/test`, `/api/upload`,
  `/api/users`).

- **Cenário 2 — Swagger UI acessível sem autenticação**: Dado o backend em
  execução, Quando um cliente sem token faz `GET /swagger-ui/index.html`,
  Então recebe `200 OK` (a UI é pública; ela apenas *descreve* as rotas —
  chamá-las autenticadas continua exigindo o botão "Authorize" com um JWT
  válido).

- **Cenário 3 — Autorização JWT na UI**: Dado o Swagger UI carregado, Quando
  o usuário clica em "Authorize" e informa `Bearer <token>`, Então
  requisições disparadas pela própria UI (via "Try it out") incluem o header
  `Authorization` e recebem o mesmo comportamento que uma chamada
  `curl`/Postman equivalente.

- **Cenário 4 — Nenhuma rota de negócio exposta indevidamente**: Dado que
  hoje `anyRequest().authenticated()` protege todas as rotas não listadas
  como `permitAll`, Quando os novos matchers de Swagger são adicionados,
  Então nenhuma rota de negócio (ex.: `GET /api/orders` sem token) passa a
  responder diferente de `401`/`403` — regressão coberta por teste
  automatizado (ver §6).

## 5. Contrato Técnico

### 5.1 Dependência (`backend/pom.xml`)

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.14</version>
</dependency>
```

(springdoc-openapi 2.x — linha compatível com Spring Boot 3.x/Java 17; a
linha 3.x do springdoc é para Spring Boot 4, não se aplica aqui. Versão
travada em **2.8.14** — não a mais recente da linha 2.x — porque 2.8.15 e
2.9.1 têm um bug conhecido [springdoc/springdoc-openapi#3210] em que
`AbstractSwaggerConfigurer.combinePatterns` gera um padrão de rota inválido
ao registrar os recursos estáticos do Swagger UI, quebrando a inicialização
do contexto Spring com `PatternParseException: No more pattern data allowed
after {*...} or ** pattern element`. Confirmado empiricamente nesta
implementação: 2.9.1 quebrava `OpenApiDocsTest`/`SecurityConfigurationsSwaggerRegressionTest`
na subida do contexto; 2.8.14 não.)

### 5.2 Novo pacote `com.h2gas.configuration.openapi`

**`OpenApiConfig.java`** — define metadados da API e o esquema de segurança
"Bearer JWT" usado pelo botão Authorize:

```java
package com.h2gas.configuration.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI h2gasOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("H2Gás API")
                        .description("Documentação das rotas REST já existentes do backend H2Gás")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
```

Nenhuma propriedade `springdoc.*` adicional é necessária para o caso de uso
mínimo; caminhos padrão (`/v3/api-docs`, `/swagger-ui/index.html`) são
usados. Como `server.servlet.context-path=/h2gas` já existe, as URLs finais
serão `http://<host>:<porta>/h2gas/v3/api-docs` e
`http://<host>:<porta>/h2gas/swagger-ui/index.html`.

### 5.3 Ajuste de segurança (`SecurityConfigurations.java`)

Adicionar, junto aos demais `permitAll` explícitos por método HTTP
(mantendo o padrão já usado no arquivo, que restringe por `HttpMethod` em
vez de usar coringa sem método):

```java
.requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**").permitAll()
.requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/swagger-ui.html").permitAll()
```

Inseridos antes de `.anyRequest().authenticated()`, no mesmo bloco dos
outros `permitAll`. Nenhum outro comportamento do filtro (CORS, CSRF,
`SecurityFilter` JWT) muda.

**Questão em aberto, não decidida aqui**: restringir Swagger UI a perfis
não-produtivos (`@Profile("!prod")` ou matcher condicional por
`spring.profiles.active`) não foi pedido pelo usuário; documentado como
opção a decidir depois, não assumida silenciosamente nem neste script nem
na implementação.

### 5.4 Anotações `@Tag` nos 12 controllers existentes (opcional, baixo risco)

Para agrupar visualmente as rotas no Swagger UI (sem alterar comportamento),
adicionar `@Tag(name = "...")` de `io.swagger.v3.oas.annotations.tags.Tag`
no topo de cada `@RestController` já existente (Endereços, Autenticação,
Categorias de Produto, Cidades, Pedidos, Itens de Pedido, Itens de Produto,
Tipos de Produto, Fornecedores, Teste/Infra, Upload, Usuários). É anotação
pura, não adiciona/remove/renomeia nenhuma rota. Não bloqueia a Definição de
Pronto — nenhum cenário BDD depende de `@Tag`.

## 6. Plano de Testes (TDD)

Novo pacote de teste: `backend/src/test/java/com/h2gas/configuration/openapi/`

1. **`OpenApiDocsTest`** (`@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)`
   + `@AutoConfigureMockMvc` — SEM `addFilters = false`, porque o objetivo é
   exercitar a `SecurityFilterChain` real — + `@ActiveProfiles("test")`):
   - `deveExporApiDocsComStatus200EJsonValido` — `GET /v3/api-docs` retorna
     `200`, `Content-Type` contendo `application/json`, corpo parseável
     contendo a chave `"paths"`.
   - `deveListarRotasDosControllersExistentesNoApiDocs` — corpo de
     `/v3/api-docs` contém as substrings `"/api/orders"`, `"/api/users"`,
     `"/api/auth"`.
   - `deveExporSwaggerUiSemAutenticacao` — `GET /swagger-ui/index.html`
     retorna `2xx` sem token.

2. **`SecurityConfigurationsSwaggerRegressionTest`** (mesmo setup):
   - `naoDeveExporRotaDeNegocioSemToken` — `GET /api/orders` sem header
     `Authorization` continua retornando `403`/`401` — regressão que prova
     que os novos `permitAll` não vazaram para rotas de negócio.
   - `deveManterLoginPublicoAposMudancaDeSeguranca` — `POST /api/auth/login`
     continua acessível sem token.

## 7. Fora de Escopo

- Qualquer rota de negócio nova — este script documenta as 12 rotas REST
  **já existentes**; não cria, renomeia nem remove nenhum endpoint.
- Anotações `@Operation`/`@ApiResponse`/`@Schema` detalhadas por endpoint
  (descrições de parâmetros, exemplos de payload, códigos de erro
  documentados um a um) — fica para uma iteração futura opcional; o Swagger
  UI já funciona e lista rotas/schemas de DTO automaticamente via
  introspecção do springdoc, mesmo sem essas anotações.
- Restringir Swagger UI/`/v3/api-docs` a perfis não-produtivos — sinalizado
  como questão em aberto (§5.3), não decidido nem implementado aqui.
- Alterações na esteira de CI (`.github/workflows/workflow.yml`) — lacuna
  conhecida (só roda testes de backend), não estendida por este script.
- Geração de cliente HTTP (TypeScript/Angular) a partir do `/v3/api-docs` —
  fora do pedido original.

## 8. Definição de Pronto

- [x] Dependência `springdoc-openapi-starter-webmvc-ui:2.9.1` adicionada ao
      `backend/pom.xml`.
- [x] `OpenApiConfig` criado em `com.h2gas.configuration.openapi` com
      `SecurityScheme` Bearer JWT configurado.
- [x] `SecurityConfigurations.java` com `permitAll` para `/v3/api-docs`,
      `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` (método GET),
      sem remover/reordenar os matchers existentes.
- [x] Todos os testes da seção 6 escritos primeiro (Red confirmado) e
      passando após a implementação (Green).
- [x] `GET /api/orders` sem token continua bloqueado (regressão verificada
      por teste automatizado, não só manualmente).
- [x] Testado manualmente (via `curl`, backend local em perfil `test`):
      `/h2gas/swagger-ui/index.html` → `200`; `/h2gas/v3/api-docs` → `200`
      com 41 rotas listadas cobrindo os 12 controllers; `/h2gas/api/orders`
      sem token → `403` (continua protegido); `/h2gas/api/auth/login`
      continua público (não retorna `403`). Verificação do botão Authorize
      na UI em navegador real fica pendente — ambiente de execução não tem
      browser disponível.
- [x] `mvn -f backend/pom.xml test` passa integralmente (suite existente +
      novos testes).
- [ ] Revisado e aprovado pelo desenvolvedor antes de considerar F6 "Done".
