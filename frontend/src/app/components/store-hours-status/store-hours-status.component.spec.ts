import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { StoreHoursStatusComponent } from './store-hours-status.component';
import { SupplierService } from '../../services/supplier.service';
import { UserService } from '../../services/user.service';

// HU18: o fornecedor vê se a loja está aberta, mas não abre nem fecha por botão
describe('StoreHoursStatusComponent', () => {
  let fixture: ComponentFixture<StoreHoursStatusComponent>;
  let supplierService: jasmine.SpyObj<SupplierService>;

  const status = () => fixture.nativeElement.querySelector('[data-testid="store-status"]') as HTMLElement;

  const criar = (aberta: boolean, abertura = '08:00', fechamento = '18:00') => {
    supplierService.getOpen.and.returnValue(of(aberta));
    supplierService.getMySettings.and.returnValue(of({
      openingTime: abertura, closingTime: fechamento, averageDeliveryTime: 30, lowStockThreshold: 8,
    }));
    fixture = TestBed.createComponent(StoreHoursStatusComponent);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    supplierService = jasmine.createSpyObj('SupplierService', ['getOpen', 'getMySettings']);
    await TestBed.configureTestingModule({
      imports: [StoreHoursStatusComponent],
      providers: [
        provideRouter([]),
        { provide: SupplierService, useValue: supplierService },
        { provide: UserService, useValue: { getUserLocalStorage: () => ({ supplierId: 'loja-1' }) } },
      ],
    }).compileComponents();
  });

  it('deveMostrarOEstadoDaLojaSemInterruptor', () => {
    criar(true);

    expect(supplierService.getOpen).toHaveBeenCalledWith('loja-1');
    expect(status().textContent).toContain('Aberta agora');
    expect(fixture.nativeElement.querySelector('input[type="checkbox"]')).toBeNull();
  });

  it('deveMostrarQuandoFechaSeEstiverAberta', () => {
    criar(true);
    expect(status().textContent).toContain('fecha às 18:00');
  });

  it('deveMostrarQuandoAbreSeEstiverFechada', () => {
    criar(false);
    expect(status().textContent).toContain('Fechada');
    expect(status().textContent).toContain('abre às 08:00');
  });

  it('deveLevarAsConfiguracoesParaAjustarOHorario', () => {
    criar(true);
    const link = fixture.nativeElement.querySelector('[data-testid="store-hours-link"]') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('/minha-loja/configuracoes');
  });
});
