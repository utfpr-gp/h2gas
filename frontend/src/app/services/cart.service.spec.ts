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
});
