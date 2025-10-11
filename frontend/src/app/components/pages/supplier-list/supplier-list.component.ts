import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';
import { ToastrService } from 'ngx-toastr';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';

@Component({
  selector: 'app-supplier-list',
  imports: [
    FormsModule,
    CommonModule,
    ReactiveFormsModule,
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



  getSuppliers() {
    this.supplierService.getAllSupplierOpenAndActive().subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.suppliers = response.data
        console.log(this.suppliers!.length);
        
        this.toastr.success('Fornecedores buscados com sucesso!');
      },
      error: (err) => {
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar fornecedores: ${err.error.message}`)
      },
    })
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

  navigate(id?: number) {
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


}
