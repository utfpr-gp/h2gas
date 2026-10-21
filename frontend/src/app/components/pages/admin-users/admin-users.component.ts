import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { AdminService } from '../../../services/admin.service';
import { CityService } from '../../../services/city.service';
import { UserService } from '../../../services/user.service';
import { AdminUser, AdminUserCreate, UserType } from '../../../models/admin';
import { City } from '../../../models/city';
import { backendMessage } from '../../../utils/backend-message';
import { fadeIn } from '../../../animations/checkout.animations';

const PAGE_SIZE = 10;
const USER_TYPES: UserType[] = ['CUSTOMER', 'SUPPLIER', 'DELIVERER', 'ADMIN'];

// HU12: gestão de usuários pelo admin (listar, buscar, criar qualquer perfil, editar, desativar)
@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-users.component.html',
  animations: [fadeIn],
})
export class AdminUsersComponent implements OnInit {
  readonly chips: { type?: UserType; label: string }[] = [
    { label: 'Todos' },
    { type: 'CUSTOMER', label: 'Clientes' },
    { type: 'SUPPLIER', label: 'Fornecedores' },
    { type: 'DELIVERER', label: 'Entregadores' },
    { type: 'ADMIN', label: 'Admins' },
  ];
  readonly typeLabel: Record<UserType, string> = {
    CUSTOMER: 'Cliente', SUPPLIER: 'Fornecedor', DELIVERER: 'Entregador', ADMIN: 'Admin',
  };
  readonly userTypes = USER_TYPES;

  users: AdminUser[] = [];
  type?: UserType;
  query = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = true;
  readonly myId: string | undefined;

  toggleTarget?: AdminUser;
  toggling = false;

  editTarget?: AdminUser;
  saving = false;
  private readonly fb = inject(FormBuilder);
  readonly editForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    phoneNumber: ['', Validators.required],
  });

  creating = false;
  submitted = false;
  cities: City[] = [];
  suppliers: AdminUser[] = [];
  readonly createForm = this.fb.nonNullable.group({
    userType: ['CUSTOMER' as UserType, Validators.required],
    name: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    phoneNumber: ['', Validators.required],
    cpf: [''],
    cnpj: [''],
    cityId: [''],
    supplierId: [''],
  });

  private readonly search$ = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private adminService: AdminService,
    private userService: UserService,
    private cityService: CityService,
    private toastr: ToastrService,
    private route: ActivatedRoute,
  ) {
    this.myId = this.userService.getUserLocalStorage()?.id;
  }

  ngOnInit(): void {
    const perfil = this.route.snapshot.queryParamMap.get('perfil') as UserType | null;
    if (perfil && USER_TYPES.includes(perfil)) this.type = perfil;

    this.search$.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe((q) => {
      this.query = q;
      this.load(0);
    });
    this.createForm.controls.userType.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((type) => this.applyTypeRules(type));
    this.load(0);
  }

  load(page: number) {
    this.loading = true;
    this.adminService.listUsers({ type: this.type, q: this.query, page, size: PAGE_SIZE }).subscribe({
      next: (result) => {
        this.users = result.data;
        this.page = result.page;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar os usuários: ${backendMessage(err)}`);
      },
    });
  }

  selectType(type?: UserType) {
    this.type = type;
    this.load(0);
  }

  onSearch(value: string) {
    this.search$.next(value);
  }

  // ---- ativar/desativar ----
  askToggle(user: AdminUser) {
    this.toggleTarget = user;
  }

  closeToggle() {
    if (!this.toggling) this.toggleTarget = undefined;
  }

  confirmToggle() {
    const user = this.toggleTarget;
    if (!user) return;
    this.toggling = true;
    this.adminService.setEnabled(user.id, !user.enabled).subscribe({
      next: () => {
        user.enabled = !user.enabled;
        this.toggling = false;
        this.toggleTarget = undefined;
        this.toastr.success(user.enabled ? `${user.name} foi reativado.` : `${user.name} foi desativado.`);
      },
      error: (err) => {
        this.toggling = false;
        this.toastr.error(`Não foi possível alterar a conta: ${backendMessage(err)}`);
      },
    });
  }

  // ---- edição ----
  openEdit(user: AdminUser) {
    this.editTarget = user;
    this.editForm.reset({ name: user.name, email: user.email, phoneNumber: user.phoneNumber ?? '' });
  }

  closeEdit() {
    if (!this.saving) this.editTarget = undefined;
  }

  saveEdit() {
    const user = this.editTarget;
    if (!user) return;
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    const data = this.editForm.getRawValue();
    this.userService.updateUser(user.id, { id: user.id, ...data }).subscribe({
      next: () => {
        Object.assign(user, data);
        this.saving = false;
        this.editTarget = undefined;
        this.toastr.success('Dados salvos.');
      },
      error: (err) => {
        this.saving = false;
        this.toastr.error(`Não foi possível salvar: ${backendMessage(err)}`);
      },
    });
  }

  // ---- criação ----
  openCreate() {
    this.creating = true;
    this.submitted = false;
    this.createForm.reset();
    this.applyTypeRules(this.createForm.controls.userType.value);
  }

  closeCreate() {
    if (!this.saving) this.creating = false;
  }

  get newType(): UserType {
    return this.createForm.controls.userType.value;
  }

  invalid(control: keyof AdminUsersComponent['createForm']['controls']): boolean {
    const c = this.createForm.controls[control];
    return c.invalid && (c.touched || this.submitted);
  }

  saveCreate() {
    this.submitted = true;
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.adminService.createUser(this.createPayload()).subscribe({
      next: (created) => {
        this.saving = false;
        this.creating = false;
        this.toastr.success(`${created.name} foi criado como ${this.typeLabel[created.userType].toLowerCase()}.`);
        this.load(0);
      },
      error: (err) => {
        this.saving = false;
        this.toastr.error(`Não foi possível criar o usuário: ${backendMessage(err)}`);
      },
    });
  }

  private createPayload(): AdminUserCreate {
    const v = this.createForm.getRawValue();
    const payload: AdminUserCreate = {
      name: v.name, email: v.email, password: v.password, phoneNumber: v.phoneNumber, userType: v.userType,
    };
    if (v.cpf.trim()) payload.cpf = v.cpf.trim();
    if (v.userType === 'SUPPLIER') {
      payload.cnpj = v.cnpj.trim();
      payload.cityId = Number(v.cityId);
    }
    if (v.userType === 'DELIVERER') payload.supplierId = v.supplierId;
    return payload;
  }

  // o campo extra de cada perfil só é obrigatório para ele; as listas carregam quando o perfil aparece
  private applyTypeRules(type: UserType) {
    const { cnpj, cityId, supplierId } = this.createForm.controls;
    cnpj.setValidators(type === 'SUPPLIER' ? Validators.required : null);
    cityId.setValidators(type === 'SUPPLIER' ? Validators.required : null);
    supplierId.setValidators(type === 'DELIVERER' ? Validators.required : null);
    [cnpj, cityId, supplierId].forEach((c) => c.updateValueAndValidity({ emitEvent: false }));

    if (type === 'SUPPLIER' && !this.cities.length) {
      this.cityService.findAll().subscribe({
        next: (cities) => (this.cities = cities),
        error: (err) => this.toastr.error(`Erro ao carregar as cidades: ${backendMessage(err)}`),
      });
    }
    if (type === 'DELIVERER' && !this.suppliers.length) {
      this.adminService.listUsers({ type: 'SUPPLIER', size: 50 }).subscribe({
        next: (r) => (this.suppliers = r.data),
        error: (err) => this.toastr.error(`Erro ao carregar os fornecedores: ${backendMessage(err)}`),
      });
    }
  }
}
