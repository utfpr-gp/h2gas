import { Component, EventEmitter, Inject, Input, LOCALE_ID, Output } from '@angular/core';
import { CommonModule, formatNumber } from '@angular/common';

// F7 §5.25/§5.26: nota da loja (média com 1 casa e contagem), no card do catálogo e no cabeçalho da loja
@Component({
  selector: 'app-store-rating',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './store-rating.component.html'
})
export class StoreRatingComponent {
  @Input() average?: number | null;
  @Input() count?: number | null;
  // tamanho do texto: o cabeçalho da loja usa um pouco maior que o card
  @Input() size: 'xs' | 'sm' = 'xs';
  // F7 §5.27: com nota, vira botão que abre a lista de avaliações
  @Input() clickable = false;
  @Output() openReviews = new EventEmitter<void>();

  constructor(@Inject(LOCALE_ID) private locale: string) {}

  get hasRating(): boolean {
    return this.average != null && (this.count ?? 0) > 0;
  }

  get ratingLabel(): string {
    return formatNumber(this.average ?? 0, this.locale, '1.1-1');
  }

  get ariaLabel(): string | null {
    if (!this.hasRating) {
      return null;
    }
    const total = this.count!;
    return `Nota ${this.ratingLabel} de 5, ${total} ${total === 1 ? 'avaliação' : 'avaliações'}`;
  }

  get buttonAriaLabel(): string {
    const total = this.count!;
    const quais = total === 1 ? 'Ver 1 avaliação' : `Ver as ${total} avaliações`;
    return `${quais} da loja, nota ${this.ratingLabel} de 5`;
  }

  // no card do catálogo o clique nas estrelas não pode abrir os produtos
  onOpen(event: Event) {
    event.stopPropagation();
    this.openReviews.emit();
  }
}
