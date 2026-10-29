import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { OrderReviewDialogComponent } from './order-review-dialog.component';
import { OrderService } from '../../services/order.service';
import { OrderResponse } from '../../models/order.model';

// HU24: diálogo em que o cliente dá as estrelas da loja e do entregador
describe('OrderReviewDialogComponent', () => {
  let fixture: ComponentFixture<OrderReviewDialogComponent>;
  let component: OrderReviewDialogComponent;
  let orderService: jasmine.SpyObj<OrderService>;
  let toastr: jasmine.SpyObj<ToastrService>;

  const pedido = (extra: Partial<OrderResponse> = {}) =>
    ({ id: 'pedido-1', supplierName: 'Gás Centro', status: 'Entregue', deliveryMode: 'DELIVERY',
      delivererName: 'Carlos', ...extra }) as OrderResponse;

  function criar(order: OrderResponse) {
    fixture = TestBed.createComponent(OrderReviewDialogComponent);
    component = fixture.componentInstance;
    component.order = order;
    fixture.detectChanges();
  }

  const el = (id: string) => fixture.nativeElement.querySelector(`[data-testid="${id}"]`) as HTMLElement | null;

  function escolher(id: string) {
    (el(id) as HTMLInputElement).click();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    orderService = jasmine.createSpyObj<OrderService>('OrderService', ['reviewOrder']);
    toastr = jasmine.createSpyObj<ToastrService>('ToastrService', ['success', 'error']);
    await TestBed.configureTestingModule({
      imports: [OrderReviewDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: OrderService, useValue: orderService },
        { provide: ToastrService, useValue: toastr },
      ],
    }).compileComponents();
  });

  it('deveMostrarANotaDoEntregadorSoComEntregador', () => {
    criar(pedido());
    expect(el('store-stars')).not.toBeNull();
    expect(el('deliverer-stars')).not.toBeNull();
    expect(el('deliverer-stars')!.textContent).toContain('Carlos');

    fixture.destroy();
    criar(pedido({ deliveryMode: 'PICKUP', delivererName: undefined }));
    expect(el('store-stars')).not.toBeNull();
    expect(el('deliverer-stars')).toBeNull();
  });

  it('deveTerEstrelasAcessiveis', () => {
    criar(pedido());
    expect(el('store-stars')!.getAttribute('role')).toBe('radiogroup');
    expect(el('store-star-4')!.getAttribute('aria-label')).toBe('4 de 5');
  });

  it('deveDesabilitarEnviarSemANotaDaLoja', () => {
    criar(pedido());
    const enviar = el('submit-review') as HTMLButtonElement;
    expect(enviar.disabled).toBeTrue();

    escolher('deliverer-star-5');
    expect(enviar.disabled).toBeTrue();

    escolher('store-star-4');
    expect(enviar.disabled).toBeFalse();
  });

  it('deveEnviarAsNotasEOComentario', () => {
    criar(pedido());
    const salvo = { storeRating: 4, delivererRating: 5, comment: 'Chegou rápido' };
    orderService.reviewOrder.and.returnValue(of(salvo));
    const emitido: unknown[] = [];
    component.saved.subscribe((r) => emitido.push(r));

    escolher('store-star-4');
    escolher('deliverer-star-5');
    const comentario = el('review-comment') as HTMLTextAreaElement;
    comentario.value = '  Chegou rápido ';
    comentario.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (el('submit-review') as HTMLButtonElement).click();

    expect(orderService.reviewOrder).toHaveBeenCalledWith('pedido-1',
      { storeRating: 4, delivererRating: 5, comment: 'Chegou rápido' });
    expect(emitido).toEqual([salvo]);
    expect(toastr.success).toHaveBeenCalled();
  });

  it('deveEnviarSemNotaDoEntregadorQuandoNaoDada', () => {
    criar(pedido());
    orderService.reviewOrder.and.returnValue(of({ storeRating: 3 }));

    escolher('store-star-3');
    (el('submit-review') as HTMLButtonElement).click();

    expect(orderService.reviewOrder).toHaveBeenCalledWith('pedido-1',
      { storeRating: 3, delivererRating: null, comment: null });
  });

  it('deveMostrarOErroDoBackend', () => {
    criar(pedido());
    orderService.reviewOrder.and.returnValue(throwError(() => ({ status: 409, error: { message: 'Este pedido já foi avaliado.' } })));

    escolher('store-star-5');
    (el('submit-review') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(el('review-error')!.textContent).toContain('já foi avaliado');
    expect((el('submit-review') as HTMLButtonElement).disabled).toBeFalse();
  });
});
