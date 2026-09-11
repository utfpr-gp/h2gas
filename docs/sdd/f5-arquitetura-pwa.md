# F5 — Arquitetura PWA

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F5 |
| Perfil | Infraestrutura (Fundação) — base para HU07 (app do entregador) e para a instalabilidade geral do cliente |
| Prioridade MoSCoW | Must have |
| Dependências | F2 (Design Tokens — cores do manifest) |
| Referência no TCC | Cap. 1.2.2 (objetivo específico "Adotar a arquitetura PWA"), Cap. 6.1 (Figura 7: "App Cliente PWA/Mobile", "App Entregador PWA/Mobile") |

## 2. Objetivo

Como cliente ou entregador usando o H2Gás pelo celular, quero poder
instalar o aplicativo na tela inicial do meu dispositivo, para acessá-lo
como um app nativo, sem depender do navegador aberto — eliminando um
passo de fricção no acesso recorrente (Frictionless UX).

## 3. Estado atual (achado)

O frontend não tem nenhuma configuração de PWA hoje: sem
`@angular/service-worker`, sem `manifest.webmanifest`, sem ícones além de
`favicon.ico`. Isso é uma implementação greenfield completa, não uma
migração — o caminho mais seguro é usar o schematic oficial
`ng add @angular/pwa`, que já resolve a maior parte da configuração
mecânica (dependência, `ngsw-config.json`, registro do service worker,
flag de produção no `angular.json`).

## 4. Critérios de Aceitação (BDD)

- **Cenário 1 — Instalação mobile**: Dado um cliente acessando o H2Gás
  pelo Chrome/Safari mobile, Quando o navegador detecta que o app cumpre
  os critérios de instalabilidade (manifest + service worker + HTTPS),
  Então oferece a opção "Adicionar à tela inicial".

- **Cenário 2 — Sessão persistente**: Dado um PWA instalado, Quando o
  usuário reabre o app depois de um tempo, Então ele abre diretamente na
  última rota (sem recarregar do zero como uma aba de navegador comum),
  reforçando o objetivo de "mitigar a necessidade de novos logins a cada
  interação diária" citado no TCC (Cap. 6.3).

- **Cenário 3 — Dados de negócio nunca em cache obsoleto**: Dado que o
  app está offline ou com service worker ativo, Quando o usuário navega
  pelo catálogo/carrinho, Então nenhuma chamada a `/api/**` (preço,
  estoque, status de pedido) é servida do cache do service worker sem
  tentar rede primeiro — RN01/RN02 dependem de dado sempre atual.

- **Cenário 4 — Build de produção gera artefatos PWA**: Dado
  `ng build --configuration production`, Quando o build termina, Então
  `dist/` contém `ngsw.json` e `manifest.webmanifest`, e `index.html`
  referencia ambos.

## 5. Contrato Técnico

### 5.1 Instalação do schematic

```bash
cd frontend
ng add @angular/pwa --project frontend
```

Isso deve gerar/alterar automaticamente:
- `frontend/package.json` — adiciona `@angular/service-worker`.
- `frontend/public/manifest.webmanifest` — a customizar (5.2).
- `frontend/src/app/app.config.ts` — adiciona
  `provideServiceWorker('ngsw-worker.js', { enabled: !isDevMode(), registrationStrategy: 'registerWhenStable:30000' })`.
- `frontend/ngsw-config.json` — a customizar (5.3).
- `frontend/angular.json` — marca `"serviceWorker": true` na configuração
  `production`.
- `frontend/src/index.html` — adiciona `<link rel="manifest" ...>` e
  `<meta name="theme-color" ...>`.

### 5.2 `manifest.webmanifest` (customização pós-schematic)

```json
{
  "name": "H2Gás",
  "short_name": "H2Gás",
  "theme_color": "#0e2068",
  "background_color": "#f7f8fc",
  "display": "standalone",
  "scope": "./",
  "start_url": "./",
  "icons": [
    { "src": "icons/icon-72x72.png", "sizes": "72x72", "type": "image/png", "purpose": "maskable any" },
    { "src": "icons/icon-96x96.png", "sizes": "96x96", "type": "image/png", "purpose": "maskable any" },
    { "src": "icons/icon-128x128.png", "sizes": "128x128", "type": "image/png", "purpose": "maskable any" },
    { "src": "icons/icon-144x144.png", "sizes": "144x144", "type": "image/png", "purpose": "maskable any" },
    { "src": "icons/icon-152x152.png", "sizes": "152x152", "type": "image/png", "purpose": "maskable any" },
    { "src": "icons/icon-192x192.png", "sizes": "192x192", "type": "image/png", "purpose": "maskable any" },
    { "src": "icons/icon-384x384.png", "sizes": "384x384", "type": "image/png", "purpose": "maskable any" },
    { "src": "icons/icon-512x512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable any" }
  ]
}
```

`theme_color`/`background_color` usam os tokens do F2
(`--primary-color`/`--background-color`). Os ícones devem ser gerados a
partir de `frontend/src/assets/logo-h2.png` (logo real do H2Gás, Figura 9
do TCC) — não usar o ícone genérico do Angular que o schematic gera por
padrão.

### 5.3 `ngsw-config.json` — estratégia de cache

```json
{
  "$schema": "./node_modules/@angular/service-worker/config/schema.json",
  "index": "/index.html",
  "assetGroups": [
    {
      "name": "app",
      "installMode": "prefetch",
      "resources": { "files": ["/favicon.ico", "/index.html", "/manifest.webmanifest", "/*.css", "/*.js"] }
    },
    {
      "name": "assets",
      "installMode": "lazy",
      "updateMode": "prefetch",
      "resources": { "files": ["/assets/**", "/icons/**"] }
    }
  ],
  "dataGroups": [
    {
      "name": "api-freshness",
      "urls": ["/h2gas/api/**"],
      "cacheConfig": {
        "strategy": "freshness",
        "maxSize": 0,
        "maxAge": "0u",
        "timeout": "5s"
      }
    }
  ]
}
```

`strategy: "freshness"` com `maxSize: 0` faz o service worker sempre
tentar a rede primeiro para qualquer chamada de API, sem servir cache
obsoleto de preço/estoque/status — só cai em cache se a rede falhar
dentro de 5s (comportamento aceitável offline, nunca mascarando dado
desatualizado quando online).

## 6. Plano de Testes

PWA/service worker não é naturalmente coberto por Karma/Jasmine (o SW só
roda em contexto de produção/HTTPS). O plano de verificação é baseado em
build e inspeção manual, documentado como tal:

1. **Build gera artefatos**: `ng build --configuration production` — 
   verificar que `dist/frontend/browser/ngsw.json` e
   `manifest.webmanifest` existem no output.
2. **`index.html` referencia o manifest**: `grep manifest dist/frontend/browser/index.html`
   deve encontrar o `<link rel="manifest">`.
3. **`dataGroups` cobre `/api/**` com estratégia `freshness`**: teste de
   configuração — parsear `ngsw-config.json` e afirmar programaticamente
   (ou via revisão manual) que nenhuma rota de API está em `assetGroups`
   (que usariam cache-first).
4. **Verificação manual (Lighthouse)**: rodar auditoria PWA do Chrome
   DevTools contra o build de produção servido localmente
   (`npx http-server dist/frontend/browser`), confirmar "Installable" =
   sim.

## 7. Fora de Escopo

- Notificações push (isso é HU17, Could have — usa o mesmo Service
  Worker, mas a lógica de push fica para lá).
- Modo offline completo com fila de pedidos pendentes de sincronização —
  o TCC explicitamente lista "Modo Offline" como Won't have (§5.3.4): "A
  aplicação exigirá conexão ininterrupta com a internet".
- Splash screen customizada por dispositivo (iOS `apple-touch-icon`
  específico) além do manifest padrão.
- Geração automatizada dos 8 tamanhos de ícone a partir do logo — pode
  ser feita manualmente ou com uma ferramenta como `@angular/pwa`'s
  companheiro `pwa-asset-generator`, mas a execução fica para a
  implementação, não faz parte deste contrato.

## 8. Definição de Pronto

- [ ] `ng add @angular/pwa` executado; `@angular/service-worker` em
      `package.json`.
- [ ] `manifest.webmanifest` customizado com nome, cores dos tokens F2 e
      ícones reais do H2Gás (não o ícone genérico do Angular).
- [ ] `ngsw-config.json` com `dataGroups` cobrindo `/h2gas/api/**` em
      estratégia `freshness`, timeout 5s.
- [ ] `ng build --configuration production` gera `ngsw.json` e
      `manifest.webmanifest` em `dist/`.
- [ ] Auditoria Lighthouse manual confirma instalabilidade.
- [ ] Revisado e aprovado — encerra a fase de Fundação (F1–F5); próximo
      script é HU01, início do sub-projeto UX Cliente.
