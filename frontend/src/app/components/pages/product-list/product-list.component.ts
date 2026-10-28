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
import { fadeIn, staggerList } from '../../../animations/checkout.animations';
import { SupplierService } from '../../../services/supplier.service';
import { Storefront } from '../../../models/storefront';
import { StoreRatingComponent } from '../../store-rating/store-rating.component';
import { StoreReviewsDialogComponent } from '../../store-reviews-dialog/store-reviews-dialog.component';
import { CartOtherStoreDialogComponent } from '../../cart-other-store-dialog/cart-other-store-dialog.component';
import { backendMessage } from '../../../utils/backend-message';
import { groupProductsByTypeAndCategory } from '../../../utils/product-sections';

@Component({
  selector: 'app-product-list',
  imports: [
    FormsModule,
    CommonModule,
    PaginationComponent,
    CategoryChipBarComponent,
    ProductCardComponent,
    StoreRatingComponent,
    StoreReviewsDialogComponent,
    CartOtherStoreDialogComponent,
  ],
  templateUrl: './product-list.component.html',
  animations: [staggerList, fadeIn],
  host: { class: 'flex flex-1 flex-col bg-gray-50 -mt-5 pt-5' }
})
export class ProductListComponent implements OnInit {
  constructor(
    private toastr: ToastrService,
    private router: Router,
    private routeActive: ActivatedRoute,
    private productItemService: ProductItemService,
    private productTypeService: ProductTypeService,
    private cartService: CartService,
    private supplierService: SupplierService
  ) {}

  idSupplier?: string = '';
  products?: ProductItem[];
  // F7 §5.26: cabeçalho da loja (banner, logo, nome, nota, horário, bairro, descrição)
  store?: Storefront;
  storeLoading = true;
  bannerBroken = false;
  logoBroken = false;
  descriptionExpanded = false;
  reviewsOpen = false; // F7 §5.27
  readonly descriptionClampLength = 120;
  loading = false;
  listVersion = 0; // muda a cada resposta, para a lista entrar em sequência mesmo com a mesma quantidade
  categories: ProductType[] = [];

  ngOnInit(): void {
    const idParam = this.routeActive.snapshot.paramMap.get('id');
    if (!idParam) {
      console.error('ID do fornecedor não encontrado!');
      return;
    }
    this.idSupplier = idParam;
    this.loadStore(idParam);
    this.loadCategories();
    this.getProducts(this.idSupplier);
  }

  currentPageProducts: number = 0;
  pageSizeProducts: number = 10;
  totalPagesProducts: number = 0;
  totalElementsProducts: number = 0;

  searchTerm = '';
  selectedCategory: string | null = null;

  loadStore(id: string) {
    this.supplierService.getStorefront(id).subscribe({
      next: (store) => {
        this.store = store;
        this.storeLoading = false;
      },
      error: (err) => {
        this.storeLoading = false;
        this.toastr.error(`Erro ao carregar a loja: ${backendMessage(err)}`);
      }
    });
  }

  get hasLongDescription(): boolean {
    return (this.store?.description?.length ?? 0) > this.descriptionClampLength;
  }

  get storeLocation(): string {
    return [this.store?.addressNeighborhood, this.store?.cityName].filter(Boolean).join(' · ');
  }

  loadCategories() {
    this.productTypeService.findAll().subscribe({
      next: (response) => {
        this.categories = response;
      },
      error: (err) => {
        this.toastr.error(`Erro ao carregar as categorias: ${backendMessage(err)}`);
      }
    });
  }

  // HU18: fora do horário a loja aparece, mas não vende
  get closedNotice(): string | null {
    if (!this.store || this.store.open) return null;
    return this.store.openingTime ? `Loja fechada · abre às ${this.store.openingTime}` : 'Loja fechada';
  }

  get productSections() {
    return groupProductsByTypeAndCategory(this.products ?? [], this.categories);
  }

  // HU15: o chip passou a emitir o tipo inteiro; aqui o filtro segue sendo pelo nome
  onCategorySelected(type: ProductType | null) {
    this.selectedCategory = type?.name ?? null;
    this.getProducts(this.idSupplier!);
  }

  getProducts(supplierId: string, page: number = 0) {
    this.loading = true;
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
          this.loading = false;
          this.listVersion++;
          this.products = response.data;
          this.currentPageProducts = response.page;
          this.pageSizeProducts = response.size;
          this.totalPagesProducts = response.totalPages;
          this.totalElementsProducts = response.totalElements;
        },
        error: (err) => {
          this.loading = false;
          console.error('Erro ao buscar produtos:', err.error?.message || err.message);
          this.toastr.error(`Erro ao buscar produtos: ${backendMessage(err)}`);
        },
      });
  }

  // HU14: produto aguardando confirmação de esvaziar o carrinho de outra loja
  pendingOtherStoreProduct?: ProductItem;

  addToCart(product: ProductItem) {
    const item = { ...product, quantitySelect: 1 };
    if (this.cartService.addProduct(item) === 'other_supplier') {
      this.pendingOtherStoreProduct = item;
      return;
    }
    this.toastr.success('Produto adicionado ao carrinho.');
  }

  confirmClearAndAdd() {
    if (!this.pendingOtherStoreProduct) {
      return;
    }
    this.cartService.clearAndAdd(this.pendingOtherStoreProduct);
    this.pendingOtherStoreProduct = undefined;
    this.toastr.success('Produto adicionado ao carrinho.');
  }

  cancelOtherStoreDialog() {
    this.pendingOtherStoreProduct = undefined;
  }

  // F7 §5.23: compra direta — item avulso, sem passar pelo carrinho
  buyNow(product: ProductItem) {
    this.cartService.startBuyNow({ ...product, quantitySelect: 1 });
    this.router.navigate(['/resumo-pedido'], { queryParams: { modo: 'compra-direta' } });
  }
}
