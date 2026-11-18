import { PushService } from '../../../services/push.service';
import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { DelivererRoutesComponent } from './deliverer-routes.component';
import { OrderService } from '../../../services/order.service';
import { OrderResponse } from '../../../models/order.model';

describe('DelivererRoutesComponent', () => {
  let component: DelivererRoutesComponent;
  let fixture: ComponentFixture<DelivererRoutesComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const PENDENTE = 'Em preparacao';
  const EM_ROTA = 'Saiu para entrega';
  const rota = (id: string, bairro: string, pagamento = 'PIX', status = EM_ROTA) => ({
    id, status, paymentMethod: pagamento, streetName: 'Rua das Flores', number: '300',
    neighborhood: bairro, customerName: 'Maria', customerPhone: '41988880000', totalPrice: 125,
    orderItems: [{ quantity: 2, productBrand: 'Ultragaz', productNameCategory: 'Gás 13kg' }],
  } as unknown as OrderResponse);

  const cards = () => Array.from(fixture.nativeElement.querySelectorAll('[data-testid=route-card]')) as HTMLElement[];

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['myRoutes', 'startDelivery', 'completeDelivery', 'deliveryHistory']);
    orderServiceSpy.deliveryHistory.and.returnValue(of({ deliveredToday: 0, items: [] }));
    orderServiceSpy.myRoutes.and.returnValue(of([rota('r1', 'Centro'), rota('r2', 'Batel', 'DINHEIRO', PENDENTE)]));
    orderServiceSpy.startDelivery.and.returnValue(of({ id: 'r2', status: EM_ROTA } as OrderResponse));
    orderServiceSpy.completeDelivery.and.returnValue(of({ id: 'r1', status: 'Entregue' } as OrderResponse));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);

    await TestBed.configureTestingModule({
      imports: [DelivererRoutesComponent],
      providers: [
        { provide: PushService, useValue: jasmine.createSpyObj('PushService', { canAsk: false, syncIfGranted: Promise.resolve(), enable: Promise.resolve(false), dismiss: undefined }) }, // HU27
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
        provideRouter([]),
        provideNoopAnimations(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DelivererRoutesComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => fixture.destroy());

  it('deveListarRotasDoEntregadorLogado', () => {
    fixture.detectChanges();

    expect(orderServiceSpy.myRoutes).toHaveBeenCalledTimes(1);
    expect(cards().length).toBe(2);
    expect(cards()[0].textContent).toContain('Centro');
    expect(cards()[0].textContent).toContain('2x Ultragaz Gás 13kg');
    expect(cards()[1].textContent).toContain('Dinheiro');
    expect(cards()[0].textContent).toContain('EM ROTA');
    expect(cards()[1].textContent).toContain('PENDENTE');
    expect(fixture.nativeElement.querySelector('[data-testid=pending-count]').textContent).toContain('2');
  });

  it('deveMostrarEstadoVazioSemRotas', () => {
    orderServiceSpy.myRoutes.and.returnValue(of([]));
    fixture.detectChanges();

    expect(cards().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Nenhuma entrega no momento');
  });

  it('deveIniciarEntregaDoCardPendenteEAbrirDetalhe', fakeAsync(() => {
    const navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture.detectChanges();

    (cards()[1].querySelector('[data-testid=start-delivery]') as HTMLButtonElement).click();
    fixture.detectChanges();
    tick(500);

    expect(orderServiceSpy.startDelivery).toHaveBeenCalledOnceWith('r2');
    expect(navigateSpy).toHaveBeenCalledWith(['/entregas', 'r2']);

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('deveAvisarENaoAbrirDetalheQuandoBackendRecusarInicio', fakeAsync(() => {
    const navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    orderServiceSpy.startDelivery.and.returnValue(
      throwError(() => ({ status: 400, error: { error: 'Finalize a entrega em rota antes de iniciar outra.' } }))
    );
    fixture.detectChanges();

    (cards()[1].querySelector('[data-testid=start-delivery]') as HTMLButtonElement).click();
    tick(500);
    fixture.detectChanges();

    expect(toastrSpy.error).toHaveBeenCalledWith('Erro ao iniciar a entrega: Finalize a entrega em rota antes de iniciar outra.');
    expect(navigateSpy).not.toHaveBeenCalled();
    expect((cards()[1].querySelector('[data-testid=start-delivery]') as HTMLButtonElement).disabled).toBeFalse();

    fixture.destroy();
    discardPeriodicTasks();
  }));

  // 3ª revisão (g): a lista não confirma; o card EM ROTA só leva ao detalhe
  it('deveAbrirDetalheDoCardEmRotaSemConfirmar', fakeAsync(() => {
    const navigateSpy = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture.detectChanges();
    expect(cards()[0].querySelector('[data-testid=start-delivery]')).toBeNull();
    expect(cards()[0].querySelector('[data-testid=confirm-delivery]')).toBeNull();

    (cards()[0].querySelector('[data-testid=continue-delivery]') as HTMLButtonElement).click();
    tick(500);

    expect(navigateSpy).toHaveBeenCalledWith(['/entregas', 'r1']);
    expect(orderServiceSpy.completeDelivery).not.toHaveBeenCalled();

    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('deveAtualizarRotasPeriodicamente', fakeAsync(() => {
    fixture.detectChanges();
    expect(orderServiceSpy.myRoutes).toHaveBeenCalledTimes(1);

    orderServiceSpy.myRoutes.and.returnValue(of([rota('r1', 'Centro'), rota('r2', 'Batel'), rota('r3', 'Portão')]));
    tick(30000);
    fixture.detectChanges();
    tick(500);
    fixture.detectChanges();

    expect(orderServiceSpy.myRoutes).toHaveBeenCalledTimes(2);
    expect(cards().length).toBe(3);

    fixture.destroy();
    discardPeriodicTasks();
  }));

  // ---- HU29 Cenário 6: a entrega confirmada não some ----
  it('deveMostrarAsEntregasDeHoje', () => {
    const agora = new Date();
    const hoje = (h: number, m: number) => new Date(agora.getFullYear(), agora.getMonth(), agora.getDate(), h, m).toISOString();
    const ontem = new Date(agora.getFullYear(), agora.getMonth(), agora.getDate() - 1, 18, 0).toISOString();
    orderServiceSpy.deliveryHistory.and.returnValue(of({
      deliveredToday: 2,
      items: [
        { orderId: 'd1', kind: 'DELIVERED', occurredAt: hoje(9, 30), neighborhood: 'Centro', streetName: 'Rua XV', number: '10', itemsSummary: '1x Ultragaz Gás 13kg', totalPrice: 111.96 },
        { orderId: 'x1', kind: 'REFUSED', occurredAt: hoje(9, 0), neighborhood: 'Batel', itemsSummary: '1x Água 20L' },
        { orderId: 'd0', kind: 'DELIVERED', occurredAt: ontem, neighborhood: 'Trianon', itemsSummary: '1x Água 20L' },
      ],
    }));
    fixture.detectChanges();

    const feitas = Array.from(fixture.nativeElement.querySelectorAll('[data-testid=delivered-today-item]')) as HTMLElement[];
    expect(feitas.length).toBe(1);
    expect(feitas[0].textContent).toContain('Centro');
    expect(feitas[0].textContent).toContain('1x Ultragaz Gás 13kg');
    expect(fixture.nativeElement.querySelector('[data-testid=delivered-today-title]')?.textContent).toContain('Entregues hoje');
    expect((fixture.nativeElement.querySelector('[data-testid=history-link]') as HTMLAnchorElement).getAttribute('href')).toBe('/entregas/historico');
  });

  it('naoDeveMostrarEntreguesHojeSemEntregas', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid=delivered-today-title]')).toBeNull();
  });
});
