import { PhonePipe } from '../../../utils/phone.pipe';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { Supplier } from '../../../models/supplier';
import { SupplierService } from '../../../services/supplier.service';
import { backendMessage } from '../../../utils/backend-message';
import { fadeIn } from '../../../animations/checkout.animations';

const PAGE_SIZE = 10;

type StatusFilter = 'pending' | 'active' | 'all';

// F7 fatia 6: aprovação de fornecedores pelo admin (filtro por status, busca, confirmação)
@Component({
  selector: 'app-supplier-approval',
  standalone: true,
  imports: [PhonePipe, CommonModule],
  templateUrl: './supplier-approval.component.html',
  host: { class: 'flex-1 flex flex-col' },
  animations: [fadeIn],
})
export class SupplierApprovalComponent implements OnInit {
  readonly chips: { status: StatusFilter; label: string }[] = [
    { status: 'pending', label: 'Aguardando aprovação' },
    { status: 'active', label: 'Ativos' },
    { status: 'all', label: 'Todos' },
  ];
  readonly emptyMessage: Record<StatusFilter, string> = {
    pending: 'Nenhum fornecedor aguardando aprovação',
    active: 'Nenhum fornecedor ativo',
    all: 'Nenhum fornecedor cadastrado',
  };

  suppliers: Supplier[] = [];
  status: StatusFilter = 'pending';
  query = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = true;

  toggleTarget?: Supplier;
  toggling = false;

  private readonly search$ = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.search$.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe((q) => {
      this.query = q;
      this.load(0);
    });
    this.load(0);
  }

  load(page: number) {
    this.loading = true;
    const active = this.status === 'all' ? undefined : this.status === 'active';
    this.supplierService.listForApproval({ active, q: this.query, page, size: PAGE_SIZE }).subscribe({
      next: (result) => {
        this.suppliers = result.data;
        this.page = result.page;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar os fornecedores: ${backendMessage(err)}`);
      },
    });
  }

  selectStatus(status: StatusFilter) {
    this.status = status;
    this.load(0);
  }

  onSearch(value: string) {
    this.search$.next(value);
  }

  address(s: Supplier): string {
    const street = [s.addressStreetName, s.addressNumber].filter(Boolean).join(', ');
    return [s.cityName, s.addressNeighborhood, street].filter(Boolean).join(' · ');
  }

  askToggle(supplier: Supplier) {
    this.toggleTarget = supplier;
  }

  closeToggle() {
    if (!this.toggling) this.toggleTarget = undefined;
  }

  confirmToggle() {
    const supplier = this.toggleTarget;
    if (!supplier) return;
    const approve = !supplier.active;
    this.toggling = true;
    this.supplierService.activeSuppliers({ active: approve }, supplier.id!).subscribe({
      next: () => {
        this.toggling = false;
        this.toggleTarget = undefined;
        this.toastr.success(approve
          ? `${supplier.name} foi aprovado e já aparece para os clientes.`
          : `${supplier.name} foi desativado e saiu do catálogo.`);
        this.load(this.page);
      },
      error: (err) => {
        this.toggling = false;
        this.toastr.error(`Não foi possível ${approve ? 'aprovar' : 'desativar'} ${supplier.name}: ${backendMessage(err)}`);
      },
    });
  }
}
