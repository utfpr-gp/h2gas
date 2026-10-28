import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Params, Router } from '@angular/router';
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
import { CartOtherStoreDialogComponent } from '../../cart-other-store-dialog/cart-other-store-dialog.component';
import { CatalogSortMenuComponent, SortOption } from '../../catalog-sort-menu/catalog-sort-menu.component';
import { backendMessage } from '../../../utils/backend-message';
import { groupProductsByTypeAndCategory } from '../../../utils/product-sections';

const SUPPLIERS_PAGE_SIZE = 10;
const PRODUCTS_PAGE_SIZE = 12;

type ViewMode = 'suppliers' | 'products';

// HU16: opções do botão "Ordenar"; a primeira de cada lista é a padrão
const SUPPLIER_SORT_OPTIONS: SortOption[] = [
  { value: 'name', label: 'Nome (A-Z)' },
  { value: 'rating', label: 'Melhor avaliação' },
  { value: 'deliveryTime', label: 'Menor tempo de entrega' },
  // HU18: "Abertas primeiro" saiu — as fechadas já vêm sempre depois das abertas
];
const PRODUCT_SORT_OPTIONS: SortOption[] = [
  { value: 'price,asc', label: 'Menor preço' },
  { value: 'price,desc', label: 'Maior preço' },
  { value: 'brand,asc', label: 'Nome (A-Z)' },
  { value: 'rating', label: 'Melhor avaliado' },
  { value: 'createdAt,desc', label: 'Mais recentes' },
];

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
    CartOtherStoreDialogComponent,
    CatalogSortMenuComponent,
  ],
  templateUrl: './supplier-list.component.html',
})
export class SupplierListComponent implements OnInit {
  viewMode: ViewMode = 'suppliers';
  query = '';
  loading = false;

  suppliers: Supplier[] = [];
  products: ProductItem[] = [];
  categories: ProductType[] = [];
  // HU15: o tipo escolhido vale nas duas visões — lojas filtram por id, produtos por nome
  selectedType: ProductType | null = null;
  // HU16: cada visão guarda a própria ordenação
  supplierSort = SUPPLIER_SORT_OPTIONS[0].value;
  productSort = PRODUCT_SORT_OPTIONS[0].value;

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
      this.applyFilters();
    });
    const params = this.route.snapshot.queryParamMap;
    if (params.get('view') === 'products') this.viewMode = 'products';
    // HU16: busca, tipo e ordenação vêm da URL, para sobreviver ao recarregar e à volta de uma loja
    this.query = params.get('q')?.trim() ?? '';
    const ordem = params.get('ordem');
    if (ordem && this.sortOptions.some((o) => o.value === ordem)) this.activeSort = ordem;
    const tipoId = Number(params.get('tipo')) || null;
    // HU15: os chips de tipo aparecem nas duas visões, então as categorias carregam sempre;
    // com tipo na URL, a primeira busca espera os tipos para saber o nome (a visão de produtos filtra por ele)
    this.loadCategories(() => {
      if (tipoId != null) {
        this.selectedType = this.categories.find((c) => c.id === tipoId) ?? null;
        this.reload();
      }
    });
    if (tipoId == null) this.reload();
  }

  get sortOptions(): SortOption[] {
    return this.viewMode === 'products' ? PRODUCT_SORT_OPTIONS : SUPPLIER_SORT_OPTIONS;
  }

  get activeSort(): string {
    return this.viewMode === 'products' ? this.productSort : this.supplierSort;
  }

  set activeSort(value: string) {
    if (this.viewMode === 'products') this.productSort = value;
    else this.supplierSort = value;
  }

  get hasActiveFilters(): boolean {
    return !!this.query || this.selectedType != null || this.activeSort !== this.sortOptions[0].value;
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
    this.supplierService.getAllSupplierOpenAndActive(
      page, SUPPLIERS_PAGE_SIZE, this.cityId, this.query || undefined, this.selectedType?.id, this.supplierSort,
    ).subscribe({
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

  get productSections() {
    return groupProductsByTypeAndCategory(this.products, this.categories);
  }

  getProducts(page: number = 0) {
    this.loading = true;
    this.productItemService.getProductsFromActiveSuppliers(
      page, PRODUCTS_PAGE_SIZE, this.cityId, this.selectedType?.name ?? undefined, this.productSort, this.query || undefined,
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
    this.applyFilters();
  }

  toggleView() {
    this.setView(this.viewMode === 'suppliers' ? 'products' : 'suppliers');
  }

  onSearch(value: string) {
    this.search$.next(value.trim());
  }

  // HU16: o campo segue o estado ([value]="query"); limpar não mexe mais no DOM à mão
  clearSearch() {
    this.query = '';
    this.applyFilters();
  }

  changeSort(value: string) {
    this.activeSort = value;
    this.applyFilters();
  }

  clearFilters() {
    this.query = '';
    this.selectedType = null;
    this.activeSort = this.sortOptions[0].value;
    this.applyFilters();
  }

  // HU16: todo filtro recarrega da primeira página e fica refletido na URL
  private applyFilters() {
    this.syncUrl();
    this.reload();
  }

  private syncUrl() {
    const queryParams: Params = {
      view: this.viewMode === 'products' ? 'products' : null,
      q: this.query || null,
      tipo: this.selectedType?.id ?? null,
      ordem: this.activeSort !== this.sortOptions[0].value ? this.activeSort : null,
    };
    this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge', replaceUrl: true });
  }

  onPageChange(page: number) {
    if (this.viewMode === 'products') this.getProducts(page);
    else this.getSuppliers(page);
  }

  // ---- produtos ----
  loadCategories(onLoaded?: () => void) {
    if (this.categories.length > 0) return;
    this.productTypeService.findAll().subscribe({
      next: (response) => {
        this.categories = response;
        onLoaded?.();
      },
      error: (err) => {
        this.toastr.error(`Erro ao carregar as categorias: ${backendMessage(err)}`);
        onLoaded?.(); // sem os tipos, a lista carrega sem o filtro de tipo em vez de ficar vazia
      },
    });
  }

  // HU15: o mesmo chip filtra as duas visões
  selectCategory(type: ProductType | null) {
    this.selectedType = type;
    this.applyFilters();
  }


  navigate(id?: string) {
    this.router.navigate(['/lojas', id]);
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

  // F7 §5.27: avaliações da loja, abertas pelas estrelas do card
  reviewsStore?: Supplier;

  openReviews(supplier: Supplier) {
    this.reviewsStore = supplier;
  }
}
