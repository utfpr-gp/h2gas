import { Component, OnInit } from '@angular/core';
import { MenuComponent } from '../../menu/menu.component';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ProductTypeService } from '../../../services/product-type.service';
import { ProductType } from '../../../models/product-type';
import { ToastrService } from 'ngx-toastr';
import { CategoryProductService } from '../../../services/category-product.service';
import { CategoryProduct } from '../../../models/category-product';

@Component({
  selector: 'app-category-products',
  imports: [
    MenuComponent,
    ReactiveFormsModule,
    CommonModule,
    FormsModule,
  ],
  templateUrl: './category-products.component.html',
  styleUrl: './category-products.component.css'
})
export class CategoryProductsComponent implements OnInit {


  mostrarFormulario = false;
  modalAberta = false;
  formCategory!: FormGroup;

  productTypes?: ProductType[];

  categories?: CategoryProduct[];

  constructor(
    private productTypeService: ProductTypeService,
    private toastr: ToastrService,
    private fb: FormBuilder, 
    private categoryService: CategoryProductService
  ) {
    this.formCategory = this.fb.group({
      name: ['', [Validators.required]],
      type: [''],
      productTypeId: [null, [Validators.required]]
    })
  }


  ngOnInit(): void {
    this.getProuctTypes();
    this.getAllCategories()
  }


  toggleFormulario() {
    this.modalAberta = !this.modalAberta;
  }

  getProuctTypes() {
    this.productTypeService.findAll().subscribe({
      next: (response) => {
        this.productTypes = response;
        // this.toastr.success("Encontrado tipos com sucesso")        
      },
      error: (err) => {
        this.toastr.error(`Erro ao encontrar os tipos de produtos: ${err.error.message}`)        
      },
    })
  }

  getAllCategories() {
    this.categoryService.findAll().subscribe({
      next: (response) => {
        this.categories = response;
        this.toastr.success("Encontrado todas as categorias de produtos")        
      },
      error: (err) => {
        this.toastr.error(`Erro ao encontrar as categorias de produtos: ${err.error.message}`)        
      },
    })
  }

}
