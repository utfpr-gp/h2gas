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
import { backendMessage } from '../../../utils/backend-message';

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
  isLoading = false;


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
      description: ['', [Validators.required]],
      categoryProductId: [null, [Validators.required]]
    })
  }


  ngOnInit(): void {
    this.idEdit = parseInt(this.routeActive.snapshot.paramMap.get('id') || '', 10);
    if (this.idEdit) {
      this.isEdit = true;
      this.finProductId(this.idEdit);
    } 
    this.user = this.userService.getUserLocalStorage();
    this.findAllCategoryProducts();
  }


  finProductId(idEdit: number) {
    this.productItemService.findById(idEdit).subscribe({
      next: (response) => {
        console.log('Produto buscado com sucesso!', response);
        this.formProductItem.patchValue(response);
        this.selectedImage = response.imageUrl!
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao carregar o produto: ${backendMessage(err)}`)
      },
    })
  }

  selectedImage: string | ArrayBuffer | null = null;
  file: any;

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    this.file = input.files[0];

    const reader = new FileReader();
    reader.onload = () => {
      this.selectedImage = reader.result;
    };
    reader.readAsDataURL(this.file);

    console.log(this.file);
  }

  removeImage() {
    this.selectedImage = null;
  }

  updateProduct() {
    if (this.formProductItem.invalid) {
      this.toastr.warning('Preencha os campos obrigatórios.')
      return;
    }

    this.productItemSave = this.formProductItem.value;

    this.productItemService.update(this.idEdit, this.productItemSave!).subscribe({
      next: (response) => {
        console.log("Atulizado com sucesso!!");
        this.isLoading = false;
        this.toastr.success('Produto atualizado.');
        this.navigateRoute('/product-items')
      },
      error: (err) => {
        console.error("Erros ao atualizar!!");
        this.isLoading = false;
        this.toastr.error(`Erro ao atualizar o produto: ${backendMessage(err)}`);
      },
    }) 
  }


  findAllCategoryProducts() {
    console.log(this.user?.supplierId);
    
    this.categoryService.findBySupplierId(this.user?.supplierId!).subscribe({
      next: (response) => {
        this.categoryProduct = response.data;
        console.log(this.categoryProduct);
        
      },
      error: (err) => {
        this.toastr.error(`Erro ao carregar as categorias: ${backendMessage(err)}`)        
      },
    })
  }

  saveItemProduct() {
    this.isLoading = true;
    if (!this.formProductItem.valid) {
      this.toastr.warning('Preencha os campos obrigatórios.')
      return;
    }
    
    this.productItemSave = this.formProductItem.value;

    // if (this.productItemSave) {
    //   this.productItemSave.supplierId = this.user?.supplierId ?? '';
    // }
    
    this.productItemService.createWithFile(this.productItemSave!!, this.file).subscribe({
      next: (value) => {
        console.log("salvo com sucesso!!");
        this.isLoading = false;
        this.toastr.success('Produto cadastrado.');
        this.navigateRoute('/product-items')
      },
      error: (err) => {
        console.error("Erros ao salvar!!");
        this.isLoading = false;
        this.toastr.error(`Erro ao cadastrar o produto: ${backendMessage(err)}`);
      },
    })
  }

  navigateRoute(routeName: string) {
    this.router.navigate([routeName]);
  }

}
