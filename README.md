# H2Gás

O H2Gás é uma aplicação web para gestão de pedidos e entregas de gás e água. Consumidores podem consultar produtos, realizar pedidos e acompanhar entregas; distribuidores têm um painel para gerenciar produtos, regiões e entregas. Entregadores contam com navegação por mapa. O objetivo é modernizar e agilizar o processo de compra e entrega.

## 📃 Equipe

Desenvolvido por: [Pablo de Paula Correia](https://github.com/PabloC5)
Orientador: [Dr. Roni Fabio Banaszewski](https://ronifabio.github.io/)

## 🔧 Stack do projeto

- Backend: Java 17, Spring Boot 3, Spring Security, Hibernate/JPA, MySQL
- Frontend: Angular 19, RxJS, Tailwind CSS + DaisyUI, Leaflet.js
- Infra: Docker, Docker Compose, Nginx
- Outros: Cloudinary (imagens), Twilio (envio/validação), JWT

## 📦 Gerenciadores e entry points

- Backend (Maven):
  - Build: mvn clean package
  - Executar: mvn spring-boot:run
  - Testes: mvn test
  - Entry point: com.h2gas.BackendApplication (Spring Boot)
  - Porta: 8080 (context-path /h2gas)
- Frontend (npm + Angular CLI):
  - Iniciar dev: npm start (ng serve)
  - Build prod: npm run build (gera dist)
  - Testes: npm test (Karma/Jasmine)
  - Servido em produção por Nginx (porta 80)
- Docker Compose: serviços db (MySQL 5.7), backend e frontend

## ✅ Requisitos

- Java 17 (JDK)
- Maven 3.8+
- Node.js 20.x e npm (Angular CLI 19)
- Docker e Docker Compose (opcional, para orquestração)
- MySQL (opcional, se não usar Docker)

## 🔐 Variáveis de ambiente

Arquivo de exemplo: .env.example na raiz.

Principais variáveis utilizadas:
- Banco de dados:
  - MYSQL_HOST (ex.: localhost)
  - MYSQL_PORT (ex.: 3306)
  - MYSQL_DATABASE (ex.: h2gas_db)
  - MYSQL_USER, MYSQL_PASSWORD
  - MYSQL_ROOT_PASSWORD (para container do MySQL)
  - SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD (usadas pelo perfil dev)
  - SPRING_DATASOURCE_URL (composta no docker-compose para rede interna: jdbc:mysql://db:3306/h2gas_db...)
  - TODO: Unificar/confirmar o uso de MYSQL_USER vs MYSQL_USER_B no docker-compose.
- JWT/Segurança:
  - JWT_SECRET
- E-mail (SMTP):
  - USER_NAME, PASSWORD_EMAIL (credenciais SMTP)
  - FRONTEND_URL_DEV (URL do frontend usada em templates de e-mail)
- Twilio (verificação/envio):
  - TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_VERIFY_SERVICE_SID
- reCAPTCHA (se aplicável):
  - google.recaptcha.site.key, google.recaptcha.secret.key
- Outros:
  - DATASOURCE_URL (existe no .env.example; verificar necessidade real no código) – TODO

Observação: o backend importa variáveis de um arquivo .env local via spring.config.import=optional:file:.env[.properties].

## 🚀 Como executar

### Opção A) Local (IDE/CLI)

1) Banco de dados
- Garanta um MySQL disponível (local ou container). Por padrão, o perfil dev lê MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE etc.
- Crie o banco se necessário: CREATE DATABASE h2gas_db;

2) Backend
- Terminal na pasta backend:
  - mvn spring-boot:run
- A API ficará em http://localhost:8080/h2gas

3) Frontend
- Terminal na pasta frontend:
  - npm install
  - npm start
- App web em http://localhost:4200
- O frontend (dev) consome a API em http://localhost:8080/h2gas/api (ver frontend/src/environments/environment.ts)

### Opção B) Docker Compose

1) Configure o arquivo .env na raiz (ver .env.example).
2) Suba os serviços:
- docker compose build
- docker compose up -d
3) Endereços padrão:
- Backend: http://localhost:8080/h2gas
- Frontend: http://localhost/
- MySQL: localhost:3307 (mapeado para 3306 do container)

Parar e remover:
- docker compose down

## 📜 Scripts úteis

- Backend (backend/):
  - mvn clean package -DskipTests
  - mvn spring-boot:run
  - mvn test
- Frontend (frontend/):
  - npm start
  - npm run build
  - npm test
  - npm run build-docker / npm run run-docker (imagens locais)

## 🧪 Testes

- Backend: mvn test (JUnit/Spring Test)
- Frontend: npm test (Karma/Jasmine)

## 🧭 Estrutura do projeto

- backend/ — API Spring Boot (Java 17, Maven)
  - src/main/java/com/h2gas/BackendApplication.java — entry point
  - src/main/resources/application.properties — configurações gerais (context-path /h2gas, JWT, e-mail, Twilio, import .env)
  - src/main/resources/application-dev.properties — datasource e JPA para dev
  - Dockerfile — build multi-stage (Maven -> JAR -> OpenJDK 17)
  - pom.xml — dependências e plugins
- frontend/ — SPA Angular 19
  - src/environments/environment.ts — configuração dev (apiUrl)
  - src/environments/environment.prod.ts — configuração prod (apiUrl)
  - Dockerfile — build com Node e serve com Nginx
  - package.json — scripts npm
- docker-compose.yml — ambiente de desenvolvimento com db, backend e frontend
- docker-compose.prod.yml — exemplo de imagens pré-buildadas para produção
- .env.example — exemplo de variáveis de ambiente
- data/ — volume para dados do MySQL (mapeado)

## 📚 Documentação da API

- TODO: Adicionar/confirmar integração com Swagger/OpenAPI (springdoc). No momento não há dependência explícita no pom.xml.

## 🌄 Layout

Protótipos no Figma: https://www.figma.com/design/pCcfhdubJ69xdHNtVqj32B/tcc2?node-id=0-1&p=f

## 📒 Monografia

Link do Overleaf: TODO (atualizar com URL definitivo)

## 🔧 Ferramentas recomendadas

- IDE: IntelliJ IDEA (backend), VS Code (frontend)
- Banco: MySQL Workbench ou equivalente
- Cliente de API: Postman/Insomnia

## 🔎 Observações

- O projeto pode rodar localmente ou via Docker. Em produção, revise CORS, variáveis sensíveis e HTTPS/SSL conforme infraestrutura.
- O backend usa context-path /h2gas; o frontend aponta para /h2gas/api. Ajuste se mudar o proxy/host.

## 🪪 Licença

- Nenhum arquivo de licença foi encontrado neste repositório. TODO: Definir e adicionar LICENSE (ex.: MIT/Apache-2.0) e atualizar esta seção.
