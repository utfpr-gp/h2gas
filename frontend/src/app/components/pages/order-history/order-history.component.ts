import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { OrderService } from '../../../services/order.service';
import { SupplierService } from '../../../services/supplier.service';
import { UserService } from '../../../services/user.service';
import { DeliveryMode, OrderResponse } from '../../../models/order.model';
import { HistoryStatus, OrderHistoryFilter, OrderHistorySummary, PaymentMethod } from '../../../models/order-history';
import { paymentLabel } from '../../../models/payment-label';
import { backendMessage } from '../../../utils/backend-message';
import { fadeIn } from '../../../animations/checkout.animations';

const PAGE_SIZE = 10;

type Period = '7d' | '30d' | 'month' | 'lastMonth' | 'all' | 'custom';

const iso = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

// F7 fatia 6: histórico de pedidos finalizados do fornecedor; filtros e números vêm do backend
@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-history.component.html',
  host: { class: 'flex-1 flex flex-col' },
  animations: [fadeIn],
})
export class OrderHistoryComponent implements OnInit {
  readonly periods: { value: Period; label: string }[] = [
    { value: '7d', label: 'Últimos 7 dias' },
    { value: '30d', label: 'Últimos 30 dias' },
    { value: 'month', label: 'Este mês' },
    { value: 'lastMonth', label: 'Mês passado' },
    { value: 'all', label: 'Todo o período' },
    { value: 'custom', label: 'Personalizado' },
  ];
  readonly statusChips: { value?: HistoryStatus; label: string }[] = [
    { label: 'Todos' }, { value: 'delivered', label: 'Entregues' }, { value: 'canceled', label: 'Cancelados' },
  ];
  readonly payments: PaymentMethod[] = ['PIX', 'DINHEIRO', 'CREDIT_CARD'];
  readonly paymentLabel = paymentLabel;
  readonly modeLabel: Record<DeliveryMode, string> = { DELIVERY: 'Entrega', PICKUP: 'Retirada' };

  period: Period = '30d';
  customFrom = '';
  customTo = '';
  status?: HistoryStatus;
  payment?: PaymentMethod;
  mode?: DeliveryMode;
  query = '';

  orders: OrderResponse[] = [];
  summary?: OrderHistorySummary;
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = true;
  expanded = new Set<string>();

  storeOpen = false;
  private supplierId?: string;
  private readonly search$ = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private orderService: OrderService,
    private supplierService: SupplierService,
    private userService: UserService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.supplierId = this.userService.getUserLocalStorage()?.supplierId;
    if (this.supplierId) {
      this.supplierService.getOpen(this.supplierId).subscribe({
        next: (open) => (this.storeOpen = !!open),
        error: (err) => this.toastr.error(`Erro ao ler se a loja está aberta: ${backendMessage(err)}`),
      });
    }
    this.search$.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe((q) => {
      this.query = q;
      this.reload();
    });
    this.reload();
  }

  // período relativo a hoje; "personalizado" usa as datas digitadas
  private range(): { from?: string; to?: string } {
    const today = new Date();
    const daysAgo = (n: number) => new Date(today.getFullYear(), today.getMonth(), today.getDate() - n);
    switch (this.period) {
      case '7d': return { from: iso(daysAgo(6)), to: iso(today) };
      case '30d': return { from: iso(daysAgo(29)), to: iso(today) };
      case 'month': return { from: iso(new Date(today.getFullYear(), today.getMonth(), 1)), to: iso(today) };
      case 'lastMonth': return {
        from: iso(new Date(today.getFullYear(), today.getMonth() - 1, 1)),
        to: iso(new Date(today.getFullYear(), today.getMonth(), 0)),
      };
      case 'custom': return { from: this.customFrom || undefined, to: this.customTo || undefined };
      default: return {};
    }
  }

  private filter(): OrderHistoryFilter {
    return { ...this.range(), status: this.status, payment: this.payment, mode: this.mode, q: this.query || undefined };
  }

  reload() {
    this.load(0);
    this.orderService.getMyHistorySummary(this.filter()).subscribe({
      next: (summary) => (this.summary = summary),
      error: (err) => this.toastr.error(`Erro ao carregar o resumo: ${backendMessage(err)}`),
    });
  }

  load(page: number) {
    this.loading = true;
    this.orderService.listMyHistory({ ...this.filter(), page, size: PAGE_SIZE }).subscribe({
      next: (result) => {
        this.orders = result.data;
        this.page = result.page;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar o histórico: ${backendMessage(err)}`);
      },
    });
  }

  // ---- filtros ----
  selectStatus(status?: HistoryStatus) { this.status = status; this.reload(); }
  selectPeriod(value: string) { this.period = value as Period; if (this.period !== 'custom') this.reload(); }
  setCustom(which: 'from' | 'to', value: string) {
    if (which === 'from') this.customFrom = value; else this.customTo = value;
    if (this.customFrom && this.customTo && this.customFrom > this.customTo) return;
    this.reload();
  }
  selectPayment(value: string) { this.payment = (value || undefined) as PaymentMethod | undefined; this.reload(); }
  selectMode(value: string) { this.mode = (value || undefined) as DeliveryMode | undefined; this.reload(); }
  onSearch(value: string) { this.search$.next(value.trim()); }

  get invalidCustomRange(): boolean {
    return this.period === 'custom' && !!this.customFrom && !!this.customTo && this.customFrom > this.customTo;
  }

  get activeFilters(): { key: 'status' | 'payment' | 'mode' | 'query'; label: string }[] {
    const chips: { key: 'status' | 'payment' | 'mode' | 'query'; label: string }[] = [];
    if (this.status) chips.push({ key: 'status', label: this.status === 'delivered' ? 'Entregues' : 'Cancelados' });
    if (this.payment) chips.push({ key: 'payment', label: paymentLabel(this.payment) });
    if (this.mode) chips.push({ key: 'mode', label: this.modeLabel[this.mode] });
    if (this.query) chips.push({ key: 'query', label: `"${this.query}"` });
    return chips;
  }

  removeFilter(key: 'status' | 'payment' | 'mode' | 'query') {
    if (key === 'query') {
      this.query = '';
      const input = document.getElementById('history-search') as HTMLInputElement | null;
      if (input) input.value = '';
    } else {
      this[key] = undefined;
    }
    this.reload();
  }

  clearFilters() {
    this.status = this.payment = this.mode = undefined;
    this.query = '';
    this.period = '30d';
    const input = document.getElementById('history-search') as HTMLInputElement | null;
    if (input) input.value = '';
    this.reload();
  }

  // ---- cards ----
  shortCode(o: OrderResponse): string {
    return '#' + (o.id ?? '').slice(0, 8).toUpperCase();
  }

  isDelivered(o: OrderResponse): boolean {
    return String(o.status) === 'Entregue' || String(o.status) === 'DELIVERED';
  }

  address(o: OrderResponse): string {
    const street = [o.streetName, o.number].filter(Boolean).join(', ');
    return [street, o.neighborhood, o.cityName].filter(Boolean).join(' · ');
  }

  toggle(o: OrderResponse) {
    if (this.expanded.has(o.id)) this.expanded.delete(o.id);
    else this.expanded.add(o.id);
  }

  // ---- loja aberta/fechada (fica aqui e em Pedidos recebidos) ----
  toggleStore(event: Event) {
    const open = (event.target as HTMLInputElement).checked;
    if (!this.supplierId) return;
    this.supplierService.openDistribuidor({ status: open }, this.supplierId).subscribe({
      next: () => {
        this.storeOpen = open;
        this.toastr.success(open ? 'Loja aberta.' : 'Loja fechada.');
      },
      error: (err) => {
        (event.target as HTMLInputElement).checked = this.storeOpen;
        this.toastr.error(`Não foi possível ${open ? 'abrir' : 'fechar'} a loja: ${backendMessage(err)}`);
      },
    });
  }
}
