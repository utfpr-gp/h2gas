import { Component, OnInit } from '@angular/core';
import { ProductItem } from '../../../models/product-item';
import { CartService } from '../../../services/cart.service';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, Validators } from '@angular/forms';
import { AddressService } from '../../../services/address.service';
import { User } from '../../../models/user';
import { UserService } from '../../../services/user.service';
import { Address } from '../../../models/address';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-order-resume',
  imports: [
    FormsModule,
    CommonModule
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
  ) {
    this.formOrder = this.fb.group({
      observation: [''],
    })
  }

  productsCarts?: ProductItem[];
  formOrder!: FormGroup;
  // order?:
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
  

}
