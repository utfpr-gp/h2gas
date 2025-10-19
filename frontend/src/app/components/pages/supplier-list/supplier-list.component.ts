import { Component, ElementRef, EventEmitter, OnInit, Output, ViewChild } from '@angular/core';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';
import { ToastrService } from 'ngx-toastr';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';
import { CartService } from '../../../services/cart.service';
import { PaginatedResponse } from '../../../models/paginated-response';
import { PaginationComponent } from '../../pagination/pagination.component';

@Component({
  selector: 'app-supplier-list',
  imports: [
    FormsModule,
    CommonModule,
    ReactiveFormsModule,
    PaginationComponent
  ],
  templateUrl: './supplier-list.component.html',
  styleUrl: './supplier-list.component.css'
})
export class SupplierListComponent implements OnInit {

  constructor(
    private supplierService: SupplierService,
    private toastr: ToastrService,
    private router: Router,
    private fb: FormBuilder,
    private cartService: CartService,
    private productItemService: ProductItemService,
  ) {
    this.formSearch = this.fb.group({
      name: ['']
    })
  }


  ngOnInit(): void {
    this.getSuppliers();
  }

  suppliers?: Supplier[];
  formSearch!: FormGroup;
  products: ProductItem[] = [];
  productSelect?: ProductItem = new ProductItem();
  @ViewChild('productModal') productModal!: ElementRef<HTMLDialogElement>;
  quantity: number = 1;
  cartProduct?: ProductItem[] = [];

  currentPage: number = 0;
  pageSize: number = 10; 
  totalPages: number = 0;
  totalElements: number = 0;

  // getSuppliers() {
  //   this.supplierService.getAllSupplierOpenAndActive().subscribe({
  //     next: (response) => {
  //       console.log('Buscado com sucesso', response);
  //       this.suppliers = response.data
  //       console.log(this.suppliers!.length);
        
  //       this.toastr.success('Fornecedores buscados com sucesso!');
  //     },
  //     error: (err) => {
  //       console.error('Erro ao buscar:', err.error.message);
  //       this.toastr.error(`Erro ao buscar fornecedores: ${err.error.message}`)
  //     },
  //   })
  // }
  

  getSuppliers(page: number = 0) {
    debugger
    this.supplierService.getAllSupplierOpenAndActive(page, this.pageSize).subscribe({
      next: (response: PaginatedResponse<Supplier>) => {
        this.suppliers = response.data;
        this.currentPage = response.page;
        this.pageSize = response.size;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
  
        this.toastr.success('Fornecedores buscados com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error?.message || err.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${err.error?.message || err.message}`)
      },
    });
  }

  viewMode: 'suppliers' | 'products' = 'suppliers';

  toggleView() {
    if (this.viewMode === 'suppliers') {
      this.viewMode = 'products';
      this.getProducts(); // busca só quando alterna
    } else {
      this.viewMode = 'suppliers';
      this.getSuppliers(); // volta a carregar fornecedores
    }
  }

  getProducts() {
    this.productItemService.getProductsFromActiveSuppliers().subscribe({
      next: (response) => {
        this.products = response.data
        console.log("salve deu boa");
        
        console.log(this.products);
        
      },
      error: (err) => {
        this.toastr.error('Erro ao buscar produtos')
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
  
        this.toastr.success('Fornecedores buscados com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${err.error.message}`);
        this.searchDone = false;
      },
    });
  }

  openModal(product: any) {
    this.productSelect = product;
    this.productModal.nativeElement.showModal();
  }

  closeModal() {
    this.productModal.nativeElement.close();
  }

  confirmRequest() {
    if (this.quantity < 1) {
      this.toastr.warning("Selecionar pelo menos 1 produto!!!")
      this.closeModal();
      return
    }

    let cart: any[] = JSON.parse(localStorage.getItem('cart-product') || '[]');
    let existing = cart.find(p => p.id === this.productSelect!!.id);

    if (existing) {
      existing.quantitySelect += 1;
    } else {
      cart.push({ ...this.productSelect, quantitySelect: this.quantity });
    }

    console.log(this.cartProduct);
    this.cartService.setCart(cart);

    this.closeModal();
  }

}
