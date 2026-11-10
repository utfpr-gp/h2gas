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
      price: [null, [Validators.required, Validators.min(0)]],
      stockQuantity: [0, [Validators.required, Validators.min(0)]],
      description: ['', [Validators.required]],
      categoryProductId: [null, [Validators.required]]
    });
  }


  ngOnInit(): void {
    // F12: a URL traz o código público; o id (para salvar) vem do produto carregado
    const code = this.routeActive.snapshot.paramMap.get('code');
    if (code) {
      this.isEdit = true;
      this.findProductByCode(code);
    }
    this.user = this.userService.getUserLocalStorage();
    this.findAllCategoryProducts();
  }


  findProductByCode(code: string) {
    this.productItemService.findByCode(code).subscribe({
      next: (response) => {
        this.idEdit = response.id!;
        this.formProductItem.patchValue(response);
        this.selectedImage = response.imageUrl!
      },
      error: (err) => {
        // F12: produto de outra loja (403) ou inexistente (404) não abre; volta para a lista
        if (err?.status === 403 || err?.status === 404) {
          this.toastr.error(err.status === 403 ? 'Este produto não é da sua loja.' : 'Produto não encontrado.');
          this.router.navigate(['/minha-loja/produtos']);
          return;
        }
        this.toastr.error(`Erro ao carregar o produto: ${backendMessage(err)}`)
      },
    })
  }

  selectedImage: string | ArrayBuffer | null = null;
  file?: File;
  dragging = false;

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    this.setFile(input.files[0]);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.dragging = true;
  }

  onDragLeave() {
    this.dragging = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.dragging = false;
    const dropped = event.dataTransfer?.files?.[0];
    if (dropped) {
      this.setFile(dropped);
    }
  }

  private setFile(file: File) {
    if (!file.type.startsWith('image/')) {
      this.toastr.warning('Selecione um arquivo de imagem.');
      return;
    }
    this.file = file;
    const reader = new FileReader();
    reader.onload = () => {
      this.selectedImage = reader.result;
    };
    reader.readAsDataURL(file);
  }

  removeImage() {
    this.selectedImage = null;
    this.file = undefined;
  }

  cancel() {
    this.navigateRoute('/minha-loja/produtos');
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
        this.navigateRoute('/minha-loja/produtos')
      },
      error: (err) => {
        console.error("Erros ao atualizar!!");
        this.isLoading = false;
        this.toastr.error(`Erro ao atualizar o produto: ${backendMessage(err)}`);
      },
    }) 
  }


  // HU19: as categorias são globais (cadastradas pelo admin) — o fornecedor escolhe entre todas
  findAllCategoryProducts() {
    this.categoryService.findAll().subscribe({
      next: (response) => {
        this.categoryProduct = response;
      },
      error: (err) => {
        this.toastr.error(`Erro ao carregar as categorias: ${backendMessage(err)}`)        
      },
    })
  }

  saveItemProduct() {
    if (!this.formProductItem.valid) {
      this.toastr.warning('Preencha os campos obrigatórios.')
      return;
    }
    // a API exige a parte 'file' no cadastro; na edição a imagem não muda
    if (!this.file) {
      this.toastr.warning('Selecione uma imagem do produto.')
      return;
    }
    this.isLoading = true;

    this.productItemSave = this.formProductItem.value;

    // if (this.productItemSave) {
    //   this.productItemSave.supplierId = this.user?.supplierId ?? '';
    // }
    
    this.productItemService.createWithFile(this.productItemSave!!, this.file!).subscribe({
      next: (value) => {
        console.log("salvo com sucesso!!");
        this.isLoading = false;
        this.toastr.success('Produto cadastrado.');
        this.navigateRoute('/minha-loja/produtos')
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
