import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { MenuComponent } from './menu.component';
import { AuthService } from '../../services/auth.service';
import { LocationContextService } from '../../services/location-context.service';
import { CartService } from '../../services/cart.service';

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
});
