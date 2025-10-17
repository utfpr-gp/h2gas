import { Component, OnInit } from '@angular/core';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductType } from '../../../models/product-type';
import { ProductTypeService } from '../../../services/product-type.service';
import { ToastrService } from 'ngx-toastr';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoryProductService } from '../../../services/category-product.service';
import { UserService } from '../../../services/user.service';
import { CategoryProduct } from '../../../models/category-product';
import { CommonModule } from '@angular/common';
import { User } from '../../../models/user';
import { ProductItem } from '../../../models/product-item';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-product-item-form',
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
  ],
  templateUrl: './product-item-form.component.html',
  styleUrl: './product-item-form.component.css'
})
export class ProductItemFormComponent implements OnInit {
  productTypes?: ProductType[];

  categoryProduct?: CategoryProduct[];

  formProductItem!: FormGroup;

  selectedFile: File | null = null;

  user?: User;

  productItemSave?: ProductItem;
  isEdit: boolean = false;
  idEdit: number = 0;

  constructor(
    private productItemService: ProductItemService,
    private productTypeService: ProductTypeService,
    private toastr: ToastrService,
    private fb: FormBuilder, 
    private categoryService: CategoryProductService,
    private userService: UserService,
    private router: Router,
    private routeActive: ActivatedRoute
  ) {
    this.formProductItem = this.fb.group({
      brand: ['', [Validators.required]],
      price: [0.0, [Validators.required]],
      stockQuantity: [0, [Validators.required]],
      categoryProductId: [null, [Validators.required]]
    })
  }


  ngOnInit(): void {
    this.idEdit = parseInt(this.routeActive.snapshot.paramMap.get('id') || '', 10);
    if (this.idEdit) {
      this.isEdit = true;
      this.finProductId(this.idEdit);
    } 
    this.findAllCategoryProducts();
    this.user = this.userService.getUserLocalStorage();
  }


  finProductId(idEdit: number) {
    this.productItemService.findById(idEdit).subscribe({
      next: (response) => {
        console.log('Produto buscado com sucesso!', response);
        this.formProductItem.patchValue(response);
        this.toastr.success('Produto buscado com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar: ${err.error.message}`)
      },
    })
  }

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
  }

  updateProduct() {
    if (this.formProductItem.invalid) {
      this.toastr.warning("Confira os campos vazios")
      return;
    }

    this.productItemSave = this.formProductItem.value;

    this.productItemService.update(this.idEdit, this.productItemSave!).subscribe({
      next: (response) => {
        console.log("Atulizado com sucesso!!");
        this.toastr.success("Produto atualizado com sucesso!!");
        this.navigateRoute('/product-items')
      },
      error: (err) => {
        console.error("Erros ao atualizar!!");
        this.toastr.error("Erros ao atualizar produtos!!");
      },
    }) 
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
    if (!this.formProductItem.valid) {
      this.toastr.warning("Confira os campos vazios")
      return;
    }
    debugger
    this.productItemSave = this.formProductItem.value;

    if (this.productItemSave) {
      this.productItemSave.supplierId = this.user?.supplierId ?? '';
    }
    
    this.productItemService.create(this.productItemSave!!).subscribe({
      next: (value) => {
        console.log("salvo com sucesso!!");
        this.toastr.success("Produto salvo com sucesso!!");
        this.navigateRoute('/product-items')
      },
      error: (err) => {
        console.error("Erros ao salvar!!");
        this.toastr.error("Erros ao salvar produtos!!");
      },
    })
  }

  navigateRoute(routeName: string) {
    this.router.navigate([routeName]);
  }

}
