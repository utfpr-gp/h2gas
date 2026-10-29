# F10 — Configurações da loja em etapas, como o checkout

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F10 |
| Perfil | Fornecedor |
| Prioridade MoSCoW | Should have. Pedido do autor em 2026-10-29: "algo no estilo da finalização do carrinho: o usuário clica e aparece tal configuração". |
| Dependências | F9 (configurações numa tela só), HU17 (seção de entrega), checkout em etapas (HU05) |
| Revoga | F9 §5.3, nos pontos de "seções empilhadas" e "sumário que rola até cada uma" |

## 2. Objetivo

Como fornecedor, quero ver uma configuração da loja por vez, escolhida
numa barra de etapas no topo, para achar o que quero mudar sem rolar por
todas as outras.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Uma seção por vez**: Dado `/minha-loja/configuracoes`,
  Então aparece só "Aparência". Quando o fornecedor toca em outra etapa da
  barra, Então o conteúdo troca, sem recarregar a página.
- **Cenário 2 — Abrir direto na seção**: Dado `#funcionamento` ou
  `#estoque` na URL (links "Ajustar horário" e "Ajustar o estoque
  mínimo"), Então a tela abre já naquela etapa.
- **Cenário 3 — URL acompanha**: Quando o fornecedor troca de etapa, Então
  o `#` da URL é atualizado com `replaceUrl`. Recarregar a página mantém a
  etapa, e o Voltar do navegador não acumula etapas.
- **Cenário 4 — Edição não se perde**: Dado um campo editado e não salvo,
  Quando o fornecedor vai para outra etapa e volta, Então o valor digitado
  continua lá.
- **Cenário 5 — Aviso de não salvo**: Dado uma etapa com edição pendente,
  Então ela ganha um ponto na barra, e o rótulo acessível diz "alterações
  não salvas".
- **Cenário 6 — Celular**: Em 390 px, a barra fica fixa no topo, com
  rótulos curtos (Aparência, Dados, Endereço, Horário, Estoque) e sem
  rolagem horizontal da página.

## 4. Estado atual (achados)

- `store-profile` (F9) empilha as cinco seções, com uma faixa de botões
  que rola até cada uma (`scrollTo`) e rolagem pelo `#fragmento` depois de
  carregar (`scrollWhenRendered`).
- `configuration` (embutido) mostra `#funcionamento` e `#estoque` juntos,
  num formulário com o salvar de cada seção e `changed('hours' | 'stock')`.
- O checkout (`order-resume`) usa a barra `ul.steps` do DaisyUI.

## 5. Contrato Técnico

Só frontend; a API não muda.

- **`store-profile`**:
  - `activeSection` (padrão `aparencia`), lido de `route.snapshot.fragment`;
  - `select(id)` troca a etapa e chama
    `router.navigate([], { fragment, replaceUrl: true, relativeTo: route })`;
  - a barra é `ul.steps`, no visual do checkout. Só a etapa ativa recebe
    `step-primary`, porque aqui não há progresso, e `aria-current="step"`;
  - as seções ficam montadas, com `[hidden]` nas que não estão ativas
    (Cenário 4);
  - sai a rolagem até a seção (`scrollWhenRendered` e os sinalizadores de
    carregamento).
- **`configuration`**:
  - `@Input() visibleSection` (`funcionamento`, `estoque` ou nulo = as
    duas) esconde a seção que não está ativa;
  - o pai lê `changed('hours' | 'stock')` pelo `@ViewChild` para o ponto de
    "não salvo"; a seção Dados usa o `changed` do perfil.
- O conteúdo de cada seção, o título da tela e os botões de salvar não
  mudam.

## 6. Plano de Testes (TDD)

1. `store-profile.component.spec.ts`:
   - `deveMostrarSoASecaoEscolhida`, `deveAbrirNaSecaoDoFragmento`;
   - `deveAtualizarOFragmentoAoTrocar`, `deveManterAEdicaoAoTrocarDeSecao`;
   - `deveMarcarEtapaComAlteracaoNaoSalva`.
2. `configuration.component.spec.ts`: `deveMostrarSoASecaoPedida`. Os
   demais seguem válidos, com `visibleSection` nulo.

**E2E (registrado no DoD)**: em 390 e 1280 px, logado como fornecedor:
- abre em "Aparência";
- "Horário" mostra só "Funcionamento e entrega";
- `#estoque` abre direto em "Estoque";
- edição mantida ao trocar de etapa, com o ponto de "não salvo";
- sem rolagem horizontal.

## 7. Fora de Escopo

- Assistente com Continuar/Voltar passando por todas as etapas.
- Mudar o conteúdo das seções.
- Um "Salvar tudo" único.

## 8. Definição de Pronto

- [x] Uma seção por vez, escolhida na barra de etapas, com o `#` na URL.
- [x] Edição pendente mantida ao trocar de etapa, com o aviso na barra.
- [x] Testes da seção 6 verdes; `ng test` (só as 10 falhas antigas de
      provider) sem falha nova; `ng build` ok.
- [x] E2E em 390 e 1280 px registrado aqui com data.
- [x] Nada do "Fora de Escopo".

### Registro do teste manual (2026-10-29)

- Logado como a loja de teste "Plantão Gás e Água":
  - `/minha-loja/configuracoes` em 390 px abre só em "Aparência", sem
    rolagem horizontal (`scrollWidth` 390);
  - `#funcionamento` abre só "Funcionamento e entrega";
  - com a abertura editada para 07:30 sem salvar, fui a "Estoque" e
    voltei: o valor continuou 07:30, a etapa "Horário" ganhou o ponto de
    "não salvo" e a URL ficou em `#funcionamento`;
  - em 1280 px, `#estoque` abre só "Estoque", com a etapa 5 ativa.
- Ajuste visual do teste: a faixa da barra passou a usar a cor da página,
  com uma borda fina, no lugar do cinza.
