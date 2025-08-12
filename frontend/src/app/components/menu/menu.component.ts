import { CommonModule } from '@angular/common';
import { Component, HostListener, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user';
import { ProductItem } from '../../models/product-item';
import { CartService } from '../../services/cart.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-menu',
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css'
})
export class MenuComponent implements OnInit {
  userMenuOpen = false;
  showDropdown = false;
  user?: User;
  cartProductss?: ProductItem[];

  quantity: number = 1;
  min: number = 1;
  max: number = 10;

  onQuantityInput() {
    if (this.quantity < this.min) {
      this.quantity = this.min;
    }
  }
  constructor(
    private router: Router,
    private authService: AuthService,
    private cartService: CartService
  ) {

  }

  ngOnInit(): void {
    const userJson = localStorage.getItem('user');
    console.log("naquele pique");

    // this.cartProduct = JSON.parse(localStorage.getItem('cart-product')!!)
    // console.log(this.cartProduct);

    this.cartService.cartProducts$.subscribe(cart => {
      this.cartProductss = cart;
      console.log('Carrinho atualizado:', this.cartProductss);
    });

    if (userJson) {
      this.user = JSON.parse(userJson);
    }
  }

  isCartOpen = false;
  cartProductTeste = [
    { name: 'Produto 1', price: 50 },
    { name: 'Produto 2', price: 30 }
  ];

  get totalPrice() {
    return this.cartProductss!.reduce((sum, p) => sum + p.price!, 0);
  }


  toggleUserMenu() {
    this.userMenuOpen = !this.userMenuOpen;
  }

  closeUserMenu() {
    this.userMenuOpen = false;
  }

  navigate(route: string) {
    this.router.navigate([route])
  }

  @HostListener('document:click', ['$event'])
  clickOutside(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.relative')) {
      this.userMenuOpen = false;
    }
  }

  logout() {
    this.authService.logout()
  }

}
