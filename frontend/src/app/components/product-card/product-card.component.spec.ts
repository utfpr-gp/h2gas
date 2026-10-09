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
    component.product = { id: 10, brand: 'Ultragaz', price: 120 } as ProductItem;
    fixture.detectChanges();
    spyOn(component.addToCart, 'emit');

    (fixture.nativeElement.querySelector('[data-testid="add-to-cart"]') as HTMLButtonElement).click();

    expect(component.addToCart.emit).toHaveBeenCalledWith(
      jasmine.objectContaining({ id: 10, quantitySelect: 1 })
    );
  });
});
