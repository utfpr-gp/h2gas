import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { DelivererHistoryComponent } from './deliverer-history.component';
import { OrderService } from '../../../services/order.service';
import { DeliveryHistory, DeliveryHistoryItem } from '../../../models/delivery-history';

registerLocaleData(localePt);

describe('DelivererHistoryComponent', () => {
  let fixture: ComponentFixture<DelivererHistoryComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const diasAtras = (dias: number, hora = 14) => {
    const d = new Date();
    d.setDate(d.getDate() - dias);
    d.setHours(hora, 30, 0, 0);
    return d.toISOString();
  };
  const item = (orderId: string, kind: 'DELIVERED' | 'REFUSED', occurredAt: string, total: number | null = 125): DeliveryHistoryItem => ({
    orderId, kind, occurredAt, streetName: 'Rua das Flores', number: '123', neighborhood: 'Centro', cityName: 'Curitiba',
    itemsSummary: '1x Ultragaz Gás 13kg', paymentMethod: 'PIX', totalPrice: total,
  });
  const historico: DeliveryHistory = {
    deliveredToday: 2,
    items: [
      item('a1', 'DELIVERED', diasAtras(0, 14)),
      item('a2', 'REFUSED', diasAtras(0, 12), null),
      item('b1', 'DELIVERED', diasAtras(3, 10)),
    ],
  };

  const cards = () => Array.from(fixture.nativeElement.querySelectorAll('[data-testid=history-item]')) as HTMLElement[];

  const setup = async (response = of(historico)) => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['deliveryHistory']);
    orderServiceSpy.deliveryHistory.and.returnValue(response);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['error']);
    await TestBed.configureTestingModule({
      imports: [DelivererHistoryComponent],
      providers: [
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        provideRouter([]),
        provideNoopAnimations(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(DelivererHistoryComponent);
    fixture.detectChanges();
  };

  afterEach(() => fixture.destroy());

  it('deveExibirEntregasDeHojeEGruposPorDia', async () => {
    await setup();

    expect(fixture.nativeElement.querySelector('[data-testid=delivered-today]').textContent.trim()).toBe('2');
    const grupos = Array.from(fixture.nativeElement.querySelectorAll('[data-testid=day-group]')) as HTMLElement[];
    expect(grupos.length).toBe(2);
    expect(grupos[0].textContent).toContain('Hoje');
    expect(grupos[1].textContent).not.toContain('Hoje');
    expect(cards().length).toBe(3);
    expect(cards()[0].textContent).toContain('Entregue');
    expect(cards()[0].textContent).toContain('Rua das Flores, 123');
    expect(cards()[0].textContent).toContain('1x Ultragaz Gás 13kg');
    expect(cards()[0].textContent).toMatch(/R\$\s125,00/);
  });

  it('deveMarcarEntregaRecusadaComoCancelada', async () => {
    await setup();

    const recusada = cards()[1];
    expect(recusada.textContent).toContain('Cancelado');
    expect(recusada.textContent).not.toContain('R$');
    expect(recusada.textContent).toContain('—');
  });

  it('deveMostrarEstadoVazioSemHistorico', async () => {
    await setup(of({ deliveredToday: 0, items: [] }));

    expect(cards().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Nenhuma entrega no histórico');
  });

  it('deveAvisarQuandoFalharAoBuscarHistorico', async () => {
    await setup(throwError(() => ({ error: { message: 'Access Denied' } })) as any);

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringMatching('Access Denied'));
  });
});
