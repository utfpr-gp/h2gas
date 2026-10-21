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
  const salvar = () => byId('save-settings') as HTMLButtonElement;

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
    expect(salvar().disabled).toBeTrue();

    preencher('delivery-time', '45');
    expect(salvar().disabled).toBeFalse();

    preencher('delivery-time', '30');
    expect(salvar().disabled).toBeTrue();
  });

  it('deveMostrarErroComFechamentoIgualAAbertura', () => {
    preencher('closing-time', '08:00');

    expect(byId('error-closing-time')?.textContent).toContain('O fechamento precisa ser diferente da abertura.');
    expect(salvar().disabled).toBeTrue();

    preencher('delivery-time', '0');
    expect(byId('error-delivery-time')?.textContent).toContain('Entre 1 e 240 minutos.');
  });

  it('deveSalvarTudoNumaChamadaComUmToast', () => {
    preencher('opening-time', '18:00');
    preencher('closing-time', '02:00');
    preencher('low-stock', '5');

    salvar().click();
    fixture.detectChanges();

    expect(supplierSpy.updateMySettings).toHaveBeenCalledOnceWith({
      openingTime: '18:00', closingTime: '02:00', averageDeliveryTime: 30, lowStockThreshold: 5,
    });
    expect(toastrSpy.success).toHaveBeenCalledOnceWith('Configurações salvas.');
    expect(salvar().disabled).toBeTrue();
  });

  it('deveMostrarMensagemDoBackendNoErro', () => {
    supplierSpy.updateMySettings.and.returnValue(throwError(() => ({
      status: 400, error: { error: 'O horário de fechamento precisa ser diferente da abertura.' },
    })));
    preencher('low-stock', '5');

    salvar().click();
    fixture.detectChanges();

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('O horário de fechamento precisa ser diferente da abertura.'));
    expect(toastrSpy.success).not.toHaveBeenCalled();
    expect(salvar().disabled).toBeFalse();
  });

  it('deveMostrarPreviaDoHorario', () => {
    expect(byId('settings-preview')?.textContent).toContain('Aberta das 08:00 às 18:00 · entrega em ~30 min');

    preencher('opening-time', '18:00');
    preencher('closing-time', '02:00');

    expect(byId('settings-preview')?.textContent).toContain('Aberta das 18:00 às 02:00 do dia seguinte');
  });
});
