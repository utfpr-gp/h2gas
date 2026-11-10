# HU29 — Entregadores: desativar e reativar, e entregas feitas à vista

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU29 |
| Perfil | Fornecedor (desativa/reativa); Entregador (vê as entregas feitas) |
| Prioridade MoSCoW | Must have. Revisão do orientador em 2026-11-09: "Não existe opção de remover ou desativar entregador" e "O entregador poderia ter o histórico de entregas já realizadas. Depois que ele diz que entregou, aquela entrega some." Decisão do autor: **desativar e reativar**, sem excluir. |
| Dependências | HU07 (despacho e módulo do entregador), HU22 (gestão de entregadores), HU24 (nota do entregador), V9 (`users.enabled`) |
| Branch | `fix/entregadores` |

Itens, na ordem (um por vez, validado pelo autor antes do commit):
(a) desativar e reativar entregador; (b) entregas feitas à vista.

## 2. História de Usuário

Como fornecedor, quero desativar um entregador que saiu da loja sem perder o
histórico dele, e reativá-lo se voltar. Como entregador, quero ver as
entregas que já fiz, sem que elas sumam ao confirmar.

## 3. Critérios de Aceitação (BDD)

### (a) Desativar e reativar

- **Cenário 1 — Desativar**: Dado um entregador livre da minha frota, Quando
  toco em "Desativar" e confirmo, Então ele aparece como "Desativado", sai da
  lista do despacho e não consegue mais entrar no app (login recusado e sessão
  aberta perde o acesso).
- **Cenário 2 — Em rota**: entregador com pedido "Saiu para entrega" não pode
  ser desativado (409, "Conclua ou reatribua a entrega em rota antes de
  desativar").
- **Cenário 3 — Reativar**: um entregador desativado volta com "Reativar":
  aparece no despacho e entra no app com a mesma senha.
- **Cenário 4 — Histórico preservado**: pedidos, recusas e avaliações do
  entregador continuam (nada é excluído).
- **Cenário 5 — Só a própria frota**: outra loja recebe 403.

### (b) Entregas feitas

- **Cenário 6**: na tela "Entregas", uma seção "Entregues hoje" lista as
  entregas confirmadas hoje, com o atalho "Ver histórico". Ao confirmar uma
  entrega, ela aparece ali (não some) e o aviso diz onde encontrá-la.

## 4. Estado atual (achados)

- `Deliverer.active` existe e o despacho já recusa entregador inativo
  (`OrderService.assignDeliverer`), mas não há rota nem botão para mudar; o
  campo não bloqueia o login.
- O bloqueio de login real é `User.enabled` (V9): o login por senha
  (`DisabledException`), o login por código e o `SecurityFilter` já respeitam.
- `GET /api/orders/my-deliveries/history` já existe (entregues e recusadas,
  `deliveredToday`) e a tela `/entregas/historico` também; depois de confirmar,
  o app volta para `/entregas`, onde a entrega some.

## 5. Contrato Técnico

### 5.1 (a)

- `PATCH /api/deliverers/{id}/active`, só SUPPLIER, corpo
  `DelivererActiveDTO { active: boolean }` (`@NotNull`, `@Schema`) → 200 com o
  `DelivererDTO` atualizado. Muda `Deliverer.active` e `User.enabled` juntos,
  na mesma transação. 403 (outra frota), 404, 409 (desativar com entrega em
  rota). OpenAPI completo.
- `DelivererService.setActive(id, supplierId, active)`.
- Front (`manage-deliverers`): no card expandido, "Desativar entregador" com
  confirmação, ou "Reativar"; selo "Desativado"; ativos primeiro na lista.
  `DelivererService.setActive(id, active)` (front).

### 5.2 (b)

- Front (`deliverer-routes`): seção "Entregues hoje" com os itens `DELIVERED`
  de hoje vindos de `GET /my-deliveries/history`, e link para
  `/entregas/historico`. `deliverer-route-detail`: ao confirmar, o aviso
  "Entrega confirmada. Ela fica em Entregues hoje." e volta para `/entregas`.

## 6. Plano de Testes (TDD)

(a)
1. `DelivererServiceTest`: `deveDesativarEBloquearOLogin`,
   `deveReativarELiberarOLogin`, `deveRecusarDesativarComEntregaEmRota`,
   `deveNegarEntregadorDeOutraFrota`.
2. `DelivererControllerTest`: `deveDesativarEntregadorDaFrota` (200) e
   `deveRecusarCorpoSemActive` (400).
3. `manage-deliverers.component.spec.ts`: `deveDesativarComConfirmacao`,
   `deveReativarEntregadorDesativado`, `deveMostrarDesativadosNoFim`.

(b)
4. `deliverer-routes.component.spec.ts`: `deveMostrarAsEntregasDeHoje`.
5. `deliverer-route-detail.component.spec.ts`: aviso ao confirmar.

## 7. Fora de Escopo

- Excluir entregador de vez.
- Reatribuir automaticamente a entrega em rota de quem será desativado.
- Filtros ou paginação no histórico do entregador.

## 8. Definição de Pronto

- [ ] Itens (a) e (b) com testes verdes e validados pelo autor.
- [ ] Rota nova documentada no Swagger.
- [ ] `mvn test` só com o `contextLoads`; `ng test` sem falha; `ng build` ok.
- [ ] Nada do "Fora de Escopo".
