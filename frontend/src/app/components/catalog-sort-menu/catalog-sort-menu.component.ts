import { Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface SortOption {
  value: string;
  label: string;
}

// HU16: botão "Ordenar" único, à direita da vitrine; as opções mudam com a visão (lojas ou produtos)
@Component({
  selector: 'app-catalog-sort-menu',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './catalog-sort-menu.component.html'
})
export class CatalogSortMenuComponent {
  @Input() options: SortOption[] = [];
  @Input() value = '';
  @Output() valueChange = new EventEmitter<string>();
  open = false;

  constructor(private host: ElementRef<HTMLElement>) {}

  get activeLabel(): string {
    return this.options.find((o) => o.value === this.value)?.label ?? '';
  }

  toggle() {
    this.open = !this.open;
  }

  choose(value: string) {
    this.open = false;
    if (value !== this.value) this.valueChange.emit(value);
  }

  @HostListener('document:click', ['$event'])
  closeOnOutsideClick(event: Event) {
    if (this.open && !this.host.nativeElement.contains(event.target as Node)) this.open = false;
  }

  @HostListener('document:keydown.escape')
  closeOnEscape() {
    this.open = false;
  }
}
