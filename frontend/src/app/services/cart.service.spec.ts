import { TestBed } from '@angular/core/testing';

import { CartService } from './cart.service';

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    localStorage.clear();
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
});
