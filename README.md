# H2Gás

### **Autor:** Igor Emanuel
### **Orientador:** Roni Fabio Banaszewski · **Coorientador:** Pablo de Paula Correia

O H2Gás é uma aplicação web (PWA) de pedidos e entregas de gás de cozinha (GLP) e água mineral. O cliente encontra
as lojas da sua cidade, compara preços e frete por distância, faz o pedido e acompanha a entrega em tempo real; a
loja gerencia pedidos, catálogo, entregadores e indicadores de venda; o entregador recebe as rotas e confirma as
entregas.

Trabalho de Conclusão de Curso de Tecnologia em Sistemas para Internet, UTFPR, Câmpus Guarapuava. A versão 2.0
evolui o MVP desenvolvido por Pablo de Paula Correia.

## 📚 Documentação do Projeto

- [📄 Especificações (SDD)](./docs/sdd/) - Um script por funcionalidade (fundações F1–F12 e histórias HU01–HU29), com
  critérios de aceitação BDD, contrato técnico, plano de testes e definição de pronto.
- [🛠️ Processo de desenvolvimento](./CLAUDE.md) - Regras de SDD + TDD, convenções de código e da API.
- [📘 Documentação da API (Swagger)](https://h2gas.187.127.22.98.sslip.io/h2gas-api/swagger-ui/index.html) - Endpoints da API no site em produção. No
  ambiente local, fica em `http://localhost:8080/h2gas/swagger-ui/index.html`.

## 📽️ Link do Vídeo  

- https://youtu.be/1AYbG5xOiCw

## 🎨 Design

- [🎨 Design tokens e guia de estilo](./docs/sdd/f2-design-tokens.md) - Cores, tema do DaisyUI e tipografia da interface.

## 🌐 Site em Produção

- https://h2gas.187.127.22.98.sslip.io

## 💻 Tecnologias e Dependências

- **Backend:** Java 17, Spring Boot 3.4, Spring Security (JWT), JPA/Hibernate, MySQL 9, Flyway, springdoc-openapi,
  Server-Sent Events e Web Push (VAPID).
- **Frontend:** Angular 19, Tailwind CSS 4 + DaisyUI 5, Leaflet e service worker (PWA).
- **Serviços:** Cloudinary (imagens), SMTP (e-mails e código de login) e Twilio (WhatsApp/SMS, opcional).
- **Infra:** Docker, GitHub Actions (testes em cada PR e imagens no GHCR a cada push na `main`) e Coolify.

## 🚀 Manual de execução

- Clonar o repositório com `git clone` (a branch `main` tem a versão mais recente).
- Copiar `.env.example` para `.env` na raiz e preencher as variáveis. O arquivo de exemplo traz as do banco e do
  Twilio; acrescentar também as que o backend exige:
  - `JWT_SECRET`: chave usada para assinar os *tokens* de acesso;
  - `USER_NAME` e `PASSWORD_EMAIL`: conta SMTP que envia os e-mails e o código de login;
  - `CLOUDINARY_URL`: conta do Cloudinary, onde ficam as imagens;
  - `FRONTEND_URL_DEV`: endereço do frontend, por exemplo `http://localhost:4200`.

  As variáveis de *push* (VAPID) e do Twilio são opcionais.
- Subir o banco e o backend com Docker:
  - Comando: `docker compose up -d --build`
  - O Flyway cria o esquema; a API fica em `http://localhost:8080/h2gas/api`.
- Executar o frontend:
  - Comando: `cd frontend && npm install && npm start`
  - Abre em `http://localhost:4200` (a URL da API fica em `frontend/src/environments/environment.ts`).
- Rodar os testes:
  - Backend: `cd backend && mvn test`
  - Frontend: `cd frontend && npx ng test --watch=false --browsers=ChromeHeadless`

## 🖼️ Telas

| Vitrine do cliente | Entrega (entregador) |
|---|---|
| <img src="./docs/images/vitrine.png" alt="Vitrine com as lojas da cidade, filtros e frete por distância" width="300"> | <img src="./docs/images/entrega.png" alt="Detalhe da entrega com mapa, cliente e confirmação" width="300"> |

**Pedidos recebidos (loja):** pedidos em tempo real, filtro por status, destaque de pedido parado e frete no detalhe.

<img src="./docs/images/pedidos-recebidos.png" alt="Tela de pedidos recebidos da loja" width="800">

**Painel da loja:** receita dos pedidos entregues, indicadores do mês e nota da loja.

<img src="./docs/images/painel-da-loja.png" alt="Painel da loja com receita, pedidos e gráfico por dia" width="800">
