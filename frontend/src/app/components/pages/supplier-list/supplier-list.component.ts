import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { ToastrService } from 'ngx-toastr';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';
import { CartService } from '../../../services/cart.service';
import { PaginationComponent } from '../../pagination/pagination.component';
import { LocationContextService } from '../../../services/location-context.service';
import { ProductTypeService } from '../../../services/product-type.service';
import { ProductType } from '../../../models/product-type';
import { CategoryChipBarComponent } from '../../category-chip-bar/category-chip-bar.component';
import { ProductCardComponent } from '../../product-card/product-card.component';
import { SupplierCardComponent } from '../../supplier-card/supplier-card.component';
import { StoreReviewsDialogComponent } from '../../store-reviews-dialog/store-reviews-dialog.component';
import { backendMessage } from '../../../utils/backend-message';

const SUPPLIERS_PAGE_SIZE = 10;
const PRODUCTS_PAGE_SIZE = 12;

type ViewMode = 'suppliers' | 'products';

// HU01/HU02 + F7 §5.21: catálogo do cliente (lojas e produtos da cidade do endereço ativo, RN05)
@Component({
  selector: 'app-supplier-list',
  imports: [
    CommonModule,
    PaginationComponent,
    CategoryChipBarComponent,
    ProductCardComponent,
    SupplierCardComponent,
    StoreReviewsDialogComponent,
  ],
  templateUrl: './supplier-list.component.html',
})
export class SupplierListComponent implements OnInit {
  readonly promoBanners = [
    { title: 'Fardinho de Água', subtitle: 'Compre água de diferentes tamanhos', icon: 'water_drop' },
    { title: 'Gás de Cozinha e Industrial', subtitle: 'Botijões de vários tamanhos, com entrega ou retirada', icon: 'local_fire_department' },
  ];

  viewMode: ViewMode = 'suppliers';
  query = '';
  loading = false;

  suppliers: Supplier[] = [];
  products: ProductItem[] = [];
  categories: ProductType[] = [];
  selectedCategory: string | null = null;
  sortByPrice = false;

  currentPage = 0;
  totalPages = 0;
  totalElements = 0;

  private readonly search$ = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
    private router: Router,
    private cartService: CartService,
    private productItemService: ProductItemService,
    private locationContext: LocationContextService,
    private productTypeService: ProductTypeService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.search$.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe((q) => {
      this.query = q;
      this.reload();
    });
    if (this.route.snapshot.queryParamMap.get('view') === 'products') {
      this.viewMode = 'products';
      this.loadCategories();
      this.getProducts();
    } else {
      this.getSuppliers();
    }
  }

  private get cityId(): number | undefined {
    return this.locationContext.current()?.cityId;
  }

  private reload() {
    if (this.viewMode === 'products') this.getProducts();
    else this.getSuppliers();
  }

  private applyPage(response: { page: number; totalPages: number; totalElements: number }) {
    this.currentPage = response.page;
    this.totalPages = response.totalPages;
    this.totalElements = response.totalElements;
    this.loading = false;
  }

  getSuppliers(page: number = 0) {
    this.loading = true;
    this.supplierService.getAllSupplierOpenAndActive(page, SUPPLIERS_PAGE_SIZE, this.cityId, this.query || undefined).subscribe({
      next: (response) => {
        this.suppliers = response.data;
        this.applyPage(response);
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao buscar fornecedores: ${backendMessage(err)}`);
      },
    });
  }

  getProducts(page: number = 0) {
    this.loading = true;
    const sort = this.sortByPrice ? 'priceDelivery,asc' : undefined;
    this.productItemService.getProductsFromActiveSuppliers(
      page, PRODUCTS_PAGE_SIZE, this.cityId, this.selectedCategory ?? undefined, sort, this.query || undefined,
    ).subscribe({
      next: (response) => {
        this.products = response.data;
        this.applyPage(response);
      },
      error: (err) => {
        this.loading = false;
        this.toastr.error(`Erro ao carregar os produtos: ${backendMessage(err)}`);
      },
    });
  }

  // ---- visão e busca ----
  setView(mode: ViewMode) {
    if (this.viewMode === mode) return;
    this.viewMode = mode;
    if (mode === 'products') this.loadCategories();
    this.reload();
  }

  toggleView() {
    this.setView(this.viewMode === 'suppliers' ? 'products' : 'suppliers');
  }

  onSearch(value: string) {
    this.search$.next(value.trim());
  }

  clearSearch() {
    this.query = '';
    const input = document.getElementById('catalog-search') as HTMLInputElement | null;
    if (input) input.value = '';
    this.reload();
  }

  onPageChange(page: number) {
    if (this.viewMode === 'products') this.getProducts(page);
    else this.getSuppliers(page);
  }

  // ---- produtos ----
  loadCategories() {
    if (this.categories.length > 0) return;
    this.productTypeService.findAll().subscribe({
      next: (response) => (this.categories = response),
      error: (err) => this.toastr.error(`Erro ao carregar as categorias: ${backendMessage(err)}`),
    });
  }

  selectCategory(name: string | null) {
    this.selectedCategory = name;
    this.getProducts();
  }

  toggleSortByPrice() {
    this.sortByPrice = !this.sortByPrice;
    this.getProducts();
  }

  navigate(id?: string) {
    this.router.navigate(['/fornecedores', id, 'produtos']);
  }

  addToCart(product: ProductItem) {
    this.cartService.addProduct({ ...product, quantitySelect: 1 });
    this.toastr.success('Produto adicionado ao carrinho.');
  }

  // F7 §5.23: compra direta — item avulso, sem passar pelo carrinho
  buyNow(product: ProductItem) {
    this.cartService.startBuyNow({ ...product, quantitySelect: 1 });
    this.router.navigate(['/resumo-pedido'], { queryParams: { modo: 'compra-direta' } });
  }

  // F7 §5.27: avaliações da loja, abertas pelas estrelas do card
  reviewsStore?: Supplier;

  openReviews(supplier: Supplier) {
    this.reviewsStore = supplier;
  }
}
