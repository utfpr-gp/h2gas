import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { ProductTypeService } from '../../../services/product-type.service';
import { ProductType } from '../../../models/product-type';
import { backendMessage } from '../../../utils/backend-message';

// HU12: o admin mantém os tipos de produto (Gás, Água…) que os fornecedores usam nas categorias
@Component({
  selector: 'app-admin-product-types',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-product-types.component.html',
})
export class AdminProductTypesComponent implements OnInit {
  private readonly productTypeService = inject(ProductTypeService);
  private readonly toastr = inject(ToastrService);
  private readonly fb = inject(FormBuilder);

  types: ProductType[] = [];
  loading = true;
  creating = false;
  readonly newForm = this.fb.nonNullable.group({ name: ['', [Validators.required, Validators.maxLength(60)]] });

  renamingId?: number;
  renaming = false;
  readonly renameForm = this.fb.nonNullable.group({ name: ['', [Validators.required, Validators.maxLength(60)]] });

  ngOnInit(): void {
    this.productTypeService.findAll().subscribe({
      next: (types) => {
        this.types = types;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar os tipos: ${backendMessage(err)}`);
      },
    });
  }

  create(): void {
    const name = this.newForm.controls.name.value.trim();
    if (!name || this.newForm.invalid || this.creating) {
      this.newForm.markAllAsTouched();
      return;
    }
    this.creating = true;
    this.productTypeService.create({ name }).subscribe({
      next: (created) => {
        this.creating = false;
        this.types = [...this.types, created];
        this.newForm.reset();
        this.toastr.success('Tipo cadastrado.');
      },
      error: (err) => {
        this.creating = false;
        this.toastr.error(`Não foi possível salvar: ${backendMessage(err)}`);
      },
    });
  }

  startRename(type: ProductType): void {
    this.renamingId = type.id;
    this.renameForm.setValue({ name: type.name ?? '' });
  }

  cancelRename(): void {
    this.renamingId = undefined;
  }

  saveRename(type: ProductType): void {
    const name = this.renameForm.controls.name.value.trim();
    if (!name || this.renameForm.invalid || this.renaming || type.id == null) {
      this.renameForm.markAllAsTouched();
      return;
    }
    this.renaming = true;
    this.productTypeService.update(type.id, { name }).subscribe({
      next: (updated) => {
        this.renaming = false;
        this.types = this.types.map((t) => (t.id === updated.id ? updated : t));
        this.renamingId = undefined;
        this.toastr.success('Tipo renomeado.');
      },
      error: (err) => {
        this.renaming = false;
        this.toastr.error(`Não foi possível salvar: ${backendMessage(err)}`);
      },
    });
  }
}
