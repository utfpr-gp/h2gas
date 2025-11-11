import { Component, OnInit } from '@angular/core';
import { ProductItem } from '../../../models/product-item';
import { CartService } from '../../../services/cart.service';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AddressService } from '../../../services/address.service';
import { User } from '../../../models/user';
import { UserService } from '../../../services/user.service';
import { Address } from '../../../models/address';
import { ToastrService } from 'ngx-toastr';
import { OrderRequest } from '../../../models/order.model';
import { OrderItemRequest } from '../../../models/order-item.model';
import { OrderService } from '../../../services/order.service';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-order-resume',
  imports: [
    FormsModule,
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './order-resume.component.html',
  styleUrl: './order-resume.component.css'
})
export class OrderResumeComponent implements OnInit {

  constructor(
    private cartService: CartService,
    private addressService: AddressService,
    private userService: UserService,
    private toastr: ToastrService,
    private fb: FormBuilder, 
    private orderService: OrderService,
    private router: Router,
  ) {
    this.formOrder = this.fb.group({
      observation: [''],
      paymentMethod: ['', Validators.required]
    })
  }

  productsCarts?: ProductItem[];
  formOrder!: FormGroup;
  order?: OrderRequest;
  userLogado?: User;
  address?: Address;
  isLoading: boolean = false;

  ngOnInit(): void {
    this.productsCarts = this.cartService.getCart();
    this.userLogado = this.userService.getUserLocalStorage();
    console.log(this.userLogado);
    console.log(this.productsCarts);
    this.getAddres();
  }

  get totalPrice() {
    return this.productsCarts!.reduce((sum, item) => sum + item.price! * item.quantitySelect!, 0);
  }

  totalPerUnit(product: ProductItem): number {
    if (product.price && product.quantitySelect) {
      return product.price * product!.quantitySelect;
    } else {
      return 0;
    }
  }

  getAddres() {
    this.addressService.findByUserId(this.userLogado?.id!!).subscribe({
      next: (response) => {
        console.log(response);
        this.address = response;
      },
      error: (err) => {
        console.log(err);
        this.toastr.error(`Erro ao buscar endereço: ${err.error.message}`);
      },
    })
  }

  createOrder() {
    if (!this.formOrder.valid) {
      this.toastr.error("Por favor selecione um meio de pagamento!!");
      return;
    }
  
    this.isLoading = true;
  
    const productsBySupplier = new Map<string, typeof this.productsCarts>();
    this.productsCarts!.forEach(p => {
      if (!productsBySupplier.has(p.supplierId)) {
        productsBySupplier.set(p.supplierId, []);
      }
      productsBySupplier.get(p.supplierId)!.push(p);
    });
  
    const pedidosObservables = Array.from(productsBySupplier.entries()).map(
      ([supplierId, products]) => {
        const orderItemRequests: OrderItemRequest[] = products!.map(p => ({
          productItemRef: p.id ?? 0,
          quantity: p.quantitySelect ?? 0
        }));
  
        const order = {
          customerUuid: this.userLogado!.id,
          supplierRef: supplierId,
          paymentMethod: this.formOrder.get('paymentMethod')?.value,
          note: this.formOrder.get('observation')?.value,
          items: orderItemRequests
        };
  
        return this.orderService.createOrder(order);
      }
    );
  
    forkJoin(pedidosObservables).subscribe({
      next: results => {
        console.log("Todos os pedidos criados:", results);
        this.cartService.setCart([]);
  
        setTimeout(() => {
          this.toastr.success("Pedidos realizados com sucesso");
          this.isLoading = false;
          this.navigate('/meus-pedidos');
        }, 800); 
      },
      error: err => {
        this.toastr.error(`Falha ao realizar o pedido: ${err.error.message}`);
        this.isLoading = false;
      }
    });
  }

  navigate(route: string) {
    this.router.navigate([route])
  }
}
