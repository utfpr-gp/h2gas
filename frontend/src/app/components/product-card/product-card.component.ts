import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductItem } from '../../models/product-item';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-card.component.html'
})
export class ProductCardComponent {
  @Input() product!: ProductItem;
  // HU18: com motivo (ex.: loja fechada), o produto aparece mas não pode ser comprado
  @Input() blockedReason: string | null = null;
  // HU15 (revisão): na vitrine de produtos, que mistura lojas, o card diz quem vende e leva até a loja
  @Input() showStore = false;
  @Output() viewStore = new EventEmitter<string>();
  // HU15 §5.5: logo da loja quebrada volta para a inicial do nome
  storeLogoBroken = false;

  get storeInitial(): string {
    return (this.product.supllierName ?? '').trim().charAt(0).toUpperCase();
  }
  @Output() addToCart = new EventEmitter<ProductItem>();

  @Output() buyNow = new EventEmitter<ProductItem>();

  onBuyNow() {
    if (this.blockedReason) return;
    this.buyNow.emit({ ...this.product, quantitySelect: 1 });
  }

  onViewStore() {
    if (this.product.supplierId) this.viewStore.emit(this.product.supplierId);
  }

  onAdd() {
    if (this.blockedReason) return;
    this.addToCart.emit({ ...this.product, quantitySelect: 1 });
  }
}
