# F9 — Área da loja: hub de cards e configurações numa tela só

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F9 |
| Perfil | Fornecedor |
| Prioridade MoSCoW | Should have. Pedido do autor em 2026-10-28: "tem muita coisa separada, que poderíamos juntar"; "me perco"; "poderíamos ter flashcards das configurações e painel". |
| Dependências | F7 fatia 6 (Minha loja e Configurações), F8 (URLs `minha-loja/…`), HU09 (painel), HU18 (aberta pelo horário) |
| Antes de | HU17, que coloca o raio e o frete na seção "Funcionamento e entrega" |
| Revoga | F7 fatia 6, nos pontos em que define "Minha loja" como a tela única do fornecedor em `/minha-loja` e o painel como tela inicial dele |

## 2. Objetivo

Como fornecedor, quero uma tela inicial com atalhos para tudo o que cuido
na loja, cada um mostrando o que importa agora, e todas as configurações
da loja num lugar só, para não me perder entre telas.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Hub na entrada**: Dado o fornecedor logado, Quando entra
  no app ou abre `/minha-loja`, Então vê seis cards: Pedidos, Painel,
  Produtos, Entregadores, Loja e Histórico.
- **Cenário 2 — Número em cada card**:
  - Pedidos mostra quantos estão pendentes;
  - Painel mostra o faturamento do mês;
  - Produtos mostra quantos estão com estoque baixo ou zerado;
  - Entregadores mostra quantos estão livres de quantos;
  - Loja mostra se está aberta e quando fecha ou abre;
  - Histórico não tem número.
- **Cenário 3 — Card leva à tela**: Quando o fornecedor toca num card,
  Então abre a tela correspondente.
- **Cenário 4 — Falha isolada**: Dado que uma das fontes falhou, Então só
  aquele card mostra "—", e os outros seguem com o número.
- **Cenário 5 — Configurações numa tela**: Dado `/minha-loja/configuracoes`,
  Então a mesma tela tem as seções Aparência (logo e banner), Dados,
  Endereço, Funcionamento e entrega (horário e tempo médio) e Estoque
  (estoque mínimo).
- **Cenário 6 — Cada seção salva sozinha**: Quando o fornecedor salva uma
  seção, Então só os campos dela mudam. Salvar o estoque não desfaz uma
  edição do horário ainda não salva, e vice-versa.
- **Cenário 7 — Ir direto à seção**: Dado "Ajustar horário" (pedidos) ou
  "Ajustar o estoque mínimo" (produtos), Então a tela de configurações
  abre já na seção certa.
- **Cenário 8 — Menu sem repetição**: Dado o menu do fornecedor, Então o
  topo tem Início, Pedidos e Produtos, e o avatar tem Minha loja e
  Configurações da loja. Painel e Pedidos não aparecem duas vezes, e
  Produtos é link direto, sem um submenu com um item só.

## 4. Estado atual (achados)

- Duas telas da loja, cada uma com seu botão de salvar: `store-profile`
  em `/minha-loja` (logo, banner, nome, telefone, descrição, endereço) e
  `configuration` em `/minha-loja/configuracoes` (horário, tempo médio,
  estoque mínimo, com um botão para os três).
- Três links levam para outra tela: o card "Horário e entrega" do perfil,
  o "Ajustar horário" do `store-hours-status` e o "Ajustar o estoque
  mínimo" da lista de produtos.
- No menu, Painel e Pedidos aparecem no topo e no dropdown; "Produtos" é
  um dropdown com um item só ("Item produtos"); Minha loja e
  Configurações só estão no dropdown; o Histórico só se acha dentro de
  Pedidos.
- A tela inicial do fornecedor (`utils/home-route.ts`) é o painel de
  gráficos.

## 5. Contrato Técnico

### 5.1 Rotas (só frontend; a API não muda)

| URL | Tela |
|---|---|
| `/minha-loja` | **hub** novo (`supplier-home`). Passa a ser a tela inicial do fornecedor. |
| `/minha-loja/configuracoes` | **tela única da loja**: `store-profile` com as seções do `configuration` embutidas |
| `distributor/edit/:id` (link antigo) | redireciona para `minha-loja/configuracoes` |

As demais `minha-loja/…` (painel, pedidos, histórico, produtos,
entregadores) não mudam.

### 5.2 Hub

Usa só endpoints que já existem, sem backend novo:

| Card | Fonte | Destino |
|---|---|---|
| Pedidos | `GET /suppliers/mine/dashboard` → `pendingOrders` | `/minha-loja/pedidos` |
| Painel | o mesmo dashboard → `totalRevenue` do mês | `/minha-loja/painel` |
| Produtos | `GET /product-items/mine/summary` → `lowStock + outOfStock` | `/minha-loja/produtos` |
| Entregadores | `GET /deliverers/mine` → ativos e não ocupados / ativos | `/minha-loja/entregadores` |
| Loja | `GET /suppliers/{id}/open` + `GET /suppliers/mine/settings` | `/minha-loja/configuracoes` |
| Histórico | sem número | `/minha-loja/pedidos/historico` |

- Cada fonte é carregada sozinha. Em caso de falha, o card mostra "—",
  sem toast. Enquanto carrega, o número aparece como esqueleto.
- Os cards são links (`<a routerLink>`) com alvo de pelo menos 44 px, em
  grade `grid-cols-2 lg:grid-cols-3`, com os tokens F2 e sem HEX novo.

### 5.3 Tela única da loja

- O `store-profile` continua sendo a tela e embute o `configuration`
  (modo `embedded`), que deixa de ter rota própria. Assim o código e os
  testes de cada parte são reaproveitados.
- Seções com âncora: `aparencia`, `dados`, `endereco`, `funcionamento`,
  `estoque`. No topo, um sumário de botões rola até cada seção. Com
  `#fragmento` na URL, a tela rola até a seção depois de carregar.
- **Funcionamento e entrega** e **Estoque** têm um botão cada. O envio
  usa `PUT /suppliers/mine/settings` com os valores **salvos** das outras
  seções e só os campos desta (Cenário 6). O formulário guarda a edição
  pendente da outra seção.
- O card "Horário e entrega", que mandava para outra tela, sai.

### 5.4 Links e menu

- "Ajustar horário" (`store-hours-status`) vai para
  `/minha-loja/configuracoes#funcionamento`; "Ajustar o estoque mínimo"
  (`product-item`) vai para `#estoque`; "Editar loja" (`received-orders`)
  vai para `/minha-loja/configuracoes`.
- Menu do fornecedor:
  - topo e hambúrguer: Início (`/minha-loja`), Pedidos
    (`/minha-loja/pedidos`) e Produtos (`/minha-loja/produtos`);
  - dropdown do avatar: Minha loja (`/minha-loja`), Configurações da loja
    (`/minha-loja/configuracoes`) e Sair.

## 6. Plano de Testes (TDD)

1. `supplier-home.component.spec.ts`:
   - `deveMostrarOsSeisCards`, `deveLevarCadaCardParaATela`;
   - `deveMostrarOsPendentesEOFaturamento`, `deveMostrarOEstoqueBaixo`,
     `deveMostrarEntregadoresLivres`, `deveMostrarSeALojaEstaAberta`;
   - `deveMostrarTracoSeUmaFonteFalhar`.
2. `configuration.component.spec.ts`:
   - `deveSalvarSoOFuncionamento`, `deveSalvarSoOEstoque`;
   - `deveManterAEdicaoPendenteDaOutraSecao`;
   - `deveTerAsAncorasDasSecoes`.
   Os testes de validação existentes continuam.
3. `store-profile.component.spec.ts`: `deveEmbutirAsConfiguracoes`,
   `deveRolarAteASecaoDoFragmento`, `naoDeveTerOLinkParaOutraTela`.
4. `app.routes.spec.ts`:
   - `/minha-loja` aponta para o hub e `minha-loja/configuracoes` para a
     tela única, as duas com papel SUPPLIER;
   - `distributor/edit/:id` redireciona para `minha-loja/configuracoes`.
5. `home-route.spec.ts`: SUPPLIER vai para `/minha-loja`.
6. `menu.component.spec.ts`: o fornecedor vê Início, Pedidos e Produtos,
   sem duplicatas, e o dropdown tem Minha loja e Configurações da loja.
7. `store-hours-status`, `product-item` e `received-orders`: links com a
   âncora.

**E2E (registrado no DoD)**: logado como fornecedor, em 390 e 1280 px:
- o login cai no hub, com os números;
- cada card abre a sua tela;
- salvar o estoque não mexe no horário;
- "Ajustar horário" abre na seção;
- o menu não tem itens repetidos.

## 7. Fora de Escopo

- Endpoint agregado para o hub.
- Mudar o conteúdo do Painel, dos Pedidos ou do Histórico.
- Apagar o `distribuitor-form` legado e o `updateTimesOperation` sem
  chamador (ficam registrados).
- Configuração de frete por km (HU17).
- Menu do admin e do cliente.

## 8. Definição de Pronto

- [ ] Hub em `/minha-loja` como tela inicial do fornecedor, com os seis
      cards, os números e a falha isolada.
- [ ] Uma tela de configurações com as cinco seções e salvar por seção.
- [ ] Links com âncora e menu sem duplicatas.
- [ ] Testes da seção 6 verdes; `ng test` sem falha nova; `ng build` ok.
- [ ] E2E em 390 e 1280 px registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
