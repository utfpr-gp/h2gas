import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';
import { ProductType } from '../../../models/product-type';
import { CartService } from '../../../services/cart.service';
import { PaginatedResponse } from '../../../models/paginated-response';
import { PaginationComponent } from '../../pagination/pagination.component';
import { ProductTypeService } from '../../../services/product-type.service';
import { CategoryChipBarComponent } from '../../category-chip-bar/category-chip-bar.component';
import { ProductCardComponent } from '../../product-card/product-card.component';

@Component({
  selector: 'app-product-list',
  imports: [
    FormsModule,
    CommonModule,
    PaginationComponent,
    CategoryChipBarComponent,
    ProductCardComponent
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
    private productTypeService: ProductTypeService,
    private cartService: CartService
  ) {}

  idSupplier?: string = '';
  products?: ProductItem[];
  categories: ProductType[] = [];

  ngOnInit(): void {
    const idParam = this.routeActive.snapshot.paramMap.get('id');
    if (!idParam) {
      console.error('ID do fornecedor não encontrado!');
      return;
    }
    this.idSupplier = idParam;
    this.loadCategories();
    this.getProducts(this.idSupplier);
  }

  currentPageProducts: number = 0;
  pageSizeProducts: number = 10;
  totalPagesProducts: number = 0;
  totalElementsProducts: number = 0;

  searchTerm = '';
  selectedCategory: string | null = null;

  loadCategories() {
    this.productTypeService.findAll().subscribe({
      next: (response) => {
        this.categories = response;
      },
      error: () => {
        this.toastr.error('Erro ao buscar categorias');
      }
    });
  }

  onCategorySelected(name: string | null) {
    this.selectedCategory = name;
    this.getProducts(this.idSupplier!);
  }

  getProducts(supplierId: string, page: number = 0) {
    this.productItemService
      .getProductsBySupplierId(
        supplierId,
        page,
        this.pageSizeProducts,
        this.searchTerm || undefined,
        this.selectedCategory || undefined
      )
      .subscribe({
        next: (response: PaginatedResponse<ProductItem>) => {
          this.products = response.data;
          this.currentPageProducts = response.page;
          this.pageSizeProducts = response.size;
          this.totalPagesProducts = response.totalPages;
          this.totalElementsProducts = response.totalElements;

          if (page === 0) {
            this.toastr.success('Produtos carregados com sucesso!');
          }
        },
        error: (err) => {
          console.error('Erro ao buscar produtos:', err.error?.message || err.message);
          this.toastr.error(`Erro ao buscar produtos: ${err.error?.message || err.message}`);
        },
      });
  }

  addToCart(product: ProductItem) {
    this.cartService.addProduct({ ...product, quantitySelect: 1 });
    this.toastr.success('Produto adicionado ao carrinho!');
  }
}
