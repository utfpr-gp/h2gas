import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Supplier } from '../../models/supplier';
import { StoreRatingComponent } from '../store-rating/store-rating.component';

@Component({
  selector: 'app-supplier-card',
  standalone: true,
  imports: [CommonModule, StoreRatingComponent],
  templateUrl: './supplier-card.component.html'
})
export class SupplierCardComponent {
  @Input() supplier!: Supplier;
  @Output() viewProducts = new EventEmitter<string>();
  // F7 §5.27: estrelas abrem as avaliações da loja
  @Output() viewReviews = new EventEmitter<Supplier>();
  logoBroken = false;

  onViewProducts() {
    this.viewProducts.emit(this.supplier.id);
  }

  // HU17: o que a linha de entrega diz — só exibe o que o backend calculou (RN01)
  get deliveryLine(): { icon: string; iconClass: string; text: string; hint?: string } | null {
    const s = this.supplier;
    if (s.offersDelivery === false || s.deliversHere === false) {
      return { icon: 'storefront', iconClass: 'text-neutral', text: 'Só retirada',
        hint: s.offersDelivery === false ? undefined : 'fora da área de entrega' };
    }
    if (s.freeDelivery) {
      return { icon: 'local_shipping', iconClass: 'text-success', text: 'Entrega grátis' };
    }
    if (s.deliveryFee != null) {
      return { icon: 'local_shipping', iconClass: 'text-accent', text: `Entrega ${BRL.format(s.deliveryFee)}`,
        hint: s.distanceKm == null ? 'distância não calculada' : undefined };
    }
    return null;
  }
}

const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
