import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { SupplierService } from '../../../services/supplier.service';
import { ProductItemService } from '../../../services/product-item.service';
import { CategoryProductService } from '../../../services/category-product.service';
import { ProductTypeService } from '../../../services/product-type.service';
import { DashboardFilter, SupplierDashboard } from '../../../models/supplier-dashboard';
import { ProductType } from '../../../models/product-type';
import { SupplierCategory } from '../../../models/supplier-category';
import { paymentLabel } from '../../../models/payment-label';
import { backendMessage } from '../../../utils/backend-message';
import { fadeIn } from '../../../animations/checkout.animations';

interface PieSlice {
  id?: number;
  name: string;
  percent: number;
  revenue: number;
  quantity: number;
  dasharray: string;
  dashoffset: number;
  strokeClass: string;
  dotClass: string;
}

// cores categóricas do tema (tokens --color-chart-*, ordem fixa validada); classes literais para o Tailwind gerar
const PIE_COLORS = [
  { stroke: 'text-chart-1', dot: 'bg-chart-1' },
  { stroke: 'text-chart-2', dot: 'bg-chart-2' },
  { stroke: 'text-chart-3', dot: 'bg-chart-3' },
  { stroke: 'text-chart-4', dot: 'bg-chart-4' },
  { stroke: 'text-chart-5', dot: 'bg-chart-5' },
];

interface MonthOption {
  value: string;
  label: string;
}

type FilterKey = keyof DashboardFilter;

// HU09 (Figura 13): visão geral do fornecedor, tela inicial após o login.
// F7 fatia 6: filtros no backend; clicar num pagamento, tipo, categoria, produto ou modalidade filtra o painel.
@Component({
  selector: 'app-supplier-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './supplier-dashboard.component.html',
  animations: [fadeIn],
})
export class SupplierDashboardComponent implements OnInit {
  readonly months: MonthOption[] = this.lastMonths(12);
  readonly paymentLabel = paymentLabel;
  readonly payments: NonNullable<DashboardFilter['payment']>[] = ['PIX', 'DINHEIRO', 'CREDIT_CARD'];
  readonly modeLabel: Record<'DELIVERY' | 'PICKUP', string> = { DELIVERY: 'Entrega', PICKUP: 'Retirada' };
  selectedMonth = this.months[0].value;
  dashboard?: SupplierDashboard;
  loading = true;
  filter: DashboardFilter = {};

  productTypes: ProductType[] = [];
  categories: SupplierCategory[] = [];
  products: { id: number; label: string }[] = [];

  constructor(
    private supplierService: SupplierService,
    private productItemService: ProductItemService,
    private categoryService: CategoryProductService,
    private productTypeService: ProductTypeService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.loadOptions();
    this.load();
  }

  changeMonth(month: string) {
    this.selectedMonth = month;
    this.load();
  }

  load() {
    this.loading = true;
    this.supplierService.getDashboard(this.selectedMonth, { ...this.filter }).subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar o dashboard: ${backendMessage(err)}`);
      },
    });
  }

  // listas dos selects: as da loja, para filtrar mesmo o que não vendeu no mês
  private loadOptions() {
    this.productTypeService.findAll().subscribe({ next: (types) => (this.productTypes = types), error: () => {} });
    this.categoryService.listMine().subscribe({ next: (categories) => (this.categories = categories), error: () => {} });
    this.productItemService.listMine({ size: 50 }).subscribe({
      next: (page) => (this.products = page.data.map((p) => ({ id: p.id!, label: `${p.brand} · ${p.category}` }))),
      error: () => {},
    });
  }

  // ---- filtros ----
  setFilter<K extends FilterKey>(key: K, value: DashboardFilter[K] | undefined) {
    const next: DashboardFilter = { ...this.filter };
    if (value == null || (value as unknown) === '') delete next[key];
    else next[key] = value;
    this.filter = next;
    this.load();
  }

  // clicar no item ativo tira o filtro
  toggleFilter<K extends FilterKey>(key: K, value: DashboardFilter[K]) {
    this.setFilter(key, this.filter[key] === value ? undefined : value);
  }

  selectFilter(key: FilterKey, raw: string) {
    const numeric = key === 'typeId' || key === 'categoryId' || key === 'productId';
    this.setFilter(key, (raw ? (numeric ? Number(raw) : raw) : undefined) as never);
  }

  isActive<K extends FilterKey>(key: K, value: DashboardFilter[K]): boolean {
    return this.filter[key] === value;
  }

  get hasFilter(): boolean {
    return Object.keys(this.filter).length > 0;
  }

  clearFilters() {
    this.filter = {};
    this.load();
  }

  get activeFilters(): { key: FilterKey; label: string }[] {
    const f = this.filter;
    const chips: { key: FilterKey; label: string }[] = [];
    if (f.payment) chips.push({ key: 'payment', label: paymentLabel(f.payment) });
    if (f.mode) chips.push({ key: 'mode', label: this.modeLabel[f.mode] });
    if (f.typeId != null) chips.push({ key: 'typeId', label: this.productTypes.find((t) => t.id === f.typeId)?.name
      ?? this.dashboard?.salesByCategory.find((c) => c.productTypeId === f.typeId)?.categoryName ?? 'Tipo' });
    if (f.categoryId != null) chips.push({ key: 'categoryId', label: this.categories.find((c) => c.id === f.categoryId)?.name
      ?? this.dashboard?.salesByStoreCategory?.find((c) => c.categoryId === f.categoryId)?.categoryName ?? 'Categoria' });
    if (f.productId != null) chips.push({ key: 'productId', label: this.products.find((p) => p.id === f.productId)?.label
      ?? this.dashboard?.salesByProduct.find((p) => p.productId === f.productId)?.productName ?? 'Produto' });
    return chips;
  }

  // ---- gráficos ----
  // pizza em SVG: círculo de raio 15,9155 tem circunferência 100, então o % vira o traço direto;
  // começa no topo (offset inicial 25) e cada fatia recua a soma das anteriores
  get categorySlices(): PieSlice[] {
    let accumulated = 0;
    return (this.dashboard?.salesByCategory ?? []).map((category, index) => {
      const color = PIE_COLORS[index % PIE_COLORS.length];
      const slice: PieSlice = {
        id: category.productTypeId,
        name: category.categoryName,
        percent: category.percent,
        revenue: category.revenue,
        quantity: category.quantity,
        dasharray: `${category.percent} ${100 - category.percent}`,
        dashoffset: 25 - accumulated,
        strokeClass: color.stroke,
        dotClass: color.dot,
      };
      accumulated += category.percent;
      return slice;
    });
  }

  get pieTotal(): number {
    return (this.dashboard?.salesByCategory ?? []).reduce((sum, c) => sum + c.revenue, 0);
  }

  get hasSales() {
    return (this.dashboard?.totalOrders ?? 0) > 0;
  }

  private get maxDayRevenue(): number {
    return Math.max(...(this.dashboard?.revenueByDay ?? []).map((p) => p.revenue), 0);
  }

  // altura de cada barra em % da maior venda do mês (a mesma escala do eixo)
  barHeight(revenue: number): number {
    const max = this.maxDayRevenue;
    return max > 0 ? (revenue / max) * 100 : 0;
  }

  get bestDay(): { day: string; revenue: number } | undefined {
    const days = this.dashboard?.revenueByDay ?? [];
    const max = this.maxDayRevenue;
    return max > 0 ? days.find((d) => d.revenue === max) : undefined;
  }

  isBestDay(point: { day: string }): boolean {
    return this.bestDay?.day === point.day;
  }

  // marcas do eixo: topo, meio e zero, nos valores que o gráfico de fato alcança
  get axisTicks(): { value: number; bottom: number }[] {
    const max = this.maxDayRevenue;
    return max > 0 ? [{ value: max, bottom: 100 }, { value: max / 2, bottom: 50 }, { value: 0, bottom: 0 }] : [];
  }

  // largura das barras horizontais, em % do maior da lista
  widthOf(value: number, values: number[]): number {
    const max = Math.max(...values, 0);
    return max > 0 ? (value / max) * 100 : 0;
  }

  get productQuantities(): number[] {
    return (this.dashboard?.salesByProduct ?? []).map((p) => p.quantity);
  }

  get storeCategoryRevenues(): number[] {
    return (this.dashboard?.salesByStoreCategory ?? []).map((c) => c.revenue);
  }

  trackByDay(_: number, point: { day: string }) {
    return point.day;
  }

  // do mês corrente para trás; o backend recusa mês futuro
  private lastMonths(count: number): MonthOption[] {
    const options: MonthOption[] = [];
    const now = new Date();
    for (let i = 0; i < count; i++) {
      const date = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const label = date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
      options.push({
        value: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`,
        label: label.charAt(0).toUpperCase() + label.slice(1),
      });
    }
    return options;
  }
}
