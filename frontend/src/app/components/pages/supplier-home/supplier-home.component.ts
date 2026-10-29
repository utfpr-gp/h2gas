import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SupplierService } from '../../../services/supplier.service';
import { ProductItemService } from '../../../services/product-item.service';
import { DelivererService } from '../../../services/deliverer.service';
import { UserService } from '../../../services/user.service';

// cada número carrega sozinho: undefined = carregando, null = falhou (o card mostra "—")
type Loadable<T> = T | null | undefined;

// F9: tela inicial do fornecedor — um card por área, cada um com o que importa agora.
// Usa os endpoints que as telas já usam; nada de rota nova no backend.
@Component({
  selector: 'app-supplier-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './supplier-home.component.html',
  styleUrl: './supplier-home.component.css',
  host: { class: 'flex-1 flex flex-col' },
})
export class SupplierHomeComponent implements OnInit {
  storeName = '';
  pendingOrders: Loadable<number>;
  outForDelivery: Loadable<number>;
  monthRevenue: Loadable<number>;
  stockAlerts: Loadable<number>;
  deliverers: Loadable<{ free: number; total: number }>;
  open: Loadable<boolean>;
  openingTime?: string;
  closingTime?: string;

  constructor(
    private supplierService: SupplierService,
    private productItemService: ProductItemService,
    private delivererService: DelivererService,
    private userService: UserService,
  ) {}

  ngOnInit(): void {
    const user = this.userService.getUserLocalStorage();
    this.storeName = user?.name ?? '';

    this.supplierService.getDashboard().subscribe({
      next: (d) => {
        this.pendingOrders = d.pendingOrders;
        this.outForDelivery = d.outForDeliveryOrders;
        this.monthRevenue = d.totalRevenue;
      },
      error: () => (this.pendingOrders = this.outForDelivery = this.monthRevenue = null),
    });
    this.productItemService.getStockSummary().subscribe({
      next: (s) => (this.stockAlerts = s.lowStock + s.outOfStock),
      error: () => (this.stockAlerts = null),
    });
    this.delivererService.listMine().subscribe({
      next: (list) => {
        const active = list.filter((d) => d.active);
        this.deliverers = { free: active.filter((d) => !d.busy).length, total: active.length };
      },
      error: () => (this.deliverers = null),
    });

    const supplierId = user?.supplierId;
    if (supplierId) {
      this.supplierService.getOpen(supplierId).subscribe({
        next: (open) => (this.open = !!open),
        error: () => (this.open = null),
      });
    } else {
      this.open = null;
    }
    this.supplierService.getMySettings().subscribe({
      next: (s) => {
        this.openingTime = s.openingTime?.slice(0, 5);
        this.closingTime = s.closingTime?.slice(0, 5);
      },
      // sem o horário, o card da loja ainda mostra se está aberta (vem de getOpen)
      error: () => (this.openingTime = this.closingTime = undefined),
    });
  }

  get storeHint(): string {
    if (this.open === true) return this.closingTime ? `fecha às ${this.closingTime}` : 'aberta agora';
    if (this.open === false) return this.openingTime ? `abre às ${this.openingTime}` : 'fora do horário';
    return 'sem horário definido';
  }
}
