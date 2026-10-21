import { TestBed } from '@angular/core/testing';

import { CartService } from './cart.service';

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('devePersistirModalidadeNoLocalStorage', () => {
    expect(service.getDeliveryMode()).toBe('DELIVERY');

    service.setDeliveryMode('PICKUP');

    expect(localStorage.getItem('cart-delivery-mode')).toBe('PICKUP');
    expect(service.getDeliveryMode()).toBe('PICKUP');
  });

  it('deveAtualizarQuantidadeEPersistir', () => {
    service.setCart([{ id: 5, quantitySelect: 1 }, { id: 7, quantitySelect: 3 }]);

    service.updateQuantity(5, 4);
    service.updateQuantity(7, 0);

    const salvo = JSON.parse(localStorage.getItem('cart-product')!);
    expect(salvo.find((p: any) => p.id === 5).quantitySelect).toBe(4);
    expect(salvo.find((p: any) => p.id === 7).quantitySelect).toBe(1);
  });

  it('deveRemoverItemEPersistir', () => {
    service.setCart([{ id: 5, quantitySelect: 1 }, { id: 7, quantitySelect: 3 }]);

    service.removeProduct(5);

    expect(service.getCart().map((p) => p.id)).toEqual([7]);
    expect(JSON.parse(localStorage.getItem('cart-product')!).map((p: any) => p.id)).toEqual([7]);
  });

  // ---- F7 §5.23: compra direta ----
  it('deveGuardarOItemAvulsoSemTocarNoCarrinho', () => {
    service.setCart([{ id: 1, quantitySelect: 2 }, { id: 2, quantitySelect: 1 }]);

    service.startBuyNow({ id: 9, brand: 'Ultragaz', supplierId: 's1', quantitySelect: 5 } as any);

    expect(service.getBuyNow()).toEqual(jasmine.objectContaining({ id: 9, quantitySelect: 1 }));
    expect(service.getCart().map((p) => p.id)).toEqual([1, 2]);
    expect(JSON.parse(localStorage.getItem('cart-product')!).length).toBe(2);
    expect(JSON.parse(sessionStorage.getItem('buy-now-item')!).id).toBe(9);
  });

  it('deveAjustarAQuantidadeDoItemAvulso', () => {
    service.startBuyNow({ id: 9, brand: 'Ultragaz' } as any);

    service.updateBuyNowQuantity(3);
    expect(service.getBuyNow()!.quantitySelect).toBe(3);
    service.updateBuyNowQuantity(0);
    expect(service.getBuyNow()!.quantitySelect).toBe(1);
  });

  it('deveLimparSoOItemAvulso', () => {
    service.setCart([{ id: 1, quantitySelect: 2 }]);
    service.startBuyNow({ id: 9 } as any);

    service.clearBuyNow();

    expect(service.getBuyNow()).toBeNull();
    expect(sessionStorage.getItem('buy-now-item')).toBeNull();
    expect(service.getCart().length).toBe(1);
  });

  it('deveLerOItemAvulsoDaSessaoAoRecarregar', () => {
    sessionStorage.setItem('buy-now-item', JSON.stringify({ id: 9, quantitySelect: 2 }));

    expect(service.getBuyNow()).toEqual(jasmine.objectContaining({ id: 9, quantitySelect: 2 }));
    sessionStorage.setItem('buy-now-item', '{quebrado');
    expect(service.getBuyNow()).toBeNull();
  });
});
