import { TestBed } from '@angular/core/testing';

import { CartService } from './cart.service';

describe('CartService', () => {
  let service: CartService;

  const criar = () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  };

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    criar();
  });

  const salvo = () => JSON.parse(localStorage.getItem('cart-product')!);

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('devePersistirModalidadeNoLocalStorage', () => {
    expect(service.getDeliveryMode()).toBe('DELIVERY');

    service.setDeliveryMode('PICKUP');

    expect(localStorage.getItem('cart-delivery-mode')).toBe('PICKUP');
    expect(service.getDeliveryMode()).toBe('PICKUP');
  });

  // ---- HU20: só id e quantidade no navegador ----
  it('deveGuardarSoIdEQuantidade', () => {
    service.addProduct({ id: 5, supplierId: 'loja-a', brand: 'Ultragaz', priceDelivery: 110, imageUrl: 'x.png',
      stockQuantity: 9, quantitySelect: 2 } as any);

    expect(salvo()).toEqual([{ productId: 5, quantity: 2 }]);
    expect(localStorage.getItem('cart-supplier')).toBe('loja-a');
    expect(service.getCart()).toEqual([{ productId: 5, quantity: 2 }]);
  });

  it('deveConverterOCarrinhoAntigo', () => {
    localStorage.setItem('cart-product', JSON.stringify([
      { id: 5, brand: 'Ultragaz', supplierId: 'loja-a', priceDelivery: 110, quantitySelect: 3 },
      { id: 7, brand: 'Minalba', supplierId: 'loja-a' },
    ]));
    criar();

    expect(service.getCart()).toEqual([{ productId: 5, quantity: 3 }, { productId: 7, quantity: 1 }]);
    expect(salvo()).toEqual([{ productId: 5, quantity: 3 }, { productId: 7, quantity: 1 }]);
    expect(service.getCartSupplierId()).toBe('loja-a');
  });

  it('deveIgnorarCarrinhoQuebrado', () => {
    localStorage.setItem('cart-product', '{quebrado');
    criar();

    expect(service.getCart()).toEqual([]);
  });

  it('deveSomarAsQuantidadesNoContador', () => {
    let contagem = -1;
    service.count$.subscribe((n) => (contagem = n));

    service.addProduct({ id: 1, supplierId: 'loja-a', quantitySelect: 2 });
    service.addProduct({ id: 2, supplierId: 'loja-a' });

    expect(contagem).toBe(3);
  });

  it('deveAtualizarQuantidadeEPersistir', () => {
    service.setCart([{ productId: 5, quantity: 1 }, { productId: 7, quantity: 3 }], 'loja-a');

    service.updateQuantity(5, 4);
    service.updateQuantity(7, 0);

    expect(salvo()).toEqual([{ productId: 5, quantity: 4 }, { productId: 7, quantity: 1 }]);
  });

  it('deveRemoverItemEPersistir', () => {
    service.setCart([{ productId: 5, quantity: 1 }, { productId: 7, quantity: 3 }], 'loja-a');

    service.removeProduct(5);

    expect(salvo()).toEqual([{ productId: 7, quantity: 3 }]);
    expect(service.getCartSupplierId()).toBe('loja-a');
  });

  it('deveEsquecerALojaAoEsvaziar', () => {
    service.setCart([{ productId: 5, quantity: 1 }], 'loja-a');

    service.removeProduct(5);

    expect(service.getCartSupplierId()).toBeNull();
    expect(localStorage.getItem('cart-supplier')).toBeNull();
  });

  it('deveLimparOCarrinho', () => {
    service.setCart([{ productId: 5, quantity: 1 }], 'loja-a');

    service.clear();

    expect(service.getCart()).toEqual([]);
    expect(service.getCartSupplierId()).toBeNull();
  });

  // ---- F7 §5.23 + HU20: compra direta ----
  it('deveGuardarSoIdNoComprarAgora', () => {
    service.setCart([{ productId: 1, quantity: 2 }], 'loja-a');

    service.startBuyNow({ id: 9, brand: 'Ultragaz', supplierId: 's1', priceDelivery: 100, quantitySelect: 5 } as any);

    expect(JSON.parse(sessionStorage.getItem('buy-now-item')!)).toEqual({ productId: 9, quantity: 1, supplierId: 's1' });
    expect(service.getBuyNow()).toEqual({ productId: 9, quantity: 1, supplierId: 's1' });
    expect(salvo()).toEqual([{ productId: 1, quantity: 2 }]);
  });

  it('deveAjustarAQuantidadeDoItemAvulso', () => {
    service.startBuyNow({ id: 9, supplierId: 's1' } as any);

    service.updateBuyNowQuantity(3);
    expect(service.getBuyNow()!.quantity).toBe(3);
    service.updateBuyNowQuantity(0);
    expect(service.getBuyNow()!.quantity).toBe(1);
  });

  it('deveLimparSoOItemAvulso', () => {
    service.setCart([{ productId: 1, quantity: 2 }], 'loja-a');
    service.startBuyNow({ id: 9 } as any);

    service.clearBuyNow();

    expect(service.getBuyNow()).toBeNull();
    expect(sessionStorage.getItem('buy-now-item')).toBeNull();
    expect(service.getCart().length).toBe(1);
  });

  it('deveLerOItemAvulsoDaSessaoNoFormatoAntigo', () => {
    sessionStorage.setItem('buy-now-item', JSON.stringify({ id: 9, supplierId: 's1', brand: 'X', quantitySelect: 2 }));

    expect(service.getBuyNow()).toEqual({ productId: 9, quantity: 2, supplierId: 's1' });
    sessionStorage.setItem('buy-now-item', '{quebrado');
    expect(service.getBuyNow()).toBeNull();
  });

  // ---- HU14: uma loja por vez, agora pelo cart-supplier ----
  it('deveAdicionarQuandoCarrinhoVazio', () => {
    expect(service.addProduct({ id: 1, supplierId: 'loja-a' })).toBe('added');
    expect(service.getCartSupplierId()).toBe('loja-a');
  });

  it('deveSomarQuandoMesmaLoja', () => {
    service.setCart([{ productId: 1, quantity: 1 }], 'loja-a');

    expect(service.addProduct({ id: 1, supplierId: 'loja-a', quantitySelect: 1 })).toBe('added');
    expect(service.addProduct({ id: 2, supplierId: 'loja-a', quantitySelect: 1 })).toBe('added');
    expect(service.getCart()).toEqual([{ productId: 1, quantity: 2 }, { productId: 2, quantity: 1 }]);
  });

  it('deveBloquearOutraLojaPeloCartSupplier', () => {
    service.setCart([{ productId: 1, quantity: 2 }], 'loja-a');

    expect(service.addProduct({ id: 9, supplierId: 'loja-b' })).toBe('other_supplier');
    expect(service.addProduct({ id: 9 })).toBe('other_supplier');
    expect(service.getCart()).toEqual([{ productId: 1, quantity: 2 }]);
  });

  it('deveEsvaziarEAdicionar', () => {
    service.setCart([{ productId: 1, quantity: 2 }], 'loja-a');

    service.clearAndAdd({ id: 9, supplierId: 'loja-b', quantitySelect: 1 });

    expect(service.getCart()).toEqual([{ productId: 9, quantity: 1 }]);
    expect(service.getCartSupplierId()).toBe('loja-b');
  });
});
