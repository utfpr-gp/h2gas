# HU22 — Fornecedor vê o e-mail e redefine a senha do entregador

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU22 |
| Perfil | Fornecedor |
| Prioridade MoSCoW | Should have. Pedido do autor em 2026-10-29: "como fornecedor, gostaria de ver e-mail e senha do entregador, caso precise". |
| Dependências | HU07 (frota do fornecedor), F6 (OpenAPI) |
| Decisão | A senha atual **não é mostrada**: é guardada com BCrypt, que não se desfaz. No lugar, o fornecedor vê o e-mail e define uma senha nova (decisão do autor, 2026-10-29). |

## 2. História de Usuário

Como fornecedor, quero ver o e-mail de cada entregador da minha frota e
poder definir uma senha nova para ele, para ajudá-lo a entrar no app quando
ele esquecer o acesso.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — E-mail na lista**: Dado a tela Entregadores, Então cada
  entregador mostra o e-mail, com um botão de copiar.
- **Cenário 2 — Redefinir senha**: Dado um entregador da minha frota,
  Quando defino uma senha nova (mínimo 6 caracteres, digitada duas vezes),
  Então ela passa a valer no login dele, e a antiga deixa de valer.
- **Cenário 3 — Só a própria frota**: Dado um entregador de outra loja,
  Quando tento redefinir a senha dele, Então recebo 403 e nada muda.
- **Cenário 4 — Validação**: Senha com menos de 6 caracteres → 400.
  Entregador inexistente → 404.
- **Cenário 5 — Sem exposição**: Nenhuma rota devolve senha, nem em texto
  nem com hash.

## 4. Estado atual (achados)

- `GET /api/deliverers/mine` já devolve o e-mail (`DelivererDTO.email`),
  mas `manage-deliverers` mostra só nome, veículo e telefone.
- A senha é gravada com `BCryptPasswordEncoder` em
  `DelivererService.createDeliverer`.
- Não há rota para o fornecedor trocar a senha de um entregador.

## 5. Contrato Técnico

- **`PUT /api/deliverers/{id}/password`**, só `SUPPLIER`:
  - corpo `DelivererPasswordDTO { password }` (`@NotBlank`, `@Size(min = 6)`);
  - resposta **204**;
  - o entregador precisa ser da frota do fornecedor logado (o id vem do
    token), senão **403**;
  - id inexistente → **404**; senha inválida → **400**.
- `DelivererService.resetPassword(delivererId, supplierId, password)`,
  `@Transactional`, grava o hash BCrypt no `User` do entregador.
- OpenAPI com os status reais.
- Frontend:
  - `manage-deliverers` mostra o e-mail com botão de copiar e o botão
    "Redefinir senha";
  - o botão abre um formulário na própria linha com "Nova senha" e
    "Repita a senha", com validação de 6 caracteres e das duas iguais;
  - ao salvar, aparece um toast "Senha de <nome> redefinida.";
  - `DelivererService.resetPassword(id, password)` no frontend.

## 6. Plano de Testes (TDD)

1. `DelivererServiceTest`:
   - `deveRedefinirASenhaDoEntregadorDaFrota` (o novo hash confere com a
     senha nova e não confere com a antiga);
   - `naoDeveRedefinirSenhaDeEntregadorDeOutraLoja` (403, sem mudança);
   - `deveRetornar404ParaEntregadorInexistente`.
2. `DelivererControllerTest`:
   - `deveRedefinirASenhaComOFornecedorDoToken` (204);
   - `deveRecusarSenhaCurta` (400).
3. `manage-deliverers.component.spec.ts`:
   - `deveMostrarOEmailDoEntregador`, `deveRedefinirASenha`;
   - `deveExigirSenhasIguaisEComSeisCaracteres`, `deveMostrarOErroDoBackend`.

**E2E (registrado no DoD)**: como fornecedor de teste, cadastrar um
entregador, redefinir a senha dele e entrar com a senha nova; a antiga é
recusada. Em 390 e 1280 px.

## 7. Fora de Escopo

- Mostrar a senha atual (impossível com BCrypt e indesejável).
- Encerrar as sessões já abertas do entregador: o JWT atual vale até
  expirar.
- E-mail automático avisando o entregador.
- Editar nome, telefone ou e-mail do entregador.

## 8. Definição de Pronto

- [ ] E-mail visível e copiável na lista da frota.
- [ ] Senha redefinida só para a própria frota, com 400, 403 e 404.
- [ ] Rota documentada no Swagger.
- [ ] Testes da seção 6 verdes; `mvn test` e `ng test` sem falha nova;
      `ng build` ok.
- [ ] E2E registrado aqui com data.
- [ ] Nada do "Fora de Escopo".
