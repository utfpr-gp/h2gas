import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { MenuComponent } from './menu.component';
import { AuthService } from '../../services/auth.service';
import { LocationContextService } from '../../services/location-context.service';
import { CartService } from '../../services/cart.service';
import { UserService } from '../../services/user.service';

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
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
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
    const router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

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
    TestBed.inject(CartService).setCart([]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="cart-count"]')).toBeNull();
  });

  it('deveMostrarQuantidadeDeItensNoContador', () => {
    TestBed.inject(CartService).setCart([{ id: 1, quantitySelect: 1 }, { id: 2, quantitySelect: 3 }]);
    fixture.detectChanges();

    const contadores = fixture.nativeElement.querySelectorAll('[data-testid="cart-count"]') as NodeListOf<HTMLElement>;
    expect(contadores.length).toBeGreaterThan(0);
    contadores.forEach((contador) => expect(contador.textContent?.trim()).toBe('2'));
  });

  it('deveExibirDashboardPedidosEProdutosParaFornecedor', () => {
    localStorage.setItem('user', JSON.stringify({ id: 'f1', name: 'Gás Centro', userType: 'SUPPLIER' }));
    fixture = TestBed.createComponent(MenuComponent);
    fixture.detectChanges();
    const nav = (fixture.nativeElement as HTMLElement).querySelector('nav') as HTMLElement;

    const hrefs = Array.from(nav.querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(hrefs).toContain('/dashboard');
    expect(hrefs).toContain('/dashboard-fornecedor');
    expect(nav.textContent).toContain('Dashboard');
    expect(nav.textContent).toContain('Pedidos');
    expect(nav.textContent).toContain('Produtos');
  });

  it('naoDeveExibirDashboardParaCliente', () => {
    const hrefs = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(hrefs).not.toContain('/dashboard');
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

    expect(itens()).toEqual(['Meus Dados', 'Dashboard', 'Pedidos recebidos', 'Configurações', 'Sair']);
  });

  it('deveMostrarAsOpcoesDoEntregador', () => {
    entrarComo({ id: 'd1', name: 'Carlos Silva', email: 'carlos@h2gas.local', userType: 'DELIVERER' });
    abrirMenu();

    expect(itens()).toEqual(['Meus Dados', 'Sair']);
  });

  it('deveLevarParaMeusDadosEFecharOMenu', () => {
    entrarComo({ id: 'u1', name: 'Maria Souza', email: 'maria@h2gas.local', userType: 'CUSTOMER' });
    const router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
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
});
