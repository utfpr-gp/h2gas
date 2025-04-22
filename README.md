# H2Gás

O **H2Gás** é uma aplicação web desenvolvida para facilitar a gestão de pedidos e entregas de gás e água. A plataforma permite que consumidores consultem produtos e preços, realizem pedidos online e acompanhem suas entregas em tempo real. Para distribuidores, oferece um painel administrativo para cadastro e gerenciamento de produtos, entregas e cidades atendidas. Com funcionalidades como navegação por mapa para os entregadores e um sistema eficiente de comunicação entre clientes e distribuidores, o H2Gás busca modernizar o setor, tornando o processo de compra e entrega mais ágil, acessível e competitivo.

## 📃 Equipe

Desenvolvido por: [Pablo de Paula Correia](https://github.com/PabloC5)\
Orientador: [Dr. Roni Fabio Banaszewski](https://ronifabio.github.io/)

## 🔧 Tecnologias utilizadas

### **Backend**

- **[Java 17](https://www.oracle.com/java)**
- **[Spring Boot 3](https://spring.io/projects/spring-boot)**
- **[Spring Security](https://spring.io/projects/spring-security)** – Controle de autenticação e autorização
- **[MySQL](https://www.mysql.org)** – Banco de dados relacional
- **[Hibernate](https://hibernate.org)** – ORM para gerenciar persistência de dados
- **[Flyway](https://flywaydb.org)** – Versionamento e migração do banco de dados
- **[Swagger](https://swagger.io)** – Documentação automática da API
- **[Docker Compose](https://docs.docker.com/compose/)** – Orquestração de containers

### **Frontend**

- **[Angular](https://angular.io)**
- **[Figma](https://www.figma.com/design/pCcfhdubJ69xdHNtVqj32B/tcc2?node-id=0-1&p=f&t=WCkkDzVxo48i4ABP-0)** - prototipação das telas
- **[RxJS](https://rxjs.dev/)** – Manipulação reativa de dados e requisições HTTP
- **[Tailwind CSS](https://tailwindcss.com/) + [DaisyUI](https://daisyui.com/)** – Estilização moderna e responsiva
- **[Leaflet.js](https://leafletjs.com/)** – Integração com mapas

### **Infraestrutura e DevOps**

- **[Docker](https://www.docker.com/)** – Empacotamento e execução de serviços
- **[GitHub Actions](https://github.com/features/actions)** – CI/CD para automação de deploy
- **[Nginx](https://www.nginx.com/)** – Servidor reverso para balanceamento de carga
- **[Cloudinary](https://cloudinary.com/)** – Armazenamento e otimização de imagens

## 🛠️ Configuração e execução do projeto

### 1. Configuração dos arquivos `.env`

Configurar os seguintes arquivos `.env` com as variáveis de ambiente necessárias:

- **Na raiz do projeto:** `.env`
- **No backend:** `backend/.env`
- **No frontend:** `frontend/src/environments/environment.ts` (para desenvolvimento, use o arquivo `environment.dev.ts`)

Exemplo de configuração das variáveis de ambiente (arquivo `.env`):

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=h2gas
DB_USER=root
DB_PASSWORD=senha
```

> **Observação:** Certifique-se de comentar ou descomentar a linha correta no arquivo `application.properties` no backend para configurar o SSL ao rodar localmente ou via Docker.

### 2. Como executar a aplicação?

#### **2.1 Rodando localmente via IDE**

**Banco de Dados:**

Certifique-se de que o MySQL está rodando na porta 3306
Crie o banco de dados manualmente (caso necessário):

```
CREATE DATABASE h2gas;
```

**Backend:**

Clonar o repositório:

```
git clone https://github.com/utfpr-gp/h2gas.git
cd h2gas
```

Configurar os arquivos .env conforme descrito anteriormente.
Abrir o backend em sua IDE (IntelliJ, Eclipse, etc.)
Verificar o arquivo application.properties
Rodar a aplicação Spring Boot pela classe principal

**Frontend:**

Acesse a pasta `frontend` e execute:

```
nvm install 20.9.0
nvm use 20.9.0
npm install
ng serve
```

O frontend estará disponível em [https://localhost:4200](https://localhost:4200)

#### 2.2 Rodando com Docker Compose

Configurar o arquivo `.env`
Construir e subir os containers:

```
docker compose build
docker compose up -d
```

Verifique se os containers estão rodando:

```
docker ps
```

Acesse a aplicação nos seguintes endereços:

- **API Backend:** [https://localhost:8080](https://localhost:8080)
- **Frontend:** [https://localhost:4200](https://localhost:4200)

## 🔗 Endpoints de documentação da API

- **Swagger UI:** [https://localhost:8080/swagger-ui.html](https://localhost:8080/swagger-ui.html)
- **Documentação JSON:** [https://localhost:8080/v3/api-docs](https://localhost:8080/v3/api-docs)


## 🌄 Layout

Os protótipos da aplicação estão disponíveis no [Figma](link).

## 📒 Monografia

A monografia da aplicação pode ser acessada neste link: [Overleaf](link)

## 🔧 Ferramentas recomendadas

- **IDE:** IntelliJ IDEA para backend e VSCode para frontend
- **Gerenciador de banco de dados:** MySQL Workbench ou outro de preferência
- **Cliente de API:** Postman (ou Insomnia, se preferir)


## 🔧 Observações adicionais

- O sistema está preparado para ser executado tanto localmente quanto em ambientes conteinerizados usando Docker.
- Para ambientes de produção, revise as configurações SSL corretamente para garantir a segurança.
