import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { AdminService } from '../../../services/admin.service';
import { AdminDashboard, UserType } from '../../../models/admin';
import { backendMessage } from '../../../utils/backend-message';

// HU12: tela inicial do admin com os números da plataforma (todos calculados no backend)
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent implements OnInit {
  readonly userCards: { type: UserType; label: string; icon: string }[] = [
    { type: 'CUSTOMER', label: 'Clientes', icon: 'person' },
    { type: 'SUPPLIER', label: 'Fornecedores', icon: 'storefront' },
    { type: 'DELIVERER', label: 'Entregadores', icon: 'two_wheeler' },
    { type: 'ADMIN', label: 'Administradores', icon: 'admin_panel_settings' },
  ];
  readonly statuses = [
    { key: 'PENDING', label: 'Pendente' },
    { key: 'CONFIRMED', label: 'Confirmado' },
    { key: 'IN_PREPARATION', label: 'Em preparação' },
    { key: 'OUT_FOR_DELIVERY', label: 'Em rota' },
    { key: 'DELIVERED', label: 'Entregue' },
    { key: 'CANCELED', label: 'Cancelado' },
  ];
  dashboard?: AdminDashboard;
  loading = true;

  constructor(
    private adminService: AdminService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.adminService.dashboard().subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar o painel: ${backendMessage(err)}`);
      },
    });
  }

  get totalOrders(): number {
    return Object.values(this.dashboard?.ordersByStatus ?? {}).reduce((sum: number, n) => sum + (n ?? 0), 0);
  }

  statusPercent(key: string): number {
    const total = this.totalOrders;
    return total ? ((this.dashboard?.ordersByStatus[key] ?? 0) / total) * 100 : 0;
  }
}
