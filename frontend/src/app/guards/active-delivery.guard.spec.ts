import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router, UrlTree, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, firstValueFrom, of, throwError } from 'rxjs';

import { activeDeliveryGuard } from './active-delivery.guard';
import { OrderService } from '../services/order.service';
import { UserService } from '../services/user.service';
import { OrderResponse } from '../models/order.model';

// HU07 (3ª revisão, decisão h): com uma entrega em rota, o entregador fica preso ao detalhe dela
describe('activeDeliveryGuard', () => {
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  const EM_ROTA = 'Saiu para entrega';
  const pedido = (id: string, status: string) => ({ id, status } as OrderResponse);
  const routeTo = (id?: string) => ({ paramMap: convertToParamMap(id ? { id } : {}) }) as any;

  const run = (id?: string) =>
    firstValueFrom(
      TestBed.runInInjectionContext(() => (activeDeliveryGuard as CanActivateFn)(routeTo(id), {} as any)) as Observable<boolean | UrlTree>
    );

  beforeEach(() => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['myRoutes']);
    userServiceSpy = jasmine.createSpyObj('UserService', ['getUserLocalStorage']);
    userServiceSpy.getUserLocalStorage.and.returnValue({ userType: 'DELIVERER' });

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: OrderService, useValue: orderServiceSpy },
        { provide: UserService, useValue: userServiceSpy },
      ],
    });
  });

  it('deveRedirecionarParaEntregaEmRota', async () => {
    orderServiceSpy.myRoutes.and.returnValue(of([pedido('r1', EM_ROTA), pedido('r2', 'Em preparacao')]));

    const result = await run();

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/entregas/r1');
  });

  it('devePermitirDetalheDaEntregaEmRota', async () => {
    orderServiceSpy.myRoutes.and.returnValue(of([pedido('r1', EM_ROTA)]));

    expect(await run('r1')).toBeTrue();
  });

  it('deveRedirecionarDetalheDeOutroPedidoParaEntregaEmRota', async () => {
    orderServiceSpy.myRoutes.and.returnValue(of([pedido('r1', EM_ROTA), pedido('r2', 'Em preparacao')]));

    const result = await run('r2');

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/entregas/r1');
  });

  it('devePermitirNavegarSemEntregaEmRota', async () => {
    orderServiceSpy.myRoutes.and.returnValue(of([pedido('r2', 'Em preparacao')]));

    expect(await run()).toBeTrue();
  });

  it('naoDevePrenderAdministrador', async () => {
    userServiceSpy.getUserLocalStorage.and.returnValue({ userType: 'ADMIN' });

    const result = TestBed.runInInjectionContext(() => (activeDeliveryGuard as CanActivateFn)(routeTo(), {} as any));

    expect(result).toBeTrue();
    expect(orderServiceSpy.myRoutes).not.toHaveBeenCalled();
  });

  it('naoDeveBloquearQuandoFalharAoBuscarRotas', async () => {
    orderServiceSpy.myRoutes.and.returnValue(throwError(() => ({ status: 500 })));

    expect(await run()).toBeTrue();
  });
});
