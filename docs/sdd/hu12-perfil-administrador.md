# HU12 — Perfil de Administrador

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU12 |
| Perfil | ADMIN |
| Prioridade MoSCoW | Should have — pedido do autor em 2026-10-20, depois da F7 Fatia 5 ("o ADM está inútil hoje") |
| Dependências | F3 (Flyway), F6 (OpenAPI), F7 (tokens, navbar da Fatia 5), HU09 (idioma visual do dashboard), HU11 (login por código) |
| Referência no TCC | Sem figura própria; extrapolação pelos critérios da F7 §5.7 (Grupo D) |

## 2. História de Usuário

Como administrador da plataforma, quero um painel com os números da
plataforma e a gestão dos usuários (listar, buscar, criar qualquer
perfil, editar, desativar e reativar), para operar o H2Gás sem depender
de acesso direto ao banco.

## 3. Critérios de Aceitação (BDD)

Decisões do autor (2026-10-20): gestão de usuários completa (listar e
buscar, criar outros admins, criar cliente/fornecedor/entregador,
desativar/reativar, editar); painel com contagens e pedidos (sem
gráficos); o admin **perde** os itens de cliente da navbar e o "Início"
dele vira o Painel.

- **Cenário 1 — Painel como tela inicial**: Dado um ADMIN logado, Quando
  entra no app ou clica no logo, Então vai para `/admin`, que mostra
  usuários por perfil, fornecedores ativos e aguardando aprovação,
  cidades, pedidos de hoje e do mês, pedidos por status e o faturamento
  do mês — tudo calculado no backend (RN01).
- **Cenário 2 — Listar e buscar usuários**: Dado o ADMIN em
  `/admin/usuarios`, Quando filtra por perfil ou digita parte do nome ou
  do e-mail, Então a lista paginada mostra só os usuários que batem, com
  o perfil e se a conta está ativa.
- **Cenário 3 — Criar usuário de qualquer perfil**: Dado o formulário
  "Novo usuário", Quando o ADMIN escolhe o perfil e preenche os dados,
  Então a conta é criada — inclusive outro ADMIN. Fornecedor exige CNPJ e
  cidade (nasce aguardando aprovação, como no cadastro público);
  entregador exige o fornecedor a que pertence.
- **Cenário 4 — Desativar e reativar**: Dado um usuário ativo, Quando o
  ADMIN o desativa (com confirmação), Então ele não entra mais por senha
  nem por código (403 "Conta desativada. Fale com o suporte.") e o token
  que já tinha deixa de valer; reativar devolve o acesso. O ADMIN não
  desativa a si mesmo.
- **Cenário 5 — Editar**: Dado um usuário na lista, Quando o ADMIN edita
  nome, telefone ou e-mail, Então os dados são salvos (o
  `PUT /api/users/{id}` já aceita ADMIN e respeita a unicidade do
  e-mail).
- **Cenário 6 — Rotas protegidas**: Dado um usuário que não é ADMIN,
  Quando chama qualquer rota `/api/admin/**`, Então recebe 403; no
  frontend, `/admin` e `/admin/usuarios` exigem o papel ADMIN.
- **Cenário 7 — Tipos de produto são do admin; produtos e categorias, do
  fornecedor** (decisão do autor, 2026-10-20): Dado o ADMIN em
  `/admin/tipos`, Quando cadastra ou renomeia um tipo (Gás, Água…),
  Então o tipo fica disponível para todos os fornecedores escolherem ao
  criar as próprias categorias. O fornecedor não cria mais tipo (403), e
  o ADMIN não cria, edita nem apaga categoria ou produto (403) — essas
  telas saem do menu dele. Os dados existentes ficam como estão (os tipos
  atuais já são globais).
- **Cenário 8 — Ícone do tipo** (decisão do autor, 2026-10-20): Dado o
  ADMIN cadastrando ou editando um tipo, Quando escolhe um ícone numa
  grade de ícones prontos, Então o catálogo do cliente mostra esse ícone
  no chip do tipo. Gás e Água já saem da migração com chama e gota; tipo
  sem ícone escolhido usa o genérico.

## 4. Estado atual (achados)

- O admin só tem três telas próprias (cidades, aprovação de
  fornecedores, categorias); o "Início" dele é o catálogo do cliente e
  "Pedidos" é o "Meus Pedidos" do cliente.
- Não há rota para listar usuários; `UserService.create` bloqueia o
  cadastro de ADMIN ("Cadastro de usuário ADMIN não permitido…"), regra
  certa para o `/auth/register` público, e não existe painel.
- `User.isEnabled()` sempre devolve `true` — não há conta desativada.
- **`Supplier` estende `User` (herança `JOINED`) e já tem `active`**
  (coluna `suppliers.active`), que significa **aprovado pelo admin**.
  Por isso a conta ativa do usuário **não** se chama `active`: é
  `enabled` (coluna `users.enabled`), o mesmo nome do `isEnabled()` do
  Spring Security. (O plano previa `users.active`; mudou por esse
  conflito.)
- `Deliverer` sempre pertence a um `Supplier`; hoje só o fornecedor cria
  entregadores (`POST /api/deliverers`).
- Última migração: `V8__login_tokens.sql`.
- Hierarquia do catálogo: `ProductType` (global: Gás, Água) →
  `CategoryProduct` (de **cada fornecedor**: coluna `supplier`) →
  `ProductItem` (do fornecedor: marca, preço, estoque). Nenhuma tela cria
  tipo; o `POST /api/product-types` aceita ADMIN e SUPPLIER, sem
  validação nem nome único. As rotas de escrita de categoria e produto
  aceitam ADMIN, e o menu "Produtos" do admin leva a essas telas.
- O ícone do tipo não é gravado: o `CategoryChipBarComponent` deduz pelo
  nome ("gás" → chama, "água" → gota, resto → genérico), então um tipo
  novo sempre aparece com o ícone genérico.

## 5. Contrato Técnico

### 5.1 Conta ativa
- `V9__user_enabled.sql`: `ALTER TABLE users ADD COLUMN enabled BOOLEAN
  NOT NULL DEFAULT TRUE`.
- `User.enabled` (padrão `true`); `isEnabled()` devolve o campo.
- Login por senha de conta desativada: o `AuthenticationManager` lança
  `DisabledException` → **403** "Conta desativada. Fale com o suporte."
  (tratada antes do 401 genérico no `GlobalExceptionHandler`).
- Login por código (`LoginTokenService.validate`): código certo de conta
  desativada → **403** com a mesma mensagem (`ForbiddenResourceException`),
  sem emitir JWT. A solicitação do código continua respondendo 200 (sem
  enumeração, HU11 1a), mas não envia e-mail para conta desativada.
- `SecurityFilter`: token válido de usuário desativado não autentica (a
  requisição segue anônima → 401/403 da rota).

### 5.2 Rotas `/api/admin/**` (`@Tag("Administração")`, `@RolesAllowed("ADMIN")`)

| Método | Rota | Corpo / parâmetros | Respostas |
|---|---|---|---|
| GET | `/api/admin/users` | `type` (opcional, `UserTypeEnum`), `q` (nome ou e-mail, opcional), `page`, `size` | 200 `PaginatedResponse<AdminUserDto>`, 401, 403 |
| POST | `/api/admin/users` | `AdminCreateUserDto` | 201 `AdminUserDto`, 400, 401, 403, 404 |
| PATCH | `/api/admin/users/{id}/enabled` | `{ "enabled": boolean }` | 204, 400, 401, 403, 404 |
| GET | `/api/admin/dashboard` | — | 200 `AdminDashboardDto`, 401, 403 |

- `AdminUserDto`: `id`, `name`, `email`, `phoneNumber`, `userType`,
  `enabled`. Ordenado por nome.
- `AdminCreateUserDto`: `name` (3+), `email` (formato), `password`
  (6+), `phoneNumber`, `cpf` (opcional), `userType` (obrigatório,
  qualquer perfil), `cnpj` e `cityId` (obrigatórios para SUPPLIER),
  `supplierId` (obrigatório para DELIVERER). Reaproveita a criação do
  `UserService` (mesma unicidade de e-mail: 400 "E-mail já cadastrado.");
  cidade ou fornecedor inexistente → 404.
- `PATCH …/enabled`: o ADMIN não altera a própria conta → 400 "Você não
  pode desativar a sua própria conta."; usuário inexistente → 404.
- `AdminDashboardDto`: `usersByType` (mapa perfil → total),
  `activeSuppliers`, `pendingSuppliers` (fornecedor com `active = false`),
  `cities`, `ordersToday`, `ordersThisMonth`, `ordersByStatus` (mapa
  status → total, todos os pedidos), `revenueThisMonth` (soma do
  `totalPrice` dos pedidos do mês **não cancelados**, por `createdAt`).
- Edição: reaproveita `PUT /api/users/{id}` (sem mudança).

### 5.2.1 Tipos de produto e permissões do catálogo (Cenário 7)

| Método | Rota | Papel | Respostas |
|---|---|---|---|
| GET | `/api/product-types` | ADMIN, SUPPLIER, CUSTOMER (sem mudança) | 200 |
| POST | `/api/product-types` | **só ADMIN** (antes ADMIN e SUPPLIER) | **201**, 400, 401, 403 |
| PUT | `/api/product-types/{id}` | **só ADMIN** (nova) | 200, 400, 401, 403, 404 |

- `ProductTypeDTO.name`: obrigatório (`@NotBlank`), até 60 caracteres,
  sem espaços nas pontas; nome repetido (sem diferenciar maiúsculas) →
  400 "Tipo já cadastrado.". Apagar tipo fica fora (tem categorias
  apontando para ele).
- Escrita de categoria (`POST/PUT/DELETE /api/category-products`) e de
  produto (`POST/PUT/DELETE /api/product-items`, `GET
  /api/product-items/{id}` da edição) passam a ser **só SUPPLIER**.
  Leituras do catálogo (`by-supplier`, `by-supplier-open`) não mudam.
- Sem migração: os dados existentes ficam como estão.

### 5.2.2 Ícone do tipo (Cenário 8)
- `V10__product_type_icon.sql`: `ALTER TABLE product_types ADD COLUMN
  icon VARCHAR(40) NOT NULL DEFAULT 'category'`; grava
  `local_fire_department` nos tipos cujo nome contém "gás"/"gas" e
  `water_drop` nos que contêm "água"/"agua".
- `ProductTypeDTO.icon`: nome de um Material Icon **da lista fechada**
  abaixo; fora dela → 400 "Ícone inválido.". No `POST`, ausente → 
  `category`; no `PUT`, ausente mantém o ícone atual. O `GET` devolve o
  `icon`.
- Lista (ícone → rótulo na tela): `local_fire_department` Chama,
  `propane_tank` Botijão, `water_drop` Gota, `local_drink` Copo,
  `liquor` Garrafa, `ac_unit` Gelo, `outdoor_grill` Carvão,
  `cleaning_services` Limpeza, `shopping_basket` Cesta, `inventory_2`
  Caixa, `category` Genérico.

### 5.3 Frontend
- `services/admin.service.ts`: `listUsers`, `createUser`, `setEnabled`,
  `dashboard`.
- `homeFor('ADMIN')` → `/admin` (logo e login).
- Rotas `admin` (`AdminDashboardComponent`) e `admin/usuarios`
  (`AdminUsersComponent`), com `authGuard` e `roles: ['ADMIN']`.
- **Painel** (`components/pages/admin-dashboard`): cards no idioma do
  dashboard do fornecedor (HU09), skeleton, erro pelo `backendMessage`,
  card "Aguardando aprovação" que leva a `/supplier-approval`, faturamento
  em BRL.
- **Usuários** (`components/pages/admin-users`): chips de perfil (Todos,
  Clientes, Fornecedores, Entregadores, Admins), busca, paginação, selo
  ativo/desativado, botão desativar/reativar com confirmação, modal de
  edição (nome, telefone, e-mail) e modal "Novo usuário" com seletor de
  perfil — CNPJ e cidade para fornecedor, fornecedor para entregador.
- **Navbar do admin**: Painel (`/admin`), Usuários (`/admin/usuarios`),
  Fornecedores, Cidades e Tipos (`/admin/tipos`); saem "Início"
  (catálogo), "Pedidos" ("Meus Pedidos") e o menu Produtos. Dropdown do
  usuário: Meus Dados e Painel.
- **Tipos** (`components/pages/admin-product-types`): lista com o ícone
  de cada tipo, formulário "Novo tipo" e editar na linha (nome e ícone),
  os dois com a grade de ícones da §5.2.2 (botões com rótulo acessível e
  `aria-pressed`); erro pelo `backendMessage`. Rota `/admin/tipos` com
  `roles: ['ADMIN']`.
- **Chips do catálogo** (`CategoryChipBarComponent`): usam o `icon` do
  tipo; sem `icon`, o genérico. Sai a dedução pelo nome.
- Rotas `category-products` e `product-items*` passam a `roles:
  ['SUPPLIER']`.
- Tokens F2, sem HEX novo.

## 6. Plano de Testes (TDD)

**Backend**
1. `AuthenticationControllerTest`: `deveRecusarLoginPorSenhaDeContaDesativada` (403).
2. `LoginTokenServiceTest`: `deveRecusarLoginPorCodigoDeContaDesativada`,
   `naoDeveEnviarCodigoParaContaDesativada`.
3. `SecurityFilterTest`: `deveIgnorarTokenDeUsuarioDesativado`.
4. `AdminUserServiceTest`: `deveListarFiltrandoPorPerfilEBusca`,
   `deveCriarOutroAdmin`, `deveExigirCnpjECidadeParaFornecedor`,
   `deveExigirFornecedorAoCriarEntregador`,
   `deveCriarEntregadorVinculadoAoFornecedor`,
   `deveDesativarEReativarUsuario`,
   `naoDevePermitirAdminDesativarASiMesmo`.
5. `AdminUserControllerTest`: `deveRetornar403ParaNaoAdmin`,
   `deveRetornar201AoCriarUsuario`, `deveRetornar204AoDesativar`.
6. `AdminDashboardServiceTest`: `deveResumirAPlataformaParaOAdmin`,
   `naoDeveSomarPedidosCanceladosNoFaturamento`.
6a. `ProductTypeServiceTest`: `deveCriarTipoSemEspacosNasPontas`,
    `deveRecusarTipoRepetidoSemDiferenciarMaiusculas`,
    `deveRenomearTipo`, `deveRetornar404AoRenomearTipoInexistente`.
6b. `ProductTypeControllerTest`: `deveCriarTipoComoAdmin` (201),
    `deveRetornar403AoFornecedorCriarTipo`, `deveRetornar400ComNomeVazio`.
6c. `CatalogoPermissoesTest` (categoria e produto):
    `deveRetornar403AoAdminEscreverCategoria`,
    `deveRetornar403AoAdminEscreverProduto`.
6d. `ProductTypeServiceTest`: `deveGravarIconeEscolhido`,
    `deveUsarIconeGenericoQuandoAusente`, `deveRecusarIconeForaDaLista`,
    `deveManterIconeAoRenomearSemIcone`.

**Frontend**
7. `admin.service.spec.ts`: rotas e parâmetros de cada método.
8. `home-route.spec.ts`: `deveLevarCadaPerfilAoSeuInicio` (ADMIN → `/admin`).
9. `admin-dashboard.component.spec.ts`: `deveMostrarOsNumerosDaPlataforma`,
   `deveLevarAosFornecedoresAguardandoAprovacao`,
   `deveMostrarAMensagemDoBackendNoErro`.
10. `admin-users.component.spec.ts`: `deveFiltrarPorPerfil`,
    `deveBuscarPorNomeOuEmail`, `deveDesativarComConfirmacao`,
    `deveExigirFornecedorParaEntregador`, `deveCriarAdmin`,
    `deveEditarUsuario`.
11. `menu.component.spec.ts`: `deveMostrarPainelEUsuariosParaAdmin`
    (sem Início do catálogo, sem Meus Pedidos e sem Produtos; com Tipos).
12. `admin-product-types.component.spec.ts`: `deveListarOsTipos`,
    `deveCriarTipo`, `deveRenomearTipo`, `deveMostrarErroDeTipoRepetido`.
13. `product-type.service.spec.ts`: criar e renomear.
14. `admin-product-types.component.spec.ts`: `deveCriarTipoComIcone`,
    `deveTrocarIconeAoEditar`.
15. `category-chip-bar.component.spec.ts`: `deveMostrarOIconeDoTipo`
    (e o genérico quando não há).

## 7. Fora de Escopo
- Apagar usuários; log de auditoria; gráficos no painel; redefinir a
  senha de outro usuário; permissões granulares entre admins; ações em
  lote; exportar a lista; painel de pedidos de todos os fornecedores com
  ações.
- Trocar a aprovação de fornecedor (`suppliers.active`) — segue na tela
  `supplier-approval`.
- Categorias globais curadas pelo admin (a categoria continua de cada
  fornecedor); apagar tipo de produto; unificar dados existentes.
- Upload de imagem ou ícone livre para o tipo; ícone na categoria ou no
  produto.

## 8. Definição de Pronto
- [x] Conta desativada não entra por senha nem por código e perde a
      sessão; migração `V9` aplicada.
- [x] `/api/admin/users` (listar, criar, ativar/desativar) e
      `/api/admin/dashboard` documentados no Swagger, só para ADMIN.
- [x] Painel como tela inicial do admin; tela de usuários com filtro,
      busca, criação, edição e desativação.
- [x] Navbar do admin sem itens de cliente.
- [x] Admin cadastra e renomeia tipos de produto; fornecedor não cria
      tipo; admin não escreve categoria nem produto (403) e não vê essas
      telas.
- [ ] Admin escolhe o ícone do tipo numa grade fechada; o catálogo do
      cliente mostra o ícone gravado; migração `V10` aplicada.
- [ ] Testes da seção 6 verdes; `mvn test`, `ng test` (sem falha nova
      além das 18 antigas) e `ng build`; E2E e prints em 390 e 1280 px.

**Verificação (20/10/2026):** `mvn test` 288 testes, só o
`BackendApplicationTests` com erro (precisa de MySQL em 3306, ambiente);
`ng test` 345 testes com 17 falhas, todas antigas de providers (o spec do
`ProductTypeService` saiu da lista); `ng build` ok (só o aviso de budget
que já existia). Swagger com a tag Administração e as rotas de tipos,
categorias e produtos com os status do contrato. E2E: 18/18 (usuários e
painel) e 15/15 (tipos e permissões do catálogo), prints em 390 e 1280 px.
O `updateProductItem` perdeu o atalho "dono ou ADMIN" da HU10: agora só o
dono edita.
