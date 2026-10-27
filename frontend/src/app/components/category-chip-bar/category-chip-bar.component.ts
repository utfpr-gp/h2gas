import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DEFAULT_PRODUCT_TYPE_ICON, ProductType } from '../../models/product-type';

@Component({
  selector: 'app-category-chip-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './category-chip-bar.component.html'
})
export class CategoryChipBarComponent {
  @Input() categories: ProductType[] = [];
  @Input() selected: string | null = null;
  // HU15: emite o tipo inteiro — a lista de lojas filtra por id, a de produtos por nome
  @Output() categorySelected = new EventEmitter<ProductType | null>();

  select(type: ProductType | null) {
    this.categorySelected.emit(type);
  }

  iconFor(type: ProductType): string {
    return type.icon || DEFAULT_PRODUCT_TYPE_ICON;
  }

}
