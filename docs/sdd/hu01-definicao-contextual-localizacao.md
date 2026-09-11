# HU01 — Definição Contextual de Localização

## 1. Cabeçalho

| Campo | Valor |
|---|---|
| ID | HU01 |
| Perfil | Cliente (CLIENT) |
| Prioridade MoSCoW | Must have |
| Dependências | F3 (Address agora `@ManyToOne`), F2 (Design Tokens) |
| Referência no TCC | Apêndice A.1 — HU01; RN05 |

## 2. História de Usuário

Como cliente, quero definir meu endereço de entrega logo ao entrar no
aplicativo, para visualizar na listagem apenas os fornecedores que
realmente atendem à minha região.

## 3. Critérios de Aceitação (BDD)

- **Cenário 1 — Cadastro enxuto**: Dado que o usuário acessa o registro,
  Quando preenche o formulário, Então o sistema exige apenas contatos
  básicos (nome, e-mail, telefone, senha), sem solicitar endereço nesta
  etapa.
- **Cenário 2 — Busca pós-login**: Dado o primeiro acesso (sem endereço
  cadastrado), Quando a home carrega, Então exibe uma interface dedicada
  para busca de endereço via CEP ou Rua.
- **Cenário 3 — Persistência e filtro**: Dado que a localidade foi
  definida, Quando o cliente navega, Então o endereço fica visível na
  navbar **e** o catálogo filtra apenas lojas compatíveis com a região
  (RN05).
- **Cenário 4 (adicional, não coberto explicitamente no TCC) — CEP fora
  de área atendida**: Dado um CEP que resolve para uma cidade sem nenhum
  fornecedor cadastrado no sistema, Quando o cliente confirma o endereço,
  Então o cadastro é aceito normalmente, mas a home exibe estado vazio
  ("nenhum fornecedor na sua região") em vez de erro — RN05 filtra, não
  bloqueia o cadastro do endereço em si.

## 4. Estado atual (achados)

- `register-user.component.ts` **hoje bloqueia o cadastro** sem endereço
  (`if (!this.address) { toastr.warning(...); return; }`) — contradiz
  diretamente o Cenário 1. Precisa ser removido.
- A lógica de busca por CEP **já existe e funciona**:
  `AddressService.buscaViaCep()` (integração com ViaCEP), hoje usada
  dentro do formulário de registro. Vai ser **reaproveitada**, não
  recriada, movendo-se para uma tela pós-login.
- `GET /api/addresses/by-user/{userId}` já existe — serve para checar se
  o cliente já tem endereço cadastrado (Cenário 2/3).
- `supplier-list.component.ts` (home autenticada do cliente) chama
  `getAllSupplierOpenAndActive(page, pageSize)` **sem nenhum filtro de
  cidade** — RN05 não está implementada. Precisa de parâmetro `cityId`.
- Não existe hoje nenhum conceito de "endereço ativo/contexto de
  localização" no frontend (nem serviço, nem exibição na navbar).

## 5. Contrato Técnico

### 5.1 Backend

**`SupplierRepository.java`** — adicionar:
```java
Page<Supplier> findByCityIdAndActiveAndOpen(Long cityId, boolean active, boolean open, Pageable pageable);
```

**`SupplierService.java`** — adicionar:
```java
public Page<SupplierDTO> findAllSuppliersOpenAndActive(Long cityId, Pageable pageable) {
    Page<Supplier> page = (cityId != null)
        ? supplierRepository.findByCityIdAndActiveAndOpen(cityId, true, true, pageable)
        : supplierRepository.findByActiveAndOpen(true, true, pageable);
    return page.map(supplierMapper::toDto);
}
```
(sobrecarrega o método existente; RN05 é aplicada no backend, nunca só no
filtro client-side.)

**`SupplierController.java`** — endpoint `/actives/open` ganha parâmetro
opcional:
```java
@GetMapping("/actives/open")
public ResponseEntity<PageResponse<SupplierDTO>> getAllSuppliersOpenAndActive(
        @RequestParam(required = false) Long cityId,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "size", required = false) Integer size) {
    ...
    Page<SupplierDTO> result = supplierService.findAllSuppliersOpenAndActive(cityId, pageable);
    ...
}
```

### 5.2 Frontend

**`register-user.component.ts`/`.html`** — remover: `registerFormAddress`,
`address`, `salvarAddress()`, `buscarCep()`, `cidadeEncontrada`,
`showAddressModal`, o modal de endereço no template, e o bloqueio
`if (!this.address)`. O payload de `createUser` deixa de incluir
`address`.

**Novo componente `first-access-address`**
(`components/pages/first-access-address/`) — reaproveita a lógica de CEP
extraída de `register-user`:
- Formulário: CEP (busca automática via `AddressService.buscaViaCep`,
  preenche rua/bairro/cidade), número, complemento.
- Ao salvar: `POST /api/addresses` com o `userId` do usuário logado.
- Após sucesso, popula `LocationContextService` (abaixo) e navega para a
  home (`supplier-list`).

**Novo serviço `LocationContextService`**
(`services/location-context.service.ts`):
```ts
@Injectable({ providedIn: 'root' })
export class LocationContextService {
  private readonly STORAGE_KEY = 'h2gas.activeAddress';
  private addressSignal = signal<{ cityId: number; cityName: string; label: string } | null>(
    this.readFromStorage()
  );
  readonly current = this.addressSignal.asReadonly();

  setAddress(address: { cityId: number; cityName: string; label: string }) {
    this.addressSignal.set(address);
    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(address));
  }

  private readFromStorage() {
    const raw = localStorage.getItem(this.STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
```

**Novo guard funcional `addressDefinedGuard`**
(`guards/address-defined.guard.ts`) — nas rotas do Cliente: se
`LocationContextService.current()` é `null`, chama
`GET /api/addresses/by-user/{userId}`; se vazio, redireciona para
`/first-access-address`; se encontrado, popula o `LocationContextService`
e libera a navegação.

**`menu.component.ts`/`.html`** — injeta `LocationContextService`, exibe
`current()?.label` (ex.: "Rua XV de Novembro, 7951 · Guarapuava") na
navbar, com um link para editar (que HU04 vai expandir para múltiplos
endereços — aqui só reabre `first-access-address` em modo edição).

**`supplier-list.component.ts`** — injeta `LocationContextService`, passa
`cityId: this.locationContext.current()?.cityId` para
`getAllSupplierOpenAndActive`.

## 6. Plano de Testes (TDD)

**Backend:**
1. `SupplierServiceTest.deveFiltrarFornecedoresPorCidadeQuandoCityIdInformado`
2. `SupplierServiceTest.deveRetornarTodosAtivosEAbertosQuandoCityIdNulo` (regressão do comportamento atual)
3. `SupplierControllerTest.deveAceitarParametroCityIdOpcionalNoEndpointActivesOpen`

**Frontend:**
4. `register-user.component.spec.ts` — `devePermitirSubmeterCadastroSemEndereco`
   (novo teste substituindo o antigo fluxo bloqueante).
5. `first-access-address.component.spec.ts` — `deveBuscarEnderecoPorCep`,
   `deveSalvarEnderecoEAtualizarLocationContext`.
6. `location-context.service.spec.ts` — `devePersistirEnderecoAtivoNoLocalStorage`,
   `deveRecuperarEnderecoAtivoAposReload`.
7. `address-defined.guard.spec.ts` — `deveRedirecionarParaFirstAccessAddressSeUsuarioNaoTiverEndereco`,
   `deveLiberarNavegacaoSeEnderecoJaExiste`.
8. `menu.component.spec.ts` — `deveExibirEnderecoAtivoNaNavbar`.
9. `supplier-list.component.spec.ts` — `deveEnviarCityIdDoContextoAoBuscarFornecedores`.

## 7. Fora de Escopo

- Gestão de múltiplos endereços / troca rápida entre eles no checkout —
  HU04.
- Navegação por categorias/ordenação dentro do catálogo já filtrado —
  HU02.
- Redesenho visual da tela de registro além da remoção do bloco de
  endereço (só remove o que não deveria estar ali).

## 8. Definição de Pronto

- [ ] Registro aceita submissão sem endereço.
- [ ] Fluxo pós-login de captura de endereço funcional, reaproveitando
      `buscaViaCep`.
- [ ] `LocationContextService` implementado e persistindo em
      `localStorage`.
- [ ] Navbar exibe endereço ativo.
- [ ] `supplier-list` filtra por `cityId` do contexto; backend aplica
      RN05 server-side.
- [ ] Todos os testes da seção 6 passando (`mvn test` + `ng test`).
- [ ] Revisado e aprovado antes de HU02 começar.
