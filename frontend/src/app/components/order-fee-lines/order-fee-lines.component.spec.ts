import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OrderFeeLinesComponent } from './order-fee-lines.component';
import { OrderResponse } from '../../models/order.model';

// HU28 Cenário 4: subtotal e frete no pedido; retirada sem linha de frete
describe('OrderFeeLinesComponent', () => {
  let fixture: ComponentFixture<OrderFeeLinesComponent>;

  const pedido = (extra: Partial<OrderResponse>) =>
    ({ id: 'p1', totalPrice: 111.96, deliveryMode: 'DELIVERY', ...extra }) as OrderResponse;

  function render(order: OrderResponse) {
    fixture = TestBed.createComponent(OrderFeeLinesComponent);
    fixture.componentInstance.order = order;
    fixture.detectChanges();
  }
  const el = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [OrderFeeLinesComponent] }).compileComponents();
  });

  it('deveMostrarSubtotalEFreteComADistancia', () => {
    render(pedido({ subtotal: 108, deliveryFee: 3.96, deliveryDistanceKm: 0.8 }));

    expect(el('order-subtotal')!.textContent).toMatch(/108[.,]00/);
    expect(el('order-fee')!.textContent).toMatch(/3[.,]96/);
    expect(el('order-fee-label')!.textContent).toMatch(/0[.,]8 km/);
  });

  it('deveMostrarFreteGratis', () => {
    render(pedido({ subtotal: 111.96, deliveryFee: 0 }));

    expect(el('order-fee')!.textContent?.trim()).toBe('Grátis');
  });

  it('naoDeveMostrarFreteNaRetirada', () => {
    render(pedido({ deliveryMode: 'PICKUP', subtotal: 111.96, deliveryFee: 0 }));

    expect(el('order-fee')).toBeNull();
    expect(el('order-subtotal')).toBeNull();
  });
});
