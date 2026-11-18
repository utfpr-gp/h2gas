# F2 — Design Tokens & Guia de Estilo

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F2 |
| Perfil | Infraestrutura (Fundação) — impacta todas as telas do Cliente, Fornecedor, Entregador e Admin |
| Prioridade MoSCoW | Must have (base visual para toda a UX Frictionless) |
| Dependências | F1 (Harness) |
| Referência no TCC | Cap. 6.2 "Identidade Visual" — Figura 8 (Guia de Estilos) |

## 2. Objetivo

Como desenvolvedor/agente de IA implementando qualquer tela da v2.0, quero
que a paleta de cores e a tipografia estejam formalizadas como Design
Tokens reutilizáveis, para que nenhuma nova tela (HU01…HU18) precise
decidir cor/fonte na hora — apenas consumir os tokens já definidos.

## 3. Divergência encontrada e decisão

O TCC afirma preservar a identidade visual do MVP 1.0 sem alteração de
paleta ("Redesign Visual Drástico" está no *Won't have*, §5.3.4). No
entanto, o código real do MVP usa `--primary-color: #6c63ff` (roxo/indigo),
hardcoded em ~20 arquivos de template/CSS, enquanto a Figura 8 do TCC
define **Primária #0E2068** (azul-escuro) e **Ação #0017BF** (azul
vibrante) como o guia de estilos oficial da v2.0.

**Decisão (confirmada com o autor):** a paleta da Figura 8 é a fonte da
verdade. F2 migra o token de cor primária do código para `#0E2068`/`#0017BF`.
Isso é tratado como *correção de um token que nunca foi implementado
corretamente*, não como redesign — a estrutura visual (layout, componentes,
DaisyUI, cards) não muda, só a cor de marca.

## 4. Critérios de Aceitação (BDD)

- **Cenário 1 — Tokens centralizados**: Dado o arquivo `src/styles.css`,
  Quando qualquer componente precisa da cor primária, de ação, de fundo, de
  texto ou de subtexto, Então ele consome uma variável CSS (`var(--primary-color)`
  etc.) ou classe DaisyUI semântica (`primary`, `bg-base-100`...), nunca um
  valor HEX literal novo.

- **Cenário 2 — Paleta migrada**: Dado o conjunto de arquivos que hoje
  usam `#6c63ff` hardcoded, Quando F2 é concluído, Então nenhum desses
  arquivos contém mais `#6c63ff` — todos referenciam o token de cor
  primária, que resolve para `#0E2068`.

- **Cenário 3 — Tema DaisyUI consistente**: Dado um componente que usa
  classes DaisyUI (`btn-primary`, `card`, `badge-*`), Quando renderizado,
  Então a cor resultante é a mesma do token CSS (`#0E2068`/`#0017BF`), sem
  overrides manuais de hover/cor no template (ex.: `hover:bg-blue-600`
  hoje presente em `login.component.html` deve ser removido em favor do
  hover nativo do tema).

- **Cenário 4 — Tipografia com escala definida**: Dado um texto que
  precisa de hierarquia (título de página, subtítulo, corpo), Quando o
  desenvolvedor aplica uma classe de tipografia, Então ela corresponde à
  escala da Figura 8 (H1 48/60px, H2 34/50px, H3 28/38px, H4 18/22px,
  Parágrafo 14/24px) na fonte Roboto.

## 5. Contrato Técnico

### 5.1 Variáveis CSS (`frontend/src/styles.css`, bloco `:root`)

```css
:root {
  --background-color: #f7f8fc;   /* já correto, mantém */
  --primary-color: #0e2068;      /* era #6c63ff — CORRIGIDO */
  --action-color: #0017bf;       /* NOVO — antes inexistente */
  --secondary-color: #f0f2f5;    /* mantém (uso pontual atual) */
  --text-color: #1c1c1e;         /* já correto, mantém */
  --subtext-color: #6b7280;      /* já correto, mantém */
  --white-color: #ffffff;
  --accent-color: #e0e7ff;       /* mantém — uso pontual, fora do guia de estilos, não migrar */
}
```

Alertas de sucesso/erro: o TCC não define HEX exatos, só "Verde e
Vermelho". O código já usa `green-600`/`red-600`/`green-800`/`red-800` do
Tailwind de forma consistente (`.status-entregue`, `.status-cancelado`,
`.btn-danger`). **Decisão**: manter as classes Tailwind padrão para
sucesso/erro (não criar `--success-color`/`--error-color` novos) — não há
inconsistência real a corrigir aqui.

### 5.2 Tema DaisyUI (`frontend/src/styles.css`, bloco `@plugin "daisyui"`)

Substituir a lista de temas genéricos (`light`, `dark`, `cupcake`) por um
tema customizado `h2gas` mapeado aos tokens acima:

```css
@plugin "daisyui" {
  themes: h2gas --default;
}

@plugin "daisyui/theme" {
  name: "h2gas";
  --color-primary: #0e2068;
  --color-primary-content: #ffffff;
  --color-secondary: #0017bf;
  --color-secondary-content: #ffffff;
  --color-base-100: #ffffff;
  --color-base-200: #f7f8fc;
  --color-base-content: #1c1c1e;
  --color-neutral: #6b7280;
}
```

### 5.3 Tipografia

- `frontend/src/index.html`: manter o `<link>` do Roboto (já presente);
  **não remover** os links de Inter/Poppins — eles estão em uso ativo em
  10+ arquivos `*.component.css` fora do escopo deste script (ver §7).
- `frontend/src/styles.css`: definir `font-family: 'Roboto', sans-serif;`
  no seletor `body` (hoje ausente — nenhum `font-family` está definido
  globalmente).
- Adicionar classes utilitárias de escala tipográfica:

```css
.text-h1 { font-size: 48px; line-height: 60px; font-weight: 700; }
.text-h2 { font-size: 34px; line-height: 50px; font-weight: 700; }
.text-h3 { font-size: 28px; line-height: 38px; font-weight: 600; }
.text-h4 { font-size: 18px; line-height: 22px; font-weight: 600; }
.text-paragraph { font-size: 14px; line-height: 24px; font-weight: 400; }
```

### 5.4 Migração de uso

Substituir `#6c63ff` (case-insensitive) por `var(--primary-color)` (ou
classe `text-primary`/`bg-primary` do tema DaisyUI, quando aplicável) nos
20 arquivos identificados:

```
src/app/components/menu/menu.component.html
src/app/components/pages/reset-password/reset-password.component.html
src/app/components/pages/check-email/check-email.component.html
src/app/components/register-user/register-user.component.html
src/app/components/pages/category-products/category-products.component.html
src/app/components/pages/category-products/category-products.component.css
src/app/components/pages/distribuitor-form/distribuitor-form.component.html
src/app/components/pages/distribuitor-form/distribuitor-form.component.css
src/app/components/pages/supplier-approval/supplier-approval.component.css
src/app/components/pages/supplier-approval/supplier-approval.component.html
src/app/components/pages/customer-panel/customer-panel.component.html
src/app/components/pages/customer-panel/customer-panel.component.css
src/app/components/pages/login/login.component.html
src/app/components/pages/about/about.component.html
src/app/components/pages/initial-screen/initial-screen.component.html
src/app/components/pages/forgot-password/forgot-password.component.html
src/app/components/pages/contact/contact.component.html
src/app/components/pages/city-register/city-register.component.css
src/app/components/pages/city-register/city-register.component.html
src/app/components/footer/footer.component.html
```

Nos templates com `hover:bg-blue-600` como override manual ao lado de
`btn-primary` (ex.: `login.component.html`), remover o override — o tema
DaisyUI já resolve o estado hover.

## 6. Plano de Testes

CSS/design tokens não são naturalmente cobertos por JUnit ou Jasmine.
O plano de verificação para este script é baseado em análise estática
(grep), não em testes unitários — isso é intencional e documentado, não
uma lacuna escondida:

1. **Verificação de migração completa**: `grep -ril "6c63ff" frontend/src`
   deve retornar vazio após a implementação.
2. **Verificação de token único**: `grep -rn "#0e2068\|#0017bf" frontend/src/styles.css`
   deve mostrar as duas cores definidas uma única vez cada, dentro de
   `:root` e do bloco de tema DaisyUI.
3. **Build sem erros**: `ng build` deve compilar sem erros/warnings de CSS
   (confirma que a sintaxe Tailwind v4 / `@plugin "daisyui/theme"` está
   correta).
4. **Verificação visual manual**: abrir `ng serve` e conferir que
   `login`, `menu` e `footer` (componentes mais visíveis) renderizam com a
   cor azul-escura em vez de roxo.

## 7. Fora de Escopo

- Migrar o `font-family` de arquivos que já usam Poppins/Inter
  explicitamente (10+ arquivos `*.component.css`) — isso seria redesign
  tipográfico, vetado por §5.3.4 do TCC (Won't have).
- Remover os `<link>` de Inter/Poppins do `index.html` — ainda estão em
  uso; removê-los quebraria essas telas.
- Redesenhar layout, espaçamento ou componentes — F2 troca apenas cor e
  formaliza tipografia, não reestrutura HTML/CSS existente além da troca
  pontual de hex.
- Definir `--success-color`/`--error-color` novos — as classes Tailwind
  `green-*`/`red-*` já em uso permanecem.

## 8. Definição de Pronto

- [x] `:root` em `styles.css` atualizado com `--primary-color: #0e2068` e
      novo `--action-color: #0017bf`.
- [x] Tema DaisyUI customizado `h2gas` substituindo `light/dark/cupcake`.
- [x] `font-family: 'Roboto'` definido no `body`; classes `.text-h1`…
      `.text-paragraph` criadas.
- [x] `grep -ril "6c63ff" frontend/src` retorna vazio.
- [x] `ng build` roda sem erros.
- [ ] Revisado e aprovado antes de F3 começar.

## 9. Divergência encontrada durante a implementação

Os 20 arquivos listados em §5.4 **não continham mais `#6c63ff`** no
momento da implementação — o código já havia migrado esses templates para
consumir `var(--primary-color)`/classes DaisyUI em algum ponto entre a
escrita do script e agora, sem que a cor em `:root` tivesse sido corrigida.
Nenhuma edição nesses 20 arquivos foi necessária além de
`login.component.html` (remoção do `hover:bg-blue-600`, conforme §5.4).

**Conflito encontrado e resolvido**: `styles.css` definia um `.btn`/
`.btn-primary` próprio (bloco "css do pedidos recebidos",
`@apply bg-blue-600 hover:bg-blue-700 ...`) que vinha depois do plugin
DaisyUI no arquivo. Por ordem de cascata CSS, esse `.btn-primary` sobrepunha
a cor do tema DaisyUI (`#0e2068`) em **qualquer** elemento `btn btn-primary`
da aplicação (confirmado no botão de login), quebrando o Cenário 3.
Confirmado via grep que `.stat-card`, `.order-card`, `.btn-danger` e
`.btn-outline` desse mesmo bloco não são referenciados em nenhum template
— o bloco inteiro era resquício de uma tela que não existe mais no código
atual. Removidas apenas as regras `.btn`/`.btn-primary` desse bloco (as
únicas que colidiam com classes de componente reais do DaisyUI e afetavam
telas em uso); `.btn-danger`/`.btn-outline` (não usadas, sem colisão) foram
deixadas como estão, fora do escopo desta correção. Há também uma segunda
definição duplicada de `.card` no mesmo arquivo (linhas ~59 e ~82) que
também poderia colidir com o componente `card` do DaisyUI — essa **não**
foi tocada porque está em uso real (`reset-password`, `check-email`) e
mudar qual das duas vence alteraria a aparência dessas telas; fica
registrado como pendência conhecida, não como bug corrigido.
Rebuild (`ng build`) confirmado sem erros após a remoção.

**Segundo bug encontrado (verificação visual manual, item 4 do Plano de
Testes) e corrigido**: o bloco `@plugin "daisyui/theme"` do §5.2, copiado
literalmente do contrato técnico, definia `name: "h2gas"` mas não
`default: true`. Sem essa flag, o DaisyUI 5.0.11 só gera o seletor
`:root:has(input.theme-controller[value=h2gas]:checked), [data-theme=h2gas]`
— como `index.html` não tem `data-theme="h2gas"` em lugar nenhum, nenhuma
variável de cor do tema chegava a ser aplicada em nenhum elemento, e todo
botão `btn-primary` renderizava cinza claro (cor de fallback do DaisyUI),
não o token. Adicionado `default: true;` e `color-scheme: light;` ao bloco
do tema — isso faz o DaisyUI gerar também `:where(:root)` na lista de
seletores, aplicando o tema globalmente sem precisar de atributo. Validado
visualmente: screenshot de `/login` via Chromium headless confirma botão
`btn-primary` em `#0e2068`/texto branco.
