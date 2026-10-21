import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { ProductTypeService } from '../../../services/product-type.service';
import { CategoryProductService } from '../../../services/category-product.service';
import { DEFAULT_PRODUCT_TYPE_ICON, ProductType } from '../../../models/product-type';
import { CategoryRequest, SupplierCategory } from '../../../models/supplier-category';
import { backendMessage } from '../../../utils/backend-message';
import { fadeIn } from '../../../animations/checkout.animations';

interface CategoryGroup { productTypeId: number; type: string; icon: string; categories: SupplierCategory[]; }

// F7 fatia 6: categorias da loja do fornecedor, agrupadas pelo tipo de produto (mantido pelo admin)
@Component({
  selector: 'app-category-products',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './category-products.component.html',
  host: { class: 'flex-1 flex flex-col' },
  animations: [fadeIn],
})
export class CategoryProductsComponent implements OnInit {
  categories: SupplierCategory[] = [];
  productTypes: ProductType[] = [];
  loading = true;

  editing?: SupplierCategory;
  formOpen = false;
  saving = false;
  private readonly fb = inject(FormBuilder);
  readonly form = this.fb.nonNullable.group({
    productTypeId: ['', Validators.required],
    name: ['', [Validators.required, Validators.maxLength(60), Validators.pattern(/\S/)]],
  });

  deleteTarget?: SupplierCategory;
  deleting = false;

  constructor(
    private categoryService: CategoryProductService,
    private productTypeService: ProductTypeService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.productTypeService.findAll().subscribe({
      next: (types) => (this.productTypes = types),
      error: (err) => this.toastr.error(`Erro ao carregar os tipos de produto: ${backendMessage(err)}`),
    });
    this.load();
  }

  load() {
    this.loading = true;
    this.categoryService.listMine().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar as categorias: ${backendMessage(err)}`);
      },
    });
  }

  // o backend já ordena por tipo e nome
  get groups(): CategoryGroup[] {
    const groups: CategoryGroup[] = [];
    for (const c of this.categories) {
      let group = groups.find((g) => g.productTypeId === c.productTypeId);
      if (!group) {
        group = { productTypeId: c.productTypeId, type: c.type, icon: c.typeIcon || DEFAULT_PRODUCT_TYPE_ICON, categories: [] };
        groups.push(group);
      }
      group.categories.push(c);
    }
    return groups;
  }

  countLabel(c: SupplierCategory): string {
    if (!c.productCount) return 'Sem produtos';
    return `${c.productCount} ${c.productCount === 1 ? 'produto' : 'produtos'}`;
  }

  // ---- criar / editar ----
  openCreate() {
    this.editing = undefined;
    this.form.reset({ productTypeId: '', name: '' });
    this.formOpen = true;
  }

  openEdit(c: SupplierCategory) {
    this.editing = c;
    this.form.reset({ productTypeId: String(c.productTypeId), name: c.name });
    this.formOpen = true;
  }

  closeForm() {
    if (!this.saving) this.formOpen = false;
  }

  invalid(control: 'productTypeId' | 'name'): boolean {
    const c = this.form.controls[control];
    return c.invalid && (c.touched || c.dirty);
  }

  private request(): CategoryRequest {
    const v = this.form.getRawValue();
    return { name: v.name.trim(), productTypeId: Number(v.productTypeId) };
  }

  get canSave(): boolean {
    if (this.form.invalid || this.saving) return false;
    const r = this.request();
    return !this.editing || r.name !== this.editing.name || r.productTypeId !== this.editing.productTypeId;
  }

  save() {
    if (!this.canSave) {
      this.form.markAllAsTouched();
      return;
    }
    const request = this.request();
    const editing = this.editing;
    this.saving = true;
    const call = editing ? this.categoryService.updateCategory(editing.id, request) : this.categoryService.createCategory(request);
    call.subscribe({
      next: () => {
        this.saving = false;
        this.formOpen = false;
        this.toastr.success(editing ? `Categoria ${request.name} salva.` : `Categoria ${request.name} criada.`);
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.toastr.error(`Não foi possível salvar a categoria: ${backendMessage(err)}`);
      },
    });
  }

  // ---- excluir ----
  askDelete(c: SupplierCategory) {
    this.deleteTarget = c;
  }

  closeDelete() {
    if (!this.deleting) this.deleteTarget = undefined;
  }

  confirmDelete() {
    const c = this.deleteTarget;
    if (!c || c.productCount > 0) return;
    this.deleting = true;
    this.categoryService.delete(c.id).subscribe({
      next: () => {
        this.deleting = false;
        this.deleteTarget = undefined;
        this.toastr.success(`Categoria ${c.name} excluída.`);
        this.load();
      },
      error: (err) => {
        this.deleting = false;
        this.deleteTarget = undefined;
        this.toastr.error(`Não foi possível excluir: ${backendMessage(err)}`);
      },
    });
  }
}
