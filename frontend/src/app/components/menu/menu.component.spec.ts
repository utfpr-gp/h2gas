import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';

import { MenuComponent } from './menu.component';
import { AuthService } from '../../services/auth.service';
import { LocationContextService } from '../../services/location-context.service';

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
});
