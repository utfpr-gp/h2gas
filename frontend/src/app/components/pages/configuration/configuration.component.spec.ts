import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { ConfigurationComponent } from './configuration.component';
import { SupplierService } from '../../../services/supplier.service';
import { SupplierSettings } from '../../../models/supplier-settings';

describe('ConfigurationComponent', () => {
  let fixture: ComponentFixture<ConfigurationComponent>;
  let supplierSpy: jasmine.SpyObj<SupplierService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const atual: SupplierSettings = { openingTime: '08:00', closingTime: '18:00', averageDeliveryTime: 30, lowStockThreshold: 8 };

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;
  const input = (id: string) => byId(id) as HTMLInputElement;
  const preencher = (id: string, valor: string) => {
    const el = input(id);
    el.value = valor;
    el.dispatchEvent(new Event('input'));
    el.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
  };
  // F9: cada seção salva sozinha
  const salvarFuncionamento = () => byId('save-hours') as HTMLButtonElement;
  const salvarEstoque = () => byId('save-stock') as HTMLButtonElement;

  beforeEach(async () => {
    supplierSpy = jasmine.createSpyObj('SupplierService', ['getMySettings', 'updateMySettings']);
    supplierSpy.getMySettings.and.returnValue(of({ ...atual }));
    supplierSpy.updateMySettings.and.callFake((s) => of(s));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [ConfigurationComponent],
      providers: [
        { provide: SupplierService, useValue: supplierSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfigurationComponent);
    fixture.detectChanges();
  });

  it('deveCarregarConfiguracoesDoFornecedor', () => {
    expect(supplierSpy.getMySettings).toHaveBeenCalled();
    expect(input('opening-time').value).toBe('08:00');
    expect(input('closing-time').value).toBe('18:00');
    expect(input('delivery-time').value).toBe('30');
    expect(input('low-stock').value).toBe('8');
  });

  it('deveDesabilitarSalvarSemAlteracao', () => {
    expect(salvarFuncionamento().disabled).toBeTrue();
    expect(salvarEstoque().disabled).toBeTrue();

    preencher('delivery-time', '45');
    expect(salvarFuncionamento().disabled).toBeFalse();
    expect(salvarEstoque().disabled).toBeTrue();

    preencher('delivery-time', '30');
    expect(salvarFuncionamento().disabled).toBeTrue();
  });

  it('deveMostrarErroComFechamentoIgualAAbertura', () => {
    preencher('closing-time', '08:00');

    expect(byId('error-closing-time')?.textContent).toContain('O fechamento precisa ser diferente da abertura.');
    expect(salvarFuncionamento().disabled).toBeTrue();

    preencher('delivery-time', '0');
    expect(byId('error-delivery-time')?.textContent).toContain('Entre 1 e 240 minutos.');
  });

  it('deveSalvarSoOFuncionamento', () => {
    preencher('opening-time', '18:00');
    preencher('closing-time', '02:00');

    salvarFuncionamento().click();
    fixture.detectChanges();

    expect(supplierSpy.updateMySettings).toHaveBeenCalledOnceWith({
      openingTime: '18:00', closingTime: '02:00', averageDeliveryTime: 30, lowStockThreshold: 8,
    });
    expect(toastrSpy.success).toHaveBeenCalledOnceWith('Horário e entrega salvos.');
    expect(salvarFuncionamento().disabled).toBeTrue();
  });

  it('deveSalvarSoOEstoque', () => {
    preencher('low-stock', '5');

    salvarEstoque().click();
    fixture.detectChanges();

    expect(supplierSpy.updateMySettings).toHaveBeenCalledOnceWith({
      openingTime: '08:00', closingTime: '18:00', averageDeliveryTime: 30, lowStockThreshold: 5,
    });
    expect(toastrSpy.success).toHaveBeenCalledOnceWith('Estoque mínimo salvo.');
  });

  it('deveManterAEdicaoPendenteDaOutraSecao', () => {
    preencher('opening-time', '09:00');
    preencher('low-stock', '5');

    salvarEstoque().click();
    fixture.detectChanges();

    // o horário editado não vai junto e continua no campo, esperando o próprio salvar
    expect(supplierSpy.updateMySettings.calls.mostRecent().args[0].openingTime).toBe('08:00');
    expect(input('opening-time').value).toBe('09:00');
    expect(salvarFuncionamento().disabled).toBeFalse();
    expect(salvarEstoque().disabled).toBeTrue();
  });

  it('deveSalvarEstoqueMesmoComHorarioInvalidoPendente', () => {
    preencher('closing-time', '08:00');
    preencher('low-stock', '5');

    expect(salvarEstoque().disabled).toBeFalse();
  });

  it('deveMostrarMensagemDoBackendNoErro', () => {
    supplierSpy.updateMySettings.and.returnValue(throwError(() => ({
      status: 400, error: { error: 'O horário de fechamento precisa ser diferente da abertura.' },
    })));
    preencher('low-stock', '5');

    salvarEstoque().click();
    fixture.detectChanges();

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('O horário de fechamento precisa ser diferente da abertura.'));
    expect(toastrSpy.success).not.toHaveBeenCalled();
    expect(salvarEstoque().disabled).toBeFalse();
  });

  it('deveTerAsAncorasDasSecoes', () => {
    expect(fixture.nativeElement.querySelector('section#funcionamento')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('section#estoque')).not.toBeNull();
  });

  it('deveAvisarQuandoCarregou', () => {
    const outro = TestBed.createComponent(ConfigurationComponent);
    const avisos: boolean[] = [];
    outro.componentInstance.loaded.subscribe(() => avisos.push(true));
    outro.detectChanges();

    expect(avisos.length).toBe(1);
  });

  it('naoDeveTerCabecalhoProprio', () => {
    // F9: as seções vivem dentro da tela da loja, que tem o título
    expect(fixture.nativeElement.querySelector('h1')).toBeNull();
  });

  it('deveMostrarPreviaDoHorario', () => {
    expect(byId('settings-preview')?.textContent).toContain('Aberta das 08:00 às 18:00 · entrega em ~30 min');

    preencher('opening-time', '18:00');
    preencher('closing-time', '02:00');

    expect(byId('settings-preview')?.textContent).toContain('Aberta das 18:00 às 02:00 do dia seguinte');
  });
});
