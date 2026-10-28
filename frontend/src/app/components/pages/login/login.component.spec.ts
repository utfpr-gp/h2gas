import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { LoginComponent } from './login.component';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { CartService } from '../../../services/cart.service';
import { LocationContextService } from '../../../services/location-context.service';

describe('LoginComponent', () => {
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let queryParams: Record<string, string> = {};
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let locationContextService: LocationContextService;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  beforeEach(async () => {
    localStorage.clear();
    authServiceSpy = jasmine.createSpyObj('AuthService', ['loginUser']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: UserService, useValue: jasmine.createSpyObj('UserService', ['getMe']) },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ToastrService, useValue: toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error']) },
        { provide: CartService, useValue: cartServiceSpy = jasmine.createSpyObj('CartService', ['setCart']) },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']) },
        { provide: ActivatedRoute, useValue: { snapshot: { get queryParamMap() { return convertToParamMap(queryParams); } } } },
      ],
    })
    .compileComponents();

    locationContextService = TestBed.inject(LocationContextService);
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveLimparEnderecoAtivoDeOutroUsuarioAoIniciarLogin', () => {
    locationContextService.setAddress({ cityId: 1, cityName: 'Pato Branco', label: 'Rua XV, 100 · Pato Branco' });
    authServiceSpy.loginUser.and.returnValue({ subscribe: () => {} } as any);

    component.loginform.setValue({ email: 'novo@teste.com', password: '123456' });
    component.login();

    expect(locationContextService.current()).toBeNull();
    expect(localStorage.getItem('h2gas.activeAddress')).toBeNull();
  });

  // F7 §5.10 (revisão do autor): o login dá boas-vindas; erro mostra a mensagem do backend
  it('deveDarBoasVindasAoLogar', () => {
    authServiceSpy.loginUser.and.returnValue(of({ token: 'jwt' }) as any);
    const userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    userService.getMe.and.returnValue(of({ id: 'u2', name: 'Cliente Teste E2E', userType: 'CUSTOMER' } as any));

    component.loginform.setValue({ email: 'cliente@teste.com', password: '123456' });
    component.login();

    expect(toastrSpy.success).toHaveBeenCalledOnceWith('Bem-vindo(a), Cliente!');
  });

  it('deveMostrarMensagemDoBackendAoFalharLogin', () => {
    authServiceSpy.loginUser.and.returnValue(throwError(() => ({ status: 401, error: { status: 401, error: 'Unauthorized', message: 'E-mail ou senha inválidos.' } })) as any);

    component.loginform.setValue({ email: 'cliente@teste.com', password: 'errada' });
    component.login();

    expect(toastrSpy.error).toHaveBeenCalledOnceWith('Não foi possível entrar: E-mail ou senha inválidos.');
  });

  it('deveRedirecionarEntregadorParaEntregas', () => {
    const userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    userService.getMe.and.returnValue(of({ id: 'u1', userType: 'DELIVERER' } as any));
    const router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    component.getAndSaveMe();

    expect(router.navigate).toHaveBeenCalledWith(['/entregas']);
  });

  it('deveRedirecionarFornecedorParaDashboard', () => {
    const userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    userService.getMe.and.returnValue(of({ id: 'u3', userType: 'SUPPLIER' } as any));
    const router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    component.getAndSaveMe();

    expect(router.navigate).toHaveBeenCalledWith(['/minha-loja/painel']);
  });

  it('deveManterInicioParaOsDemaisPerfis', () => {
    const userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    userService.getMe.and.returnValue(of({ id: 'u2', userType: 'CUSTOMER' } as any));
    const router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    component.getAndSaveMe();

    expect(router.navigate).toHaveBeenCalledWith(['/inicio']);
  });

  it('deveLevarParaOLoginPorCodigo', () => {
    const router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    (fixture.nativeElement.querySelector('[data-testid=code-login]') as HTMLButtonElement).click();

    expect(router.navigate).toHaveBeenCalledWith(['/login/token']);
  });

  it('naoDeveTerDoctypeNemHexLiteralNoTemplate', () => {
    const html = (fixture.nativeElement as HTMLElement).innerHTML;

    expect(html).not.toContain('<body');
    expect(html).not.toMatch(/#0E2068|#3B82F6/i);
  });

  // HU13: entrar não apaga o carrinho montado como visitante
  it('deveManterOCarrinhoAoEntrar', () => {
    authServiceSpy.loginUser.and.returnValue(of({ token: 't' }) as any);
    (TestBed.inject(UserService) as jasmine.SpyObj<UserService>).getMe.and.returnValue(of({ id: 'u1', userType: 'CUSTOMER' }) as any);
    component.loginform.setValue({ email: 'cliente@h2gas.local', password: 'Teste@123' } as any);

    component.login();

    expect(cartServiceSpy.setCart).not.toHaveBeenCalled();
  });

  // ---- HU13 ----
  it('deveVoltarAoReturnUrlInterno', () => {
    queryParams = { returnUrl: '/resumo-pedido' };
    authServiceSpy.loginUser.and.returnValue(of({ token: 't' }) as any);
    (TestBed.inject(UserService) as jasmine.SpyObj<UserService>).getMe.and.returnValue(of({ id: 'u1', userType: 'CUSTOMER' }) as any);
    component.loginform.setValue({ email: 'cliente@h2gas.local', password: 'Teste@123' } as any);

    component.login();

    expect((TestBed.inject(Router) as jasmine.SpyObj<Router>).navigateByUrl).toHaveBeenCalledWith('/resumo-pedido');
    queryParams = {};
  });

  it('deveAvisarQueOLoginEParaFinalizarACompra', () => {
    queryParams = { returnUrl: '/resumo-pedido' };
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="checkout-login-hint"]').textContent).toContain('Entre para finalizar sua compra');
    queryParams = {};
  });
});
