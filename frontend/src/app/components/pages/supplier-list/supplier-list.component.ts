import { Component, OnInit } from '@angular/core';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';
import { ToastrService } from 'ngx-toastr';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';
import { CartService } from '../../../services/cart.service';
import { PaginatedResponse } from '../../../models/paginated-response';
import { PaginationComponent } from '../../pagination/pagination.component';
import { LocationContextService } from '../../../services/location-context.service';
import { ProductTypeService } from '../../../services/product-type.service';
import { ProductType } from '../../../models/product-type';
import { CategoryChipBarComponent } from '../../category-chip-bar/category-chip-bar.component';
import { ProductCardComponent } from '../../product-card/product-card.component';
import { SupplierCardComponent } from '../../supplier-card/supplier-card.component';
import { backendMessage } from '../../../utils/backend-message';

@Component({
  selector: 'app-supplier-list',
  imports: [
    FormsModule,
    CommonModule,
    ReactiveFormsModule,
    PaginationComponent,
    CategoryChipBarComponent,
    ProductCardComponent,
    SupplierCardComponent
  ],
  templateUrl: './supplier-list.component.html',
})
export class SupplierListComponent implements OnInit {

  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
    private router: Router,
    private fb: FormBuilder,
    private cartService: CartService,
    private productItemService: ProductItemService,
    private locationContext: LocationContextService,
    private productTypeService: ProductTypeService,
    private route: ActivatedRoute,
  ) {
    this.formSearch = this.fb.group({
      name: ['']
    })
  }


  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('view') === 'products') {
      this.viewMode = 'products';
      this.loadCategories();
      this.getProducts();
    } else {
      this.getSuppliers();
    }
  }

  suppliers?: Supplier[];
  formSearch!: FormGroup;
  products: ProductItem[] = [];

  readonly promoBanners = [
    { title: 'Fardinho de Água', subtitle: 'Compre água de diferentes tamanhos' },
    { title: 'Gás de Cozinha e Industrial', subtitle: 'Até 15% OFF em itens selecionados' },
  ];

  currentPage: number = 0;
  pageSize: number = 10; 
  totalPages: number = 0;
  totalElements: number = 0;
  

  getSuppliers(page: number = 0) {
    const cityId = this.locationContext.current()?.cityId;

    this.supplierService.getAllSupplierOpenAndActive(page, this.pageSize, cityId).subscribe({
      next: (response: PaginatedResponse<Supplier>) => {
        this.suppliers = response.data;
        this.currentPage = response.page;
        this.pageSize = response.size;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        console.log(this.suppliers);
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error?.message || err.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${backendMessage(err)}`)
      },
    });
  }

  viewMode: 'suppliers' | 'products' = 'suppliers';
  categories: ProductType[] = [];
  selectedCategory: string | null = null;
  sortByPrice = false;

  toggleView() {
    if (this.viewMode === 'suppliers') {
      this.viewMode = 'products';
      this.loadCategories();
      this.getProducts(); // busca só quando alterna
    } else {
      this.viewMode = 'suppliers';
      this.getSuppliers(); // volta a carregar fornecedores
    }
  }

  loadCategories() {
    if (this.categories.length > 0) {
      return;
    }
    this.productTypeService.findAll().subscribe({
      next: (response) => {
        this.categories = response;
      },
      error: (err) => {
        this.toastr.error(`Erro ao carregar as categorias: ${backendMessage(err)}`);
      }
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

  getProducts() {
    const cityId = this.locationContext.current()?.cityId;
    const sort = this.sortByPrice ? 'priceDelivery,asc' : undefined;

    this.productItemService.getProductsFromActiveSuppliers(
      0, 20, cityId, this.selectedCategory ?? undefined, sort
    ).subscribe({
      next: (response) => {
        this.products = response.data
      },
      error: (err) => {
        this.toastr.error(`Erro ao carregar os produtos: ${backendMessage(err)}`)
      }
    });
  }

  navigate(id?: string) {
    this.router.navigate(['/fornecedores', id, 'produtos']);
  }

  searchDone = false;

  buscar() {
    const searchValue = this.formSearch.get('name')?.value?.trim();

    if (!searchValue) {
      this.searchDone = false;
      this.getSuppliers()
      return;
    }

    this.supplierService.searchSuppliers(searchValue).subscribe({
      next: (response) => {
        this.suppliers = response.data;
  
        this.searchDone = this.suppliers && this.suppliers.length > 0;
  
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${backendMessage(err)}`);
        this.searchDone = false;
      },
    });
  }

  addToCart(product: ProductItem) {
    this.cartService.addProduct({ ...product, quantitySelect: 1 });
    this.toastr.success('Produto adicionado ao carrinho.');
  }

}
