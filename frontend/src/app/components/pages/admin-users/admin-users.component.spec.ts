import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { AdminUsersComponent } from './admin-users.component';
import { AdminService } from '../../../services/admin.service';
import { CityService } from '../../../services/city.service';
import { UserService } from '../../../services/user.service';
import { AdminUser } from '../../../models/admin';
import { PaginatedResponse } from '../../../models/paginated-response';

describe('AdminUsersComponent', () => {
  let fixture: ComponentFixture<AdminUsersComponent>;
  let adminSpy: jasmine.SpyObj<AdminService>;
  let userSpy: jasmine.SpyObj<UserService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const maria: AdminUser = { id: 'u-1', name: 'Maria Souza', email: 'maria@h2gas.local', phoneNumber: '46999990000', userType: 'CUSTOMER', enabled: true };
  const eu: AdminUser = { id: 'admin-1', name: 'Igor Admin', email: 'igor@h2gas.local', userType: 'ADMIN', enabled: true };
  const gas: AdminUser = { id: 's-1', name: 'Gás Centro', email: 'gas@h2gas.local', userType: 'SUPPLIER', enabled: true };

  const pagina = (data: AdminUser[]): PaginatedResponse<AdminUser> =>
    ({ data, page: 0, size: 10, totalElements: data.length, totalPages: 1, hasNext: false, hasPrevious: false });

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const click = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };
  const preencher = (id: string, valor: string) => {
    const el = byId(id) as HTMLInputElement | HTMLSelectElement;
    el.value = valor;
    el.dispatchEvent(new Event(el instanceof HTMLSelectElement ? 'change' : 'input'));
    fixture.detectChanges();
  };

  const setup = async (queryParams: Record<string, string> = {}) => {
    adminSpy = jasmine.createSpyObj('AdminService', ['listUsers', 'createUser', 'setEnabled']);
    adminSpy.listUsers.and.callFake((f) => of(pagina(f.type === 'SUPPLIER' ? [gas] : [maria, eu])));
    adminSpy.setEnabled.and.returnValue(of(void 0));
    userSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage', 'updateUser']);
    userSpy.getUserLocalStorage.and.returnValue({ id: 'admin-1', userType: 'ADMIN' });
    const citySpy = jasmine.createSpyObj('CityService', ['findAll']);
    citySpy.findAll.and.returnValue(of([{ id: 1, name: 'Pato Branco', state: 'PR' }]));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [AdminUsersComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } } },
        { provide: AdminService, useValue: adminSpy },
        { provide: UserService, useValue: userSpy },
        { provide: CityService, useValue: citySpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUsersComponent);
    fixture.detectChanges();
  };

  const preencherBasico = (nome: string, email: string) => {
    preencher('new-name', nome);
    preencher('new-email', email);
    preencher('new-password', 'senha123');
    preencher('new-phone', '46999990000');
  };

  it('deveFiltrarPorPerfil', async () => {
    await setup();
    expect(adminSpy.listUsers).toHaveBeenCalledWith(jasmine.objectContaining({ type: undefined, page: 0 }));

    click('chip-SUPPLIER');

    expect(adminSpy.listUsers).toHaveBeenCalledWith(jasmine.objectContaining({ type: 'SUPPLIER', page: 0 }));
    expect(byId('user-s-1')?.textContent).toContain('Gás Centro');
    expect(byId('chip-SUPPLIER')?.getAttribute('aria-pressed')).toBe('true');
  });

  it('deveAbrirJaFiltradoPeloPerfilDaUrl', async () => {
    await setup({ perfil: 'SUPPLIER' });

    expect(adminSpy.listUsers).toHaveBeenCalledWith(jasmine.objectContaining({ type: 'SUPPLIER' }));
  });

  it('deveBuscarPorNomeOuEmail', fakeAsync(async () => {
    await setup();
    adminSpy.listUsers.calls.reset();

    preencher('user-search', 'souza');
    tick(300);

    expect(adminSpy.listUsers).toHaveBeenCalledOnceWith(jasmine.objectContaining({ q: 'souza', page: 0 }));
  }));

  it('deveDesativarComConfirmacao', async () => {
    await setup();

    click('toggle-u-1');
    expect(byId('toggle-modal')).not.toBeNull();
    expect(adminSpy.setEnabled).not.toHaveBeenCalled();

    click('confirm-toggle');

    expect(adminSpy.setEnabled).toHaveBeenCalledWith('u-1', false);
    expect(byId('status-u-1')?.textContent).toContain('Desativado');
    expect(byId('toggle-modal')).toBeNull();
  });

  it('naoDeveOferecerDesativarAPropriaConta', async () => {
    await setup();

    expect(byId('user-admin-1')).not.toBeNull();
    expect(byId('toggle-admin-1')).toBeNull();
  });

  it('deveExigirFornecedorParaEntregador', async () => {
    await setup();
    adminSpy.createUser.and.returnValue(of({ ...maria, id: 'd-1', userType: 'DELIVERER' }));
    click('new-user');

    preencher('new-type', 'DELIVERER');
    preencherBasico('Carlos', 'carlos@h2gas.local');
    click('save-new-user');

    expect(adminSpy.createUser).not.toHaveBeenCalled();
    expect(byId('new-supplier-error')).not.toBeNull();
    expect(byId('new-cnpj')).toBeNull();

    preencher('new-supplier', 's-1');
    click('save-new-user');

    expect(adminSpy.createUser).toHaveBeenCalledWith(jasmine.objectContaining({ userType: 'DELIVERER', supplierId: 's-1' }));
  });

  it('deveCriarAdmin', async () => {
    await setup();
    adminSpy.createUser.and.returnValue(of({ id: 'a-2', name: 'Ana', email: 'ana@h2gas.local', userType: 'ADMIN', enabled: true }));
    click('new-user');
    adminSpy.listUsers.calls.reset();

    preencher('new-type', 'ADMIN');
    preencherBasico('Ana', 'ana@h2gas.local');
    expect(byId('new-supplier')).toBeNull();
    click('save-new-user');

    const enviado = adminSpy.createUser.calls.mostRecent().args[0];
    expect(enviado).toEqual(jasmine.objectContaining({ userType: 'ADMIN', name: 'Ana', email: 'ana@h2gas.local' }));
    expect(enviado.supplierId).toBeUndefined();
    expect(enviado.cnpj).toBeUndefined();
    expect(toastrSpy.success).toHaveBeenCalled();
    expect(adminSpy.listUsers).toHaveBeenCalled();
    expect(byId('new-user-modal')).toBeNull();
  });

  it('deveEditarUsuario', async () => {
    await setup();
    userSpy.updateUser.and.returnValue(of({ ...maria, name: 'Maria S. Lima' }));

    click('edit-u-1');
    preencher('edit-name', 'Maria S. Lima');
    click('save-edit');

    expect(userSpy.updateUser).toHaveBeenCalledWith('u-1', jasmine.objectContaining({
      name: 'Maria S. Lima', email: 'maria@h2gas.local', phoneNumber: '46999990000',
    }));
    expect(byId('user-u-1')?.textContent).toContain('Maria S. Lima');
  });

  it('deveDeixarOProprioEmailSomenteLeituraNaEdicao', async () => {
    await setup();

    click('edit-admin-1');
    expect((byId('edit-email') as HTMLInputElement).readOnly).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Troque o seu e-mail em Meus Dados.');
    click('cancel-edit');
    expect(byId('edit-modal')).toBeNull();

    click('edit-u-1');
    expect((byId('edit-email') as HTMLInputElement).readOnly).toBeFalse();
  });
});
