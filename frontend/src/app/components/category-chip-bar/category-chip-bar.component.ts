import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductType } from '../../models/product-type';

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

  iconFor(name?: string): string {
    const normalized = (name ?? '').toLowerCase();
    if (normalized.includes('gás') || normalized.includes('gas')) {
      return 'local_fire_department';
    }
    if (normalized.includes('água') || normalized.includes('agua')) {
      return 'water_drop';
    }
    return 'category';
  }
}
