# F8 — URLs das telas em português, agrupadas por área

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F8 |
| Perfil | Infra (todos os perfis) |
| Prioridade MoSCoW | Should have — pedido do autor em 2026-10-27 ("nomes de rotas estão incorretos, precisamos revê-las") |
| Dependências | F7 (navbar e barras de abas), HU12 (área do admin), HU13 (`returnUrl`), HU19 (tela de categorias do admin) |
| Referência no TCC | Padronização da navegação (Figuras 8 e 11) |

## 2. Objetivo

Como usuário, quero que o endereço de cada tela esteja em português e diga
de quem é a tela, para entender onde estou; e como desenvolvedor, quero um
padrão único de URLs, para não misturar `register-city` com `meus-pedidos`.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Padrão único**: Dado qualquer tela, Então a URL está em
  português, em kebab-case: telas do admin em `admin/…`, do fornecedor em
  `minha-loja/…`, do cliente e públicas na raiz.
- **Cenário 2 — URL antiga continua funcionando**: Dado um favorito ou link
  antigo (ex.: `/register-city`, `/dashboard-fornecedor`,
  `/fornecedores/7/produtos`), Quando é aberto, Então o app redireciona para
  a URL nova correspondente, mantendo o `:id`.
- **Cenário 3 — Navegação toda atualizada**: Dado o menu, as barras de abas,
  os botões das telas e os guards, Então todos levam às URLs novas, sem
  passar pelo redirect.
- **Cenário 4 — Link do e-mail intacto**: Dado o e-mail de redefinir senha,
  Então o link `/resetar-senha` (montado no backend) continua igual.

## 4. Estado atual (achados)

- `frontend/src/app/app.routes.ts` tem 44 rotas, metade em inglês
  (`register`, `register-city`, `supplier-approval`, `product-items`,
  `dashboard`, `email-sent`…) e metade em português (`inicio`,
  `meus-pedidos`, `minha-loja`, `entregas`…), às vezes no mesmo path
  (`admin/usuarios`, `login/senha`).
- Nomes enganosos: `dashboard-fornecedor` abre "Pedidos recebidos", e o
  painel real é `dashboard`; `register-city` e `supplier-approval` são do
  admin mas ficam fora de `admin/`; `distributor/new` usa "distributor".
- Fixam paths do front, além do arquivo de rotas: `menu.component.ts` e
  `.html`, `bottom-tab-bar`, `deliverer-tab-bar`, `utils/home-route.ts`,
  `utils/return-url.ts`, `app.component.ts` (`hiddenMenuRoutes`), os guards
  (`auth`, `address-defined`, `supplier-store`) e os `routerLink` /
  `router.navigate` das telas; ~31 specs fixam rotas.
- Nenhum padrão de nomes de URL está documentado (CLAUDE.md, F1, F6).

## 5. Contrato Técnico

### 5.1 Tabela de URLs

| Hoje | Nova |
|---|---|
| `register` | `cadastro` |
| `register-supplier` | `cadastro/fornecedor` |
| `unauthorized` | `acesso-negado` |
| `email-sent` | `email-enviado` |
| `register-city` | `admin/cidades` |
| `supplier-approval` | `admin/fornecedores` |
| `distributor/new` | `admin/fornecedores/novo` |
| `product-items` | `minha-loja/produtos` |
| `product-items/new` | `minha-loja/produtos/novo` |
| `product-items/edit/:id` | `minha-loja/produtos/:id/editar` |
| `dashboard` | `minha-loja/painel` |
| `dashboard-fornecedor` | `minha-loja/pedidos` |
| `historico-pedidos` | `minha-loja/pedidos/historico` |
| `entregadores` | `minha-loja/entregadores` |
| `configuracao` | `minha-loja/configuracoes` |
| `fornecedores/:id/produtos` | `lojas/:id` |

Ficam como estão: `inicio`, `onde-voce-esta`, `login`, `login/senha`,
`minha-loja`, `meus-pedidos`, `meus-dados`, `meus-enderecos`,
`resumo-pedido`, `primeiro-acesso-endereco`, `entregas/**`, `admin`,
`admin/usuarios`, `admin/tipos`, `admin/categorias` (HU19),
`recuperar-senha`, `resetar-senha`, `checagem-email`,
`aguardando-aprovacao`, `sobre`, `contato`. Os redirects legados que já
existem (`login/token`, `painel-cliente`, `distributor/edit/:id`)
continuam.

### 5.2 Redirects
- Cada URL antiga da tabela vira `{ path: '<antiga>', redirectTo: '<nova>',
  pathMatch: 'full' }`, mantendo os parâmetros (`fornecedores/:id/produtos`
  → `lojas/:id`, `product-items/edit/:id` → `minha-loja/produtos/:id/editar`).
- Guards, `data.roles` e `allowVisitor` acompanham a rota nova.

### 5.3 Quem navega
- Todos os pontos listados na §4 passam a apontar para as URLs novas.

## 6. Plano de Testes (TDD)

1. `app.routes.spec.ts` (novo): `deveRedirecionarCadaUrlAntigaParaANova`
   (tabela inteira, com o `:id`), `deveManterOsPapeisNasRotasNovas`.
2. `menu.component.spec.ts`, `bottom-tab-bar`, `deliverer-tab-bar`:
   os links apontam para as URLs novas.
3. `home-route.spec.ts` e `return-url.spec.ts`: telas iniciais e retornos
   nas URLs novas.
4. Specs dos guards e das telas que fixam rotas: atualizados para as novas.

**E2E (registrado no DoD)**: navegar por todos os itens de menu de cada
perfil; abrir cinco URLs antigas e conferir o redirect.

## 7. Fora de Escopo

- Renomear rotas da **API** (decisão do autor: só as telas). O
  inventário achou problemas que ficam registrados para um script futuro:
  verbos no path (`update-active`, `update-times`, `update-order-status`),
  `/suppliers/actives/open` e `/product-items/by-supplier-open`, três
  estilos para "do usuário logado" (`/mine`, `/me`, `my-*`), ids do próprio
  usuário no path, 15 rotas sem chamador, e `GET /api/addresses` /
  `GET /api/users?id=` sem checagem de dono.
- Página 404 / rota curinga.
- Apagar o componente morto `distributor-management`.

## 8. Definição de Pronto

- [x] Todas as rotas da tabela nas URLs novas, com redirect das antigas.
- [x] Menu, abas, guards e telas sem nenhuma URL antiga.
- [x] Testes da seção 6 verdes; `ng test` sem falha nova (só as 10 antigas
      de provider); `ng build` ok.
- [x] E2E registrado aqui com data.
- [x] Nada do "Fora de Escopo".

### Registro do teste manual (2026-10-27)

- Abertas como visitante, em 390 px: `/fornecedores/seed-loja-24h/produtos`
  foi para `/lojas/seed-loja-24h` (com o id), e a loja abriu. `/register`
  foi para `/cadastro`, `/register-supplier` para `/cadastro/fornecedor`,
  `/unauthorized` para `/acesso-negado` e `/email-sent` para
  `/email-enviado`. `/dashboard` foi para `/minha-loja/painel` e dali, sem
  login, para `/login`, pelo guard.
- Achado no TDD: `safeReturnUrl` recusava só `/login` e `/register`. Passou
  a recusar também `/cadastro` (a URL nova); `/register` continua recusado.
- Os itens de menu de cada perfil logado ficaram cobertos pelos specs do
  menu e das barras (hrefs nas URLs novas). Não foram clicados no
  navegador.
