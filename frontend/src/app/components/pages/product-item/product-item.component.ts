import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';
import { ProductStockSummary, StockFilter } from '../../../models/product-stock-summary';
import { backendMessage } from '../../../utils/backend-message';
import { fadeIn } from '../../../animations/checkout.animations';

const PAGE_SIZE = 10;

type StockStatus = 'available' | 'low' | 'out';

// F7 fatia 6: produtos do fornecedor logado; contadores e filtros calculados no backend
@Component({
  selector: 'app-product-item',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './product-item.component.html',
  host: { class: 'flex-1 flex flex-col' },
  animations: [fadeIn],
})
export class ProductItemComponent implements OnInit {
  readonly emptyMessage: Record<StockFilter, string> = {
    all: 'Nenhum produto cadastrado',
    available: 'Nenhum produto disponível',
    low: 'Nenhum produto com estoque baixo',
    out: 'Nenhum produto esgotado',
  };
  readonly statusLabel: Record<StockStatus, string> = {
    available: 'Disponível', low: 'Estoque baixo', out: 'Esgotado',
  };

  products: ProductItem[] = [];
  summary?: ProductStockSummary;
  stock: StockFilter = 'all';
  query = '';
  page = 0;
  totalPages = 0;
  loading = true;
  brokenImages = new Set<number>();

  deleteTarget?: ProductItem;
  deleting = false;

  private readonly search$ = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private productItemService: ProductItemService,
    private toastr: ToastrService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.search$.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe((q) => {
      this.query = q;
      this.load(0);
    });
    this.loadSummary();
    this.load(0);
  }

  get filters(): { stock: StockFilter; label: string; count?: number; hint?: string; icon: string }[] {
    const s = this.summary;
    return [
      { stock: 'all', label: 'Todos', count: s?.total, icon: 'inventory_2' },
      { stock: 'available', label: 'Disponíveis', count: s?.available, icon: 'check_circle' },
      { stock: 'low', label: 'Estoque baixo', count: s?.lowStock, icon: 'warning', hint: s ? `até ${s.lowStockThreshold} un.` : undefined },
      { stock: 'out', label: 'Esgotados', count: s?.outOfStock, icon: 'block' },
    ];
  }

  loadSummary() {
    this.productItemService.getStockSummary().subscribe({
      next: (summary) => (this.summary = summary),
      error: (err) => this.toastr.error(`Erro ao carregar o resumo do estoque: ${backendMessage(err)}`),
    });
  }

  load(page: number) {
    this.loading = true;
    this.productItemService.listMine({ q: this.query, stock: this.stock, page, size: PAGE_SIZE }).subscribe({
      next: (result) => {
        this.products = result.data;
        this.page = result.page;
        this.totalPages = result.totalPages;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar os produtos: ${backendMessage(err)}`);
      },
    });
  }

  selectStock(stock: StockFilter) {
    this.stock = stock;
    this.load(0);
  }

  onSearch(value: string) {
    this.search$.next(value);
  }

  status(p: ProductItem): StockStatus {
    const qty = p.stockQuantity ?? 0;
    if (qty <= 0) return 'out';
    return qty <= (this.summary?.lowStockThreshold ?? 8) ? 'low' : 'available';
  }

  name(p: ProductItem): string {
    return [p.brand, p.category].filter(Boolean).join(' · ');
  }

  // sem foto (ou foto quebrada) mostra o ícone do tipo
  typeIcon(p: ProductItem): string {
    const type = (p.type ?? '').toLowerCase();
    if (type.includes('gás') || type.includes('gas')) return 'local_fire_department';
    if (type.includes('água') || type.includes('agua')) return 'water_drop';
    return 'inventory_2';
  }

  edit(p: ProductItem) {
    this.router.navigate(['/minha-loja/produtos', p.id, 'editar']);
  }

  askDelete(p: ProductItem) {
    this.deleteTarget = p;
  }

  closeDelete() {
    if (!this.deleting) this.deleteTarget = undefined;
  }

  confirmDelete() {
    const p = this.deleteTarget;
    if (!p?.id) return;
    this.deleting = true;
    this.productItemService.delete(p.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteTarget = undefined;
        this.toastr.success(`${this.name(p)} foi excluído.`);
        this.loadSummary();
        this.load(this.products.length === 1 && this.page > 0 ? this.page - 1 : this.page);
      },
      error: (err) => {
        this.deleting = false;
        this.deleteTarget = undefined;
        this.toastr.error(`Não foi possível excluir: ${backendMessage(err)}`);
      },
    });
  }
}
