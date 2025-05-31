import { Component, OnInit } from '@angular/core';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductType } from '../../../models/product-type';
import { ProductTypeService } from '../../../services/product-type.service';
import { ToastrService } from 'ngx-toastr';
import { FormBuilder, FormGroup, FormsModule, Validators } from '@angular/forms';
import { CategoryProductService } from '../../../services/category-product.service';
import { UserService } from '../../../services/user.service';
import { CategoryProduct } from '../../../models/category-product';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-product-item-form',
  imports: [
    CommonModule,
    FormsModule,
  ],
  templateUrl: './product-item-form.component.html',
  styleUrl: './product-item-form.component.css'
})
export class ProductItemFormComponent implements OnInit {
  productTypes?: ProductType[];

  categoryProduct?: CategoryProduct[];

  formProductItem?: FormGroup;

  constructor(
    private productItemService: ProductItemService,
    private productTypeService: ProductTypeService,
    private toastr: ToastrService,
    private fb: FormBuilder, 
    private categoryService: CategoryProductService,
    private userService: UserService
  ) {
    this.formProductItem = this.fb.group({
      brand: ['', [Validators.required]],
      price: [0, [Validators.required]],
      stockQuantity: [0, [Validators.required]]
    })
  }


  ngOnInit(): void {
    this.findAllCategoryProducts();
  }


  findAllCategoryProducts() {
    this.categoryService.findAll().subscribe({
      next: (response) => {
        this.categoryProduct = response
        console.log("Buscado com sucesso");
      },
      error: (err) => {
        this.toastr.error("Erro ao buscar as categorias!!")
      },
    })
  }

  saveItemProduct() {
    
  }

}
