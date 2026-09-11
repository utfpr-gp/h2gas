# HU04 — Gestão de Múltiplos Endereços

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU04 |
| Perfil | Cliente (CLIENT) |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (`Address` `@ManyToOne`), HU01 (`LocationContextService`, `first-access-address`), HU03 (checkout já toca `order-resume`) |
| Referência no TCC | Apêndice A.1 — HU04 |

## 2. História de Usuário

Como cliente, quero cadastrar múltiplos endereços, para escolher
rapidamente o local de entrega durante a finalização do pedido.

## 3. Critérios de Aceitação (BDD)

*(texto conferido diretamente contra o Apêndice A, pág. 51 do TCC —
correção de uma versão anterior deste script que havia reconstruído os
cenários a partir do resumo do capítulo 5.3, não do Apêndice A.)*

- **Cenário 1 — Novo endereço**: Dado a gestão de perfil, Quando cadastra
  um novo local, Então o sistema permite salvar e atribuir um rótulo
  identificador (exemplo: casa, trabalho).
- **Cenário 2 — Seleção no checkout**: Dado a tela de finalização, Quando
  chega à confirmação logística, Então exibe a lista de endereços salvos
  para alternância ágil.

## 4. Estado atual (achado crítico)

O Cenário 1 real exige "atribuir um rótulo identificador" — o campo
`Address.addressName` já existe no código desde antes do F3 (achado já
registrado na HU01), então essa parte do requisito já está coberta
estruturalmente; falta só a tela de gestão que exponha isso ao usuário.

O F3 tornou `Address.user` um `@ManyToOne` (schema aceita N endereços por
usuário), mas **a camada de aplicação nunca foi atualizada** — e isso
quebra em produção assim que um cliente tiver 2+ endereços:

- `AddressRepository.findByUserId(String id)` é um método derivado que
  retorna `Optional<Address>`. Com mais de uma linha correspondente, o
  Spring Data JPA lança `IncorrectResultSizeDataAccessException` **em
  runtime**, não em compilação — silencioso até alguém realmente ter dois
  endereços.
- `OrderService.createOrder` chama
  `addressService.findEntityByUserId(user.getId())` para decidir o
  endereço de entrega do pedido — ou seja, **o checkout nunca de fato
  deixa o cliente escolher**; o backend sempre infere "o" endereço do
  usuário, e quebra (exceção acima) assim que existir mais de um. Corrigir
  isso é o núcleo técnico desta história, não só adicionar uma tela nova.

## 5. Contrato Técnico

### 5.1 Backend

**`AddressRepository.java`**:
```java
// substitui o antigo findByUserId (perigoso com N linhas)
Optional<Address> findFirstByUserIdOrderByIdAsc(String userId);

// novo — lista completa
List<Address> findAllByUserId(String userId);
```

**`AddressService.java`**:
- `findByUserIdAddress`/`findEntityByUserId` passam a chamar
  `findFirstByUserIdOrderByIdAsc` (mantém o contrato usado pelo guard da
  HU01 — "existe pelo menos um endereço?" — sem quebrar nada lá).
- Novo `findAllByUserIdAddress(String userId): List<AddressDTO>`, usando
  `findAllByUserId`.

**`AddressController.java`** — novo endpoint:
```java
@GetMapping("/by-user/{userId}/all")
@RolesAllowed({"ADMIN", "SUPPLIER", "CUSTOMER"})
public ResponseEntity<List<AddressDTO>> getAllAddressesByUserId(@PathVariable String userId) {
    return ResponseEntity.ok(addressService.findAllByUserIdAddress(userId));
}
```

**`OrderRequestDTO.java`** — adicionar `private Long addressId;`.

**`OrderService.createOrder`** — parar de inferir o endereço, usar o que
o cliente escolheu no checkout, validando posse:
```java
Address deliveryAddress = addressService.findByIdEntity(orderRequestDTO.getAddressId());
if (!deliveryAddress.getUser().getId().equals(user.getId())) {
    throw new BusinessRuleException("Endereço não pertence ao cliente informado.");
}
order.setDeliveryAddress(deliveryAddress);
```
(a validação de posse é necessária porque, sem ela, qualquer cliente
autenticado poderia enviar o `addressId` de outra pessoa.)

### 5.2 Frontend

**Componente compartilhado `AddressFormComponent`**
(`components/address-form/`) — extrai o formulário de CEP+endereço hoje
embutido em `first-access-address` (HU01) para um componente reutilizável
com `@Input() mode: 'first' | 'add'` e `@Output() saved`. `first-access-address`
passa a envolver esse componente; `manage-addresses` (abaixo) também.

**Novo componente `manage-addresses`**
(`components/pages/manage-addresses/`):
- Lista todos os endereços via `AddressService.findAllByUser(userId)`
  (novo método), exibindo `addressName` (rótulo) + `streetName`.
- Botão "+ Adicionar endereço" abre `AddressFormComponent` em modo `add`
  — ao salvar, **cria** um novo registro (`POST /api/addresses`), não
  edita o existente.

**`address.service.ts`** — novo método:
```ts
findAllByUser(userId: string) {
  return this.http.get<Address[]>(`${this.baseUrl}/by-user/${userId}/all`);
}
```

**`LocationContextService` (HU01)** — `current()` passa a incluir
`addressId`; novo `selectAddress(address)` usado tanto pela tela de
gestão quanto pelo seletor do checkout.

**`order-resume.component.ts`** (checkout, já tocado na HU03) — adiciona
seletor de endereço: busca `findAllByUser`, exibe como lista/rádio,
seleção atualiza `LocationContextService` **e** é enviada como
`addressId` no payload de `createOrder` (fechando o gap do §4).

**`menu.component.ts`** (navbar, HU01) — o link "editar endereço" passa a
rotear para `/manage-addresses` em vez de reabrir `first-access-address`.

## 6. Plano de Testes (TDD)

**Backend:**
1. `AddressRepositoryTest` — `deveRetornarTodosOsEnderecosDeUmUsuario`,
   `deveRetornarPrimeiroEnderecoDeFormaDeterministicaComMultiplosEnderecos`
   (regressão do bug do §4).
2. `AddressServiceTest` — `deveListarTodosEnderecosDoUsuario`.
3. `OrderServiceTest` —
   `deveUsarAddressIdInformadoNoPedidoEmVezDeInferirDoUsuario`,
   `deveRejeitarPedidoComAddressIdQueNaoPertenceAoCliente` (espera
   `BusinessRuleException`).
4. `AddressControllerTest` — `deveRetornarListaCompletaNoEndpointAll`.

**Frontend:**
5. `address.service.spec.ts` — `deveBuscarTodosEnderecosDoUsuario`.
6. `manage-addresses.component.spec.ts` —
   `deveAdicionarNovoEnderecoComRotuloIdentificadorSemAfetarExistentes`,
   `deveListarEnderecosJaCadastrados`.
7. `order-resume.component.spec.ts` —
   `devePermitirSelecionarEnderecoNoCheckoutSemRedigitar`,
   `deveEnviarAddressIdEscolhidoAoCriarPedido`.

## 7. Fora de Escopo

- Editar ou excluir um endereço existente — não é critério de aceitação
  desta HU (`AbstractService.delete()` já existe no frontend por herança
  genérica, mas não há endpoint de exclusão no backend; não conectar sem
  um requisito explícito).
- Marcar um endereço como "padrão/favorito" com toggle de UI — a ordenação
  por `id` (§5.1) é só um fallback técnico interno, não uma feature
  visível ao usuário.
- Qualquer mudança na tela `first-access-address` além de passar a usar
  o `AddressFormComponent` compartilhado.

## 8. Definição de Pronto

- [ ] `AddressRepository`/`AddressService` não quebram mais com múltiplos
      endereços por usuário.
- [ ] `GET /api/addresses/by-user/{userId}/all` implementado.
- [ ] `OrderService.createOrder` usa `addressId` do request, valida posse.
- [ ] `manage-addresses` permite adicionar endereço com rótulo
      identificador, preservando os já cadastrados.
- [ ] Checkout permite selecionar entre endereços salvos sem redigitar.
- [ ] Todos os testes da seção 6 passando.
- [ ] Revisado e aprovado antes de HU05 começar.
