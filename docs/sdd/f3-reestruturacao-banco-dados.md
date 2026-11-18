# F3 — Reestruturação do Banco de Dados

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | F3 |
| Perfil | Infraestrutura (Fundação) — base para HU07, HU08, HU09, HU10, HU14 |
| Prioridade MoSCoW | Must have |
| Dependências | F1 (Harness) |
| Referência no TCC | Cap. 6.6 "Modelagem do Banco de Dados" — Figura 19 |

## 2. Objetivo

Como desenvolvedor implementando as próximas histórias do módulo logístico
e de avaliações, quero que o schema do banco de dados já suporte
`deliverers`, `reviews`, preço segregado por modalidade e os timestamps de
SSE, para que HU07, HU08, HU09, HU10 e HU14 só precisem de lógica de
aplicação, sem mexer em estrutura de dados.

## 3. Divergências encontradas e decisões

1. **PostgreSQL vs MySQL** (já decidido no F1): schema real segue MySQL.
   Todo SQL abaixo é dialeto MySQL 8+/9.x.
2. **Ausência de Flyway** (já decidido no F1): este script introduz Flyway,
   substituindo `ddl-auto=update` para mudanças estruturais.
3. **Gap não documentado no TCC — endereços já são "único por usuário"**:
   a Figura 19 do TCC lista a coluna `address_name` como a única mudança em
   `addresses`. Na inspeção do código real, porém, `Address.addressName`
   **já existe** (não é gap) — mas a associação
   `Address.user` está mapeada como `@OneToOne`, e
   `AddressRepository.findByUserId` retorna `Optional<Address>` (singular).
   Ou seja, **o schema atual só permite 1 endereço por usuário**, o que
   inviabiliza HU04 (Gestão de Múltiplos Endereços) por completo. O TCC não
   menciona essa mudança de cardinalidade explicitamente, mas ela é
   estruturalmente obrigatória para a Must-have HU04 — por isso está
   incluída aqui, na Fundação, e não em HU04 (HU04 só vai cuidar da
   UI/lógica de seleção, assumindo que o schema já suporta N endereços).

## 4. Critérios de Aceitação (BDD)

- **Cenário 1 — Migração aplicada sem perda de dados**: Dado um banco com
  dados legados do MVP 1.0 (produtos com `price` preenchido), Quando a
  migração `V2` roda, Então todo produto passa a ter `price_delivery` e
  `price_pickup` preenchidos com o valor antigo de `price`, e a coluna
  `price` deixa de existir.

- **Cenário 2 — Múltiplos endereços por usuário**: Dado um usuário
  cliente, Quando ele tenta salvar um segundo endereço, Então a operação é
  aceita no banco (sem violar constraint de unicidade em `user_id`).

- **Cenário 3 — Entregador vinculado a fornecedor**: Dado um usuário com
  `user_type = DELIVERER`, Quando um registro é criado em `deliverers`
  referenciando esse usuário e um `supplier_id`, Então a FK é aceita e o
  campo `active` tem default `true`.

- **Cenário 4 — Pedido rastreável por entregador e horário**: Dado um
  pedido existente, Quando ele é atribuído a um entregador e despachado,
  Então `orders.deliverer_id` e `orders.dispatched_at` podem ser
  preenchidos; ao concluir, `orders.delivered_at` também.

- **Cenário 5 — Avaliação restrita por FK**: Dado um pedido `DELIVERED`,
  Quando uma `review` é inserida referenciando esse pedido, seu fornecedor
  e (opcionalmente) o entregador, Então a nota é aceita apenas se
  `rating` estiver entre 1 e 5 (constraint `CHECK`), rejeitada fora desse
  intervalo.

- **Cenário 6 — Baseline sem downtime**: Dado o banco de produção/dev já
  populado (criado via `ddl-auto=update`), Quando o Flyway é introduzido,
  Então a aplicação sobe normalmente sem tentar recriar tabelas existentes
  (baseline correto), e só then aplica a `V2` de fato.

## 5. Contrato Técnico

### 5.1 Setup do Flyway

`backend/pom.xml` — adicionar dependência:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

`backend/src/main/resources/application-dev.properties` — trocar:

```properties
# antes: spring.jpa.hibernate.ddl-auto=update
spring.jpa.hibernate.ddl-auto=validate

spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
```

`spring.jpa.hibernate.ddl-auto=validate` garante que, dali em diante, toda
mudança de schema passa obrigatoriamente por uma migration — se a entidade
Java e o banco divergirem, a aplicação falha ao subir (em vez de aplicar
silenciosamente uma alteração via Hibernate).

### 5.2 Migração `V1__baseline.sql`

Arquivo vazio (apenas comentário), em
`backend/src/main/resources/db/migration/V1__baseline.sql`:

```sql
-- Baseline: schema já existente, criado historicamente via ddl-auto=update.
-- Este arquivo não altera nada; com spring.flyway.baseline-version=1,
-- o Flyway assume que o banco já satisfaz V1 e só aplica V2 em diante.
```

### 5.3 Migração `V2__h2gas_2_0_schema.sql`

```sql
-- 1) addresses: permitir múltiplos endereços por usuário
--    (a coluna address_name JÁ EXISTE — só remover a unicidade de user_id)
SET @idx := (
  SELECT INDEX_NAME FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'addresses'
    AND COLUMN_NAME = 'user_id' AND NON_UNIQUE = 0
  LIMIT 1
);
SET @sql := IF(@idx IS NOT NULL,
  CONCAT('ALTER TABLE addresses DROP INDEX `', @idx, '`'),
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) product_items: segregar preço por modalidade (RN01/HU10)
ALTER TABLE product_items ADD COLUMN price_delivery DECIMAL(10,2) NULL;
ALTER TABLE product_items ADD COLUMN price_pickup DECIMAL(10,2) NULL;
UPDATE product_items SET price_delivery = price, price_pickup = price;
ALTER TABLE product_items MODIFY COLUMN price_delivery DECIMAL(10,2) NOT NULL;
ALTER TABLE product_items MODIFY COLUMN price_pickup DECIMAL(10,2) NOT NULL;
ALTER TABLE product_items DROP COLUMN price;

-- 3) deliverers: novo perfil operacional (HU07)
CREATE TABLE deliverers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  supplier_id VARCHAR(255) NOT NULL,
  active BIT NOT NULL DEFAULT 1,
  vehicle_info VARCHAR(255) NULL,
  CONSTRAINT fk_deliverers_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_deliverers_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(user_id)
);

-- 4) orders: rastreabilidade logística (HU07/HU08)
ALTER TABLE orders ADD COLUMN deliverer_id BIGINT NULL;
ALTER TABLE orders ADD COLUMN change_for DECIMAL(10,2) NULL;
ALTER TABLE orders ADD COLUMN dispatched_at DATETIME NULL;
ALTER TABLE orders ADD COLUMN delivered_at DATETIME NULL;
ALTER TABLE orders ADD CONSTRAINT fk_orders_deliverer
  FOREIGN KEY (deliverer_id) REFERENCES deliverers(id);

-- 5) reviews: avaliação pós-entrega (HU14)
CREATE TABLE reviews (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id VARCHAR(255) NOT NULL,
  supplier_id VARCHAR(255) NOT NULL,
  deliverer_id BIGINT NULL,
  rating INT NOT NULL,
  comment TEXT NULL,
  CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_reviews_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(user_id),
  CONSTRAINT fk_reviews_deliverer FOREIGN KEY (deliverer_id) REFERENCES deliverers(id),
  CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
```

> **Atenção na implementação**: o `SET @idx := ...` do passo 1 depende do
> nome real do índice único gerado pelo Hibernate para o `@OneToOne`
> atual, que não pôde ser inspecionado nesta fase (sem acesso a um banco
> rodando). O bloco é escrito para descobrir o nome dinamicamente e ser
> seguro mesmo se não houver índice único (`SELECT 1` como no-op) — mas
> **deve ser validado contra o banco de dev real antes do merge**.

### 5.4 Entidades JPA

**`Address.java`** — trocar cardinalidade:

```java
// antes
@OneToOne
@JoinColumn(name = "user_id")
private User user;

// depois
@ManyToOne
@JoinColumn(name = "user_id")
private User user;
```

**`UserTypeEnum.java`** — adicionar `DELIVERER("deliverer")`.

**`ProductItem.java`** — trocar `private BigDecimal price;` por:

```java
private BigDecimal priceDelivery;
private BigDecimal pricePickup;
```

**`Order.java`** — adicionar:

```java
@ManyToOne
@JoinColumn(name = "deliverer_id")
private Deliverer deliverer;

private BigDecimal changeFor;
private LocalDateTime dispatchedAt;
private LocalDateTime deliveredAt;
```

**Nova entidade `Deliverer.java`** (`model/entity/Deliverer.java`):

```java
@Entity
@Table(name = "deliverers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Deliverer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    private boolean active = true;

    private String vehicleInfo;
}
```

**Nova entidade `Review.java`** (`model/entity/Review.java`):

```java
@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "deliverer_id")
    private Deliverer deliverer;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}
```

### 5.5 Repositórios novos

`DelivererRepository extends JpaRepository<Deliverer, Long>` e
`ReviewRepository extends JpaRepository<Review, Long>`, seguindo o padrão
existente (interface simples, sem implementação customizada nesta fase —
métodos de busca específicos ficam para HU07/HU14).

## 6. Plano de Testes (TDD)

Escrever **antes** da implementação (Red), seguindo o padrão de
`backend/src/test/java/com/h2gas/services/*ServiceTest.java`:

1. `AddressServiceTest` — novo caso: `deveAceitarDoisEnderecosParaMesmoUsuario`
   (salva dois `Address` com o mesmo `user`, espera sucesso sem exceção de
   constraint).
2. `DelivererRepositoryTest` (novo, `@DataJpaTest`) —
   `deveSalvarEntregadorVinculadoAUsuarioESupplier`.
3. `ReviewRepositoryTest` (novo, `@DataJpaTest`) —
   `deveSalvarAvaliacaoValida` (rating 1–5) e
   `deveRejeitarAvaliacaoComNotaForaDoIntervalo` (rating 0 ou 6, espera
   `DataIntegrityViolationException` pela constraint `chk_reviews_rating`).
4. `ProductItemServiceTest` — atualizar testes existentes que hoje usam
   `.setPrice(...)` para `.setPriceDelivery(...)`/`.setPricePickup(...)`;
   nenhum teste deve mais compilar contra o campo `price` removido.
5. Teste de contexto (`BackendApplicationTests`, já existente) — deve
   continuar subindo com sucesso, validando que `ddl-auto=validate` +
   Flyway não quebram o boot da aplicação (é o teste de regressão da
   migração como um todo).

## 7. Fora de Escopo

- Endpoints REST, DTOs ou services de negócio para `Deliverer`/`Review`
  (ficam para HU07/HU14 — F3 só entrega entidade + repositório).
- Lógica de aplicação que efetivamente lista múltiplos endereços de um
  usuário (`AddressRepository.findByUserId` continua retornando um único
  `Optional<Address>` por enquanto — a mudança de assinatura/uso é de
  HU04). F3 só remove o bloqueio estrutural do banco.
- UI de preço segregado no cadastro de produto (HU10).
- Qualquer migração de dados de produção real — este script assume
  ambiente de desenvolvimento; migração de produção exige plano de
  rollback à parte, não coberto aqui.

## 8. Definição de Pronto

- [x] Flyway configurado (`pom.xml`, `application-dev.properties`),
      `ddl-auto=validate`.
- [x] `V1__baseline.sql` e `V2__h2gas_2_0_schema.sql` criados e aplicados
      com sucesso em banco de dev local (índice único de `addresses`
      validado manualmente, não apenas assumido).
- [x] Entidades `Address`, `ProductItem`, `Order`, `UserTypeEnum`
      atualizadas; `Deliverer` e `Review` criadas.
- [x] `DelivererRepository` e `ReviewRepository` criados.
- [x] Todos os testes da seção 6 passando; suíte completa
      (`mvn -f backend/pom.xml test`) verde.
- [x] Revisado e aprovado antes de F4 começar.
