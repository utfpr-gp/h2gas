import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { SupplierService } from '../../../services/supplier.service';
import { SupplierDashboard } from '../../../models/supplier-dashboard';
import { paymentLabel } from '../../../models/payment-label';
import { fadeIn } from '../../../animations/checkout.animations';

interface MonthOption {
  value: string;
  label: string;
}

// HU09 (Figura 13): visão geral do fornecedor, tela inicial após o login
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
  selectedMonth = this.months[0].value;
  dashboard?: SupplierDashboard;
  loading = true;

  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  changeMonth(month: string) {
    this.selectedMonth = month;
    this.load();
  }

  load() {
    this.loading = true;
    this.supplierService.getDashboard(this.selectedMonth).subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar o dashboard: ${err?.error?.error || err?.error?.message || err?.message || 'erro inesperado'}`);
      },
    });
  }

  get hasSales() {
    return (this.dashboard?.totalOrders ?? 0) > 0;
  }

  // altura de cada barra em % da maior venda do mês
  barHeight(revenue: number): number {
    const max = Math.max(...(this.dashboard?.revenueByDay ?? []).map((p) => p.revenue), 0);
    return max > 0 ? (revenue / max) * 100 : 0;
  }

  // largura da barra de progresso de "Vendas por Produto", em % da mais vendida
  productWidth(quantity: number): number {
    const max = Math.max(...(this.dashboard?.salesByProduct ?? []).map((p) => p.quantity), 0);
    return max > 0 ? (quantity / max) * 100 : 0;
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
