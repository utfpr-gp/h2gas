import { ComponentFixture, TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { DelivererRoutesComponent } from './deliverer-routes.component';
import { OrderService } from '../../../services/order.service';
import { OrderResponse } from '../../../models/order.model';

describe('DelivererRoutesComponent', () => {
  let component: DelivererRoutesComponent;
  let fixture: ComponentFixture<DelivererRoutesComponent>;
  let orderServiceSpy: jasmine.SpyObj<OrderService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const rota = (id: string, bairro: string, pagamento = 'PIX') => ({
    id, status: 'Saiu para entrega', paymentMethod: pagamento, streetName: 'Rua das Flores', number: '300',
    neighborhood: bairro, customerName: 'Maria', customerPhone: '41988880000', totalPrice: 125,
    orderItems: [{ quantity: 2, productBrand: 'Ultragaz', productNameCategory: 'Gás 13kg' }],
  } as unknown as OrderResponse);

  const cards = () => Array.from(fixture.nativeElement.querySelectorAll('[data-testid=route-card]')) as HTMLElement[];

  beforeEach(async () => {
    orderServiceSpy = jasmine.createSpyObj('OrderService', ['myRoutes', 'completeDelivery']);
    orderServiceSpy.myRoutes.and.returnValue(of([rota('r1', 'Centro'), rota('r2', 'Batel', 'DINHEIRO')]));
    orderServiceSpy.completeDelivery.and.returnValue(of({ id: 'r1', status: 'Entregue' } as OrderResponse));
    toastrSpy = jasmine.createSpyObj('ToastrService', ['success', 'warning', 'error']);

    await TestBed.configureTestingModule({
      imports: [DelivererRoutesComponent],
      providers: [
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
    expect(fixture.nativeElement.querySelector('[data-testid=pending-count]').textContent).toContain('2');
  });

  it('deveMostrarEstadoVazioSemRotas', () => {
    orderServiceSpy.myRoutes.and.returnValue(of([]));
    fixture.detectChanges();

    expect(cards().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Nenhuma entrega no momento');
  });

  it('deveConfirmarEntregaComUmToque', fakeAsync(() => {
    fixture.detectChanges();

    (cards()[0].querySelector('[data-testid=confirm-delivery]') as HTMLButtonElement).click();
    fixture.detectChanges();
    tick(500);
    fixture.detectChanges();

    expect(orderServiceSpy.completeDelivery).toHaveBeenCalledOnceWith('r1');
    expect(component.routes.map((r) => r.id)).toEqual(['r2']);
    expect(cards().length).toBe(1);
    expect(toastrSpy.success).toHaveBeenCalled();

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
});
