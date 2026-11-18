# F11 — Specs do frontend sem as 10 falhas antigas

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F11 |
| Tipo | Fundação (qualidade de testes) |
| Prioridade MoSCoW | Must have. Pedido do autor em 2026-11-03: "corrige essas 10 falhas ai em um fix na develop". |
| Dependências | — |

## 2. Objetivo

Hoje o `ng test` termina com 10 falhas. Elas aparecem desde o MVP e todas
as entregas foram medidas como "só as 10 antigas". O objetivo é que o
`ng test` termine com **0 falhas**, para que qualquer falha nova apareça na
hora.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1**: Dado o `ng test --watch=false`, Então o resultado é `0
  FAILED`.
- **Cenário 2**: As correções ficam só nos `*.spec.ts`; nenhum código de
  produção muda.

## 4. Estado atual (achados)

Todas as 10 são specs gerados pelo Angular CLI que não registram as
dependências dos componentes (`NullInjectorError`):

| Spec | Falta |
|---|---|
| `AbstractService`, `CityService`, `OrderItemService` | `HttpClient` |
| `ForgotPasswordComponent`, `RegisterComponent`, `RegisterSupplierComponent`, `DistributorManagementComponent` | `HttpClient` e outros |
| `ResetPasswordComponent` | `ActivatedRoute` |
| `DistribuitorFormComponent` | `ToastrService` |
| `AppComponent` (`should render title`) | `HttpClient`; além disso, espera o `<h1>Hello, frontend</h1>` do projeto inicial do CLI |

## 5. Contrato Técnico

- **Specs de serviço**: `provideHttpClient()` e `provideHttpClientTesting()`.
- **Specs de componente**:
  - os mesmos providers, mais `provideRouter([])` e
    `provideNoopAnimations()` quando o componente usar;
  - um `ToastrService` falso (`jasmine.createSpyObj`), no padrão dos specs
    que já passam.
- **`app.component.spec.ts`**:
  - os providers sobem para o `beforeEach`;
  - o `should render title` sai, porque confere um `<h1>` que o app nunca
    teve.

## 6. Plano de Testes

O próprio `ng test` é o teste: a meta é 0 falhas e o mesmo total de testes,
menos o `should render title` removido.

## 7. Fora de Escopo

- Apagar os componentes sem rota (`distributor-management` e
  `distribuitor-form`, já registrados como mortos na F8).
- Rodar o `ng test` na CI (lacuna conhecida no CLAUDE.md; fica como próximo
  passo sugerido).
- Ampliar os testes desses componentes além do `should create`.

## 8. Definição de Pronto

- [x] `ng test` com 0 falhas.
- [x] `ng build` ok.
- [x] Só `*.spec.ts` e este script no diff.

### Registro (2026-11-03)

- `ng test --watch=false --browsers=ChromeHeadless`: `TOTAL: 712 SUCCESS`, 0
  falhas. Antes eram 703 verdes e 10 falhas.
- A diferença: as 10 passaram a contar como verdes e o `should render title`
  saiu. O total ficou maior porque a develop também ganhou testes novos.
- Além do que a seção 4 previa, `Register`, `RegisterSupplier` e
  `DistribuitorForm` também precisavam do `provideNgxMask()`.
- `ng build` ok. Diff só em `*.spec.ts` e neste script.
