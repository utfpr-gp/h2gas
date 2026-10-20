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
});
