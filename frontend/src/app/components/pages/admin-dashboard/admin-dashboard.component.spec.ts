import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ToastrService } from 'ngx-toastr';

import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminService } from '../../../services/admin.service';
import { AdminDashboard } from '../../../models/admin';

registerLocaleData(localePt);

describe('AdminDashboardComponent', () => {
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let adminServiceSpy: jasmine.SpyObj<AdminService>;
  let toastrSpy: jasmine.SpyObj<ToastrService>;

  const painel: AdminDashboard = {
    usersByType: { ADMIN: 1, CUSTOMER: 120, SUPPLIER: 8, DELIVERER: 14 },
    activeSuppliers: 6,
    pendingSuppliers: 2,
    cities: 4,
    ordersToday: 9,
    ordersThisMonth: 210,
    ordersByStatus: { PENDING: 3, CONFIRMED: 1, IN_PREPARATION: 0, OUT_FOR_DELIVERY: 2, DELIVERED: 180, CANCELED: 12 },
    revenueThisMonth: 18450,
  };

  const byId = (id: string) => fixture.nativeElement.querySelector(`[data-testid=${id}]`) as HTMLElement | null;
  const texto = (id: string) => byId(id)?.textContent?.replace(/\s+/g, ' ').trim();

  const setup = async (response = of(painel)) => {
    adminServiceSpy = jasmine.createSpyObj('AdminService', ['dashboard']);
    adminServiceSpy.dashboard.and.returnValue(response);
    toastrSpy = jasmine.createSpyObj('ToastrService', ['error']);

    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [
        provideNoopAnimations(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'pt-BR' },
        { provide: AdminService, useValue: adminServiceSpy },
        { provide: ToastrService, useValue: toastrSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    fixture.detectChanges();
  };

  it('deveMostrarOsNumerosDaPlataforma', async () => {
    await setup();

    expect(texto('kpi-users-CUSTOMER')).toContain('120');
    expect(texto('kpi-users-DELIVERER')).toContain('14');
    expect(texto('kpi-active-suppliers')).toContain('6');
    expect(texto('kpi-cities')).toContain('4');
    expect(texto('kpi-orders-today')).toContain('9');
    expect(texto('kpi-orders-month')).toContain('210');
    expect(texto('kpi-revenue')).toContain('18.450,00');
    expect(texto('status-DELIVERED')).toContain('Entregue');
    expect(texto('status-DELIVERED')).toContain('180');
    expect(texto('status-CANCELED')).toContain('12');
  });

  it('deveLevarAosFornecedoresAguardandoAprovacao', async () => {
    await setup();

    const card = byId('kpi-pending-suppliers') as HTMLAnchorElement;
    expect(card.textContent).toContain('2');
    expect(card.getAttribute('href')).toBe('/admin/fornecedores');
  });

  it('deveMostrarAMensagemDoBackendNoErro', async () => {
    await setup(throwError(() => ({ status: 403, error: { status: 403, message: 'Você não tem permissão para acessar este recurso.' } })));

    expect(toastrSpy.error).toHaveBeenCalledWith(jasmine.stringContaining('Você não tem permissão para acessar este recurso.'));
    expect(byId('kpi-revenue')).toBeNull();
  });
});
