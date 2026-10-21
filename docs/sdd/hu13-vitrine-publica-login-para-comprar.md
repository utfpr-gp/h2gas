# HU13 — Vitrine pública: ver produtos sem login, entrar para comprar

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU13 |
| Perfil | Visitante (sem conta) e Cliente (CUSTOMER) |
| Prioridade MoSCoW | Should have |
| Dependências | F7 (catálogo, cabeçalho da loja, avaliações, Comprar agora — PR `feature/f7-geral`), HU01 (RN05 pela cidade), HU11 (login) |
| Referência no TCC | RN05; extensão da HU01 (contexto de localização) |

## 2. História de Usuário

Como visitante, quero ver as lojas e os produtos da minha região sem
precisar criar conta, para decidir se vale a pena comprar; e, quando
decidir comprar, entrar (ou me cadastrar) e voltar direto para a compra,
sem perder o que escolhi.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Entrar na vitrine sem conta**: Dado um visitante na tela
  inicial (`/`), Quando informa o CEP e confirma ("Ver lojas da minha
  cidade"), Então o catálogo abre filtrado pela cidade (sem passo
  intermediário "Ver produtos sem entrar"). A mesma tela oferece "Entrar"
  com peso visual equivalente para quem já tem conta.
- **Cenário 2 — CEP define a cidade (RN05)**: Dado o visitante na porta de
  entrada ( `/` ou `/onde-voce-esta` ao trocar o CEP), Quando informa um
  CEP de cidade atendida, Então o catálogo (`/inicio`) mostra só lojas e
  produtos daquela cidade, e o topo mostra "Bairro · Cidade" com opção de
  trocar.
- **Cenário 3 — CEP fora da área**: Dado um CEP válido de cidade que o
  H2Gás não atende, Quando o visitante confirma, Então vê "Ainda não
  atendemos {Cidade}/{UF}" e pode tentar outro CEP; o catálogo não abre
  sem cidade.
- **Cenário 4 — CEP inválido ou ViaCEP fora do ar**: Dado um CEP com
  formato errado, inexistente, ou o ViaCEP sem resposta, Então o visitante
  vê a mensagem do caso ("CEP inválido", "CEP não encontrado", "Não foi
  possível consultar o CEP agora") e pode tentar de novo.
- **Cenário 5 — Navegar como visitante**: Dado a cidade escolhida, Quando
  o visitante busca, filtra por tipo, abre a página de uma loja e as
  avaliações, Então tudo funciona como para o cliente logado.
- **Cenário 6 — Carrinho sem conta**: Dado o visitante no catálogo, Quando
  toca em "Adicionar", Então o item vai para o carrinho (guardado no
  navegador) e o contador do topo atualiza.
- **Cenário 7 — Comprar pede login e volta**: Dado o visitante com itens
  no carrinho (ou que tocou em "Comprar agora"), Quando segue para o
  checkout, Então vai para o login com o aviso "Entre para finalizar sua
  compra"; ao entrar, volta ao checkout com o carrinho (ou o item avulso)
  intacto.
- **Cenário 8 — Primeiro acesso sem endereço**: Dado um cliente recém-
  cadastrado sem endereço, Quando entra a partir do checkout, Então passa
  pela tela de primeiro endereço e depois volta ao checkout.
- **Cenário 9 — Cidade depois do login**: Dado um visitante que escolheu
  uma cidade pelo CEP, Quando entra com uma conta que tem endereço, Então
  o catálogo passa a usar o endereço ativo da conta (RN05 do cliente).
- **Cenário 10 — Áreas da conta seguem protegidas**: Dado um visitante,
  Quando abre `/meus-pedidos`, `/meus-dados`, `/resumo-pedido` ou qualquer
  tela de fornecedor/admin, Então vai para o login (com retorno para a
  página pedida).
- **Cenário 11 — Sem dados internos na vitrine**: Dado um visitante,
  Quando o catálogo ou a página da loja carrega, Então as respostas da API
  não trazem e-mail, CNPJ, telefone nem endereço completo da loja.
- **Cenário 12 — Token vencido não quebra a vitrine**: Dado um usuário cujo
  token venceu, Quando abre o catálogo, Então a vitrine carrega como
  visitante (o token vencido não é enviado), sem mandar para o login.

## 4. Estado atual (achados)

- **Backend**: só login, cadastro, código por e-mail, reset de senha,
  `GET /api/cities` e o Swagger são públicos (`SecurityConfigurations`).
  As rotas do catálogo exigem token e algumas têm `@RolesAllowed`:
  `GET /api/suppliers/actives/open`, `GET /api/suppliers/{id}`,
  `GET /api/suppliers/{id}/reviews`, `GET /api/product-items/by-supplier/{id}`,
  `GET /api/product-items/by-supplier-open`, `GET /api/product-types`.
- **Dados expostos**: as rotas de loja devolvem `SupplierDTO`, que inclui
  e-mail, telefone, CNPJ e endereço completo. `GET /suppliers/{id}` também é
  usada pelo admin (`distribuitor-form`) e pelo checkout (endereço de
  retirada), que precisam desses dados.
- **RN05**: com `cityId` nulo, `/actives/open` e `/by-supplier-open`
  devolvem **todas** as cidades (comportamento do admin). Para visitante
  isso furaria a RN05.
- **Token vencido**: o `SecurityFilter` responde **401** para token vencido
  ou inválido mesmo em rota pública; o `auth.interceptor` manda para
  `/login` em todo 401.
- **Frontend**: `/inicio` e `/fornecedores/:id/produtos` usam `authGuard`;
  `/inicio` também usa `addressDefinedGuard`, que busca o endereço do
  usuário logado. O `authGuard` manda para `/login` sem lembrar a página
  pedida, e o login sempre leva à tela inicial do perfil.
- **Reaproveitável**: `AddressService.buscaViaCep`, `utils/city-match.ts`
  (`matchCityFromCep`, casa pelo código IBGE), `LocationContextService`
  (guarda a cidade no navegador), carrinho e "Comprar agora" já ficam no
  navegador (`localStorage`/`sessionStorage`).

## 5. Contrato Técnico

### 5.1 Backend — rotas públicas de leitura

- `SecurityConfigurations`: `permitAll` **só para GET** em
  `/api/suppliers/actives/open`, `/api/suppliers/*/storefront`,
  `/api/suppliers/*/reviews`, `/api/product-items/by-supplier/*`,
  `/api/product-items/by-supplier-open` e `/api/product-types`; sai o
  `@RolesAllowed` dessas rotas. Nenhuma outra rota muda.
- **DTO público `StorefrontDTO`** (`@Schema`): `id`, `name`, `description`,
  `logoUrl`, `bannerUrl`, `openingTime`, `closingTime`,
  `averageDeliveryTime`, `open`, `cityId`, `cityName`,
  `addressNeighborhood`, `averageRating`, `reviewCount`. **Sem** e-mail,
  telefone, CNPJ, rua/número/complemento e lista de produtos.
- `GET /api/suppliers/actives/open` passa a devolver
  `PageResponse<StorefrontDTO>` (para todos, logado ou não).
- **Nova** `GET /api/suppliers/{id}/storefront` → `200 StorefrontDTO`
  (com a nota), `404` loja inexistente. A página da loja passa a usá-la.
  `GET /api/suppliers/{id}` continua autenticada e com `SupplierDTO`
  (admin, checkout).
- **RN05 no visitante**: em `/actives/open` e `/by-supplier-open`, sem
  usuário autenticado o `cityId` é **obrigatório** → `400` "Informe a
  cidade para ver o catálogo." O admin logado mantém o `cityId` opcional.
- OpenAPI: as rotas públicas aparecem sem cadeado (sem security
  requirement); `@Operation`/`@ApiResponses` com os status reais
  (200, 400, 404).

### 5.2 Frontend — vitrine sem login

- **Porta de entrada unificada** (`/` e `/onde-voce-esta`): mesmo shell
  visual (marca H2Gás + painel de CEP + CTA "Entrar"). O formulário de CEP
  (`visitor-cep-panel`) usa máscara; `buscaViaCep` → `GET /cities` →
  `matchCityFromCep`; casa → `LocationContextService.setAddress({ cityId,
  cityName, label: "Bairro · Cidade" })` e navega para `returnUrl`
  (padrão `/inicio`); não casa → "Ainda não atendemos {Cidade}/{UF}"; CEP
  inválido/inexistente/ViaCEP sem resposta → mensagem do Cenário 4.
  `/onde-voce-esta` permanece para a guarda e para "trocar CEP" no menu;
  `/` é a landing com o mesmo painel (sem botão intermediário).
- **Guardas**: `/inicio` e `/fornecedores/:id/produtos` marcadas com
  `data.allowVisitor` — o `authGuard` deixa o visitante passar (e descarta
  sessão vencida, `AuthService.dropExpiredSession`, sem tocar no carrinho
  nem na cidade); o `addressDefinedGuard` manda o visitante sem cidade
  para `/onde-voce-esta?returnUrl=…`; logado segue a regra atual de papéis
  e de endereço. `/resumo-pedido` ganha o `addressDefinedGuard` (Cenário 8).
  *(Revisão da implementação: em vez de uma guarda nova, as duas existentes
  leem `allowVisitor` — o Angular executa todas as guardas da rota, e assim
  elas não disputam navegação.)*
- **`authGuard`** manda para `/login?returnUrl=<url pedida>`; com
  `returnUrl=/resumo-pedido…` o login mostra "Entre para finalizar sua
  compra". Os dois logins (código e senha) e o cadastro voltam ao
  `returnUrl` depois de entrar, **só se for caminho interno** (começa com
  `/`, não com `//` nem esquema); senão, tela inicial do perfil. O
  `returnUrl` passa pelo primeiro endereço quando o cliente não tem
  endereço (Cenário 8).
- **Cidade após o login**: ao entrar, a cidade de visitante é descartada e
  o `addressDefinedGuard` carrega o endereço ativo da conta (Cenário 9).
- **Carrinho**: deixa de ser limpo ao **entrar** (os dois logins limpavam) e
  passa a ser limpo ao **sair** (`AuthService.logout`, junto com o item do
  "Comprar agora") — o visitante leva o carrinho ao login (Cenário 7) e o
  carrinho de quem sai não passa para o próximo usuário.
- **Interceptor**: não anexa token vencido (Cenário 12); 401 só derruba a
  sessão (e vai ao login com `returnUrl`) quando a requisição levou token —
  senha errada e visitante não perdem carrinho nem retorno.
- **Menu do visitante**: barra clara do cliente, cidade ("Bairro · Cidade",
  toque troca o CEP), carrinho com contador e botão "Entrar" (volta para a
  página atual); sem "Pedidos"/"Perfil". O menu relê o usuário salvo a cada
  navegação (sessão vencida descartada vira visitante). A barra inferior do
  celular mostra "Início", "Categorias" e "Entrar".
- **RN02 no visitante**: `/product-items/by-supplier/{id}` esconde produto
  zerado para cliente **e visitante** (antes só para o papel CUSTOMER).
- Página da loja usa `SupplierService.getStorefront(id)`; modelo
  `Storefront` no frontend.

## 6. Plano de Testes (TDD)

**Backend**
1. `SecurityConfigurationsTest` (ou testes de controller com segurança):
   `devePermitirCatalogoSemToken` (as 6 rotas GET → 200 sem token),
   `deveExigirTokenNoCheckoutEMinhasAreas` (`POST /orders`,
   `GET /suppliers/{id}`, `/suppliers/mine/**` → 401/403 sem token),
   `naoDevePermitirEscritaNasRotasDoCatalogo` (POST/PUT/DELETE nas
   mesmas bases continuam protegidos).
2. `SupplierServiceTest`: `deveMontarAVitrineSemDadosInternos`
   (StorefrontDTO sem e-mail/CNPJ/telefone/rua; com nota),
   `deveResponder404NaVitrineDeLojaInexistente`.
3. `SupplierControllerTest` / `ProductItemControllerTest`:
   `deveExigirCidadeDoVisitanteNoCatalogo` (sem usuário e sem `cityId` →
   400), `deveManterCidadeOpcionalParaOAdmin`,
   `deveDevolverAVitrineDaLoja` (JSON sem `email`/`cnpj`/`phone`).

**Frontend**
4. `visitor-cep-panel.component.spec.ts` (e/ou `visitor-location`):
   `deveDefinirACidadePeloCep`, `deveAvisarCidadeNaoAtendida`,
   `deveAvisarCepInvalidoOuInexistente`, `deveAvisarFalhaDoViaCep`,
   `deveVoltarParaOReturnUrl`. `initial-screen`: `deveExibirOPainelDeCep`,
   `deveLevarAoLogin`.
5. `address-defined.guard.spec.ts`: `devePermitirVisitanteComCidade`,
   `deveMandarVisitanteSemCidadeParaOCep`, `deveLevarOReturnUrlAoPrimeiroEndereco`
   (os testes do logado seguem valendo).
6. `auth.guard.spec.ts`: `deveMandarParaOLoginComReturnUrl`,
   `devePermitirVisitanteNoCatalogo`; `auth.service.spec.ts`:
   `deveLimparOCarrinhoAoSair`, `deveDescartarSessaoVencidaSemLimparOCarrinho`;
   `return-url.spec.ts` (caminho interno, externo, login).
7. `login` e `login-token` specs: `deveVoltarAoReturnUrlInterno`,
   `deveIgnorarReturnUrlExterno`, `deveAvisarQueOLoginEParaFinalizarACompra`;
   `register`: `deveManterOReturnUrlAposOCadastro`.
8. `auth.interceptor.spec.ts`: `naoDeveEnviarTokenVencido`,
   `deveMandarParaOLoginComRetornoEm401`, `naoDeveDerrubarASessaoEm401SemToken`;
   `login`/`login-token`: `deveManterOCarrinhoAoEntrar`.
9. `menu.component.spec.ts`: `deveMostrarEntrarECarrinhoParaVisitante`,
   `deveMostrarACidadeDoVisitante`, `deveLevarOVisitanteAoLoginVoltandoParaAPaginaAtual`,
   `deveVirarVisitanteQuandoASessaoSomeAoNavegar`; `bottom-tab-bar`:
   `deveMostrarEntrarParaVisitante`;
   backend `ProductItemControllerTest`: `deveForcarOnlyAvailableParaVisitante`.
10. `supplier.service.spec.ts`: `deveBuscarAVitrineDaLoja`;
    `product-list.component.spec.ts`: `deveUsarAVitrineNoCabecalho`.
11. Cidade de visitante descartada ao entrar: já coberta pelo teste
    existente `deveLimparEnderecoAtivoDeOutroUsuarioAoIniciarLogin` e pelo E2E.

**E2E (Playwright, manual registrado no DoD)**: visitante na `/` informa
CEP de Curitiba → catálogo filtrado → loja → avaliações → adiciona 2
itens → checkout → login → volta ao checkout com os 2 itens → conclui
pedido; CEP de cidade não atendida; `/meus-pedidos` sem login → login →
volta; token vencido no `localStorage` → catálogo abre como visitante.
Em 390 e 1280 px.

## 7. Fora de Escopo

- Checkout como convidado (sem conta) — comprar continua exigindo login.
- Geolocalização do navegador para descobrir a cidade.
- Busca de endereço por rua (só CEP).
- Sincronizar o carrinho do visitante com o servidor ou entre aparelhos.
- Validar, no checkout, loja de outra cidade quando o visitante escolheu
  um CEP e a conta tem endereço em outra cidade (comportamento atual do
  checkout se mantém).
- SEO/renderização no servidor da vitrine.
- Mudar o que o cliente logado vê além do DTO público do catálogo.

## 8. Definição de Pronto

- [x] Rotas públicas só de leitura, com `StorefrontDTO` sem dados internos
      e RN05 obrigatória para visitante; OpenAPI sem cadeado nessas rotas.
- [x] Tela de CEP, guardas, `returnUrl` nos logins e no cadastro,
      interceptor e menu do visitante.
- [x] Testes do Plano (itens 1–11) verdes; `mvn test` e `ng test` sem
      falhas novas; `ng build`.
- [x] E2E do item acima em 390 e 1280 px, registrado aqui com data.
- [x] Nada do "Fora de Escopo".
- [x] Porta de entrada unificada: `/` e `/onde-voce-esta` com o mesmo
      shell (marca + CEP + Entrar); Cenário 1 sem passo intermediário;
      verificado em 390 e 1280 px.

> **Verificação porta unificada (2026-10-21)**: `visitor-cep-panel` +
> `initial-screen` + `visitor-location` specs verdes (17). `/` e
> `/onde-voce-esta?returnUrl=/inicio` com o mesmo shell; CEP 80420-000 →
> `/inicio` (Centro · Curitiba); Entrar → `/login`. Footer oculto na porta
> de entrada (390 e 1280).

> **Verificação (2026-10-21)**: `mvn test` 393 testes, só o `BackendApplicationTests.contextLoads` antigo (MySQL
> local); `ng test` 546, só as 10 falhas antigas de providers; `ng build` ok. API real sem token: catálogo sem
> `cityId` → 400 "Informe a cidade para ver o catálogo."; com `cityId` → 200; `/storefront` → 200 só com os 14
> campos públicos; `GET /suppliers/{id}` → 403. E2E (front 4400, backend 8090, ViaCEP real) em 1280 e 390 px:
> tela inicial → "Ver produtos sem entrar" → CEP 80420-000 → `/inicio` com as 3 lojas de Curitiba e "Entrar" no
> menu (sem avatar); loja da Gás Centro com 10 avaliações; 2 itens no carrinho → "Finalizar Pedido" →
> `/login?returnUrl=/resumo-pedido` com "Entre para finalizar sua compra"; senha errada fica na tela com o
> retorno e os 2 itens; senha certa → `/resumo-pedido` com os 2 itens e o endereço da conta (Cenário 9); CEP de
> Londrina → "Ainda não atendemos Londrina/PR"; `/meus-pedidos` sem login → `/login?returnUrl=/meus-pedidos`;
> token vencido no navegador → `/inicio` como visitante, sem 401 e com "Entrar". O pedido em si não foi concluído
> no E2E (fluxo de pedido inalterado nesta HU).
> **Achados**: (1) os dois logins limpavam o carrinho ao entrar — passou para o logout; (2) com isso, um 401 de
> senha errada passaria a apagar o carrinho (o interceptor tratava todo 401 como sessão caída) — agora só com
> token; (3) `/by-supplier/{id}` só escondia produto zerado para o papel CUSTOMER — visitante veria (RN02);
> (4) o menu lia o usuário uma vez e seguia com avatar depois da sessão vencida.
