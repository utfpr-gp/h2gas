import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ToastrService } from 'ngx-toastr';
import { ProductItemService } from '../../../services/product-item.service';
import { ProductItem } from '../../../models/product-item';

@Component({
  selector: 'app-product-list',
  imports: [
    FormsModule,
    CommonModule
  ],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css'
})
export class ProductListComponent implements OnInit {
  constructor(
    private toastr: ToastrService,
    private router: Router,
    private routeActive: ActivatedRoute,
    private productItemService: ProductItemService
  ) {}
  @ViewChild('productModal') productModal!: ElementRef<HTMLDialogElement>;
  idSupplier?: number;
  products?: ProductItem[];
  productSelect?: ProductItem = new ProductItem();
  quantity: number = 1;
  cartProduct?: ProductItem[] = [];

  ngOnInit(): void {
    this.idSupplier = parseInt(this.routeActive.snapshot.paramMap.get('id') || '', 10);
    console.log(this.idSupplier);
    this.getProducts(this.idSupplier);
    // localStorage.setItem('cart-product', JSON.stringify(null))
  }

  getProducts(supplierId: number) {
    this.productItemService.getProductsBySupllierId(supplierId).subscribe({
      next: (response) => {
        console.log('Buscado com sucesso', response);
        this.products = response.data
        console.log(this.products);
        
        this.toastr.success('Produtos encontrado com sucesso!');
      },
      error: (err) => {
        debugger
        console.error('Erro ao buscar:', err.error.message);
        this.toastr.error(`Erro ao buscar produtos: ${err.error.message}`)
      },
    })
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

    console.log("salveeee");
    
    console.log(this.cartProduct);
    localStorage.setItem('cart-product', JSON.stringify(cart))
    
    // this.cartProduct = cart;

    this.closeModal();
  }


}
