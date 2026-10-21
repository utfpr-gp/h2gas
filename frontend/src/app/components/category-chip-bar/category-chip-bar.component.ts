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
  @Output() categorySelected = new EventEmitter<string | null>();

  select(name: string | null) {
    this.categorySelected.emit(name);
  }

  iconFor(type: ProductType): string {
    return type.icon || DEFAULT_PRODUCT_TYPE_ICON;
  }

}
