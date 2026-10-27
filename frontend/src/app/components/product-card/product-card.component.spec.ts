import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProductCardComponent } from './product-card.component';
import { ProductItem } from '../../models/product-item';

describe('ProductCardComponent', () => {
  let component: ProductCardComponent;
  let fixture: ComponentFixture<ProductCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductCardComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductCardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    component.product = { id: 1, brand: 'Ultragaz' } as ProductItem;
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('deveEmitirProdutoComQuantidadeUmAoClicarEmAdicionar', () => {
    component.product = { id: 10, brand: 'Ultragaz', priceDelivery: 120, pricePickup: 100 } as ProductItem;
    fixture.detectChanges();
    spyOn(component.addToCart, 'emit');

    (fixture.nativeElement.querySelector('[data-testid="add-to-cart"]') as HTMLButtonElement).click();

    expect(component.addToCart.emit).toHaveBeenCalledWith(
      jasmine.objectContaining({ id: 10, quantitySelect: 1 })
    );
  });

  it('deveExibirBadgeDeFreteGratisQuandoFreteZero', () => {
    component.product = { id: 10, brand: 'Ultragaz', priceDelivery: 100, pricePickup: 100, freeDelivery: true } as ProductItem;
    fixture.detectChanges();

    const selo = fixture.nativeElement.querySelector('[data-testid="free-delivery"]') as HTMLElement;
    expect(selo).toBeTruthy();
    expect(selo.textContent).toContain('Entrega Grátis');
  });

  it('naoDeveExibirBadgeQuandoHouverFrete', () => {
    component.product = { id: 10, brand: 'Ultragaz', priceDelivery: 110, pricePickup: 95, freeDelivery: false } as ProductItem;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="free-delivery"]')).toBeNull();
  });

  it('deveExibirAmbosPrecosSimultaneamente', () => {
    component.product = { id: 10, brand: 'Ultragaz', priceDelivery: 110, pricePickup: 95 } as ProductItem;
    fixture.detectChanges();

    const entrega = fixture.nativeElement.querySelector('[data-testid="price-delivery"]') as HTMLElement;
    const retirada = fixture.nativeElement.querySelector('[data-testid="price-pickup"]') as HTMLElement;
    expect(entrega.textContent).toMatch(/110[.,]00/);
    expect(retirada.textContent).toMatch(/95[.,]00/);
  });

  it('deveUsarAAcaoDeDestaqueNoAdicionar', () => {
    component.product = { id: 1, brand: 'Ultragaz', priceDelivery: 120, pricePickup: 100 } as any;
    fixture.detectChanges();

    const botao = fixture.nativeElement.querySelector('[data-testid="add-to-cart"]') as HTMLButtonElement;
    expect(botao.classList).toContain('btn-accent');
    expect(botao.classList).not.toContain('bg-primary');
  });

  it('deveCarregarAImagemSobDemandaComTamanhoReservado', () => {
    component.product = { id: 1, brand: 'Ultragaz', imageUrl: 'x.jpg', priceDelivery: 120, pricePickup: 100 } as any;
    fixture.detectChanges();

    const img = fixture.nativeElement.querySelector('img') as HTMLImageElement;
    expect(img.getAttribute('loading')).toBe('lazy');
    expect(img.getAttribute('width')).toBeTruthy();
    expect(img.getAttribute('height')).toBeTruthy();
  });

  it('deveTerAlvoDeToque44pxNoCelular', () => {
    component.product = { id: 1, brand: 'Ultragaz', priceDelivery: 120, pricePickup: 100 } as any;
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="add-to-cart"]').classList).toContain('min-h-11');
  });

  it('deveEmitirCompraDiretaComQuantidadeUm', () => {
    component.product = { id: 10, brand: 'Ultragaz', priceDelivery: 120, pricePickup: 100 } as ProductItem;
    fixture.detectChanges();
    spyOn(component.buyNow, 'emit');
    spyOn(component.addToCart, 'emit');

    (fixture.nativeElement.querySelector('[data-testid="buy-now"]') as HTMLButtonElement).click();

    expect(component.buyNow.emit).toHaveBeenCalledWith(jasmine.objectContaining({ id: 10, quantitySelect: 1 }));
    expect(component.addToCart.emit).not.toHaveBeenCalled();
  });

  // ---- HU18: produto de loja fechada aparece, mas não dá para comprar ----
  it('deveBloquearACompraComMotivo', () => {
    component.product = { id: 10, brand: 'Ultragaz', priceDelivery: 120, pricePickup: 100 } as ProductItem;
    component.blockedReason = 'Loja fechada · abre às 08:00';
    fixture.detectChanges();
    spyOn(component.addToCart, 'emit');
    spyOn(component.buyNow, 'emit');

    const adicionar = fixture.nativeElement.querySelector('[data-testid="add-to-cart"]') as HTMLButtonElement;
    const comprar = fixture.nativeElement.querySelector('[data-testid="buy-now"]') as HTMLButtonElement;
    expect(adicionar.disabled).toBeTrue();
    expect(comprar.disabled).toBeTrue();
    expect(adicionar.getAttribute('title')).toBe('Loja fechada · abre às 08:00');

    component.onAdd();
    component.onBuyNow();
    expect(component.addToCart.emit).not.toHaveBeenCalled();
    expect(component.buyNow.emit).not.toHaveBeenCalled();
  });

  // ---- HU15 (revisão): na vitrine de produtos o card diz qual loja vende ----
  const loja = () => fixture.nativeElement.querySelector('[data-testid="product-store"]') as HTMLButtonElement | null;
  const deLoja = { id: 10, brand: 'Ultragaz', supplierId: 'loja-7', supllierName: 'Gás Centro Curitiba' } as ProductItem;

  it('deveMostrarALojaQuandoPedido', () => {
    component.product = deLoja;
    component.showStore = true;
    fixture.detectChanges();

    expect(loja()?.textContent).toContain('Gás Centro Curitiba');
    expect(loja()?.getAttribute('aria-label')).toBe('Ver a loja Gás Centro Curitiba');
    expect(loja()?.classList).toContain('min-h-11');
  });

  it('naoDeveMostrarALojaPorPadrao', () => {
    component.product = deLoja;
    fixture.detectChanges();

    expect(loja()).toBeNull();
  });

  it('naoDeveMostrarALojaSemNome', () => {
    component.product = { id: 10, brand: 'Ultragaz', supplierId: 'loja-7' } as ProductItem;
    component.showStore = true;
    fixture.detectChanges();

    expect(loja()).toBeNull();
  });

  it('deveEmitirALojaAoTocarNoNome', () => {
    component.product = deLoja;
    component.showStore = true;
    fixture.detectChanges();
    spyOn(component.viewStore, 'emit');
    spyOn(component.addToCart, 'emit');
    spyOn(component.buyNow, 'emit');

    loja()!.click();

    expect(component.viewStore.emit).toHaveBeenCalledOnceWith('loja-7');
    expect(component.addToCart.emit).not.toHaveBeenCalled();
    expect(component.buyNow.emit).not.toHaveBeenCalled();
  });
});
