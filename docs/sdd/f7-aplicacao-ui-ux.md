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

**Pendente (próximas fatias):**
- [ ] Grupos A–E migrados pros tokens (grep: zero hex novo fora de
      `styles.css`).
- [ ] Revisado e aprovado (escopo completo do F7).
