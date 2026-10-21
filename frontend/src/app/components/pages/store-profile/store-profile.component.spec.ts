import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideNgxMask } from 'ngx-mask';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { StoreProfileComponent } from './store-profile.component';
import { SupplierService } from '../../../services/supplier.service';
import { AddressService } from '../../../services/address.service';
import { CityService } from '../../../services/city.service';
import { UserService } from '../../../services/user.service';
import { SupplierProfile } from '../../../models/supplier-profile';

describe('StoreProfileComponent', () => {
  let fixture: ComponentFixture<StoreProfileComponent>;
  let supplierSpy: jasmine.SpyObj<SupplierService>;
  let addressSpy: jasmine.SpyObj<AddressService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const perfil: SupplierProfile = {
    name: 'Gás Centro Curitiba', phone: '41991110000', description: 'Desde 1990', cnpj: '12345678000101', logoUrl: null,
    addressId: 3, addressStreetName: 'Rua XV de Novembro', addressNumber: '700', addressNeighborhood: 'Centro',
    cityName: 'Curitiba', cityState: 'PR',
  };

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const click = (id: string) => { byId(id)!.click(); fixture.detectChanges(); };
  const preencher = (id: string, valor: string) => {
    const el = byId(id) as HTMLInputElement;
    el.value = valor;
    el.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  };
  const escolherArquivo = (arquivo: File) => {
    const input = byId('logo-input') as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: [arquivo], configurable: true });
    input.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  };
  const salvar = () => byId('save-profile') as HTMLButtonElement;

  const setup = async (p: SupplierProfile = perfil) => {
    supplierSpy = jasmine.createSpyObj('SupplierService', ['getMyProfile', 'updateMyProfile', 'uploadMyLogo', 'removeMyLogo']);
    supplierSpy.getMyProfile.and.returnValue(of({ ...p }));
    supplierSpy.updateMyProfile.and.callFake((r) => of({ ...p, ...r }));
    supplierSpy.uploadMyLogo.and.returnValue(of({ logoUrl: 'https://cdn/logo.png' }));
    supplierSpy.removeMyLogo.and.returnValue(of(void 0));
    addressSpy = jasmine.createSpyObj('AddressService', ['findByUserId', 'buscaViaCep', 'update']);
    addressSpy.findByUserId.and.returnValue(of({ id: 3, zipCode: '80020000', complement: 'Loja 2' }));
    const citySpy = jasmine.createSpyObj('CityService', ['findAll']);
    citySpy.findAll.and.returnValue(of([{ id: 1, name: 'Curitiba', state: 'PR', ibgeCode: 4106902 }]));
    const userSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userSpy.getUserLocalStorage.and.returnValue({ id: 'forn-1', supplierId: 'forn-1', userType: 'SUPPLIER' });
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error', 'warning']);

    await TestBed.configureTestingModule({
      imports: [StoreProfileComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        provideNgxMask(),
        { provide: SupplierService, useValue: supplierSpy },
        { provide: AddressService, useValue: addressSpy },
        { provide: CityService, useValue: citySpy },
        { provide: UserService, useValue: userSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(StoreProfileComponent);
    fixture.detectChanges();
  };

  it('deveCarregarPerfilDaLoja', async () => {
    await setup();

    expect((byId('store-name') as HTMLInputElement).value).toBe('Gás Centro Curitiba');
    expect((byId('store-description') as HTMLTextAreaElement).value).toBe('Desde 1990');
    expect(byId('store-cnpj')?.textContent).toContain('12.345.678/0001-01');
    expect(byId('store-address')?.textContent).toContain('Rua XV de Novembro, 700 · Centro · Curitiba - PR');
    expect(byId('hours-link')?.getAttribute('href')).toBe('/configuracao');
  });

  it('deveSalvarSoNomeTelefoneEDescricao', async () => {
    await setup();
    preencher('store-name', 'Gás Centro');

    salvar().click();
    fixture.detectChanges();

    expect(supplierSpy.updateMyProfile).toHaveBeenCalledOnceWith({ name: 'Gás Centro', phone: '41991110000', description: 'Desde 1990' });
    expect(toastrSpy.success).toHaveBeenCalledOnceWith('Dados da loja salvos.');
    expect(salvar().disabled).toBeTrue();
  });

  it('deveDesabilitarSalvarSemAlteracao', async () => {
    await setup();
    expect(salvar().disabled).toBeTrue();

    preencher('store-name', '   ');
    expect(salvar().disabled).toBeTrue();
    expect(byId('error-name')?.textContent).toContain('Informe o nome da loja.');

    preencher('store-name', 'Gás Centro Curitiba');
    expect(salvar().disabled).toBeTrue();
  });

  it('deveEnviarLogoEscolhida', async () => {
    await setup();

    escolherArquivo(new File([new Uint8Array(2048)], 'logo.png', { type: 'image/png' }));

    expect(supplierSpy.uploadMyLogo).toHaveBeenCalled();
    expect(toastrSpy.success).toHaveBeenCalledWith('Logo atualizada.');
    expect((byId('logo-image') as HTMLImageElement).src).toBe('https://cdn/logo.png');
  });

  it('deveRecusarArquivoGrandeAntesDeEnviar', async () => {
    await setup();

    escolherArquivo(new File([new Uint8Array(1_048_577)], 'logo.jpg', { type: 'image/jpeg' }));
    escolherArquivo(new File(['oi'], 'logo.gif', { type: 'image/gif' }));

    expect(supplierSpy.uploadMyLogo).not.toHaveBeenCalled();
    expect(byId('logo-error')?.textContent).toContain('Envie uma imagem JPG, PNG ou WEBP de até 1 MB.');
  });

  it('deveRemoverLogoComConfirmacao', async () => {
    await setup({ ...perfil, logoUrl: 'https://cdn/antiga.png' });

    click('remove-logo');
    expect(supplierSpy.removeMyLogo).not.toHaveBeenCalled();
    click('confirm-remove-logo');

    expect(supplierSpy.removeMyLogo).toHaveBeenCalled();
    expect(byId('logo-image')).toBeNull();
    expect(byId('logo-initials')?.textContent).toContain('GC');
  });

  it('deveMostrarIniciaisSemLogo', async () => {
    await setup();

    expect(byId('logo-image')).toBeNull();
    expect(byId('logo-initials')?.textContent).toContain('GC');
    expect(byId('remove-logo')).toBeNull();
  });

  it('deveMostrarMensagemDoBackendAoFalharOEnvio', async () => {
    await setup();
    supplierSpy.uploadMyLogo.and.returnValue(throwError(() => ({ status: 502, error: { message: 'Não foi possível enviar a imagem. Tente novamente.' } })));

    escolherArquivo(new File([new Uint8Array(10)], 'logo.png', { type: 'image/png' }));

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Não foi possível enviar a imagem'));
    expect(byId('logo-image')).toBeNull();
  });

  it('deveVoltarParaAsIniciaisSeALogoNaoCarregar', async () => {
    await setup({ ...perfil, logoUrl: 'https://cdn/quebrada.png' });

    byId('logo-image')!.dispatchEvent(new Event('error'));
    fixture.detectChanges();

    expect(byId('logo-image')).toBeNull();
    expect(byId('logo-initials')?.textContent).toContain('GC');
  });
});
