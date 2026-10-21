# F7 — Aplicação da Nova UI/UX (Figuras 8 e 11 do TCC)

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F7 |
| Perfil | Infraestrutura (Fundação) — impacta a camada de apresentação de todas as telas do Cliente, Fornecedor, Entregador e Admin |
| Prioridade MoSCoW | Must have (consolidação visual da v2.0 sobre a base já migrada no F2) |
| Dependências | F2 (Design Tokens — Figura 8, já implementado), HU01 (`LocationContextService`), HU02 (lógica de filtro/ordenação de `supplier-list`/`product-list`, já implementada) |
| Referência no TCC | Cap. 6.2 "Identidade Visual" — Figura 8 (Guia de Estilos); Cap. 6 — Figura 11 (Interfaces móveis da rota principal do cliente) |

## 2. Objetivo

Como desenvolvedor/agente de IA implementando a v2.0, quero levar a
aplicação da aparência roxa/desktop-genérica herdada do MVP 1.0 para a
linguagem visual definida no TCC — tokens da Figura 8 em toda parte, e a
experiência mobile-first completa da Figura 11 na rota principal do
cliente — para que a v2.0 deixe de ser "o MVP recolorido" (resultado do
F2) e passe a ser a experiência desenhada no TCC.

### 2.1 Divergência de escopo entre telas e decisão

O TCC só tem mockup novo (Figura 11) para a rota principal do cliente
(catálogo de fornecedores/produtos — HU02). As ~26 telas restantes do
sistema (ver §5.7) não têm nenhuma figura correspondente. Duas decisões
foram confirmadas diretamente com o autor:

1. **HU02 é reconstrução mobile-first completa**, não só recolorização:
   `supplier-list.component` e `product-list.component` adotam bottom tab
   bar, chips de categoria circulares, cards de loja com rating e grid de
   produto 2 colunas, conforme Figura 11.
2. **As telas sem mockup são extrapoladas com critério documentado**:
   aplicam os tokens da Figura 8 e os padrões de componente já
   estabelecidos nas telas com mockup novo (cards arredondados, chips,
   botões full-width), mas sem inventar estrutura de layout que não
   existe em nenhuma referência. Cada grupo de tela tem sua decisão
   registrada em §5.7.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Chips de categoria (Figura 11)**: Dado o cliente na visão
  de produtos de `supplier-list` ou em `product-list`, Quando a lista de
  categorias é carregada via `ProductTypeService.findAll()`, Então cada
  categoria é exibida como um chip circular com ícone e, ao ser clicado,
  filtra a listagem exatamente como antes — sem alterar o contrato de
  `selectCategory()`/lógica de filtro já testada no HU02.

- **Cenário 2 — Bottom tab bar (Figura 11)**: Dado um cliente autenticado
  em qualquer tela, Quando a viewport é mobile (`md:hidden`), Então uma
  barra fixa inferior com 4 itens (Início, Categorias, Pedidos, Perfil) é
  exibida *junto* com a navbar superior (não a substitui), navegando para
  a rota correspondente (§5.3).

- **Cenário 3 — Adicionar direto ao carrinho**: Dado um produto exibido em
  grid, Quando o cliente clica em "+ Adicionar", Então o produto é
  adicionado ao carrinho com quantidade 1 imediatamente, sem modal — ajuste
  de quantidade continua possível no drawer do carrinho já existente.

- **Cenário 4 — Acesso via "Categorias" do bottom tab bar**: Dado o
  cliente na tela inicial, Quando toca em "Categorias", Então é navegado
  para `/inicio?view=products`, e `supplier-list` inicializa direto em
  `viewMode='products'`.

- **Cenário 5 — Rating sem dado real**: Dado que não existe endpoint de
  rating médio por fornecedor, Quando um card de "Loja Próxima" é
  renderizado, Então exibe um indicador neutro ("Sem avaliações ainda") —
  nunca um valor inventado.

- **Cenário 6 — Banners estáticos**: Dado que não existe modelo de dados
  nem admin para banners, Quando a visão de fornecedores é renderizada,
  Então até 2-3 banners com conteúdo **hardcoded no frontend** são
  exibidos — nenhuma API nova.

- **Cenário 7 — Extrapolação sem invenção estrutural**: Dado uma tela sem
  mockup no TCC, Quando F7 é aplicado, Então ela recebe tokens de
  cor/tipografia e componentes de baixo nível padronizados, mas mantém sua
  estrutura de layout atual — nenhuma bottom tab bar/grid mobile é
  adicionado sem evidência no TCC.

## 4. Estado atual (achados)

- `menu.component.html` usa `bg-[#0017bf]` (cor de Ação) na navbar; a
  Figura 11 usa a cor Primária (`#0E2068`) — correção de token mal
  aplicado, mesmo padrão do F2.
- Botão de endereço ativo tem `hidden md:flex` — invisível no mobile,
  apesar de a Figura 11 mostrá-lo central na navbar mobile.
- Não existe bottom tab bar em nenhuma rota. `MenuComponent` é renderizado
  globalmente em `app.component.html` — é o ponto de integração natural
  pra inserir a bottom tab bar.
- `ReviewRepository` só estende `JpaRepository`, sem query de rating
  médio; nenhum controller/service expõe isso — gap de backend, fora de
  escopo do F7.
- Não há agregação de "preço mínimo por fornecedor" nem de "categoria
  resumida" no DTO/model de `Supplier` — cards de loja não mostram esses
  dados nesta fatia.
- `product-list.component.html` filtra categoria com 3 botões hardcoded
  em vez de consumir `ProductTypeService.findAll()` como `supplier-list`
  já faz — inconsistente, será corrigido na Fatia 1.
- 28 componentes em `components/pages/`; 2 cobertos pela Fatia 1. Os ~26
  restantes agrupados em §5.7. `distributor-management` não está em
  `app.routes.ts` — código morto, não tocado.

## 5. Contrato Técnico

### 5.1 Correções de token transversais (antes da Fatia 1)
- `menu.component.html`: `bg-[#0017bf]` → `bg-primary` (nas 2 ocorrências);
  remover `hidden md:flex` do botão de endereço, manter truncamento.
- `pagination.component.html`: `bg-blue-500`/`hover:bg-blue-600` →
  `btn-primary`/`bg-primary`.

### 5.2 Fatia 1 — HU02 mobile-first (Figura 11): componentes novos

| Componente novo | Caminho | Reuso |
|---|---|---|
| `CategoryChipBarComponent` | `components/category-chip-bar/` | `supplier-list` (produtos) + `product-list` |
| `ProductCardComponent` | `components/product-card/` | `supplier-list` (produtos) + `product-list` |
| `SupplierCardComponent` | `components/supplier-card/` | `supplier-list` (fornecedores) |
| `BottomTabBarComponent` | `components/bottom-tab-bar/` | Global via `menu.component.html`, só pra `user?.userType === 'CUSTOMER'` |

### 5.3 Bottom Tab Bar — rotas confirmadas em `app.routes.ts`

| Ícone | Label | Rota | Query param |
|---|---|---|---|
| `home` | Início | `/inicio` | `{}` |
| `category` | Categorias | `/inicio` | `{ view: 'products' }` |
| `shopping_bag` | Pedidos | `/meus-pedidos` | `{}` |
| `person` | Perfil | `/painel-cliente` | `{}` |

Visível só `md:hidden`, coexiste com a navbar superior (não substitui).

### 5.4 Banners promocionais — decisão
Conteúdo estático/hardcoded no frontend (`readonly promoBanners = [...]`,
2–3 itens), sem CRUD nem backend novo. "Banners gerenciáveis via admin"
é Fora de Escopo (futura HU).

### 5.5 Rating médio — gap de backend registrado
Sem agregação real (confirmado via grep), `SupplierCardComponent` mostra
estado neutro fixo ("☆ Sem avaliações ainda") até existir uma HU de
avaliações que implemente o agregado.

### 5.6 "Lojas Próximas" — conteúdo do card
Nome, ícone, endereço resumido, horário, `averageDeliveryTime`,
placeholder de rating, botão "Ver Produtos". **Omitidos** nesta fatia:
categoria resumida e preço de entrega mínimo (exigiriam novo DTO/query de
backend — fora de escopo de um script de UI).

### 5.7 Extrapolação por grupo (telas sem mockup)

| Grupo | Telas | Decisão |
|---|---|---|
| A — Autenticação/Onboarding | `login`, `register`, `register-supplier`, `forgot-password`, `check-email`, `reset-password`, `email-sent`, `first-access-address`, `unauthorized` | Só tokens + padronização de card. Sem bottom tab bar. |
| B — Área do Cliente (fora da rota principal) | `customer-panel`, `order-list`, `order-resume` | Tokens + idioma visual da Fatia 1 (cards arredondados, badges). Recebe bottom tab bar (são destinos dela). |
| C — Painel do Fornecedor | `received-orders`, `order-history`, `awaiting-approval`, `distribuitor-form`, `category-products`, `product-item`, `product-item-form`, `configuration` | Só tokens. Mantém layout desktop denso — sem reconstrução mobile-first (TCC não mostra tela de fornecedor na Figura 11). |
| D — Administração | `supplier-approval`, `city-register` | Mesmo tratamento do Grupo C. |
| E — Institucional/Landing | `about`, `contact`, `initial-screen` | Tokens + idioma visual de card/banner extrapolado da Figura 11 (pré-login, sem bottom tab bar). |
| *(código morto)* | `distributor-management` | Não tocado — fora de `app.routes.ts`. |

Grupos A–E não recebem teste automatizado novo (puro CSS/classe) —
verificação por grep + checagem visual manual, como no F2.

### 5.8 Cobertura por HU (HU01–HU11)

Decisão do autor: o F7 vale para todas as HUs. Telas de HUs já
implementadas recebem o F7 em fatias; HUs ainda não implementadas recebem
o F7 **dentro da própria implementação** — a Definição de Pronto de cada
uma passa a incluir "telas no padrão F7" (tokens do F2, sem HEX novo,
idioma visual das Fatias 1 e 2, espaço para a bottom tab bar no mobile
quando for tela do cliente).

| HU | Telas | Quando recebe o F7 | Referência visual |
|---|---|---|---|
| HU01 | `first-access-address`, endereço ativo na navbar | Fatia 2 | Grupo A + navbar da Figura 11 |
| HU02 | `supplier-list`, `product-list` | Fatia 1 (feita) | Figura 11 |
| HU03 | `order-resume` (preços), cards de produto | Na implementação da HU03 | Figura 12a |
| HU04 | `manage-addresses` (nova), `address-form` (novo), seletor de endereço do `order-resume`, `customer-panel` | Fatia 2 | Figura 3c (Aiqfome, citada no TCC como inspiração) + Figura 12a |
| HU05 | `order-list` (Meus Pedidos, recompra) | Na implementação da HU05 | Figura 12b |
| HU06 | `received-orders`, `orders-widget` | Na implementação da HU06 | Figura 14 |
| HU07 | módulo do entregador, modal de atribuição | Na implementação da HU07 | Figuras 15 e 17 |
| HU08 | notificações em tempo real nas telas existentes | Na implementação da HU08 | idioma das fatias anteriores |
| HU09 | dashboard do fornecedor | Na implementação da HU09 | Figura 13 |
| HU10 | `product-item`, `product-item-form`, `category-products` | Na implementação da HU10 | Figura 16 |
| HU11 | telas de autenticação | Na implementação da HU11 | Figura 10 |

Telas que nenhuma HU toca (ex.: `about`, `contact`, `supplier-approval`,
`city-register`) seguem a §5.7 numa fatia final de varredura.

### 5.9 Fatia 2 — telas de endereço e Meus Dados

- `manage-addresses` e `address-form` nascem no padrão. O TCC não tem
  mockup próprio do H2Gás para "Meus Endereços"; a referência é a Figura
  3c (Aiqfome): endereço ativo em destaque, "outros endereços" abaixo,
  rótulo (Casa/Trabalho) e "+ Adicionar endereço".
- `first-access-address` (Grupo A): gradiente com HEX
  (`#0E2068 → #3B82F6`) troca por tokens.
- `customer-panel` (Grupo B): template reescrito no padrão; o
  `customer-panel.component.css` (317 linhas que reimplementavam classes
  do Tailwind, com blocos duplicados e uma regra `body` sem efeito dentro
  de CSS de componente) é apagado; o modal de endereço sai (ver HU04) e
  entra um atalho para "Meus endereços".
- `order-resume`: só o bloco de seleção de endereço (HU04); o resto da
  tela fica para a HU03.

### 5.10 Fatia 3 — feedback (toasts), Meus Dados e menu do usuário

Pedido do autor em 2026-10-15, depois de testar a HU05: toasts com
visual padrão do `ngx-toastr` destoando do F7, toast de "sucesso" a cada
busca, e "Meus Dados"/ícone de usuário pobres. Ordem: **depois da HU11**.

**Toasts (global)** — revisado em 2026-10-18, no teste da HU08 (dois
"Pedidos buscados com sucesso" empilhados na tela do cliente). Decisões
do autor: **um toast por vez** (o novo substitui o atual, nunca empilha),
**topo central sempre** (celular e desktop — não briga com a tab bar, a
barra do checkout nem a barra fixa do detalhe do entregador), **sucesso
só em ação do usuário** e ~~sem toast no login~~ — **revisto pelo autor
no teste: o login dá boas-vindas** ("Bem-vindo(a), <primeiro nome>!").
Achado no mesmo teste: senha errada devolvia **500 "Internal Server
Error"** — o `GlobalExceptionHandler` não tratava `AuthenticationException`
(caía no genérico) e o helper lia `error` (nome do status) antes de
`message` no `ErrorResponse`. Agora: **401 "E-mail ou senha inválidos."**
(sem dizer qual dos dois errou), login documentado no Swagger e o helper
usa `message` quando o corpo é um `ErrorResponse`. A parte de toasts sai
antes da HU11, na branch da HU08; menu/avatar e Meus Dados seguem
depois da HU11.
- `toastr.config.ts` (`TOASTR_OPTIONS`, usado por `ToastrModule.forRoot`
  em `src/main.ts`): `positionClass: 'toast-top-center'`,
  `maxOpened: 1`, `autoDismiss: true`, `preventDuplicates: true`,
  `resetTimeoutOnDuplicate: true`, `timeOut: 3500`,
  `extendedTimeOut: 1500`, `closeButton: true`, `progressBar: true`,
  `tapToDismiss: true`, `newestOnTop: true`.
- Visual em `styles.css` sobrescrevendo `#toast-container`/`.ngx-toastr`/
  `.toast-success`/`.toast-error`/`.toast-warning`/`.toast-info`: card
  branco `rounded-xl` com sombra, largura `min(92vw, 420px)`, abaixo da
  navbar, faixa lateral e ícone Material (`check_circle`, `error`,
  `warning`, `info`) na cor semântica (tokens `success`/`error`/
  `warning`/`info`), texto em `base-content`, barra de progresso na cor
  do tipo. Sem HEX novo. O `node_modules/ngx-toastr/toastr.css` continua
  importado (estrutura), só a aparência muda.
- **Regra de uso**: toast de sucesso só em **ação do usuário** (salvar,
  excluir, editar, aprovar, despachar, cancelar, iniciar/confirmar/
  devolver entrega, abrir/fechar loja, adicionar ao carrinho, trocar o
  endereço de entrega) e boas-vindas no login. **Leitura/busca não gera
  toast.** Uma
  ação gera **um** toast: onde saíam dois em sequência, vira uma
  mensagem só (`order-list.reorder`). Texto curto, sem "!!", com
  ortografia revisada.
- **Erro** sempre com a mensagem do backend, por um helper único
  `utils/backend-message.ts` (`message` do `ErrorResponse` →
  `err.error.error` → `err.error.message` →
  "sem conexão com o servidor" no status 0 → texto padrão), que
  substitui as cópias privadas de `backendMessage` e os
  `${err.error.message}` que viravam "undefined".
- Toasts de busca a remover (levantamento de 2026-10-18):
  `order-list` ("Pedidos buscados"), `supplier-list` (2×),
  `supplier-approval`, `product-list`, `product-item`,
  `product-item-form` (2×), `category-products`, `configuration`,
  `city-register`, `order-history` (3×), `distribuitor-form` (2×,
  inclui "Cep buscado") e `address-form` ("Cep buscado"). O
  "Usuario logado com sucesso!" do `login` vira "Bem-vindo(a), <nome>!". `distributor-management` é código morto (fora).

**Mensagens genéricas (varredura de 2026-10-19, pedido do autor):**
- Front: os toasts "Erro ao buscar categorias" (`supplier-list`,
  `product-list`), "Erro ao buscar produtos" (`supplier-list`) e "Erro
  ao buscar cidades" (`address-form`) descartavam o erro — passam a
  mostrar a causa pelo `backendMessage`.
- Backend, 500 genérico: o `GlobalExceptionHandler` devolvia
  `ex.getMessage()` cru (texto de Java/SQL no toast). Passa a responder
  "Erro interno. Tente novamente em instantes." e registrar o detalhe só
  no log. `ResponseStatusException` ganha tratamento próprio (status e
  motivo dela).
- Backend, status errado: categoria de outro fornecedor no cadastro de
  produto (`ProductItemService`) era `RuntimeException` (500) → **403**
  (`ForbiddenResourceException`); tempo médio de entrega não numérico
  (`SupplierService`) era `IllegalArgumentException` com nome de campo
  (500) → **400** "Tempo médio de entrega inválido: informe os minutos.";
  falha no envio de imagem ao Cloudinary (`ProductItemService`,
  `UploadController`) → **502** "Não foi possível enviar a imagem. Tente
  novamente.".
- Backend, inglês e grafia: "User not found…"/"City not found"/"Address
  not found"/"Error generating token" → "Usuário não encontrado.",
  "Cidade não encontrada.", "Endereço não encontrado.", "Não foi possível
  gerar o acesso."; "Impossivel alterar senha de um administrador" →
  "Não é possível alterar a senha de um administrador."; "Horario…" →
  "Horário…"; "Email já cadastrado." → "E-mail já cadastrado."; "City e
  Address devem estar definidos…" → "Cidade e endereço devem estar
  definidos antes de salvar.".

### 5.11 Fatia 4 — Meus Dados e menu do usuário

> Escrita em 2026-10-20, depois da HU11 (que estava na frente desta
> fatia). Substitui as duas últimas listas da §5.10. Decisões do autor:
> (a) **cliente, fornecedor e entregador** passam a ter "Meus Dados";
> (b) o **e-mail fica somente leitura** (é a identidade do login por
> código da HU11; trocar de e-mail pede confirmação no endereço novo,
> evolução futura).

**Achados (diagnóstico do código atual)**
- **Falha de segurança em `PUT /api/users/{id}`**: a rota não confere
  o dono. Qualquer cliente ou fornecedor logado edita nome, **e-mail** e
  telefone de qualquer usuário pelo id. Com o login por código, trocar o
  e-mail da vítima pelo seu e pedir o código é **tomada de conta**
  (CLAUDE.md: "nunca confiar em `userId` se o principal define o dono").
  Entra nesta fatia por ser a rota que o "Meus Dados" usa.
- Hoje a rota `PUT` aceita ADMIN, SUPPLIER e CUSTOMER; o **entregador
  não está** na lista e não tem tela de dados.
- `customer-panel` (rota `/painel-cliente`, só ADMIN/CUSTOMER) já está
  no padrão F7 de cartões, mas sem avatar, com campos `input-bordered`
  (sem borda visível no tema `h2gas`), sem skeleton nem estado de
  salvando, e deixa editar o e-mail.
- O `menu` usa o ícone genérico `fa-user` e um dropdown estreito
  (`w-48`), sem nome nem e-mail, sem Esc, sem `aria`; o avatar do topo
  **não atualiza** depois de salvar os dados (o `menu` lê o usuário uma
  vez, no `ngOnInit`).
- A tab bar do cliente tem "Perfil" apontando para `/painel-cliente`.

**Backend**
- `PUT /api/users/{id}` (`@RolesAllowed` ADMIN, SUPPLIER, CUSTOMER e
  **DELIVERER**): o id vem da URL, mas só vale se for o **do usuário do
  token** (ADMIN pode editar qualquer um). Outro usuário → **403**
  (`ForbiddenResourceException`).
- `UserService.update(id, dto, actor)`:
  - e-mail ausente ou igual ao atual → mantém;
  - e-mail **diferente** com ator não ADMIN → `BusinessRuleException`
    ("O e-mail não pode ser alterado por aqui.", 400);
  - e-mail diferente com ADMIN segue a regra de unicidade atual
    ("E-mail já cadastrado.");
  - nome e telefone continuam editáveis e validados (`name` com 3+
    caracteres, `phoneNumber` não vazio).
- OpenAPI no `PUT` (`@Operation`, 200/400/403/404, `@Parameter`).

**Menu do usuário (`menu`)**
- O ícone genérico vira **avatar com as iniciais** (primeira letra do
  primeiro e do último nome; nome único usa as duas primeiras letras),
  círculo `bg-secondary text-secondary-content`, e acompanha o nome
  quando "Meus Dados" é salvo (o `UserService` expõe o usuário atual
  como fluxo observável).
- Dropdown no padrão F7 (`rounded-xl`, sombra, `w-64`): cabeçalho com
  avatar, nome e e-mail; itens por perfil — **CUSTOMER**: Meus Dados,
  Meus Pedidos, Meus Endereços; **SUPPLIER**: Meus Dados, Dashboard,
  Pedidos recebidos, Configurações; **DELIVERER**: Meus Dados;
  **ADMIN**: Meus Dados, Pedidos recebidos, Configurações — e **Sair**
  separado no fim.
- Abre e fecha com `fadeIn` (`animations/checkout.animations.ts`), fecha
  com **Esc** e clique fora, `aria-expanded`, `aria-haspopup="menu"`,
  `role="menu"`/`menuitem` e foco no primeiro item ao abrir.

**Meus Dados (`customer-panel`)**
- Rota nova **`/meus-dados`** (ADMIN, CUSTOMER, SUPPLIER, DELIVERER);
  `/painel-cliente` vira redirecionamento para ela, e a tab "Perfil" do
  cliente aponta para a rota nova.
- Cabeçalho com **avatar grande de iniciais**, nome e e-mail.
- Carrega os dados atuais da API (`/users/me`) com **skeleton**; campos
  Nome e Telefone com ícone e rótulo; **E-mail somente leitura**, com a
  dica "Para trocar o e-mail, fale com o suporte."
- Botão "Salvar alterações" **desabilitado sem alteração** e com
  spinner enquanto salva; sucesso atualiza o `localStorage` e o avatar
  do menu; erro mostra a mensagem do backend pelo helper.
- Itens de lista com chevron: **cliente** — Meus endereços e Meus
  pedidos; **fornecedor** — Configurações da loja; entregador sem
  atalhos.
- Tokens F2, sem HEX novo, `rounded-xl`, `pb-24` no mobile; campos com
  borda explícita (Tailwind), não `input-bordered`.

### 5.12 Fatia 5 — navbar, footer, tema estrutural e varredura de HEX

> Escrita em 2026-10-20, depois da Fatia 4. Pedido do autor: rodada de
> UX/UI na "casca" do app (navbar, footer e etc.). Decisões do autor: as
> quatro frentes abaixo entram juntas; a Fatia 4 vai antes para o PR.

**Achados (diagnóstico do código atual)**
- **Navbar**: os 18 links usam `href` — cada clique recarrega o app
  inteiro (perde o estado, pisca e derruba o SSE da HU08). Não há
  destaque do link ativo; os ícones misturam Font Awesome e Material; o
  "Início" do fornecedor leva ao catálogo do cliente; o carrinho aparece
  para fornecedor e admin (que não compram); o cliente no celular tem um
  hambúrguer que repete a bottom tab bar; o menu mobile abre sem
  animação e não fecha ao navegar.
- **Footer**: `bg-[#0E2068]` e `hover:text-[#67E8F9]` (HEX), ano fixo
  "© 2025", ocupa meia tela no celular, links por `href` e aparece nas
  telas "de app" (entregador, login).
- **Layout**: `app.component.html` tem `<!DOCTYPE>`/`<head>`/`<body>`
  dentro do template.
- **Tema**: o bloco `@plugin "daisyui/theme"` do tema `h2gas`
  (`styles.css`) não define as variáveis estruturais do DaisyUI 5
  (`--border`, `--radius-*`, `--size-*`, `--depth`, `--noise`): campos
  `input-bordered`/`select` sem borda (remendados tela a tela) e raios
  irregulares. Dívida registrada na HU05 e na Fatia 3.
- **HEX solto** (pendência "Grupos A–E"): landing `/` (10), Sobre e
  Contato; e as 4 telas de admin/fornecedor (`supplier-approval`,
  `city-register`, `category-products`, `distribuitor-form`), cujos CSS
  de 303 linhas reimplementam à mão classes do Tailwind com cor fixa
  (sobrescrevendo o Tailwind real), além de `<!DOCTYPE>`/`<head>`, link
  de Font Awesome por CDN (já carregado no `index.html`), título "Azul
  Cidades" e um `#mobile-menu` sem uso nos templates.

**Tema estrutural (`styles.css`)**
- No tema `h2gas`: `--radius-selector: 0.5rem`, `--radius-field:
  0.75rem`, `--radius-box: 1rem`, `--size-selector: 0.25rem`,
  `--size-field: 0.25rem`, `--border: 1px`, `--depth: 0`, `--noise: 0`.
  Campos `input-bordered`/`select-bordered` passam a ter borda visível
  sem remendo; os remendos existentes (borda explícita do Tailwind)
  continuam válidos e não são reescritos nesta fatia.

**Layout do app (`app.component`)**
- Template sem `<!DOCTYPE>`/`<head>`/`<body>`: casca `header` (menu) +
  `main` (`flex-1`, coluna com altura mínima da tela, footer no fim) +
  `footer`.
- **Footer escondido** nas telas de autenticação/onboarding (as de
  `hiddenMenuRoutes`, **exceto** a landing `/`, `/sobre` e `/contato`,
  que são institucionais e têm no footer o caminho entre si) e nas do
  entregador (`/entregas` e filhas — tela de app com abas fixas).

**Navbar (`menu`)**
- Todos os links internos (desktop, mobile e logo) por `routerLink` —
  navegação sem recarregar o app.
- Link ativo destacado: `routerLinkActive` com `bg-white/15` e
  `aria-current="page"` (`ariaCurrentWhenActive`); "Início" com
  correspondência exata.
- Logo leva ao **início do perfil**: cliente e admin `/inicio`,
  fornecedor `/dashboard`, entregador `/entregas` — a mesma regra do
  login por senha e por código, extraída para `utils/home-route.ts`
  (`homeFor`) e usada pelos três.
- **Fornecedor** sem o link "Início" do catálogo; **carrinho só para o
  cliente**.
- **Admin não cadastra endereço** (pedido do autor no teste): o
  `addressDefinedGuard` libera o ADMIN direto, sem buscar endereço nem
  mandar para `/primeiro-acesso-endereco`; o catálogo do admin vem sem
  `cityId` (o backend devolve todos os fornecedores ativos e abertos). O
  cliente segue obrigado a ter endereço (RN05).
- Ícones da navbar só Material (o dropdown do usuário já usa).
- **Hambúrguer só para fornecedor e admin** (o cliente navega pela
  bottom tab bar no celular; o entregador pelas abas dele). Painel
  mobile com `expandCollapse` (`animations/checkout.animations.ts`),
  fecha ao navegar (`NavigationEnd`) e com **Esc**; botão com
  `aria-expanded`, `aria-controls="mobile-menu"` e rótulo acessível.

**Footer (`footer`)**
- Cores do tema (`bg-primary`, `text-primary-content`), sem HEX; ano
  atual automático; versão compacta no celular (logo menor, marca e
  links em linha); `routerLink` para Sobre e Contato.

**Varredura de HEX**
- Grupo E (`initial-screen`, `about`, `contact`): HEX → tokens, sem
  wrappers de documento.
- Grupos C/D (`supplier-approval`, `city-register`,
  `category-products`, `distribuitor-form`): o CSS legado e o
  `styleUrl` saem; saem também `<!DOCTYPE>`/`<head>`, o Font Awesome
  por CDN, o título "Azul Cidades" e os blocos mortos; HEX → tokens;
  `href` internos → `routerLink`. Layout mantido (só tokens, §5.7).
- Resíduos do Grupo A (`forgot-password`, `reset-password`,
  `check-email`: 1 HEX cada), `supplier-list.component.css` e
  `menu.component.css`.

### 5.13 Fatia 6 — back-office tela a tela: aprovação de fornecedores

> Escrita em 2026-10-21. Pedido do autor: melhorar a UI/UX das telas de
> back-office uma a uma, com boas práticas ("eu passo e você faz"). Isso
> revoga, **só para a tela citada em cada subseção**, o item "Redesenho
> mobile-first dos Grupos C e D" do Fora de Escopo. Feita na branch da
> HU12, a pedido do autor. Primeira tela: `/supplier-approval`.

**Achados**
- O campo "Buscar fornecedores..." não está ligado a nada.
- Tabela de 10 colunas (nome, telefone, ativo, cidade, bairro, rua,
  número, CNPJ, e-mail): no celular só cabe rolando de lado, e o status
  fica no meio da linha.
- O interruptor muda na hora, sem confirmação, e **não volta** se o
  backend falhar (a tela mostra um status que não foi salvo); o toast
  de erro diz "Erro ao buscar" numa ação de atualizar.
- Sem filtro de status: o admin não acha quem está aguardando
  aprovação, embora o card "Aguardando aprovação" do painel (HU12)
  traga o admin para cá. Sem skeleton, estado vazio nem paginação
  (carrega só a 1ª página de 10).
- `GET /api/suppliers` não tem OpenAPI e não filtra.

**Contrato**
- Backend: `GET /api/suppliers` ganha `active` (opcional; `false` =
  aguardando aprovação, `true` = ativos, ausente = todos) e `q`
  (opcional; nome, e-mail ou CNPJ, sem diferenciar maiúsculas), além de
  `page`/`size`; ordenado por nome. Filtro no backend (paginação
  correta). Rota passa a ter `@Operation`, `@ApiResponses` (200, 403) e
  `@Parameter`. Papéis inalterados.
- Frontend (`supplier-approval`):
  - chips "Aguardando aprovação" (padrão, `active=false`), "Ativos",
    "Todos", com `aria-pressed`; busca com debounce de 300 ms no
    backend; contador do filtro;
  - lista de cards (padrão da tela de Usuários da HU12): nome, CNPJ,
    e-mail e telefone, cidade · bairro · rua e número, badge de status
    ("Aguardando aprovação" / "Ativo");
  - ação "Aprovar" (aguardando) ou "Desativar" (ativo) com modal de
    confirmação; desativar avisa que o fornecedor sai do catálogo dos
    clientes. Sucesso: toast com o nome e a lista recarrega a página
    atual (o card sai do filtro). Erro: toast com a causa
    (`backendMessage`) e o status **não muda**;
  - skeleton, estado vazio por filtro, paginação Anterior/Próxima;
  - sem `console.log`, sem `DynamicTableComponent` nesta tela (segue em
    `city-register`), sem HEX.

**Fora de escopo (desta tela)**: editar dados do fornecedor (é a tela de
Usuários da HU12), motivo da recusa, aviso por e-mail ao aprovar, ações
em lote, mexer no `PATCH /{id}/update-active`.

### 5.14 Fatia 6 — cidades atendidas com a base do IBGE

> Escrita em 2026-10-21, segunda tela da Fatia 6 (§5.13). Pedido do
> autor: cadastrar cidade por dropdowns com a lista oficial de cidades
> do Brasil, sem digitar à mão, e garantir que a cidade cadastrada case
> com a do CEP quando um usuário novo informa o endereço.

**Achados**
- Nome e estado digitados livres: nada impede "curitiba", "Curitba"
  ou estado "Paraná" no lugar de "PR".
- O CEP (ViaCEP) é casado com a cidade **só pelo nome**, sem
  diferenciar acento nem UF: "Sao Jose" não casa com "São José", e
  "Bom Jesus" existe em vários estados (`address-form`,
  `distribuitor-form`). O `distribuitor-form` nem preenche a cidade a
  partir do CEP.
- `POST /api/cities` devolve o corpo recebido (sem `id`), duplicidade
  é 404 e só olha o nome; apagar cidade usada por fornecedor ou
  endereço estoura a FK (500). As rotas não têm OpenAPI.
- Tela: tabela genérica, botões com cor fixa, `console.log`, modal de
  exclusão por checkbox, Font Awesome.

**Decisão**: a fonte é a API pública de localidades do IBGE
(`servicodados.ibge.gov.br/api/v1/localidades`, sem chave). O ViaCEP
devolve no campo `ibge` o mesmo código de município, então a cidade
guarda esse código e o casamento CEP ↔ cidade passa a ser exato.

**Contrato**
- Flyway `V12__city_ibge_code.sql`: `cities.ibge_code INT NULL UNIQUE`;
  preenche Curitiba/PR (4106902). Cidades antigas sem código seguem
  funcionando pelo casamento por nome + UF (abaixo).
- `IbgeClient` (Spring `RestClient`, sem dependência nova):
  `findMunicipality(code)` → nome e UF oficiais.
- `POST /api/cities` (ADMIN) recebe só `{ "ibgeCode": 4106902 }`
  (`@NotNull`). Nome e UF vêm do IBGE, nunca do corpo. **201** com
  `id`, `name`, `state`, `ibgeCode`; **409** se a cidade já é atendida;
  **400** se o código não existe no IBGE; **502** se o IBGE não
  responder.
- `DELETE /api/cities/{id}` (ADMIN): **409** com mensagem se houver
  fornecedor ou endereço na cidade; **404** se não existir.
- `GET /api/cities` passa a trazer `ibgeCode` e vem ordenado por UF e
  nome. As três rotas ganham OpenAPI.
- Frontend:
  - `services/ibge.service.ts`: UFs e municípios por UF direto do IBGE
    (como o ViaCEP), ordenados por nome e guardados em cache na sessão;
  - `utils/city-match.ts`: `matchCityFromCep(cities, viaCep)` casa pelo
    código IBGE e, sem código, por nome sem acento/maiúscula **e** UF.
    Usado em `address-form` e `distribuitor-form` (que passa a
    preencher a cidade pelo CEP). O `address-form` mostra a cidade
    casada ("Curitiba - PR"), só leitura, junto com rua e bairro;
  - `city-register`: lista das cidades atendidas com busca local
    (lista curta, só do admin), contador, estado vazio, skeleton;
    "Nova cidade" em modal com dropdown de UF e de município (cidades
    já atendidas aparecem desabilitadas), botão salvar só com município
    escolhido; excluir com modal de confirmação próprio e a mensagem do
    409; rota sob demanda (`loadComponent`).

**Fora de escopo (desta tela)**: backend conferir se o CEP do endereço
pertence ao `cityId` enviado; preencher o código IBGE das cidades
antigas de outros ambientes (o casamento por nome + UF cobre); bairros
ou faixas de CEP atendidas; editar cidade.

### 5.15 Fatia 6 — configuração da loja do fornecedor

> Escrita em 2026-10-21. Pedido do autor: melhorar a UI/UX das telas de
> fornecedor uma a uma; primeira: `/configuracao`. Revoga, só para esta
> tela, o item "Redesenho mobile-first das telas de admin/fornecedor" do
> Fora de Escopo. Branch `feature/f7-back-office` (da `develop`); as
> §5.13 e §5.14 estão na branch da HU12, que entra antes. Decisões do
> autor: o estoque mínimo vai para o backend; abrir/fechar a loja fica
> onde está (Pedidos recebidos e Histórico).

**Achados**
- `PATCH /api/suppliers/{id}/update-times` aceita qualquer `id` de
  qualquer SUPPLIER: um fornecedor muda o horário de outro.
- O estoque mínimo fica só no `localStorage` (`ConfigurationService`):
  some em outro aparelho; só alimenta o contador "estoque baixo" da tela
  de produtos (padrão 8).
- Um botão salva duas coisas: com estoque inválido avisa e ainda envia
  o horário (dois toasts); sem validação de horário nem estado de
  carregando/salvando; Salvar sempre habilitado.
- Visual fora do padrão (azul fixo, gradiente, Font Awesome, `p-10` no
  celular, `min-h-screen`, `console.log`); a rota aceita ADMIN, que não
  tem `supplierId` (a tela quebra).

**Contrato**
- Flyway `V13__supplier_low_stock_threshold.sql`:
  `suppliers.low_stock_threshold INT NOT NULL DEFAULT 8`. Número 13
  porque a HU12 traz `V9`–`V12` e é mergeada antes.
- `GET /api/suppliers/mine/settings` e `PUT /api/suppliers/mine/settings`
  (SUPPLIER): o fornecedor vem do token, nunca do corpo nem da URL.
  Corpo/resposta `SupplierSettingsDTO`: `openingTime` e `closingTime`
  (`HH:mm`, obrigatórios), `averageDeliveryTime` (minutos, 1–240),
  `lowStockThreshold` (1–9999). Fechamento igual à abertura → **400**
  "O horário de fechamento precisa ser diferente da abertura." (fechar
  depois da meia-noite é permitido). OpenAPI nas duas rotas.
- `PATCH /{id}/update-times`: SUPPLIER só altera o próprio id (**403**);
  ADMIN continua podendo. OpenAPI.
- Frontend: `SupplierService.getMySettings`/`updateMySettings`; a tela de
  produtos lê o `lowStockThreshold` salvo; `ConfigurationService` sai.
- Tela "Configurações da loja": um formulário só, seções Horário de
  funcionamento, Entrega e Estoque, com ajuda por campo e prévia
  ("Aberta das 08:00 às 18:00 · entrega em ~30 min"; fechamento depois
  da meia-noite mostra "do dia seguinte"); erros por campo; Salvar
  desabilitado sem alteração ou inválido, com spinner; um toast só;
  erro com a causa; skeleton ao carregar; rota só SUPPLIER e sob
  demanda; sem HEX nem `console.log`.

**Fora de escopo (desta tela)**: abrir/fechar a loja aqui; horário por
dia da semana e feriados; fechar sozinho fora do horário; alerta ou
e-mail de estoque baixo; `distributor-management` (código morto).

### 5.16 Fatia 6 — produtos do fornecedor (`/product-items`)

> Escrita em 2026-10-21, segunda tela de fornecedor da Fatia 6 (§5.15).

**Achados**
- `DELETE /api/product-items/{id}` não confere o dono: qualquer
  fornecedor apaga produto de outro.
- Produto que já aparece em pedido não pode ser apagado (FK de
  `order_items`), e a rota devolve 500. Todos os produtos do fornecedor
  de teste têm vendas.
- A tela busca só a 1ª página (10) e conta Total, Disponíveis, Estoque
  baixo e Esgotados em cima dela: com mais de 10 produtos os números
  erram, e não há paginação.
- Todo card mostra o selo "Disponível", até o esgotado; o rótulo
  "Fornecedor:" mostra a marca; sem busca nem filtro; imagem quebrada sem
  alternativa; exclusão por checkbox escondido; azul/verde/vermelho
  fixos, Font Awesome, `console.log`.

**Contrato**
- `GET /api/product-items/mine` (SUPPLIER, dono do token): `q`
  (marca, categoria ou tipo, sem diferenciar maiúsculas), `stock`
  (`all` | `available` = estoque > 0 | `low` = de 1 até o estoque
  mínimo | `out` = 0; padrão `all`), `page`, `size`; ordenado por marca.
- `GET /api/product-items/mine/summary` (SUPPLIER): `total`,
  `available`, `lowStock`, `outOfStock` e `lowStockThreshold`, contados
  no banco com o estoque mínimo da configuração (§5.15).
- `DELETE /api/product-items/{id}`: **403** se o produto é de outro
  fornecedor; **409** "Este produto já está em pedidos e não pode ser
  excluído. Zere o estoque para tirá-lo do catálogo." se já foi vendido.
- OpenAPI nas três rotas.
- Tela: cabeçalho com "Novo produto"; os quatro contadores viram os
  filtros (com `aria-pressed`) e o de estoque baixo diz o limite e leva à
  configuração; busca com debounce no backend; cards com imagem (ícone
  do tipo quando falta ou falha), marca, categoria, tipo, preços de
  entrega e retirada, selo de entrega grátis, estoque com selo
  Disponível / Estoque baixo / Esgotado e data da atualização; editar e
  excluir com rótulo acessível; exclusão com modal próprio e a mensagem
  do 409; skeleton, estado vazio por filtro e paginação; rota sob
  demanda; sem HEX nem `console.log`.

**Fora de escopo (desta tela)**: ajustar estoque direto na lista;
exclusão lógica/arquivar produto; ordenar por coluna; mexer no
formulário de produto (`product-item-form`).

### 5.17 Fatia 6 — categorias do fornecedor (`/category-products`)

> Escrita em 2026-10-21, terceira tela de fornecedor da Fatia 6, já
> com a HU12 mergeada na branch.

**Achados**
- `POST /api/category-products` usa o `supplierId` do corpo: um
  fornecedor cria categoria na loja de outro. `PUT` e `DELETE` não
  conferem o dono.
- Excluir categoria que tem produtos estoura a FK (500). Sem validação
  (nome vazio passa) nem checagem de nome repetido no mesmo tipo.
- A tela busca só a 1ª página (10) do `by-supplier`; tem um formulário
  morto (`mostrarFormulario`), emoji no título, azul fixo, Font Awesome,
  exclusão por checkbox escondido e `console.log`; a checagem "nenhuma
  alteração" compara objetos por referência e nunca dispara.

**Contrato**
- `GET /api/category-products/mine` (SUPPLIER): todas as categorias do
  fornecedor do token (lista curta, sem paginação), ordenadas por tipo e
  nome, com `id`, `name`, `type`, `productTypeId`, `typeIcon` e
  `productCount`.
- `POST /api/category-products` e `PUT /{id}` recebem
  `CategoryRequestDTO` (`name` obrigatório, até 60 caracteres;
  `productTypeId` obrigatório); o fornecedor vem do token. Nome repetido
  no mesmo tipo e na mesma loja (sem diferenciar maiúsculas) → **409**
  "Você já tem a categoria 20L em Água." `POST` devolve **201**.
- `PUT` e `DELETE` de categoria de outro fornecedor → **403**.
  `DELETE` de categoria com produtos → **409** "20L tem 3 produtos.
  Mova ou exclua os produtos antes de excluir a categoria."
- OpenAPI atualizado nas quatro rotas. `by-supplier` segue igual (usado
  pelo formulário de produto).
- Tela: cabeçalho com "Nova categoria"; categorias agrupadas por tipo
  (com o ícone do tipo, HU12), cada uma com a contagem de produtos;
  editar e excluir com rótulo acessível; modal de criar/editar com tipo
  e nome, erro por campo, Salvar só com alteração válida; excluir com
  modal próprio — com produtos, o modal explica e não deixa confirmar;
  mensagens do 409; skeleton e estado vazio; rota sob demanda; sem HEX,
  emoji nem `console.log`.

**Ajuste do popup de criar/editar** (pedido do autor, 2026-10-21):
- tipo escolhido em cartões com o ícone do tipo (`role="radiogroup"`,
  `aria-checked`), no lugar do select;
- sugestões de nome do tipo escolhido em chips que preenchem o campo
  (Gás: P2, P5, P8, P13, P20, P45; Água: 5L, 10L, 20L; outros tipos sem
  sugestão), escondendo as que a loja já tem;
- prévia "Gás · P13" e aviso de nome repetido no mesmo tipo antes de
  enviar (com as categorias já carregadas; o 409 do backend continua
  valendo); contador de caracteres (até 60);
- botão de fechar no cabeçalho, Esc fecha, foco no nome ao abrir.

**Fora de escopo (desta tela)**: mover produtos entre categorias;
reordenar categorias; descrição da categoria; o formulário de produto
(`product-item-form`) continua lendo `by-supplier` com 10 por página —
registrar como dívida para a tela dele.

### 5.18 Fatia 6 — dados da loja com logo (`/distributor/edit/:id`)

> Escrita em 2026-10-21. Pedido do autor: o fornecedor ganha foto/logo
> da loja ("importante só para ele; usuários não querem foto"). Decisões
> do autor: a logo aparece no card da loja do catálogo; a tela recebe
> logo, correção de segurança e redesenho.

**Achados**
- `PUT /api/suppliers/{id}` não confere o dono: qualquer fornecedor
  edita a loja de outro; a tela usa o `id` da URL.
- O `PUT` exige e regrava horário e tempo de entrega (hoje da
  Configuração, §5.15): editar o nome sobrescreve o horário, e campo
  vazio estoura `LocalTime.parse`. A cidade vem do formulário, mas é do
  endereço.
- Tela fora do padrão (azul fixo, Font Awesome, `select` manual de
  cidade no endereço, toasts errados como "Erro ao buscar cidades" ao
  salvar, `console.log`, sem validação visível). `/distributor/new` só é
  aberto pelo `distributor-management` (código morto).
- Não existe imagem de fornecedor; o upload de produto já usa o
  `CloudinaryService` (502 em falha) com limite multipart de 1 MB.

**Contrato**
- Flyway `V14__supplier_logo.sql`: `suppliers.logo_url VARCHAR(500)
  NULL`; `SupplierDTO.logoUrl` (o catálogo recebe sem rota nova).
- `GET /api/suppliers/mine/profile` (SUPPLIER): nome, telefone,
  descrição, CNPJ (só leitura), logo e endereço resumido.
- `PUT /api/suppliers/mine/profile` (SUPPLIER): só `name` (obrigatório,
  até 80), `phone` (obrigatório, 10 ou 11 dígitos) e `description` (até
  500); não mexe em horário, cidade nem aprovação.
- `PUT /api/suppliers/mine/logo` (multipart `file`): só JPG, PNG ou WEBP
  de até 1 MB, senão **400** "Envie uma imagem JPG, PNG ou WEBP de até
  1 MB."; falha no Cloudinary → **502**; devolve `{ logoUrl }`.
  `DELETE /api/suppliers/mine/logo` → **204**.
- `PUT /{id}`: SUPPLIER só a própria loja (**403**); ADMIN continua.
  OpenAPI em todas.
- Tela "Dados da loja" (componente novo `store-profile` na rota
  `distributor/edit/:id`; o `distribuitor-form` fica só para o
  `/distributor/new`): logo com prévia (iniciais da loja sem logo),
  trocar e remover (com confirmação), checagem de tipo e tamanho antes de
  enviar e upload na hora; formulário de nome, telefone e descrição com
  contador, CNPJ só leitura, erro por campo, Salvar só com alteração
  válida e um toast; endereço em cartão com modal restilizado (o CEP
  define a cidade); link para Horário e entrega (`/configuracao`); lê e
  grava pelos `/mine`, sem depender do `:id`; rota sob demanda; sem HEX
  nem `console.log`.
- `supplier-card` do catálogo: a logo no lugar do ícone de loja, com o
  ícone de volta sem logo ou se a imagem falhar.

**Ajuste: uma tela só para o fornecedor** (pedido do autor, 2026-10-21:
"meus dados e dados da loja devem ser a mesma coisa, está ambíguo").
`Supplier` herda de `User`: nome e telefone são a mesma linha do banco,
e `/meus-dados` e a tela da loja editavam os mesmos campos com rótulos
diferentes. Decisão:
- rota canônica `/minha-loja` (só SUPPLIER, sob demanda), título "Minha
  loja"; `/meus-dados` do fornecedor redireciona para ela (guard que
  devolve `UrlTree`; os outros perfis seguem no Meus Dados);
  `/distributor/edit/:id` vira redirecionamento para `/minha-loja`;
- `SupplierProfileDTO` ganha `email` (só leitura); a tela mostra o
  e-mail com "Para trocar o e-mail, fale com o suporte.";
- salvar o nome atualiza o nome e as iniciais do menu
  (`UserService.setCurrentUser`);
- menu do fornecedor: "Minha loja" no lugar de "Meus Dados"; os botões
  de Pedidos recebidos e Histórico levam a `/minha-loja`; o Meus Dados
  perde o ramo de fornecedor.

**Fora de escopo (desta tela)**: foto de cliente e entregador; logo no
menu e nos pedidos; recortar ou redimensionar a imagem no cliente; apagar
a imagem antiga no Cloudinary; checar o dono no `PUT /addresses/{id}`
(dívida registrada); refazer `/distributor/new`.

### 5.19 Fatia 6 — histórico de pedidos do fornecedor (`/historico-pedidos`)

> Escrita em 2026-10-21. Pedido do autor: melhorar o histórico e, depois,
> o dashboard, mantendo as duas telas separadas.

**Achados**
- `GET /api/orders/{supplierId}/exclude-status` e `/by-supplier/{supplierId}`
  não conferem o dono: um fornecedor lista pedidos de outro, com nome,
  telefone e endereço dos clientes.
- A tela carrega só os 10 primeiros pedidos e calcula receita e contagens
  no navegador sobre eles; sem paginação.
- Só filtra por status; template com `<!DOCTYPE>`/`<html>`/`<body>`, Font
  Awesome e `console.log`; o `orders-widget` (só usado aqui) traz ações de
  mudar status, que não cabem em pedido finalizado.

**Contrato**
- `GET /api/orders/mine/history` (SUPPLIER, dono do token): só pedidos
  entregues ou cancelados; filtros opcionais `status` (`delivered` |
  `canceled`), `from`/`to` (datas `YYYY-MM-DD`, pela criação do pedido),
  `payment` (`PIX` | `DINHEIRO` | `CREDIT_CARD`), `mode` (`DELIVERY` |
  `PICKUP`) e `q` (nome do cliente ou início do código do pedido);
  paginado, mais recente primeiro. Filtro inválido ou `from` depois de
  `to` → **400**.
- `GET /api/orders/mine/history/summary`, mesmos filtros: `delivered`,
  `canceled`, `revenue` (soma dos entregues) e `averageTicket`, contados
  no banco (RN01).
- Rotas antigas por `supplierId`: SUPPLIER só o próprio (**403**); ADMIN
  continua. OpenAPI em todas.
- Tela: cabeçalho com o interruptor de loja aberta/fechada (fica, decisão
  anterior); cartões de resumo que acompanham os filtros; período (7 dias,
  30 dias, este mês, mês passado, personalizado), status, pagamento,
  entrega/retirada e busca com debounce; chips dos filtros ativos com
  "Limpar"; cards com código curto, data, cliente, total, status,
  pagamento e modo, que expandem para itens, endereço, entregador, entrega
  e motivo do cancelamento; skeleton, estado vazio e paginação; rota só do
  fornecedor e sob demanda; o `orders-widget` sai.

**Fora de escopo (desta tela)**: dashboard (próxima tela); exportar CSV ou
PDF; reabrir pedido; avaliações; histórico do admin.

### 5.20 Fatia 6 — dashboard do fornecedor com filtros (`/dashboard`)

> Escrita em 2026-10-21. Pedido do autor: "relatório muito simples; os
> dados precisam ser dinâmicos, clicar em PIX filtra, filtrar por produto,
> tipo, categoria, dinheiro; dados mais bonitos". Histórico e dashboard
> seguem em telas separadas (decisão do autor).

**Achados**
- Os números só mudam com o mês; pagamento, produtos e tipos são listas
  estáticas, sem clique nem filtro.
- Não há quebra por entrega/retirada nem pelas categorias da loja (P13,
  20L); o top 5 de produtos não mostra a receita.
- Gráfico diário sem eixo, sem valores e sem destaque do melhor dia;
  pizza sem total no centro.

**Contrato**
- `GET /api/suppliers/mine/dashboard` ganha filtros opcionais
  `payment` (`PIX` | `DINHEIRO` | `CREDIT_CARD`), `mode` (`DELIVERY` |
  `PICKUP`), `typeId`, `categoryId` e `productId`; valor inválido → **400**.
  Os filtros valem também para o mês anterior da variação. "Pendentes
  agora" e "Em rota agora" continuam sem filtro.
- Regra dos filtros de item (tipo, categoria, produto): entram os pedidos
  com pelo menos um item que bate, e receita, ticket, série diária,
  pagamentos e quebras somam **só os itens que batem**. Pagamento e modo
  filtram o pedido inteiro.
- Resposta: `salesByProduct` com `productId` e receita; `salesByCategory`
  (tipo) com `productTypeId`; novos `salesByStoreCategory` (categorias da
  loja, com `categoryId` e o tipo) e `deliveryModeBreakdown` (entrega ×
  retirada, pedidos, receita e %). OpenAPI atualizado.
- Pagamento e modo são validados por uma regra só, compartilhada com o
  histórico (§5.19).
- Tela: barra de filtros (mês, pagamento, entrega/retirada, tipo,
  categoria e produto, com as listas da loja) e chips dos filtros ativos
  com "Limpar"; **clicar** numa forma de pagamento, num tipo da pizza,
  numa categoria, num produto ou em entrega/retirada aplica o filtro (de
  novo, tira), com destaque do item ativo e `aria-pressed`; cartões de
  indicadores com ícone e variação; gráfico diário em SVG com eixo de
  valores, área, linha, destaque do melhor dia e valor por dia no
  `title`; pizza com o total no centro; barras com quantidade e receita;
  estado vazio que diz se é por causa do filtro; sem HEX.

- Atalho para a central de pedidos (Pedidos recebidos,
  `/dashboard-fornecedor`, pedido do autor): botão "Central de pedidos"
  no cabeçalho e os cartões "Pendentes agora" e "Em rota agora" viram
  links para ela (`routerLink`, sem recarregar).

**Fora de escopo (desta tela)**: período livre (segue por mês); exportar
relatório; comparar lojas; abrir o histórico já filtrado; biblioteca de
gráficos (o SVG é feito à mão, como hoje).

### 5.21 Cliente — Início e Produtos (`/inicio`)

> Escrita em 2026-10-21. Pedido do autor: melhorar o básico para o
> cliente final; "botões com cores muito escuras não são legais".
> Decisões do autor: **hierarquia** (uma ação forte em azul vivo, o resto
> leve com contorno, banners em fundo azul suave) e **busca sem botão
> Filtrar**. Escopo desta rodada: só `/inicio` (lojas e produtos); a
> navbar fica para depois.

**Achados**
- Tudo usa o azul-marinho da marca cheio: Filtrar, Ver Produtos/Ver
  Fornecedores, banners em gradiente escuro, "Ver Produtos" das lojas,
  chip ativo, Adicionar e página ativa; nenhuma ação se destaca.
- **RN05 quebrada na busca de lojas**: `GET /suppliers/search?name=` não
  filtra pela cidade do endereço ativo — a busca mostra lojas de outras
  cidades.
- Na visão Produtos a busca não faz nada (chama a busca de lojas); a
  paginação chama a lista de lojas também na visão Produtos, e os
  produtos carregam só 20.
- Template com `<!DOCTYPE>`/`<html>`/`<body>` e Font Awesome.

**Contrato**
- Tema: `--color-accent` `#2a78d6` (azul vivo, texto branco ≈ 4,6:1, AA)
  e `--color-accent-content` branco, para a ação principal.
- `GET /api/suppliers/actives/open` e `GET /api/product-items/by-supplier-open`
  ganham `q` (loja: nome; produto: marca, categoria ou tipo), sem
  diferenciar maiúsculas, mantendo o `cityId` (RN05); OpenAPI.
- Tela: busca única com debounce, sem botão, na visão atual e na cidade do
  endereço ativo, com botão de limpar; alternância Lojas/Produtos em
  controle segmentado claro; banners em fundo azul suave com texto
  escuro; "Ver produtos" da loja em contorno; chip de tipo ativo em fundo
  claro com borda e texto primários; "Menor preço" como chip claro;
  **"Adicionar" é a única ação cheia** (accent); página ativa em fundo
  claro; paginação dos produtos e contador pelo total do backend; sem
  wrappers de documento nem Font Awesome.

**Fora de escopo (desta tela)**: navbar do cliente; carrinho, checkout,
pedidos e endereços; produtos de uma loja (`/fornecedores/:id/produtos`);
avaliações; banners dinâmicos.

### 5.22 Cliente — barra de navegação clara

> Escrita em 2026-10-21, continuação da §5.21 (pedido do autor: tirar o
> peso do azul-marinho cheio também no topo).

**Achado**: a barra é um componente só, escuro para todos os perfis, com a
cor repetida em 36 trechos do template.

**Contrato**
- Barra do **cliente** clara: fundo branco com filete inferior, texto
  neutro (`gray-700`), marca e link ativo em `primary` (fundo `primary/10`),
  foco em `primary`. Fornecedor, admin e entregador **mantêm a barra
  escura**.
- As cores vêm de um getter por perfil (`t`), em vez de classes soltas no
  template; o link ativo usa `[routerLinkActive]="t.active"`.
- "Finalizar Pedido" no carrinho passa a ser a ação de destaque (`accent`),
  como o "Adicionar".
- Contraste ≥ 4,5:1 em links, endereço e marca; a logo continua visível.

**Fora de escopo**: tema escuro; barra clara para os outros perfis; o
painel móvel do fornecedor e do admin.

## 6. Fora de Escopo
- CRUD/admin de banners promocionais.
- Agregação de rating médio por fornecedor (gap de backend).
- Agregação de "preço mínimo de entrega" por fornecedor.
- Categoria resumida no card de fornecedor.
- Mover a barra de busca para dentro da navbar global (fica no corpo de
  `supplier-list`/`product-list`, só restilizada).
- Redesenho mobile-first dos Grupos C e D (back-office).
- `distributor-management` (código morto).
- Modalidade de entrega/retirada lado a lado nos cards (isso é HU03).
- Filtro por sub-categoria nos chips (já era fora de escopo do HU02).
- Fatia 3: trocar a biblioteca de toast; fila de toasts (com um por vez,
  o novo substitui o atual); foto de perfil (upload) no
  avatar; telas de admin/fornecedor além do menu (fatia final de
  varredura); autenticação (HU11); variáveis estruturais do tema DaisyUI
  (`--radius-*`, `--size-*`, `--border` — dívida registrada na HU05,
  pede revisão visual própria).

- Fatia 4: trocar de e-mail com confirmação no endereço novo; trocar a
  senha por esta tela; foto de perfil (upload); aba "Perfil" na barra do
  entregador (segue fora, como na HU07); edição de CPF/CNPJ.

- Fatia 5: sino de notificações das Figuras 13/16 (sem backend); trocar
  o Font Awesome no app inteiro (só navbar e footer); redesenho
  mobile-first das telas de admin/fornecedor (só tokens, §5.7);
  reescrever os remendos de borda já existentes; `distributor-management`
  (código morto); dark mode.

## 7. Plano de Testes (TDD)

**Novos componentes:**
1. `category-chip-bar.component.spec.ts` — emite categoria/null ao clicar.
2. `product-card.component.spec.ts` — emite produto com `quantitySelect: 1`.
3. `supplier-card.component.spec.ts` — emite id do fornecedor.
4. `bottom-tab-bar.component.spec.ts` — navega pras 4 rotas de §5.3.

**Componentes alterados:**
5. `supplier-list.component.spec.ts` — query param `view=products`;
   `addToCart` chama `CartService.addProduct` com quantidade 1.
6. `product-list.component.spec.ts` — reescreve
   `deveFiltrarPorCategoriaAguaComNomeCorreto` (categoria agora vem de
   `ProductTypeService` mockado, não de botão hardcoded); novo teste de
   `addToCart`.
7. `menu.component.spec.ts` — bottom tab bar visível só para `CUSTOMER`.

**Fatia 3:**
8. *(fatia 4, §5.11)* **Backend** — `UserServiceTest`:
   `deveAtualizarNomeETelefoneDoProprioUsuario`,
   `deveRejeitarEdicaoDeOutroUsuario` (403),
   `devePermitirAdminEditarQualquerUsuario`,
   `deveManterOEmailQuandoOCampoVierIgualOuAusente`,
   `deveRejeitarTrocaDeEmailPorUsuarioComum` (400),
   `devePermitirAdminTrocarEmailRespeitandoUnicidade`;
   `UserControllerTest`: `deveRetornar403AoEditarOutroUsuario`,
   `deveRetornar200AoEditarOProprioPerfil`,
   `devePermitirEntregadorEditarOProprioPerfil`.
   **Frontend** — `menu.component.spec.ts`:
   `deveExibirIniciaisDoUsuarioNoAvatar`,
   `deveAtualizarAvatarQuandoOsDadosForemSalvos`,
   `deveAbrirMenuDoUsuarioComNomeEmailEOpcoesDoPerfil`,
   `deveFocarOPrimeiroItemAoAbrir`, `deveFecharMenuComEsc`,
   `deveFecharMenuAoClicarFora`;
   `user.service.spec.ts`: `deveEmitirOUsuarioAtualAoSalvar`.
9. `customer-panel.component.spec.ts` —
   `deveExibirAvatarComIniciaisENome`,
   `deveExibirSkeletonEnquantoCarrega`,
   `deveManterEmailSomenteLeitura`,
   `deveDesabilitarSalvarSemAlteracoes`,
   `deveHabilitarSalvarAoAlterarNomeOuTelefone`,
   `deveSalvarENotificarOMenuComSucesso`,
   `deveMostrarAMensagemDoBackendNoErro`,
   `deveExibirAtalhosPorPerfil`;
   `app.routes`/`bottom-tab-bar.component.spec.ts`:
   `deveLevarAbaPerfilParaMeusDados`.
10. Amostra da regra de toast — `order-list.component.spec.ts`
    (`naoDeveExibirToastDeSucessoAoBuscar`,
    `deveExibirUmUnicoAvisoAoRefazerComItensIndisponiveis`),
    `product-list.component.spec.ts`
    (`naoDeveExibirToastDeSucessoAoBuscar`) e
    `login.component.spec.ts` (`deveDarBoasVindasAoLogar`,
    `deveMostrarMensagemDoBackendAoFalharLogin`);
    `AuthenticationControllerTest`
    (`deveRetornar401ComMensagemClaraQuandoSenhaErrada`).
11. Config global — `toastr.config.spec.ts`:
    `deveConfigurarToastrComOpcoesGlobais` e
    `deveSubstituirToastAtualPeloNovo` (ToastrModule real: dois toasts
    seguidos deixam só o último no DOM).
11b. `backend-message.spec.ts` — `deveUsarErrorDoBackend`,
    `deveUsarMessageDoErrorResponse`, `deveUsarMessageQuandoNaoHaError`,
    `deveAvisarSemConexaoNoStatusZero`,
    `deveUsarTextoPadraoSemCorpo`.

11c. Mensagens genéricas — `GlobalExceptionHandlerTest`
    (`deveEsconderDetalheTecnicoNoErroInterno`,
    `deveUsarStatusEMotivoDaResponseStatusException`);
    `ProductItemServiceTest`
    (`deveRetornar403AoUsarCategoriaDeOutroFornecedor`);
    `SupplierServiceTest`
    (`deveRejeitarTempoMedioDeEntregaNaoNumericoComMensagemClara`);
    `UserServiceTest` (`deveAvisarUsuarioNaoEncontradoEmPortugues`);
    `product-list.component.spec.ts`
    (`deveMostrarCausaAoFalharCarregarCategorias`).

12. *(fatia 5, §5.12)* `app.component.spec.ts`:
    `deveEsconderFooterNasTelasDeAppEAutenticacao` (e mostrar na
    landing, Sobre, Contato e telas do cliente);
    `menu.component.spec.ts` (Router real via `provideRouter`):
    `deveNavegarSemRecarregarOApp` (todo link interno com `routerLink`,
    clique sem recarregar), `deveDestacarOLinkAtivo`,
    `deveLevarALogoParaOInicioDoPerfil`, `deveMostrarCarrinhoSoParaCliente`,
    `deveMostrarHamburguerSoParaFornecedorEAdmin`,
    `deveFecharMenuMobileAoNavegarEComEsc`;
    `home-route.spec.ts`: `deveLevarCadaPerfilAoSeuInicio`;
    `address-defined.guard.spec.ts`: `deveLiberarAdminSemExigirEndereco`;
    `footer.component.spec.ts`: `deveMostrarOAnoAtual`,
    `deveUsarRouterLinkNosLinks`, `naoDeveTerHexLiteral`.

13. *(fatia 6, §5.13)* **Backend** — `SupplierServiceTest`:
    `deveFiltrarFornecedoresAguardandoAprovacao`,
    `deveBuscarFornecedoresPorNomeEmailOuCnpj`;
    `SupplierControllerTest`: `deveRepassarFiltrosDeStatusEBusca`.
    **Frontend** — `supplier-approval.component.spec.ts`:
    `deveAbrirNoFiltroAguardandoAprovacao`, `deveFiltrarPorStatus`,
    `deveBuscarNoBackendComDebounce`, `deveExibirEstadoVazio`,
    `deveConfirmarAntesDeAprovar`, `deveRecarregarAoAprovarComSucesso`,
    `deveManterStatusQuandoAprovacaoFalhar`, `devePaginar`;
    `supplier.service.spec.ts`: `deveEnviarFiltrosNaListagem`.

14. *(fatia 6, §5.14)* **Backend** — `CityServiceTest`:
    `deveCadastrarCidadeComNomeEUfDoIbge`,
    `deveRecusarCidadeJaAtendida` (409),
    `deveRecusarCodigoInexistenteNoIbge` (400),
    `deveRecusarExcluirCidadeComFornecedorOuEndereco` (409);
    `IbgeClientTest`: `deveLerNomeEUfDoMunicipio`,
    `deveDevolverVazioParaCodigoInexistente`,
    `deveAvisarQuandoOIbgeNaoResponde` (502);
    `CityControllerTest`: `deveRetornar201AoCadastrarCidade`,
    `deveRetornar400SemCodigoIbge`.
    **Frontend** — `city-match.spec.ts`:
    `deveCasarPeloCodigoIbge`, `deveCasarPorNomeSemAcentoEUf`,
    `naoDeveCasarMesmoNomeEmOutraUf`;
    `ibge.service.spec.ts`: `deveListarUfsPorNome`,
    `deveListarMunicipiosDaUfComCache`;
    `city-register.component.spec.ts`:
    `deveListarCidadesAtendidas`, `deveCarregarMunicipiosAoEscolherUf`,
    `deveDesabilitarMunicipioJaAtendido`,
    `deveCadastrarPeloCodigoIbge`, `deveConfirmarAntesDeExcluir`,
    `deveMostrarMensagemQuandoCidadeEmUso`;
    `address-form.component.spec.ts`:
    `devePreencherCidadePeloCodigoIbgeDoCep`.
15. *(fatia 6, §5.15)* **Backend** — `SupplierServiceTest`:
    `deveLerConfiguracoesDoProprioFornecedor`,
    `deveSalvarHorarioTempoEEstoqueMinimo`,
    `deveRecusarFechamentoIgualAAbertura`;
    `SupplierControllerTest`: `deveUsarOFornecedorDoToken`,
    `deveRetornar400ComTempoDeEntregaForaDoLimite`,
    `deveRetornar403AoAlterarHorarioDeOutroFornecedor`.
    **Frontend** — `supplier.service.spec.ts`:
    `deveBuscarConfiguracoesDoFornecedorLogado`, `deveSalvarConfiguracoes`;
    `product-item.component.spec.ts`:
    `deveUsarOEstoqueMinimoSalvoNoBackend`;
    `configuration.component.spec.ts`:
    `deveCarregarConfiguracoesDoFornecedor`,
    `deveDesabilitarSalvarSemAlteracao`,
    `deveMostrarErroComFechamentoIgualAAbertura`,
    `deveSalvarTudoNumaChamadaComUmToast`,
    `deveMostrarMensagemDoBackendNoErro`, `deveMostrarPreviaDoHorario`.

16. *(fatia 6, §5.16)* **Backend** — `ProductItemServiceTest`:
    `deveListarProdutosDoFornecedorPorEstoque`,
    `deveBuscarPorMarcaCategoriaOuTipo`,
    `deveResumirEstoqueComOLimiteDaConfiguracao`,
    `deveRecusarExcluirProdutoDeOutroFornecedor`,
    `deveRecusarExcluirProdutoJaVendido`;
    `ProductItemControllerTest`: `deveListarSoOsProdutosDoToken`,
    `deveRetornar400ComFiltroDeEstoqueInvalido`.
    **Frontend** — `product-item.service.spec.ts`:
    `deveListarMeusProdutosComFiltros`, `deveBuscarResumoDoEstoque`;
    `product-item.component.spec.ts`: `deveMostrarContadoresDoResumo`,
    `deveFiltrarPeloContador`, `deveBuscarComDebounce`,
    `deveMostrarSeloDeEstoquePorProduto`, `deveConfirmarAntesDeExcluir`,
    `deveMostrarMensagemQuandoProdutoJaVendido`, `devePaginar`.

17. *(fatia 6, §5.17)* **Backend** — `CategoryProductServiceTest`:
    `deveListarCategoriasDoFornecedorComContagemDeProdutos`,
    `deveCriarCategoriaNaLojaDoToken`,
    `deveRecusarNomeRepetidoNoMesmoTipo`,
    `deveRecusarEditarOuExcluirCategoriaDeOutroFornecedor`,
    `deveRecusarExcluirCategoriaComProdutos`;
    `CategoryProductControllerTest`: `deveRetornar201AoCriarCategoria`,
    `deveRetornar400SemNomeOuTipo`, `deveListarAsCategoriasDoToken`.
    **Frontend** — `category-product.service.spec.ts`:
    `deveListarMinhasCategorias`, `deveCriarSoComNomeETipo`;
    `category-products.component.spec.ts`:
    `deveAgruparCategoriasPorTipo`, `deveCriarCategoria`,
    `deveDesabilitarSalvarSemAlteracaoNaEdicao`,
    `deveMostrarMensagemDoBackendQuandoNomeRepetido`,
    `deveImpedirExcluirCategoriaComProdutos`,
    `deveExcluirCategoriaVazia`, `deveExibirEstadoVazio`.
    Popup: `deveEscolherTipoPorCartaoComIcone`,
    `deveSugerirNomesDoTipoSemOsQueJaExistem`,
    `deveAvisarNomeRepetidoAntesDeEnviar`,
    `deveMostrarPreviaEContador`, `deveFecharComEsc`.

18. *(fatia 6, §5.18)* **Backend** — `SupplierServiceTest`:
    `deveAtualizarPerfilSemMexerNoHorario`, `deveSalvarERemoverALogo`,
    `deveRecusarArquivoQueNaoEImagem`, `deveRecusarImagemMaiorQue1MB`;
    `SupplierControllerTest`: `deveUsarALojaDoTokenNoPerfil`,
    `deveRetornar400ComNomeVazio`,
    `deveRetornar403AoEditarLojaDeOutroPeloId`,
    `deveEnviarALogoComoMultipart`; `SupplierServiceTest`:
    `deveResponder502QuandoOCloudinaryRecusa`;
    `GlobalExceptionHandlerTest`: `deveResponder400QuandoArquivoPassaDoLimite`.
    **Frontend** — `supplier.service.spec.ts`: `deveBuscarPerfilDaLoja`,
    `deveEnviarLogoComoFormData`, `deveRemoverLogo`;
    `store-profile.component.spec.ts`: `deveCarregarPerfilDaLoja`,
    `deveSalvarSoNomeTelefoneEDescricao`,
    `deveDesabilitarSalvarSemAlteracao`, `deveEnviarLogoEscolhida`,
    `deveRecusarArquivoGrandeAntesDeEnviar`,
    `deveRemoverLogoComConfirmacao`, `deveMostrarIniciaisSemLogo`,
    `deveMostrarMensagemDoBackendAoFalharOEnvio`,
    `deveVoltarParaAsIniciaisSeALogoNaoCarregar`,
    `deveMostrarEmailSomenteLeitura`, `deveAtualizarOMenuAoSalvarONome`;
    `supplier-card.component.spec.ts`: `deveMostrarLogoDaLojaOuIcone`.
    Tela única: `SupplierServiceTest.deveTrazerOEmailNoPerfil`;
    `supplier-store.guard.spec.ts`:
    `deveLevarFornecedorDeMeusDadosParaMinhaLoja`,
    `deveDeixarOutrosPerfisEmMeusDados`; `menu.component.spec.ts`:
    `deveMostrarMinhaLojaNoLugarDeMeusDadosParaFornecedor`.

19. *(fatia 6, §5.19)* **Backend** — `OrderServiceTest`:
    `deveListarSoFinalizadosDoFornecedor`,
    `deveFiltrarPorPeriodoPagamentoModoEBusca`,
    `deveResumirReceitaEContagensComOsFiltros`; `OrderControllerTest`:
    `deveUsarOFornecedorDoTokenNoHistorico`,
    `deveRetornar400ComFiltroInvalido`,
    `deveRetornar403AoListarPedidosDeOutroFornecedor`.
    **Frontend** — `order.service.spec.ts`: `deveEnviarFiltrosDoHistorico`,
    `deveBuscarResumoDoHistorico`; `order-history.component.spec.ts`:
    `deveMostrarResumoDoBackend`, `deveFiltrarPorStatusEPeriodo`,
    `deveFiltrarPorPagamentoEModo`, `deveBuscarComDebounce`,
    `deveLimparFiltros`, `deveExpandirDetalhesDoPedido`,
    `deveMostrarMotivoDoCancelamento`, `devePaginar`,
    `deveExibirEstadoVazio`.

20. *(fatia 6, §5.20)* **Backend** — `SupplierDashboardServiceTest`:
    `deveFiltrarPorPagamentoEModo`,
    `deveSomarSoOsItensQueBatemComTipoCategoriaOuProduto`,
    `deveAplicarOFiltroNaVariacaoDoMesAnterior`,
    `deveQuebrarPorCategoriaDaLojaEPorModo`,
    `deveTrazerIdsNasQuebrasParaOFiltro`; `SupplierControllerTest`:
    `deveRepassarOsFiltrosDoDashboard`,
    `deveRetornar400ComFiltroInvalidoNoDashboard`.
    **Frontend** — `supplier.service.spec.ts`:
    `deveEnviarFiltrosDoDashboard`; `supplier-dashboard.component.spec.ts`:
    `deveFiltrarAoClicarNoPagamento`, `deveTirarOFiltroAoClicarDeNovo`,
    `deveFiltrarAoClicarNoTipoCategoriaProdutoEModo`,
    `deveFiltrarPelosSelects`, `deveMostrarChipsELimpar`,
    `deveDestacarOMelhorDiaNoGrafico`, `deveMostrarTotalNoCentroDaPizza`,
    `deveExplicarVazioCausadoPeloFiltro`,
    `deveLevarParaACentralDePedidos`, `deveMostrarCadaDiaDoMesNoEixo`
    (pedido do autor: cada dia do mês abaixo da sua coluna; no celular,
    só os ímpares, para não sobrepor). Ajuste: o melhor dia deixou de ter
    cor própria (o laranja parecia outra série/tipo); segue no texto
    "Melhor dia" e no número em negrito.

21. *(cliente, §5.21)* **Backend** — `SupplierServiceTest`:
    `deveBuscarLojasAbertasDaCidadePeloNome`; `ProductItemServiceTest`:
    `deveBuscarProdutosDisponiveisDaCidade`; `SupplierControllerTest` e
    `ProductItemControllerTest`: `deveRepassarABuscaComACidade`.
    **Frontend** — services: parâmetros `q`, `cityId` e `page`;
    `supplier-list.component.spec.ts`: `deveBuscarLojasDaCidadeAoDigitar`,
    `deveBuscarProdutosNaVisaoProdutos`, `deveLimparABusca`,
    `devePaginarProdutos`, `deveAlternarEntreLojasEProdutos`;
    `product-card.component.spec.ts`: `deveUsarAAcaoDeDestaqueNoAdicionar`;
    `category-chip-bar.component.spec.ts`:
    `deveDestacarOChipAtivoSemFundoEscuro`; `pagination.component.spec.ts`:
    `deveMarcarAPaginaAtualSemFundoEscuro`.
22. *(cliente, §5.22)* `menu.component.spec.ts`: `deveUsarBarraClaraParaCliente`,
    `deveDestacarOLinkAtivoDoClienteEmTomClaro`,
    `deveManterBarraEscuraParaOsOutrosPerfis`,
    `deveUsarAAcaoDeDestaqueNoFinalizarPedido`.

CSS puro (Grupos A–E) sem teste automatizado — grep + visual manual.

## 8. Definição de Pronto

**Fatia 1 (HU02) — concluída:**
- [x] `menu.component` usa `--primary-color`; endereço visível no mobile.
- [x] `BottomTabBarComponent` criado e funcional (§5.3).
- [x] `CategoryChipBarComponent`/`ProductCardComponent`/`SupplierCardComponent`
      criados e integrados.
- [x] `supplier-list`/`product-list` reconstruídos conforme Figura 11, sem
      quebrar a lógica de filtro/ordenação do HU02 (testes existentes
      continuam verdes).
- [x] Banners estáticos e placeholder de rating implementados.
- [x] Testes da seção 7 (itens 1-7) verdes; `ng build` sem erro.

**Fatia 2 (HU01/HU04) — concluída:**
- [x] `manage-addresses` e `address-form` no padrão (referência Figura 3c).
- [x] `first-access-address` sem HEX (gradiente nos tokens `primary`/`secondary`).
- [x] `customer-panel` reescrito no padrão; CSS de 317 linhas removido.
- [x] Seletor de endereço do `order-resume` no padrão.

**Fatia 3 — toasts (antecipada, na branch da HU08):**
- [x] Um toast por vez, no topo central, com visual por tokens.
- [x] Nenhum toast de sucesso em busca/leitura (lista da §5.10); login
      com boas-vindas; uma ação gera um toast só.
- [x] Sem mensagem genérica: toasts com a causa, 500 sem detalhe técnico,
      403/400/502 nos casos da §5.10, mensagens do backend em português.
- [x] Senha errada devolve 401 "E-mail ou senha inválidos." (não 500).
- [x] Erros com a mensagem do backend pelo helper único.
- [x] Testes da seção 7 (itens 10, 11 e 11b) verdes; specs antigos
      falhando não aumentam; teste manual no Chromium.

> Verificado em 2026-10-18: `ng test` 205 ok e as 22 falhas antigas;
> E2E no Chromium (celular 420 px e desktop 1280 px): login errado 2×
> mostra 1 toast, um aviso seguinte substitui o erro, toast no topo
> central, login certo e "Meus Pedidos" sem toast.

**Fatia 4 — Meus Dados e menu do usuário (§5.11):**
- [x] `PUT /api/users/{id}` só edita o próprio usuário (403 nos demais;
      ADMIN pode) e não troca e-mail por usuário comum; documentado no
      Swagger.
- [x] Avatar com iniciais (atualiza ao salvar) e dropdown do usuário no
      padrão, com itens por perfil, Esc, clique fora e foco.
- [x] `/meus-dados` para cliente, fornecedor e entregador, com avatar,
      skeleton, e-mail somente leitura e estados de salvando; antiga
      `/painel-cliente` redireciona.
- [x] Testes da seção 7 (itens 8-9) verdes; `mvn test` e `ng test`
      (≤ 19 falhas antigas); sem HEX novo; teste manual e E2E.

> Verificado em 2026-10-20: backend 256 testes (único erro: `BackendApplicationTests.contextLoads`, que precisa do MySQL local);
> frontend 309 testes com 18 falhas antigas (uma a menos); E2E por API (11 checagens, incluindo o ataque ao `PUT /users/{id}` com
> contas de fornecedor, cliente e entregador: 403 e banco intacto) e por interface (19 checagens: avatar, dropdown por perfil, Esc,
> clique fora, foco, `/meus-dados`, redirecionamento da rota antiga, avatar atualizando sem recarregar, e carrinho sem sobrepor o
> avatar em 390 e 360 px).
>
> **Defeitos encontrados no teste e corrigidos**: (1) o `PUT /api/users/{id}` aceitava editar qualquer usuário (tomada de conta com o
> login por código); (2) o menu reaproveitava o usuário do início do app e mostraria a sessão anterior após novo login; (3) os itens
> do dropdown eram recriados a cada ciclo de detecção, e o foco voltava ao `body`; (4) o carrinho encolhia para 6 px no celular e
> ficava embaixo do avatar.
>
> **Dívidas registradas (fora desta fatia)**: `GET /api/users?id=` ainda lê o usuário de qualquer id (só leitura, sem checagem de
> dono); `DELETE /api/users` e rotas de cadastro de ADMIN não foram revisadas; a troca de e-mail com confirmação no endereço novo.

**Fatia 5 — navbar, footer, tema estrutural e varredura de HEX (§5.12):**
- [x] Tema `h2gas` com as variáveis estruturais; campos com borda sem
      remendo.
- [x] Casca do app sem wrappers de documento; footer escondido nas telas
      de autenticação e do entregador.
- [x] Navbar sem recarregar o app, com link ativo, logo por perfil,
      carrinho só do cliente, hambúrguer só de fornecedor/admin, ícones
      Material e painel mobile animado que fecha ao navegar e com Esc.
- [x] Footer nos tokens, com ano automático, compacto no celular.
- [x] Testes da seção 7 (item 12) verdes; `ng test` (≤ 18 falhas
      antigas) e `ng build`; teste manual e E2E (navegação sem recarregar,
      SSE sem reconectar, prints em 390 e 1280 px).

> Verificado em 2026-10-20: frontend 322 testes com as 18 falhas antigas (providers faltando em specs antigos); `ng build` sem
> erro; E2E no Chromium com 14 checagens (admin pela navbar desktop, fornecedor pelo menu mobile, cliente em "Meus Pedidos":
> marcador em `window` sobrevive, nenhum documento recarregado, `aria-current` no link ativo, painel fecha ao navegar e com Esc, o
> SSE continua com uma conexão ao clicar em "Pedidos"); grep sem HEX nos componentes (fora `distributor-management` e specs que
> verificam a ausência de HEX); prints antes/depois em 390 e 1280 px dos quatro perfis, landing, Sobre, Contato e telas de admin.
>
> **Achados do teste**: (1) com `href`, o próprio Karma recarregava a página ao clicar num link da navbar — o defeito reproduzido
> no teste; (2) a coluna "Ativo" da aprovação de fornecedores não mostrava os interruptores (o `toggle` do DaisyUI depende das
> variáveis estruturais do tema) — voltaram com o tema; (3) o "body" dentro dos templates de admin recebia a regra `body` do CSS
> legado (fundo cinza, `min-h-screen`) — o fundo virou `bg-base-200` num wrapper; (4) os ícones dentro dos campos do Meus Dados,
> do login por senha e do cadastro ficavam atrás do `input` do DaisyUI — sobem com `z-10`; (5) a página Sobre carregava uma
> segunda cópia do Tailwind 2 por CDN.
>
> (6) o "Início" do admin (`/inicio`) caía no cadastro de primeiro endereço — o admin agora passa sem endereço e vê o catálogo
> sem filtro de cidade.

- [x] Grupos A–E migrados pros tokens (grep: zero hex novo fora de
      `styles.css`).

**Fatia 6 — aprovação de fornecedores (§5.13):**
- [x] `GET /api/suppliers` filtra por status e busca no backend, com
      OpenAPI.
- [x] Tela com filtros, busca, cards, confirmação, skeleton, estado
      vazio e paginação; erro não muda o status.
- [x] Testes da seção 7 (item 13) verdes; `mvn test`, `ng test` (sem
      falhas novas) e `ng build`; teste manual em 390 e 1280 px.

> Verificado em 2026-10-21: backend 306 testes (único erro: `BackendApplicationTests.contextLoads`, que precisa do MySQL local);
> frontend 363 testes com 16 falhas antigas, nenhuma desta tela; `ng build` sem erro; E2E no Chromium em 1280 e 390 px com 17
> checagens (abre em "Aguardando", filtros, busca "batel", estado vazio, aprovar e desativar de verdade com o dado devolvido ao
> estado original, "Voltar" fecha o modal, sem rolagem lateral nem erro de JS).
>
> **Achados do teste**: (1) a tela entrava no bundle inicial e estourava o orçamento de 1 MB do `ng build` por 1 kB — a rota
> passou a `loadComponent` (tela só do admin); (2) no celular o campo de busca não ocupava a largura toda.

**Fatia 6 — cidades com a base do IBGE (§5.14):**
- [x] Cidade cadastrada só por código IBGE, com nome e UF oficiais;
      409/400/502 com mensagem; exclusão de cidade em uso é 409.
- [x] CEP de usuário novo casa com a cidade pelo código IBGE (e por
      nome + UF nas antigas) no `address-form` e no `distribuitor-form`.
- [x] Tela com dropdowns de UF e município, busca, confirmação de
      exclusão; rotas com OpenAPI.
- [x] Testes da seção 7 (item 14) verdes; `mvn test`, `ng test` (sem
      falhas novas) e `ng build`; teste manual em 390 e 1280 px.

> Verificado em 2026-10-21: backend 316 testes (único erro: `contextLoads`, que precisa do MySQL local); Flyway V12 aplicada no
> MySQL local (Curitiba com 4106902); frontend 377 testes com 15 falhas antigas (uma a menos: o spec antigo de `city-register`);
> `ng build` sem erro; E2E com o IBGE real em 14 checagens (27 UFs, 399 municípios do PR, Curitiba desabilitada, cadastro de São
> José dos Pinhais pela tela, 409 duplicada, 400 para código inexistente e para nome digitado, 409 ao excluir Curitiba em uso,
> CEP 83005-000 de cliente preenchendo "São José dos Pinhais - PR", CEP de Florianópolis avisando "Florianópolis - SC", exclusão
> de cidade sem uso, celular sem rolagem lateral). A cidade de teste foi removida no fim.
>
> **Achados do teste**: o `address-form` não mostrava qual cidade o CEP casou — ganhou o campo "Cidade" só leitura.

**Fatia 6 — configuração da loja (§5.15):**
- [x] Horário, tempo de entrega e estoque mínimo salvos no backend pelo
      fornecedor do token; `update-times` de outro fornecedor é 403;
      `V13` aplicada; OpenAPI.
- [x] Tela com um formulário, validação por campo, prévia, Salvar só com
      alteração válida e um toast; tela de produtos usa o limite salvo.
- [x] Testes da seção 7 (item 15) verdes; `mvn test`, `ng test` (sem
      falhas novas) e `ng build`; teste manual em 390 e 1280 px.

> Verificado em 2026-10-21: backend 262 testes (único erro: `contextLoads`, que precisa do MySQL local); `V13` aplicada no MySQL
> local; frontend 330 testes com 17 falhas antigas de providers (uma a menos: o spec antigo da tela); `ng build` sem erro; E2E em
> 1280 e 390 px com 20 checagens (carrega do token, prévia, Salvar desabilitado sem alteração, erro de fechamento igual, horário
> até 02:00 "do dia seguinte", um toast só, valor salvo após recarregar, contador de estoque baixo em Produtos seguindo o limite,
> `update-times` de outro fornecedor 403, 400 da API, sem rolagem lateral nem erro de JS, dado original restaurado).
>
> **Achados do teste**: no celular, o aviso "Alterações não salvas" saía pela esquerda da barra fixa — passou para cima do botão.
> O "Configurações" do menu do admin aponta para uma rota que agora é só do fornecedor; a HU12 já tira esse item do menu.

**Fatia 6 — produtos do fornecedor (§5.16):**
- [x] Lista, filtros e contadores calculados no backend para o
      fornecedor do token; exclusão de produto alheio é 403 e de produto
      vendido é 409 com orientação; OpenAPI.
- [x] Tela com contadores-filtro, busca, selo de estoque correto,
      confirmação de exclusão, skeleton, estado vazio e paginação.
- [x] Testes da seção 7 (item 16) verdes; `mvn test`, `ng test` (sem
      falhas novas) e `ng build`; teste manual em 390 e 1280 px.

> Verificado em 2026-10-21: backend 270 testes (único erro: `contextLoads`, que precisa do MySQL local); frontend 338 testes com as
> 17 falhas antigas de providers; `ng build` sem erro; E2E em 1280 e 390 px com 22 checagens no Gás Centro (resumo 7/6/1/1 com
> limite 8, 403 ao excluir produto alheio, 409 ao excluir produto vendido com toast orientando a zerar o estoque, 400 para filtro
> inválido, selos Esgotado/Estoque baixo, filtro pelo contador, busca "minalba", estado vazio, link para a configuração, sem
> rolagem lateral nem erro de JS); o estoque mexido para o teste foi restaurado.
>
> **Observação**: com a HU12 mergeada, a rota da tela é só do fornecedor, como as rotas `/mine`.

**Fatia 6 — categorias do fornecedor (§5.17):**
- [x] Categoria criada, editada e excluída só pelo dono do token;
      nome repetido e categoria com produtos dão 409 com mensagem;
      validação no boundary; OpenAPI.
- [x] Tela agrupada por tipo, com contagem de produtos, modal com
      validação e exclusão protegida.
- [x] Popup com tipo em cartões, sugestões de nome, prévia, aviso de
      repetido antes de enviar, contador e Esc (ajuste pedido).

> Popup verificado em 2026-10-21: 13 specs da tela verdes; frontend 405 testes com as 12 falhas antigas; `ng build` sem erro; E2E
> em 1280 e 390 px com 14 checagens (foco no nome, sugestões sem as que a loja já tem, repetido avisado antes de enviar, prévia,
> criação pela sugestão, Esc fecha, sem rolagem lateral nem erro de JS); categoria de teste removida. **Achado**: as categorias
> do banco local se chamam "Água 10L"/"Água 20L", e o popup ainda sugeria "10L"/"20L" — a sugestão some quando já aparece como
> palavra no nome de uma categoria do mesmo tipo.
- [x] Testes da seção 7 (item 17) verdes; `mvn test`, `ng test` (sem
      falhas novas) e `ng build`; teste manual em 390 e 1280 px.

> Verificado em 2026-10-21: backend 338 testes (único erro: `contextLoads`, que precisa do MySQL local); frontend 400 testes com 12
> falhas antigas de providers (duas a menos: os specs antigos do service e da tela); `ng build` sem erro; E2E em 1280 e 390 px com
> 16 checagens (contagem por categoria, 403 ao editar categoria alheia, 409 ao excluir com produtos, `supplierId` do corpo
> ignorado, criação pela tela, 409 de nome repetido com o modal aberto, modal de exclusão bloqueada, exclusão da vazia, sem
> rolagem lateral nem erro de JS); as categorias criadas no teste foram removidas.
>
> **Achado do teste (dado, não código)**: no banco local, os produtos de gás do Gás Centro estão em categorias de outro
> fornecedor (`fixture-supplier-hu02`) ou sem dono, criadas antes da regra da HU10. A tela mostra só as categorias da loja (as de
> Água); esses produtos não aparecem em nenhum grupo dele.

**Fatia 6 — dados da loja com logo (§5.18):**
- [x] Perfil e logo da loja pelo fornecedor do token; editar a loja de
      outro é 403; editar o perfil não altera o horário; imagem validada
      (400) e falha de upload 502; `V14` aplicada; OpenAPI.
- [x] Tela "Dados da loja" com logo, formulário validado e endereço pelo
      CEP; logo no card do catálogo com o ícone de reserva.
- [x] Testes da seção 7 (item 18) verdes; `mvn test`, `ng test` (sem
      falhas novas) e `ng build`; teste manual em 390 e 1280 px.

> Verificado em 2026-10-21: backend 348 testes (único erro: `contextLoads`, que precisa do MySQL local); `V14` aplicada no MySQL
> local; frontend 420 testes com as 12 falhas antigas de providers; `ng build` sem erro; E2E em 1280 e 390 px com 19 checagens
> (403 ao editar outra loja pelo id, dados e iniciais, .txt e imagem > 1 MB recusados antes de enviar, falha do Cloudinary com o
> 502 amigável, logo salva na tela, um toast ao salvar, horário intacto ao salvar o perfil, cidade no modal de endereço, logo no
> card do catálogo para o cliente e ícone na loja sem logo, remoção da logo, sem rolagem lateral nem erro de JS); dados do Gás
> Centro restaurados no fim.
>
> **Achados do teste**: (1) as credenciais do Cloudinary no `.env` local são recusadas ("Invalid Signature") — o envio real não
> pôde ser testado; a exibição foi testada com uma imagem pública de demonstração do Cloudinary gravada no banco. (2) O SDK do
> Cloudinary lança `RuntimeException` quando a API recusa, e o envio virava 500 — agora é 502 com mensagem clara. O upload de
> foto de produto (`ProductItemService`) tem o mesmo defeito: **dívida registrada**. (3) A logo que não carrega volta para as
> iniciais também na tela da loja. Dívida também registrada: o `PUT /addresses/{id}` não confere o dono.
>
> **Envio real verificado depois (2026-10-21)**: o `.env` local tinha a API Key no lugar do secret, depois ficou sem o
> `@<cloud>` no fim, e a chave tinha permissão restrita ("missing permissions (actions=create)"). Com o `CLOUDINARY_URL`
> corrigido pelo autor, o E2E passou 5/5: upload aceito, URL `https://res.cloudinary.com/<cloud>/...` gravada, imagem pública e
> logo no card do catálogo para o cliente. A logo de teste foi tirada do Gás Centro no fim.

- [x] Tela única "Minha loja" para o fornecedor: `/meus-dados` e
      `/distributor/edit/:id` levam a `/minha-loja`, e-mail só leitura,
      menu com "Minha loja" e atualizado ao salvar o nome (ajuste pedido).

> Tela única verificada em 2026-10-21: backend 349 testes (único erro: `contextLoads`); frontend 425 testes com as 12 falhas
> antigas; `ng build` sem erro; E2E em 1280 e 390 px com 15 checagens (menu com "Minha loja" e sem "Meus Dados", e-mail só
> leitura, `/meus-dados` e `/distributor/edit/<id>` caindo em `/minha-loja`, iniciais do menu atualizadas ao salvar o nome sem
> recarregar, "Editar loja" de Pedidos recebidos levando a `/minha-loja`, cliente seguindo no Meus Dados, sem rolagem lateral nem
> erro de JS); nome restaurado. O `navigateEdit` de Pedidos recebidos virou `goToStore()`, e o do Histórico (sem uso) saiu.

**Fatia 6 — histórico de pedidos (§5.19):**
- [x] Histórico e resumo pelo fornecedor do token, filtrados e contados
      no banco; rotas antigas por `supplierId` dão 403 para outra loja;
      OpenAPI.
- [x] Tela com resumo, filtros, cards expansíveis, skeleton, estado vazio
      e paginação, sem o `orders-widget`.
- [x] Testes da seção 7 (item 19) verdes; `mvn test`, `ng test` (sem
      falhas novas) e `ng build`; teste manual em 390 e 1280 px.

> Verificado em 2026-10-21: backend 357 testes (único erro: `contextLoads`); frontend 434 testes com 10 falhas antigas de
> providers (duas a menos: os specs antigos do histórico e do `orders-widget`); `ng build` sem erro; E2E em 1280 e 390 px com 16
> checagens no Gás Centro, comparando a tela com o banco (resumo de 30 dias: 46 entregues, R$ 7.203,00, 19 cancelados; PIX
> entregue no período todo; + retirada; Limpar; busca por cliente; motivo do cancelamento no detalhe; paginação; 403 de outra
> loja em `exclude-status`, `by-supplier` e `PATCH /suppliers/{id}/status`; sem rolagem lateral nem erro de JS).
>
> **Achados**: (1) o `PATCH /api/suppliers/{id}/status` (abrir/fechar loja) também não conferia o dono — agora é 403 para outra
> loja; (2) o link "Histórico" de Pedidos recebidos usava `href` e recarregava o app — virou `routerLink`; (3) no celular, a
> data do card dividia a linha com o nome do cliente e era cortada — foi para a linha de pagamento.

**Fatia 6 — dashboard com filtros (§5.20):**
- [x] Filtros de pagamento, modo, tipo, categoria e produto no backend
      (itens que batem), com ids nas quebras, quebra por categoria da
      loja e por modo; 400 em filtro inválido; OpenAPI.
- [x] Tela com filtros, gráficos clicáveis que filtram, chips, gráfico
      diário com eixo e destaque, pizza com total; estado vazio do filtro.
- [x] Testes da seção 7 (item 20) verdes; `mvn test`, `ng test` (sem
      falhas novas) e `ng build`; teste manual em 390 e 1280 px.

> Verificado em 2026-10-21: backend 365 testes (único erro: `contextLoads`); frontend 443 testes com 10 falhas antigas; `ng
> build` sem erro; E2E em 1280 e 390 px com 13 checagens no Gás Centro comparando com o banco (receita e pedidos do mês; clicar
> em PIX filtra e clicar de novo tira; clicar no tipo Gás soma só os itens de gás; retirada; retirada + cartão; melhor dia e
> eixo; sem rolagem lateral nem erro de JS).
>
> **Decisões da implementação**: o gráfico diário ficou em colunas (total por dia é discreto), com eixo de três marcas nos
> valores alcançados, grade tracejada discreta, o melhor dia em outra cor e o valor de cada dia no `title`; as cores
> categóricas foram validadas pelo script do skill de visualização (a paleta antiga reprovava: azul-marinho fora da faixa de
> luminosidade e par rosa/verde com ΔE 3,9 para daltônicos) e entraram como tokens `--color-chart-1..5` no tema. **Achado**: no
> celular, rótulos longos ("Cartão de crédito") colavam nos valores — as linhas passaram a quebrar.

**Cliente — Início e Produtos (§5.21):**
- [x] Busca de lojas e de produtos no backend com a cidade do endereço
      ativo (RN05); OpenAPI.
- [x] Hierarquia visual: só "Adicionar" cheio (accent); demais ações
      claras; banners suaves; busca sem botão; paginação dos produtos.
- [x] Testes da seção 7 (item 21) verdes; `mvn test`, `ng test` (sem
      falhas novas) e `ng build`; teste manual em 390 e 1280 px.

> Verificado em 2026-10-21: backend 369 testes (único erro: `contextLoads`); frontend 456 testes com as 10 falhas antigas; `ng
> build` sem erro; E2E em 1280 e 390 px com 17 checagens como `cliente.teste` em Curitiba (nenhum botão marinho cheio no
> conteúdo, "Adicionar" em `rgb(42,120,214)`, lojas da cidade, busca "centro" sem botão só na cidade, limpar, 14 produtos pelo
> total do backend, paginação 12 + 2, busca "minalba" na visão Produtos, sem rolagem lateral nem erro de JS).
>
> **Auditoria com a skill Impeccable (2026-10-21, 13/20)** e correções: (1) o `accent` `#2a78d6` dava 4,42:1 com texto branco
> (a conta anterior de ≈4,6:1 estava errada) — virou `#2772ca`, 4,83:1; (2) "Sem avaliações ainda" em `gray-400` (2,6:1) passou
> para `gray-600`; (3) o banner prometia "Até 15% OFF" sem promoção real — o texto virou "Botijões de vários tamanhos, com
> entrega ou retirada"; (4) alvos de toque de 44 px no celular (alternância, busca, "Menor preço", "Adicionar", "Ver produtos" e
> paginação), mantendo 32 px no desktop; (5) imagem do produto com `loading="lazy"` e tamanho reservado; (6) `h2` "Produtos
> disponíveis" (oculto) para não pular de `h1` para `h3`. Os 2 avisos de imagem quebrada do detector estático são falsos
> positivos (`src` ligado por Angular). Navegação por teclado não foi verificada. Spec: `deveManterAHierarquiaDeTitulosNaVisaoProdutos`,
> `deveUsarTextoVerdadeiroNosBanners`, `deveTerAlvosDeToqueDe44px…`, `deveCarregarAImagemSobDemandaComTamanhoReservado`.
>
> **Achados**: (1) a busca antiga de lojas (`/suppliers/search`) ignorava a cidade (RN05) — o frontend passou a usar
> `/actives/open?q=` e o `searchSuppliers` saiu; (2) a visão Produtos não buscava nem paginava — agora busca pelo backend e pagina
> de 12 em 12. A rota `/suppliers/search` continua no backend, mas sem uso no frontend (candidata a sair).

**Cliente — barra de navegação (§5.22):**
- [x] Barra clara para o cliente (contraste ≥ 4,5:1, logo visível), barra
      escura mantida nos outros perfis; "Finalizar Pedido" em destaque.
- [x] Testes (item 22) verdes; `ng test` com as 10 falhas antigas; `ng
      build`; E2E em 1280 e 390 px: 16 checagens (barra branca, contrastes
      medidos no navegador, logo, dropdown, carrinho, fornecedor ainda
      escuro, sem erro de JS).

**Pendente:**
- [ ] Revisado e aprovado (escopo completo do F7).
