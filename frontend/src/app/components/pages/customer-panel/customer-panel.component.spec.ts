import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { provideNgxMask } from 'ngx-mask';

import { CustomerPanelComponent } from './customer-panel.component';
import { UserService } from '../../../services/user.service';

describe('CustomerPanelComponent', () => {
  let component: CustomerPanelComponent;
  let fixture: ComponentFixture<CustomerPanelComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const igor = { id: 'user-1', name: 'Igor Emanuel', email: 'igor@h2gas.test', phoneNumber: '41999999999', userType: 'CUSTOMER' };
  const el = () => fixture.nativeElement as HTMLElement;
  const byId = (id: string) => el().querySelector(`[data-testid=${id}]`) as HTMLElement | null;
  const salvar = () => byId('save-profile') as HTMLButtonElement;

  const setup = async (user: Record<string, unknown> = igor, getMe$: any = of(user)) => {
    localStorage.clear();
    localStorage.setItem('user', JSON.stringify(user));
    userServiceSpy = jasmine.createSpyObj('UserService', ['getMe', 'getUserLocalStorage', 'updateUser', 'setCurrentUser', 'requestEmailChange', 'confirmEmailChange']);
    userServiceSpy.requestEmailChange.and.returnValue(of(void 0));
    userServiceSpy.confirmEmailChange.and.returnValue(of({ token: 'jwt-novo' }));
    userServiceSpy.getMe.and.returnValue(getMe$);
    userServiceSpy.getUserLocalStorage.and.returnValue(user);
    userServiceSpy.updateUser.and.returnValue(of({ ...user, name: 'Igor E. Souza' } as any));
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);

    await TestBed.configureTestingModule({
      imports: [CustomerPanelComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideNgxMask(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CustomerPanelComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  afterEach(() => fixture.destroy());

  it('should create', async () => {
    await setup();
    expect(component).toBeTruthy();
  });

  it('deveExibirSkeletonEnquantoCarrega', async () => {
    const resposta = new Subject<any>();
    await setup(igor, resposta);

    expect(byId('profile-skeleton')).toBeTruthy();
    expect(byId('save-profile')).toBeNull();

    resposta.next(igor);
    fixture.detectChanges();

    expect(byId('profile-skeleton')).toBeNull();
    expect(byId('save-profile')).toBeTruthy();
  });

  it('deveExibirAvatarComIniciaisENome', async () => {
    await setup();

    expect(byId('profile-avatar')!.textContent!.trim()).toBe('IE');
    expect(byId('profile-name')!.textContent).toContain('Igor Emanuel');
    expect(byId('profile-email')!.textContent).toContain('igor@h2gas.test');
  });

  it('deveUsarOUsuarioGuardadoSeABuscaFalhar', async () => {
    await setup(igor, throwError(() => ({ error: { message: 'fora do ar' } })));

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringMatching('fora do ar'));
    expect(byId('profile-name')!.textContent).toContain('Igor Emanuel');
    expect(byId('profile-skeleton')).toBeNull();
  });

  it('deveManterEmailSomenteLeitura', async () => {
    await setup();

    const email = byId('email-field') as HTMLInputElement;
    expect(email.readOnly).toBeTrue();
    expect(email.value).toBe('igor@h2gas.test');
    expect(el().textContent).toContain('fale com o suporte');
  });

  it('deveDesabilitarSalvarSemAlteracoes', async () => {
    await setup();

    expect(salvar().disabled).toBeTrue();
  });

  it('deveHabilitarSalvarAoAlterarNomeOuTelefone', async () => {
    await setup();

    component.profileForm.get('name')!.setValue('Igor E. Souza');
    fixture.detectChanges();
    expect(salvar().disabled).toBeFalse();

    component.profileForm.get('name')!.setValue('Igor Emanuel');
    fixture.detectChanges();
    expect(salvar().disabled).toBeTrue();

    component.profileForm.get('phoneNumber')!.setValue('41988887777');
    fixture.detectChanges();
    expect(salvar().disabled).toBeFalse();
  });

  it('naoDeveSalvarComNomeCurtoOuTelefoneVazio', async () => {
    await setup();
    component.profileForm.patchValue({ name: 'Ig', phoneNumber: '' });
    fixture.detectChanges();

    component.save();

    expect(userServiceSpy.updateUser).not.toHaveBeenCalled();
    expect(salvar().disabled).toBeTrue();
  });

  it('deveSalvarENotificarOMenuComSucesso', async () => {
    await setup();
    component.profileForm.get('name')!.setValue('Igor E. Souza');
    fixture.detectChanges();

    salvar().click();
    fixture.detectChanges();

    expect(userServiceSpy.updateUser).toHaveBeenCalledOnceWith('user-1', jasmine.objectContaining({
      name: 'Igor E. Souza', phoneNumber: '41999999999', email: 'igor@h2gas.test',
    }));
    expect(userServiceSpy.setCurrentUser).toHaveBeenCalledWith(jasmine.objectContaining({ name: 'Igor E. Souza' }));
    expect(toastrSpy.success).toHaveBeenCalledWith('Dados atualizados.');
    expect(byId('profile-avatar')!.textContent!.trim()).toBe('IS');
    expect(salvar().disabled).toBeTrue();
  });

  it('deveMostrarSpinnerEDesabilitarEnquantoSalva', async () => {
    await setup();
    const resposta = new Subject<any>();
    userServiceSpy.updateUser.and.returnValue(resposta as any);
    component.profileForm.get('name')!.setValue('Igor E. Souza');
    fixture.detectChanges();

    salvar().click();
    fixture.detectChanges();

    expect(salvar().disabled).toBeTrue();
    expect(salvar().querySelector('.loading')).toBeTruthy();
    expect(salvar().textContent).toContain('Salvando');

    resposta.next({ ...igor, name: 'Igor E. Souza' });
    fixture.detectChanges();
    expect(salvar().querySelector('.loading')).toBeNull();
  });

  it('deveMostrarAMensagemDoBackendNoErro', async () => {
    await setup();
    userServiceSpy.updateUser.and.returnValue(throwError(() => ({ error: { message: 'Você só pode editar os seus próprios dados.' } })));
    component.profileForm.get('name')!.setValue('Igor E. Souza');
    fixture.detectChanges();

    salvar().click();
    fixture.detectChanges();

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringMatching('seus próprios dados'));
    expect(userServiceSpy.setCurrentUser).not.toHaveBeenCalled();
    expect(salvar().querySelector('.loading')).toBeNull();
  });

  it('deveExibirAtalhosPorPerfil', async () => {
    await setup();
    expect(byId('atalho-meus-enderecos')).toBeTruthy();
    expect(byId('atalho-meus-pedidos')).toBeTruthy();
    expect(byId('atalho-configuracao')).toBeNull();
    fixture.destroy();
    TestBed.resetTestingModule();

    await setup({ ...igor, userType: 'DELIVERER' });
    expect(byId('atalho-meus-enderecos')).toBeNull();
    expect(byId('atalho-meus-pedidos')).toBeNull();
    expect(byId('atalho-configuracao')).toBeNull();
  });

  it('deveNavegarPelosAtalhos', async () => {
    await setup();

    (byId('atalho-meus-enderecos') as HTMLButtonElement).click();
    (byId('atalho-meus-pedidos') as HTMLButtonElement).click();

    expect(routerSpy.navigate).toHaveBeenCalledWith(['/meus-enderecos']);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/meus-pedidos']);
  });

  const admin = { id: 'admin-1', name: 'Admin H2Gás', email: 'admin@h2gas.local', phoneNumber: '41990000000', userType: 'ADMIN' };
  const digitar = (id: string, valor: string) => {
    const input = byId(id) as HTMLInputElement;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const clicar = (id: string) => { (byId(id) as HTMLButtonElement).click(); fixture.detectChanges(); };

  it('naoDeveMostrarAtalhosDeClienteParaAdmin', async () => {
    await setup(admin);

    expect(byId('atalho-meus-enderecos')).toBeNull();
    expect(byId('atalho-meus-pedidos')).toBeNull();
    expect(el().textContent).not.toContain('fale com o suporte');
    expect(byId('change-email')).toBeTruthy();
  });

  it('deveTrocarEmailDoAdminComCodigo', async () => {
    await setup(admin);

    clicar('change-email');
    digitar('new-email', 'novo.admin@h2gas.local');
    clicar('send-email-code');
    expect(userServiceSpy.requestEmailChange).toHaveBeenCalledWith('novo.admin@h2gas.local');
    expect(el().textContent).toContain('novo.admin@h2gas.local');

    digitar('email-code', '123456');
    clicar('confirm-email-code');

    expect(userServiceSpy.confirmEmailChange).toHaveBeenCalledWith('123456');
    expect(localStorage.getItem('access_token')).toBe('jwt-novo');
    expect(userServiceSpy.setCurrentUser).toHaveBeenCalledWith(jasmine.objectContaining({ email: 'novo.admin@h2gas.local' }));
    expect(byId('profile-email')!.textContent).toContain('novo.admin@h2gas.local');
    expect(byId('email-code')).toBeNull();
    expect(toastrSpy.success).toHaveBeenCalled();
  });

  it('deveMostrarErroDoCodigoSemTrocarOEmail', async () => {
    await setup(admin);
    userServiceSpy.confirmEmailChange.and.returnValue(throwError(() => ({ error: { status: 400, message: 'Código inválido.' } })));

    clicar('change-email');
    digitar('new-email', 'novo.admin@h2gas.local');
    clicar('send-email-code');
    digitar('email-code', '000000');
    clicar('confirm-email-code');

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Código inválido.'));
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(byId('profile-email')!.textContent).toContain('admin@h2gas.local');
    expect(byId('email-code')).toBeTruthy();
  });
});
