import { Component, OnInit } from '@angular/core';
import { MenuComponent } from '../../menu/menu.component';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ProductTypeService } from '../../../services/product-type.service';
import { ProductType } from '../../../models/product-type';
import { ToastrService } from 'ngx-toastr';
import { CategoryProductService } from '../../../services/category-product.service';
import { CategoryProduct } from '../../../models/category-product';
import { DeleteConfirmationModalComponent } from '../../delete-confirmation-modal/delete-confirmation-modal.component';
import { User } from '../../../models/user';
import { UserService } from '../../../services/user.service';
import { backendMessage } from '../../../utils/backend-message';

@Component({
  selector: 'app-category-products',
  imports: [
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
    DeleteConfirmationModalComponent
  ],
  templateUrl: './category-products.component.html',
  styleUrl: './category-products.component.css'
})
export class CategoryProductsComponent implements OnInit {


  mostrarFormulario = false;
  modalAberta = false;
  formCategory!: FormGroup;

  productTypes?: ProductType[];

  category?: CategoryProduct; 

  categories?: CategoryProduct[];

  deleteNameCategory?: string;
  idCategoryRow?: number;
  userLogado?: User;

  isEdit: boolean = false;

  constructor(
    private productTypeService: ProductTypeService,
    private toastr: ToastrService,
    private fb: FormBuilder, 
    private categoryService: CategoryProductService,
    private userService: UserService
  ) {
    this.formCategory = this.fb.group({
      name: ['', [Validators.required]],
      type: [''],
      productTypeId: [null, [Validators.required]],
      supplierId: ['']
    })
  }


  ngOnInit(): void {
    this.userLogado = this.userService.getUserLocalStorage()
    this.getProuctTypes();
    this.getAllCategories()
  }


  toggleFormulario() {
    this.formCategory.reset();
    this.modalAberta = !this.modalAberta;
    this.isEdit = false;
  }

  openModalEdit(editFormValue: CategoryProduct, isEditForm: boolean) {
    this.isEdit = isEditForm;
    this.modalAberta = !this.modalAberta;
    this.category = editFormValue;
    this.idCategoryRow = editFormValue.id
    this.formCategory.patchValue(editFormValue);
  }

  getProuctTypes() {
    this.productTypeService.findAll().subscribe({
      next: (response) => {
        this.productTypes = response;
        // this.toastr.success("Encontrado tipos com sucesso")        
      },
      error: (err) => {
        this.toastr.error(`Erro ao carregar os tipos de produto: ${backendMessage(err)}`)        
      },
    })
  }

  openModalDelete(category: CategoryProduct) {
    
    this.deleteNameCategory = `${category.type} de ${category.name}`
    this.idCategoryRow = category.id
    
    setTimeout(() => {
      const checkbox: any = document.getElementById('delete-modal');
      if (checkbox) checkbox.checked = true;
    });
  }

  getAllCategories() {
    this.categoryService.findBySupplierId(this.userLogado?.supplierId!).subscribe({
      next: (response) => {
        this.categories = response.data;
      },
      error: (err) => {
        this.toastr.error(`Erro ao carregar as categorias: ${backendMessage(err)}`)        
      },
    })
  }

  salvarCategoria() {
    if (this.formCategory.invalid) {
      this.formCategory.markAllAsTouched();
      return;
    }
    this.formCategory.get('supplierId')?.setValue(this.userLogado?.supplierId)

    this.category = this.formCategory.value; 

    console.log(this.category);
    

    this.categoryService.create(this.category!!).subscribe({
      next: (value) => {
        this.toastr.success('Categoria salva.')        
        this.getAllCategories();
        this.toggleFormulario();
      },
      error: (err) => {
        
        this.toastr.error(`Erro ao salvar categoria: ${backendMessage(err)}`)        
      }
    })
  }

  deleteConfirmed() {
    
    this.categoryService.delete(this.idCategoryRow!!).subscribe({
      next: (value) => {
        
        this.toastr.success('Categoria excluída.')        
        this.getAllCategories();
      },
      error: (err) => {
        
        this.toastr.error(`Erro ao excluir a categoria: ${backendMessage(err)}`)        
      }
    })
  }

  update() {
    
    if (this.formCategory.invalid) {
      this.formCategory.markAllAsTouched();
      return;
    } else if (this.category == this.formCategory.value) {
      this.toastr.warning('Nenhuma alteração para salvar.')        
      return;
    }

    this.category = this.formCategory.value; 

    this.categoryService.update(this.idCategoryRow!!, this.category!!).subscribe({
      next: (value) => {
        
        this.toastr.success('Categoria atualizada.')        
        this.getAllCategories();
        this.toggleFormulario();
      },
      error: (err) => {
        
        this.toastr.error(`Erro ao atualizar categoria: ${backendMessage(err)}`)        
      }
    })
  }
  

}
