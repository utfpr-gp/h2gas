import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { LoginTokenComponent } from './login-token.component';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';
import { CartService } from '../../../services/cart.service';
import { LocationContextService } from '../../../services/location-context.service';

describe('LoginTokenComponent', () => {
  let cartServiceSpy: jasmine.SpyObj<CartService>;
  let component: LoginTokenComponent;
  let fixture: ComponentFixture<LoginTokenComponent>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;
  let navigateSpy: jasmine.Spy;
  let queryParams: Record<string, string> = {};

  const el = () => fixture.nativeElement as HTMLElement;
  const byId = (id: string) => el().querySelector(`[data-testid=${id}]`) as HTMLElement | null;

  const digitarCodigo = (code: string) => {
    component.codeForm.setValue({ code });
    fixture.detectChanges();
  };

  const enviarEmail = (email = 'cliente@h2gas.local') => {
    component.emailForm.setValue({ email });
    (byId('send-code') as HTMLButtonElement).click();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    localStorage.clear();
    queryParams = {};
    authServiceSpy = jasmine.createSpyObj('AuthService', ['requestLoginToken', 'validateLoginToken']);
    authServiceSpy.requestLoginToken.and.returnValue(of(undefined as void));
    authServiceSpy.validateLoginToken.and.returnValue(of({ token: 'jwt-do-codigo' }));
    userServiceSpy = jasmine.createSpyObj('UserService', ['getMe']);
    userServiceSpy.getMe.and.returnValue(of({ id: 'u1', name: 'Maria Souza', userType: 'CUSTOMER' } as any));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']);

    await TestBed.configureTestingModule({
      imports: [LoginTokenComponent],
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: CartService, useValue: cartServiceSpy = jasmine.createSpyObj('CartService', ['setCart']) },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { get queryParamMap() { return convertToParamMap(queryParams); } } } },
      ],
    }).compileComponents();

    navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(LoginTokenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveManterOCanalCelularDesabilitado', () => {
    const celular = byId('channel-sms') as HTMLButtonElement;
    expect(celular.disabled).toBeTrue();
    expect(celular.textContent).toContain('Em breve');
    expect((byId('channel-email') as HTMLButtonElement).disabled).toBeFalse();
  });

  it('deveAvancarParaEtapaDeCodigoAposEnviarEmail', () => {
    expect(byId('code-input')).toBeNull();

    enviarEmail();

    expect(authServiceSpy.requestLoginToken).toHaveBeenCalledOnceWith('cliente@h2gas.local');
    expect(byId('code-input')).toBeTruthy();
    expect(el().textContent).toContain('cliente@h2gas.local');
  });

  it('naoDeveEnviarComEmailInvalido', () => {
    component.emailForm.setValue({ email: 'sem-arroba' });
    (byId('send-code') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(authServiceSpy.requestLoginToken).not.toHaveBeenCalled();
    expect(byId('code-input')).toBeNull();
  });

  it('deveAvancarMesmoQuandoOEmailNaoExiste', () => {
    authServiceSpy.requestLoginToken.and.returnValue(throwError(() => ({ error: { message: 'erro' } })));

    enviarEmail('nao.existe@h2gas.local');

    expect(byId('code-input')).toBeTruthy();
  });

  it('deveExigirSeisDigitosParaValidar', () => {
    enviarEmail();

    digitarCodigo('123');
    (byId('validate-code') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(authServiceSpy.validateLoginToken).not.toHaveBeenCalled();
    expect((byId('validate-code') as HTMLButtonElement).disabled).toBeTrue();
  });

  it('deveLogarEIrParaODestinoDoPerfilAoValidarCodigo', () => {
    enviarEmail();
    digitarCodigo('123456');

    (byId('validate-code') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(authServiceSpy.validateLoginToken).toHaveBeenCalledOnceWith('cliente@h2gas.local', '123456');
    expect(localStorage.getItem('access_token')).toBe('jwt-do-codigo');
    expect(userServiceSpy.getMe).toHaveBeenCalled();
    expect(navigateSpy).toHaveBeenCalledWith(['/inicio']);
  });

  it('deveLevarEntregadorParaEntregas', () => {
    userServiceSpy.getMe.and.returnValue(of({ id: 'u9', name: 'Carlos', userType: 'DELIVERER' } as any));
    enviarEmail();
    digitarCodigo('123456');

    (byId('validate-code') as HTMLButtonElement).click();

    expect(navigateSpy).toHaveBeenCalledWith(['/entregas']);
  });

  it('deveExibirMensagemDoBackendParaCodigoInvalido', () => {
    authServiceSpy.validateLoginToken.and.returnValue(throwError(() => ({ error: { error: 'Código inválido.' } })));
    enviarEmail();
    digitarCodigo('000000');

    (byId('validate-code') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringMatching('Código inválido.'));
    expect(byId('code-input')).toBeTruthy();
    expect(localStorage.getItem('access_token')).toBeNull();
  });

  it('devePermitirReenviarOCodigoETrocarDeEmail', () => {
    enviarEmail();

    (byId('resend-code') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(authServiceSpy.requestLoginToken).toHaveBeenCalledTimes(2);
    expect(toastrSpy.success).toHaveBeenCalled();

    (byId('change-email') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(byId('code-input')).toBeNull();
    expect(byId('send-code')).toBeTruthy();
  });

  it('deveLevarParaOLoginPorSenhaEParaOCadastro', () => {
    (byId('password-login') as HTMLButtonElement).click();
    expect(navigateSpy).toHaveBeenCalledWith(['/login/senha']);

    (byId('go-register') as HTMLButtonElement).click();
    expect(navigateSpy).toHaveBeenCalledWith(['/register']);
  });

  // HU13: entrar não apaga o carrinho montado como visitante
  it('deveManterOCarrinhoAoEntrar', () => {
    enviarEmail();
    digitarCodigo('123456');

    (byId('validate-code') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(cartServiceSpy.setCart).not.toHaveBeenCalled();
  });

  // ---- HU13: voltar para onde estava ----
  it('deveVoltarAoReturnUrlInterno', () => {
    const byUrl = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
    queryParams = { returnUrl: '/resumo-pedido?modo=compra-direta' };
    enviarEmail();
    digitarCodigo('123456');

    (byId('validate-code') as HTMLButtonElement).click();

    expect(byUrl).toHaveBeenCalledWith('/resumo-pedido?modo=compra-direta');
    expect(navigateSpy).not.toHaveBeenCalledWith(['/inicio']);
  });

  it('deveIgnorarReturnUrlExterno', () => {
    queryParams = { returnUrl: 'https://evil.com' };
    enviarEmail();
    digitarCodigo('123456');

    (byId('validate-code') as HTMLButtonElement).click();

    expect(navigateSpy).toHaveBeenCalledWith(['/inicio']);
  });

  it('deveAvisarQueOLoginEParaFinalizarACompra', () => {
    queryParams = { returnUrl: '/resumo-pedido' };
    fixture.detectChanges();

    expect(byId('checkout-login-hint')!.textContent).toContain('Entre para finalizar sua compra');
  });

  it('deveLevarOReturnUrlAoCadastro', () => {
    queryParams = { returnUrl: '/resumo-pedido' };

    (byId('go-register') as HTMLButtonElement).click();

    expect(navigateSpy).toHaveBeenCalledWith(['/register'], { queryParams: { returnUrl: '/resumo-pedido' } });
  });
});
