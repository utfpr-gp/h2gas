import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OrderStatusTimelineComponent } from './order-status-timeline.component';
import { OrderStatus } from '../../models/order-status.enum';

describe('OrderStatusTimelineComponent', () => {
  let component: OrderStatusTimelineComponent;
  let fixture: ComponentFixture<OrderStatusTimelineComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [OrderStatusTimelineComponent] }).compileComponents();
    fixture = TestBed.createComponent(OrderStatusTimelineComponent);
    component = fixture.componentInstance;
  });

  const etapas = () => Array.from(fixture.nativeElement.querySelectorAll('[data-testid=timeline-step]')) as HTMLElement[];

  it('deveMarcarEtapasConcluidasEAtual', () => {
    component.status = OrderStatus.IN_PREPARATION;
    fixture.detectChanges();

    const estados = etapas().map((e) => e.getAttribute('data-state'));
    expect(estados).toEqual(['done', 'done', 'current', 'todo', 'todo']);
    expect(etapas()[2].getAttribute('aria-current')).toBe('step');
  });

  it('deveUsarRotuloDeRetiradaQuandoPickup', () => {
    component.status = OrderStatus.PENDING;
    component.deliveryMode = 'PICKUP';
    fixture.detectChanges();

    expect(etapas()[3].textContent).toContain('Pronto para retirada');
  });

  it('deveMostrarCanceladoSemEtapas', () => {
    component.status = OrderStatus.CANCELED;
    fixture.detectChanges();

    expect(etapas().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Pedido cancelado');
  });
});
