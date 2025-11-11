import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';
import { CartService } from '../../../services/cart.service';
import { PaginatedResponse } from '../../../models/paginated-response';
import { PaginationComponent } from '../../pagination/pagination.component';

@Component({
  selector: 'app-product-list',
  imports: [
    FormsModule,
    CommonModule,
    PaginationComponent
  ],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css'
})
export class ProductListComponent implements OnInit {
  constructor(
    private toastr: ToastrService,
    private router: Router,
    private routeActive: ActivatedRoute,
    private productItemService: ProductItemService,
    private cartService: CartService
  ) {}

  @ViewChild('productModal') productModal!: ElementRef<HTMLDialogElement>;
  idSupplier?: string = '';
  products?: ProductItem[];
  productSelect?: ProductItem = new ProductItem();
  quantity: number = 1;
  cartProduct?: ProductItem[] = [];

  ngOnInit(): void {
    const idParam = this.routeActive.snapshot.paramMap.get('id');
    if (!idParam) {
      console.error('ID do fornecedor não encontrado!');
      return;
    }
    this.idSupplier = idParam;
    console.log(this.idSupplier);
    this.getProducts(this.idSupplier);
  }

  currentPageProducts: number = 0;
  pageSizeProducts: number = 10;
  totalPagesProducts: number = 0;
  totalElementsProducts: number = 0;

  getProducts(supplierId: string, page: number = 0) {
    this.productItemService.getProductsBySupllierId(supplierId, page, this.pageSizeProducts).subscribe({
      next: (response: PaginatedResponse<ProductItem>) => {
        this.products = response.data;
        this.currentPageProducts = response.page;
        this.pageSizeProducts = response.size;
        this.totalPagesProducts = response.totalPages;
        this.totalElementsProducts = response.totalElements;
  
        this.toastr.success('Produtos encontrados com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error?.message || err.message);
        this.toastr.error(`Erro ao buscar produtos: ${err.error?.message || err.message}`);
      },
    });
  }

  openModal(product: any) {
    this.productSelect = product;
    this.quantity = 1;
    this.productModal.nativeElement.showModal();
  }

  closeModal() {
    this.productModal.nativeElement.close();
  }

  confirmRequest() {
    if (this.quantity < 1) {
      this.toastr.warning("Selecione pelo menos 1 produto!");
      this.closeModal();
      return;
    }
  
    const productToAdd = {
      ...this.productSelect,
      quantitySelect: this.quantity
    };
  
    this.cartService.addProduct(productToAdd);
  
    this.toastr.success("Produto adicionado ao carrinho!");
    this.closeModal();
  }


}
