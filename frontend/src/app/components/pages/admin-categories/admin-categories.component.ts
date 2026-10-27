import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { CategoryProductService } from '../../../services/category-product.service';
import { ProductTypeService } from '../../../services/product-type.service';
import { CategoryProduct } from '../../../models/category-product';
import { DEFAULT_PRODUCT_TYPE_ICON, ProductType } from '../../../models/product-type';
import { backendMessage } from '../../../utils/backend-message';

// tamanhos comuns por tipo; outros tipos ficam sem sugestão
const NAME_SUGGESTIONS: { match: string[]; names: string[] }[] = [
  { match: ['gas', 'gás'], names: ['P2', 'P5', 'P8', 'P13', 'P20', 'P45'] },
  { match: ['agua', 'água'], names: ['5L', '10L', '20L'] },
];

interface CategoryGroup { type: ProductType; categories: CategoryProduct[]; }

// HU19: o admin mantém as categorias (P13, P45, Galão 20L…), globais e agrupadas por tipo; o fornecedor só escolhe
@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-categories.component.html',
})
export class AdminCategoriesComponent implements OnInit {
  private readonly categoryService = inject(CategoryProductService);
  private readonly productTypeService = inject(ProductTypeService);
  private readonly toastr = inject(ToastrService);
  private readonly fb = inject(FormBuilder);

  types: ProductType[] = [];
  categories: CategoryProduct[] = [];
  loading = true;
  creating = false;
  readonly newForm = this.fb.nonNullable.group({
    productTypeId: [0, [Validators.min(1)]],
    name: ['', [Validators.required, Validators.maxLength(60)]],
  });

  renamingId?: number;
  renaming = false;
  readonly renameForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(60)]],
  });

  confirmingDeleteId?: number;

  ngOnInit(): void {
    this.productTypeService.findAll().subscribe({
      next: (types) => (this.types = types),
      error: (err) => this.toastr.error(`Erro ao carregar os tipos: ${backendMessage(err)}`),
    });
    this.categoryService.findAll().subscribe({
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

  get groups(): CategoryGroup[] {
    return this.types
      .map((type) => ({ type, categories: this.categories.filter((c) => c.productTypeId === type.id) }))
      .filter((group) => group.categories.length > 0);
  }

  iconOf(type: ProductType): string {
    return type.icon ?? DEFAULT_PRODUCT_TYPE_ICON;
  }

  get suggestions(): string[] {
    const type = this.types.find((t) => t.id === Number(this.newForm.controls.productTypeId.value));
    const name = (type?.name ?? '').toLowerCase();
    return NAME_SUGGESTIONS.find((s) => s.match.some((m) => name.includes(m)))?.names ?? [];
  }

  useSuggestion(name: string): void {
    this.newForm.controls.name.setValue(name);
  }

  create(): void {
    const name = this.newForm.controls.name.value.trim();
    const productTypeId = Number(this.newForm.controls.productTypeId.value);
    if (!name || !productTypeId || this.creating) {
      this.newForm.markAllAsTouched();
      return;
    }
    this.creating = true;
    this.categoryService.createCategory({ name, productTypeId }).subscribe({
      next: (created) => {
        this.creating = false;
        this.categories = [...this.categories, created];
        this.newForm.controls.name.reset();
        this.toastr.success('Categoria cadastrada.');
      },
      error: (err) => {
        this.creating = false;
        this.toastr.error(`Não foi possível salvar: ${backendMessage(err)}`);
      },
    });
  }

  startRename(category: CategoryProduct): void {
    this.confirmingDeleteId = undefined;
    this.renamingId = category.id;
    this.renameForm.setValue({ name: category.name ?? '' });
  }

  cancelRename(): void {
    this.renamingId = undefined;
  }

  saveRename(category: CategoryProduct): void {
    const name = this.renameForm.controls.name.value.trim();
    if (!name || this.renameForm.invalid || this.renaming || category.id == null || category.productTypeId == null) {
      this.renameForm.markAllAsTouched();
      return;
    }
    this.renaming = true;
    this.categoryService.updateCategory(category.id, { name, productTypeId: category.productTypeId }).subscribe({
      next: (updated) => {
        this.renaming = false;
        this.categories = this.categories.map((c) => (c.id === updated.id ? updated : c));
        this.renamingId = undefined;
        this.toastr.success('Categoria renomeada.');
      },
      error: (err) => {
        this.renaming = false;
        this.toastr.error(`Não foi possível salvar: ${backendMessage(err)}`);
      },
    });
  }

  askDelete(category: CategoryProduct): void {
    this.renamingId = undefined;
    this.confirmingDeleteId = category.id;
  }

  cancelDelete(): void {
    this.confirmingDeleteId = undefined;
  }

  confirmDelete(category: CategoryProduct): void {
    if (category.id == null) return;
    this.categoryService.delete(category.id).subscribe({
      next: () => {
        this.categories = this.categories.filter((c) => c.id !== category.id);
        this.confirmingDeleteId = undefined;
        this.toastr.success('Categoria excluída.');
      },
      error: (err) => {
        this.confirmingDeleteId = undefined;
        this.toastr.error(`Não foi possível excluir: ${backendMessage(err)}`);
      },
    });
  }
}
