import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, RouterLink, provideRouter } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { MenuComponent } from './menu.component';
import { AuthService } from '../../services/auth.service';
import { LocationContextService } from '../../services/location-context.service';
import { CartService } from '../../services/cart.service';
import { UserService } from '../../services/user.service';
import { CartApiService } from '../../services/cart-api.service';
import { CartPreview } from '../../models/cart';
import { of } from 'rxjs';

@Component({ template: '' })
class TelaVazia {}

describe('MenuComponent', () => {
  let component: MenuComponent;
  let fixture: ComponentFixture<MenuComponent>;
  let locationContextService: LocationContextService;

  beforeEach(async () => {
    localStorage.clear();
    localStorage.setItem('user', JSON.stringify({ id: 'user-1', userType: 'CUSTOMER' }));

    await TestBed.configureTestingModule({
      imports: [MenuComponent],
      providers: [
        provideRouter([{ path: '**', component: TelaVazia }]),
        { provide: AuthService, useValue: jasmine.createSpyObj('AuthService', ['logout', 'isLogged']) },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']) },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
      ]
    })
    .compileComponents();

    locationContextService = TestBed.inject(LocationContextService);
    fixture = TestBed.createComponent(MenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveExibirEnderecoAtivoNaNavbar', () => {
    locationContextService.setAddress({ cityId: 1, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Rua XV, 100 · Pato Branco');
  });

  it('deveIrParaMeusEnderecosAoClicarNoEnderecoAtivo', () => {
    locationContextService.setAddress({ cityId: 1, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });
    fixture.detectChanges();
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    const botaoEndereco = Array.from(fixture.nativeElement.querySelectorAll('button') as HTMLButtonElement[])
      .find((b) => b.textContent?.includes('Rua XV, 100 · Pato Branco'));
    botaoEndereco?.click();

    expect(router.navigate).toHaveBeenCalledWith(['/meus-enderecos']);
  });

  it('deveExibirBottomTabBarParaCliente', () => {
    expect(fixture.nativeElement.querySelector('app-bottom-tab-bar')).toBeTruthy();
  });

  it('naoDeveExibirBottomTabBarParaFornecedor', () => {
    localStorage.setItem('user', JSON.stringify({ id: 'user-1', userType: 'SUPPLIER' }));
    fixture = TestBed.createComponent(MenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-bottom-tab-bar')).toBeNull();
  });

  it('deveOcultarContadorComCarrinhoVazio', () => {
    TestBed.inject(CartService).setCart([], null);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="cart-count"]')).toBeNull();
  });

  // HU20 Cenário 8: o contador soma as quantidades locais, sem ir ao servidor
  it('deveMostrarQuantidadeDeItensNoContador', () => {
    const previewSpy = spyOn(TestBed.inject(CartApiService), 'preview');
    TestBed.inject(CartService).setCart([{ productId: 1, quantity: 1 }, { productId: 2, quantity: 3 }], 'loja-a');
    fixture.detectChanges();

    const contadores = fixture.nativeElement.querySelectorAll('[data-testid="cart-count"]') as NodeListOf<HTMLElement>;
    expect(contadores.length).toBeGreaterThan(0);
    contadores.forEach((contador) => expect(contador.textContent?.trim()).toBe('4'));
    expect(previewSpy).not.toHaveBeenCalled();
  });

  // ---- HU20: o drawer mostra o que o backend devolve ----
  const previewCom = (items: CartPreview['items'], subtotal = 0): CartPreview =>
    ({ supplierId: 'loja-a', supplierName: 'Gás Centro', items, subtotal });

  const abrirCarrinho = () => {
    const toggle = fixture.nativeElement.querySelector('#cart-drawer') as HTMLInputElement;
    toggle.checked = true;
    toggle.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };

  it('deveBuscarOsItensAoAbrirOCarrinho', () => {
    const previewSpy = spyOn(TestBed.inject(CartApiService), 'preview').and.returnValue(of(previewCom([
      { productId: 1, brand: 'Ultragaz', type: 'Gás', category: 'Gás 13kg', quantity: 2, stockQuantity: 5,
        unitPrice: 110, totalPrice: 220, available: true },
    ], 220)));
    TestBed.inject(CartService).setCart([{ productId: 1, quantity: 2 }], 'loja-a');
    fixture.detectChanges();
    expect(previewSpy).not.toHaveBeenCalled();

    abrirCarrinho();

    expect(previewSpy).toHaveBeenCalledWith([{ productId: 1, quantity: 2 }], 'DELIVERY');
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="cart-item"]')?.textContent).toContain('Ultragaz');
    expect(el.querySelector('[data-testid="cart-item-quantity"]')?.textContent?.trim()).toBe('2');
    expect(el.querySelector('[data-testid="cart-total"]')?.textContent).toMatch(/220[.,]00/);
  });

  it('deveAtualizarOPreviewAoMudarAQuantidade', () => {
    const previewSpy = spyOn(TestBed.inject(CartApiService), 'preview').and.returnValue(of(previewCom([
      { productId: 1, brand: 'Ultragaz', quantity: 2, stockQuantity: 5, unitPrice: 110, totalPrice: 220, available: true },
    ], 220)));
    TestBed.inject(CartService).setCart([{ productId: 1, quantity: 2 }], 'loja-a');
    abrirCarrinho();

    (fixture.nativeElement.querySelector('[aria-label="Aumentar quantidade"]') as HTMLButtonElement).click();

    expect(TestBed.inject(CartService).getCart()).toEqual([{ productId: 1, quantity: 3 }]);
    expect(previewSpy).toHaveBeenCalledWith([{ productId: 1, quantity: 3 }], 'DELIVERY');
  });

  it('deveMostrarItemIndisponivelComMotivo', () => {
    spyOn(TestBed.inject(CartApiService), 'preview').and.returnValue(of(previewCom([
      { productId: 1, brand: 'Ultragaz', quantity: 1, stockQuantity: 0, unitPrice: 110, totalPrice: 110,
        available: false, unavailableReason: 'OUT_OF_STOCK' },
    ])));
    TestBed.inject(CartService).setCart([{ productId: 1, quantity: 1 }], 'loja-a');
    abrirCarrinho();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="cart-item-unavailable"]')?.textContent).toContain('Sem estoque suficiente');
    expect((el.querySelector('[data-testid="checkout-button"]') as HTMLButtonElement).disabled).toBeTrue();
    expect(el.querySelector('[data-testid="cart-blocked"]')).not.toBeNull();

    (el.querySelector('[data-testid="cart-item-remove"]') as HTMLButtonElement).click();
    expect(TestBed.inject(CartService).getCart()).toEqual([]);
  });

  it('deveExibirDashboardPedidosEProdutosParaFornecedor', () => {
    localStorage.setItem('user', JSON.stringify({ id: 'f1', name: 'Gás Centro', userType: 'SUPPLIER' }));
    fixture = TestBed.createComponent(MenuComponent);
    fixture.detectChanges();
    const nav = (fixture.nativeElement as HTMLElement).querySelector('nav') as HTMLElement;

    const hrefs = Array.from(nav.querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(hrefs).toContain('/minha-loja/painel');
    expect(hrefs).toContain('/minha-loja/pedidos');
    expect(nav.textContent).toContain('Dashboard');
    expect(nav.textContent).toContain('Pedidos');
    expect(nav.textContent).toContain('Produtos');
  });

  it('naoDeveExibirDashboardParaCliente', () => {
    const hrefs = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(hrefs).not.toContain('/minha-loja/painel');
  });

  it('naoDeveExibirAbasDoEntregadorParaCliente', () => {
    expect(fixture.nativeElement.querySelector('app-deliverer-tab-bar')).toBeNull();
  });

  it('deveMostrarSoAMarcaESairParaEntregador', () => {
    localStorage.setItem('user', JSON.stringify({ id: 'u9', name: 'Carlos', userType: 'DELIVERER' }));
    fixture = TestBed.createComponent(MenuComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('#cart-drawer')).toBeNull();
    expect(el.querySelector('nav:not([aria-label="Módulo do entregador"])')).toBeNull();
    expect(el.querySelector('app-bottom-tab-bar')).toBeNull();
    expect(el.querySelector('app-deliverer-tab-bar')).toBeTruthy();
    expect((el.querySelector('a[aria-label="Ir para a tela inicial"]') as HTMLAnchorElement).getAttribute('href')).toBe('/entregas');
  });

  // ---- F7 fatia 4: avatar e dropdown do usuário ----
  const entrarComo = (user: Record<string, unknown>) => {
    localStorage.setItem('user', JSON.stringify(user));
    fixture = TestBed.createComponent(MenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };
  const byId = (id: string) => (fixture.nativeElement as HTMLElement).querySelector(`[data-testid=${id}]`) as HTMLElement | null;
  const abrirMenu = () => {
    (byId('user-menu-button') as HTMLButtonElement).click();
    fixture.detectChanges();
  };
  const itens = () => Array.from((byId('user-menu') as HTMLElement).querySelectorAll('[role=menuitem] [data-menu-label]')).map((i) => i.textContent!.trim());

  it('deveExibirIniciaisDoUsuarioNoAvatar', () => {
    entrarComo({ id: 'u1', name: 'Maria Souza Lima', email: 'maria@h2gas.local', userType: 'CUSTOMER' });

    expect(byId('user-avatar')!.textContent!.trim()).toBe('ML');
    expect(byId('user-menu-button')!.querySelector('.fa-user')).toBeNull();
  });

  it('deveAtualizarAvatarQuandoOsDadosForemSalvos', () => {
    entrarComo({ id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' });
    expect(byId('user-avatar')!.textContent!.trim()).toBe('MS');

    TestBed.inject(UserService).setCurrentUser({ id: 'u1', name: 'Ana Paula', email: 'maria@h2gas.local', userType: 'CUSTOMER' } as any);
    fixture.detectChanges();

    expect(byId('user-avatar')!.textContent!.trim()).toBe('AP');
  });

  it('deveAbrirMenuDoUsuarioComNomeEmailEOpcoesDoPerfil', () => {
    entrarComo({ id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' });
    expect(byId('user-menu')).toBeNull();

    abrirMenu();

    const menu = byId('user-menu')!;
    expect(menu.textContent).toContain('Maria Souza');
    expect(menu.textContent).toContain('maria@h2gas.local');
    expect(menu.getAttribute('role')).toBe('menu');
    expect(byId('user-menu-button')!.getAttribute('aria-expanded')).toBe('true');
    expect(byId('user-menu-button')!.getAttribute('aria-haspopup')).toBe('menu');
    expect(itens()).toEqual(['Meus Dados', 'Meus Pedidos', 'Meus Endereços', 'Sair']);
  });

  it('deveMostrarAsOpcoesDoFornecedor', () => {
    entrarComo({ id: 'f1', name: 'Gás Centro', email: 'gas@h2gas.local', userType: 'SUPPLIER' });
    abrirMenu();

    expect(itens()).toEqual(['Minha loja', 'Dashboard', 'Pedidos recebidos', 'Configurações', 'Sair']);
  });

  it('deveMostrarMinhaLojaNoLugarDeMeusDadosParaFornecedor', () => {
    entrarComo({ id: 'f1', name: 'Gás Centro', email: 'gas@h2gas.local', userType: 'SUPPLIER' });
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    abrirMenu();

    expect(byId('menu-item-meus-dados')).toBeNull();
    (byId('menu-item-minha-loja') as HTMLButtonElement).click();

    expect(router.navigate).toHaveBeenCalledWith(['/minha-loja']);
  });

  it('deveMostrarAsOpcoesDoEntregador', () => {
    entrarComo({ id: 'd1', name: 'Carlos Silva', email: 'carlos@h2gas.local', userType: 'DELIVERER' });
    abrirMenu();

    expect(itens()).toEqual(['Meus Dados', 'Sair']);
  });

  it('deveLevarParaMeusDadosEFecharOMenu', () => {
    entrarComo({ id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' });
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');
    abrirMenu();

    (byId('menu-item-meus-dados') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(router.navigate).toHaveBeenCalledWith(['/meus-dados']);
    expect(byId('user-menu')).toBeNull();
  });

  it('deveFocarOPrimeiroItemAoAbrir', fakeAsync(() => {
    entrarComo({ id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' });

    abrirMenu();
    tick();

    expect(document.activeElement).toBe(byId('menu-item-meus-dados'));
  }));

  it('deveManterOFocoNoItemDepoisDeNovosCiclosDeDeteccao', fakeAsync(() => {
    entrarComo({ id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' });
    abrirMenu();
    tick();
    const primeiro = byId('menu-item-meus-dados');
    expect(document.activeElement).toBe(primeiro);

    fixture.detectChanges();
    fixture.detectChanges();

    // os itens não podem ser recriados a cada ciclo: o elemento focado precisa ser o mesmo
    expect(byId('menu-item-meus-dados')).toBe(primeiro);
    expect(document.activeElement).toBe(primeiro);
  }));

  it('deveFecharMenuComEsc', () => {
    entrarComo({ id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' });
    abrirMenu();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect(byId('user-menu')).toBeNull();
    expect(byId('user-menu-button')!.getAttribute('aria-expanded')).toBe('false');
  });

  it('deveFecharMenuAoClicarFora', () => {
    entrarComo({ id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' });
    abrirMenu();

    document.body.click();
    fixture.detectChanges();

    expect(byId('user-menu')).toBeNull();
  });

  it('deveManterOMenuAbertoAoClicarDentroDele', () => {
    entrarComo({ id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' });
    abrirMenu();

    (byId('user-menu')!.querySelector('[data-testid=user-menu-header]') as HTMLElement).click();
    fixture.detectChanges();

    expect(byId('user-menu')).toBeTruthy();
  });

  // ---- F7 fatia 5: navegação da navbar ----
  const perfis = {
    cliente: { id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' },
    fornecedor: { id: 'f1', name: 'Gás Centro', email: 'gas@h2gas.local', userType: 'SUPPLIER' },
    entregador: { id: 'd1', name: 'Carlos Silva', email: 'carlos@h2gas.local', userType: 'DELIVERER' },
    admin: { id: 'a1', name: 'Admin H2Gás', email: 'admin@h2gas.local', userType: 'ADMIN' },
  };
  const linkPara = (href: string) =>
    (fixture.nativeElement as HTMLElement).querySelector(`nav a[href="${href}"]`) as HTMLAnchorElement | null;
  const navegar = async (url: string) => {
    await TestBed.inject(Router).navigateByUrl(url);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  it('deveNavegarSemRecarregarOApp', async () => {
    for (const perfil of Object.values(perfis)) {
      entrarComo(perfil);
      (byId('mobile-menu-button') as HTMLButtonElement | null)?.click();
      fixture.detectChanges();

      const links = fixture.debugElement.queryAll(By.css('a'));
      expect(links.length).withContext(perfil.userType).toBeGreaterThan(0);
      links.forEach((link) =>
        expect(link.injector.get(RouterLink, null)).withContext(`${perfil.userType} ${link.nativeElement.getAttribute('href')}`).not.toBeNull());
    }

    entrarComo(perfis.admin);
    const clique = new MouseEvent('click', { bubbles: true, cancelable: true });
    linkPara('/admin/cidades')!.dispatchEvent(clique);
    await fixture.whenStable();

    expect(clique.defaultPrevented).toBeTrue();
    expect(TestBed.inject(Router).url).toBe('/admin/cidades');
  });

  it('deveDestacarOLinkAtivo', async () => {
    entrarComo(perfis.admin);

    await navegar('/admin/cidades');

    expect(linkPara('/admin/cidades')!.classList).toContain('bg-white/15');
    expect(linkPara('/admin/cidades')!.getAttribute('aria-current')).toBe('page');
    expect(linkPara('/admin')!.classList).not.toContain('bg-white/15');
    expect(linkPara('/admin')!.getAttribute('aria-current')).toBeNull();

    await navegar('/admin/usuarios');

    expect(linkPara('/admin/usuarios')!.getAttribute('aria-current')).toBe('page');
    expect(linkPara('/admin')!.getAttribute('aria-current')).withContext('Painel só ativo na própria rota').toBeNull();
  });

  it('deveLevarALogoParaOInicioDoPerfil', () => {
    const logo = () => (fixture.nativeElement as HTMLElement).querySelector('a[aria-label="Ir para a tela inicial"]')!.getAttribute('href');

    entrarComo(perfis.cliente);
    expect(logo()).toBe('/inicio');
    entrarComo(perfis.fornecedor);
    expect(logo()).toBe('/minha-loja');
    entrarComo(perfis.entregador);
    expect(logo()).toBe('/entregas');
    entrarComo(perfis.admin);
    expect(logo()).toBe('/admin');
  });

  it('deveMostrarPainelEUsuariosParaAdmin', () => {
    entrarComo(perfis.admin);
    (byId('mobile-menu-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const desktop = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('nav[aria-label="Navegação principal"] a'))
      .map((a) => a.getAttribute('href'));
    const mobile = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('#mobile-menu a'))
      .map((a) => a.getAttribute('href'));

    for (const [onde, hrefs] of [['desktop', desktop], ['mobile', mobile]] as const) {
      expect(hrefs).withContext(onde).toEqual(jasmine.arrayContaining(['/admin', '/admin/usuarios', '/admin/fornecedores', '/admin/cidades', '/admin/tipos', '/admin/categorias']));
      expect(hrefs).withContext(onde).not.toContain('/inicio');
      expect(hrefs).withContext(onde).not.toContain('/meus-pedidos');
    }
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Produtos');

    abrirMenu();
    expect(itens()).toEqual(['Meus Dados', 'Painel', 'Sair']);
  });

  // ---- HU19: categorias são do admin ----
  it('naoDeveOferecerCategoriasAoFornecedor', () => {
    entrarComo(perfis.fornecedor);
    (byId('mobile-menu-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const hrefs = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(hrefs).not.toContain('/category-products');
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Categoria de produtos');
  });

  it('naoDeveMostrarInicioDoCatalogoParaFornecedor', () => {
    entrarComo(perfis.fornecedor);
    (byId('mobile-menu-button') as HTMLButtonElement).click();
    fixture.detectChanges();

    const hrefs = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(hrefs.filter((h) => h === '/inicio')).toEqual([]);
  });

  it('deveMostrarCarrinhoSoParaCliente', () => {
    entrarComo(perfis.cliente);
    expect(fixture.nativeElement.querySelector('#cart-drawer')).withContext('cliente').toBeTruthy();

    for (const perfil of [perfis.fornecedor, perfis.entregador, perfis.admin]) {
      entrarComo(perfil);
      expect(fixture.nativeElement.querySelector('#cart-drawer')).withContext(perfil.userType).toBeNull();
    }
  });

  it('deveMostrarHamburguerSoParaFornecedorEAdmin', () => {
    for (const perfil of [perfis.fornecedor, perfis.admin]) {
      entrarComo(perfil);
      expect(byId('mobile-menu-button')).withContext(perfil.userType).toBeTruthy();
    }
    for (const perfil of [perfis.cliente, perfis.entregador]) {
      entrarComo(perfil);
      expect(byId('mobile-menu-button')).withContext(perfil.userType).toBeNull();
    }
  });

  it('deveUsarSoIconesMaterialNaNavbar', () => {
    for (const perfil of Object.values(perfis)) {
      entrarComo(perfil);
      (byId('mobile-menu-button') as HTMLButtonElement | null)?.click();
      fixture.detectChanges();
      expect((fixture.nativeElement as HTMLElement).querySelectorAll('[class*="fa-"]').length).withContext(perfil.userType).toBe(0);
    }
  });

  it('deveFecharMenuMobileAoNavegarEComEsc', async () => {
    entrarComo(perfis.fornecedor);
    const botao = () => byId('mobile-menu-button') as HTMLButtonElement;
    const painel = () => (fixture.nativeElement as HTMLElement).querySelector('#mobile-menu');
    expect(botao().getAttribute('aria-controls')).toBe('mobile-menu');
    expect(botao().getAttribute('aria-expanded')).toBe('false');

    botao().click();
    fixture.detectChanges();
    expect(painel()).toBeTruthy();
    expect(botao().getAttribute('aria-expanded')).toBe('true');

    await navegar('/minha-loja/pedidos');
    expect(painel()).withContext('ao navegar').toBeNull();

    botao().click();
    fixture.detectChanges();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    await fixture.whenStable();
    expect(painel()).withContext('com Esc').toBeNull();
    expect(botao().getAttribute('aria-expanded')).toBe('false');
  });

  // ---- F7 §5.22: barra clara para o cliente; os outros perfis mantêm a escura ----
  const barra = () => (fixture.nativeElement as HTMLElement).firstElementChild as HTMLElement;

  it('deveUsarBarraClaraParaCliente', () => {
    entrarComo(perfis.cliente);

    expect(barra().classList).toContain('bg-white');
    expect(barra().classList).not.toContain('bg-primary');
    expect(linkPara('/inicio')!.classList).toContain('text-gray-700');
    expect(linkPara('/inicio')!.classList).not.toContain('text-white');
    expect(fixture.nativeElement.querySelector('span.text-xl')!.classList).toContain('text-primary');
  });

  it('deveDestacarOLinkAtivoDoClienteEmTomClaro', async () => {
    entrarComo(perfis.cliente);
    await navegar('/inicio');

    expect(linkPara('/inicio')!.classList).toContain('bg-primary/10');
    expect(linkPara('/inicio')!.classList).toContain('text-primary');
    expect(linkPara('/inicio')!.getAttribute('aria-current')).toBe('page');
  });

  it('deveUsarAAcaoDeDestaqueNoFinalizarPedido', () => {
    TestBed.inject(CartService).setCart([{ productId: 1, quantity: 1 }], 'loja-a');
    entrarComo(perfis.cliente);
    fixture.detectChanges();

    const botao = (fixture.nativeElement as HTMLElement).querySelector('[data-testid="checkout-button"]') as HTMLElement;
    expect(botao.classList).toContain('btn-accent');
    expect(botao.classList).not.toContain('bg-primary');
  });

  it('deveManterBarraEscuraParaOsOutrosPerfis', () => {
    for (const perfil of [perfis.fornecedor, perfis.admin, perfis.entregador]) {
      entrarComo(perfil);

      expect(barra().classList).withContext(perfil.userType).toContain('bg-primary');
      expect(barra().classList).withContext(perfil.userType).not.toContain('bg-white');
    }
  });

  // ---- HU13: visitante (sem login) ----
  const comoVisitante = () => {
    localStorage.removeItem('user');
    fixture = TestBed.createComponent(MenuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  it('deveMostrarEntrarECarrinhoParaVisitante', () => {
    comoVisitante();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-testid="visitor-login"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="user-menu-button"]')).toBeNull();
    expect(el.querySelector('label[for="cart-drawer"]')).not.toBeNull();
    expect(el.querySelector('a[routerLink="/meus-pedidos"]')).toBeNull();
    expect(el.querySelector('app-bottom-tab-bar [data-testid="tab-entrar"]')).not.toBeNull();
    expect(el.querySelector('app-bottom-tab-bar [data-testid="tab-pedidos"]')).toBeNull();
  });

  it('deveMostrarACidadeDoVisitante', () => {
    locationContextService.setAddress({ cityId: 3, cityName: 'Curitiba', label: 'Batel · Curitiba' });
    comoVisitante();
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    const botao = Array.from(fixture.nativeElement.querySelectorAll('button') as HTMLButtonElement[])
      .find((b) => b.textContent?.includes('Batel · Curitiba'));
    botao!.click();

    expect((router.navigate as jasmine.Spy).calls.mostRecent().args[0]).toEqual(['/onde-voce-esta']);
  });

  it('deveLevarOVisitanteAoLoginVoltandoParaAPaginaAtual', () => {
    comoVisitante();
    const router = TestBed.inject(Router);
    spyOnProperty(router, 'url', 'get').and.returnValue('/lojas/loja-1');
    spyOn(router, 'navigate');

    (fixture.nativeElement.querySelector('[data-testid="visitor-login"]') as HTMLButtonElement).click();

    expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/lojas/loja-1' } });
  });

  // HU13 Cenário 12: a guarda descarta a sessão vencida; na navegação seguinte o menu já é de visitante
  it('deveVirarVisitanteQuandoASessaoSomeAoNavegar', async () => {
    expect(fixture.nativeElement.querySelector('[data-testid="user-menu-button"]')).not.toBeNull();

    localStorage.removeItem('user');
    await TestBed.inject(Router).navigateByUrl('/inicio');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="visitor-login"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="user-menu-button"]')).toBeNull();
  });
});
