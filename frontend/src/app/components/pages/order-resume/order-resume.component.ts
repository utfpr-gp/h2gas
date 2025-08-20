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
    private orderService: OrderService
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
        console.log("aeee");
        console.log(this.address);
        
      },
      error: (err) => {
        console.log(err);
        this.toastr.error(`Erro ao buscar endereço: ${err.error.message}`);
      },
    })
  }

  createOrder() {
    if (!this.formOrder.valid) {
      this.toastr.error("Por favor selecione um meio de pagamento!!")
      return
    }
    debugger
    let orderItemRequests: OrderItemRequest[] = [];

    this.productsCarts!.forEach(element => {
      orderItemRequests.push({
        productItemRef: element.id ?? 0, 
        quantity: element.quantitySelect ?? 0
      })
    });

    this.order = {
      customerUuid: this.userLogado!.id,
      supplierRef: this.productsCarts![0].supplierId,
      paymentMethod: this.formOrder.get('paymentMethod')?.value, 
      note: this.formOrder.get('observation')?.value,
      items: orderItemRequests
    }
    debugger
    this.orderService.createOrder(this.order).subscribe({
      next: (value) => {
        debugger
        console.log(value);
        this.cartService.setCart([]);
        this.toastr.success("Pedido realizado com sucesso")
      },
      error: (err) => {
        this.toastr.error(`Falha ao realizar o pedido: ${err.error.message}`)
      },
    })
    
  }
  

}
