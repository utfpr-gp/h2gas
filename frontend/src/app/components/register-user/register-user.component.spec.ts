import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { provideNgxMask } from 'ngx-mask';

import { RegisterUserComponent } from './register-user.component';
import { UserService } from '../../services/user.service';

describe('RegisterUserComponent', () => {
  let queryParams: Record<string, string> = {};
  let component: RegisterUserComponent;
  let fixture: ComponentFixture<RegisterUserComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let navigateSpy: jasmine.Spy;

  const el = () => fixture.nativeElement as HTMLElement;
  const byId = (id: string) => el().querySelector(`[data-testid=${id}]`) as HTMLElement | null;

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['createUser']);
    userServiceSpy.createUser.and.returnValue(of({} as any));

    await TestBed.configureTestingModule({
      imports: [RegisterUserComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']) },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { get queryParamMap() { return convertToParamMap(queryParams); } } } },
        provideNgxMask(),
      ],
    }).compileComponents();

    navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(RegisterUserComponent);
    component = fixture.componentInstance;
    component.userType = 'CUSTOMER';
    component.tittleRegister = 'Você esta a um passo de se tornar nosso cliente';
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('deveAlternarEntreClienteEFornecedorNoSeletor', () => {
    expect(el().querySelector('input[formControlName=cnpj]')).toBeNull();

    (byId('profile-supplier') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(component.userType).toBe('SUPPLIER');
    expect(el().querySelector('input[formControlName=cnpj]')).toBeTruthy();

    (byId('profile-customer') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(component.userType).toBe('CUSTOMER');
    expect(el().querySelector('input[formControlName=cnpj]')).toBeNull();
  });

  it('deveEnviarOPerfilEscolhidoNoCadastro', () => {
    (byId('profile-supplier') as HTMLButtonElement).click();
    fixture.detectChanges();
    component.registerForm.patchValue({
      name: 'Loja Teste', email: 'loja@h2gas.local', phoneNumber: '41999999999',
      cnpj: '12345678000100', password: 'Teste@123',
    });
    component.confirmPassword = 'Teste@123';

    component.register();

    expect(userServiceSpy.createUser).toHaveBeenCalledWith(jasmine.objectContaining({ userType: 'SUPPLIER' }));
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });

  it('naoDeveTerDoctypeNemHexLiteralNoTemplate', () => {
    const html = el().innerHTML;

    expect(html).not.toContain('<body');
    expect(html).not.toMatch(/#0E2068|#3B82F6/i);
  });

  it('deveTrocarOTituloAoEscolherOPerfil', () => {
    expect(el().querySelector('h1')!.textContent).toContain('cliente');

    (byId('profile-supplier') as HTMLButtonElement).click();
    fixture.detectChanges();

    const titulo = el().querySelector('h1')!.textContent!;
    expect(titulo).toContain('fornecedor');
    expect(titulo).not.toContain('nosso cliente');
  });

  // HU13: quem veio do checkout se cadastra e segue para o login levando o retorno
  it('deveManterOReturnUrlAposOCadastro', () => {
    queryParams = { returnUrl: '/resumo-pedido' };
    component.registerForm.patchValue({ name: 'Maria', email: 'maria@h2gas.local', phoneNumber: '41999999999', password: 'Teste@123' });
    component.confirmPassword = 'Teste@123';

    component.register();

    expect(navigateSpy).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/resumo-pedido' } });
    queryParams = {};
  });
});
