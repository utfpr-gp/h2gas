import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { DelivererRoutesComponent } from './deliverer-routes.component';
import { DelivererRouteDetailComponent } from '../deliverer-route-detail/deliverer-route-detail.component';
import { activeDeliveryGuard } from '../../../guards/active-delivery.guard';
import { OrderService } from '../../../services/order.service';
import { UserService } from '../../../services/user.service';
import { OrderResponse } from '../../../models/order.model';

@Component({ standalone: true, template: '<p>histórico</p>' })
class HistoricoStubComponent {}

/**
 * HU07 (3ª revisão): jornada do entregador com as rotas e o activeDeliveryGuard reais.
 * Iniciar leva ao detalhe e prende nele; confirmar volta à lista (a próxima pendente NÃO
 * começa sozinha); só se já houver outra em rota (dado antigo) ele vai direto para ela.
 */
describe('Fluxo do entregador (HU07, 3ª revisão)', () => {
  const PENDENTE = 'Em preparacao';
  const EM_ROTA = 'Saiu para entrega';
  const pedido = (id: string, status: string, bairro: string) => ({
    id, status, paymentMethod: 'PIX', streetName: 'Rua das Flores', number: '300', neighborhood: bairro,
    cityName: 'Curitiba', customerName: 'Maria', customerPhone: '41988880000', totalPrice: 125,
    orderItems: [{ quantity: 1, productBrand: 'Ultragaz', productNameCategory: 'Gás 13kg' }],
  } as unknown as OrderResponse);

  let rotasNoServidor: OrderResponse[];
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let harness: RouterTestingHarness;
  let router: Router;

  const url = () => router.url;
  const byId = (id: string) => harness.routeNativeElement?.querySelector(`[data-testid=${id}]`) as HTMLButtonElement | null;
  const cards = () => Array.from(harness.routeNativeElement?.querySelectorAll('[data-testid=route-card]') ?? []) as HTMLElement[];
  // o clique dispara a chamada e depois uma navegação assíncrona; espera as duas assentarem
  const settle = async () => {
    for (let i = 0; i < 3; i++) {
      await new Promise((resolve) => setTimeout(resolve));
      harness.detectChanges();
    }
  };

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['myRoutes', 'startDelivery', 'completeDelivery', 'cancelDelivery', 'deliveryHistory']);
    orderServiceSpy.deliveryHistory.and.returnValue(of({ deliveredToday: 0, items: [] })); // HU29: Entregues hoje
    orderServiceSpy.myRoutes.and.callFake(() => of(rotasNoServidor));
    orderServiceSpy.startDelivery.and.callFake((id: string) => {
      rotasNoServidor = rotasNoServidor.map((r) => (r.id === id ? ({ ...r, status: EM_ROTA } as unknown as OrderResponse) : r));
      return of(rotasNoServidor.find((r) => r.id === id)!);
    });
    orderServiceSpy.completeDelivery.and.callFake((id: string) => {
      rotasNoServidor = rotasNoServidor.filter((r) => r.id !== id);
      return of({ id, status: 'Entregue' } as OrderResponse);
    });
    const userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ userType: 'DELIVERER' });

    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'entregas', component: DelivererRoutesComponent, canActivate: [activeDeliveryGuard] },
          { path: 'entregas/historico', component: HistoricoStubComponent, canActivate: [activeDeliveryGuard] },
          { path: 'entregas/:id', component: DelivererRouteDetailComponent, canActivate: [activeDeliveryGuard] },
        ]),
        provideNoopAnimations(),
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
        { provide: ToastrService, useValue: jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']) },
      ],
    });

    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
  });

  afterEach(() => harness.fixture.destroy());

  it('deveIrAoDetalheAoIniciarEFicarPresoNele', async () => {
    rotasNoServidor = [pedido('r1', PENDENTE, 'Centro'), pedido('r2', PENDENTE, 'Batel')];
    await harness.navigateByUrl('/entregas');
    expect(cards().length).toBe(2);

    (cards()[0].querySelector('[data-testid=start-delivery]') as HTMLButtonElement).click();
    await settle();

    expect(url()).toBe('/entregas/r1');
    expect(byId('back')).toBeNull();

    for (const tentativa of ['/entregas', '/entregas/historico', '/entregas/r2']) {
      await harness.navigateByUrl(tentativa);
      expect(url()).withContext(tentativa).toBe('/entregas/r1');
    }
    expect(orderServiceSpy.startDelivery).toHaveBeenCalledTimes(1);
  });

  it('deveVoltarParaListaAoConfirmarSemIniciarAProximaSozinho', async () => {
    rotasNoServidor = [pedido('r1', EM_ROTA, 'Centro'), pedido('r2', PENDENTE, 'Batel')];
    await harness.navigateByUrl('/entregas');
    expect(url()).toBe('/entregas/r1');

    byId('confirm-delivery')!.click();
    await settle();

    expect(orderServiceSpy.completeDelivery).toHaveBeenCalledOnceWith('r1');
    expect(url()).toBe('/entregas');
    expect(cards().length).toBe(1);
    expect(cards()[0].textContent).toContain('PENDENTE');
    expect(byId('start-delivery')).toBeTruthy();
    expect(orderServiceSpy.startDelivery).not.toHaveBeenCalled();
  });

  it('deveIrDiretoParaOutraEntregaJaEmRotaAoConfirmar', async () => {
    // duas em rota só existem em dados de antes da regra "uma por vez"
    rotasNoServidor = [pedido('r1', EM_ROTA, 'Centro'), pedido('r2', EM_ROTA, 'Batel')];
    await harness.navigateByUrl('/entregas');
    expect(url()).toBe('/entregas/r1');

    byId('confirm-delivery')!.click();
    await settle();

    expect(url()).toBe('/entregas/r2');
    expect(harness.routeNativeElement?.textContent).toContain('Batel');
  });

  it('deveLiberarListaEHistoricoSemEntregaEmRota', async () => {
    rotasNoServidor = [pedido('r1', PENDENTE, 'Centro')];

    await harness.navigateByUrl('/entregas/historico');
    expect(url()).toBe('/entregas/historico');

    await harness.navigateByUrl('/entregas');
    expect(url()).toBe('/entregas');
    expect(byId('start-delivery')).toBeTruthy();
  });
});
