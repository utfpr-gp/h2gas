import { CommonModule } from '@angular/common';
import { Component, HostListener, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user';
import { ProductItem } from '../../models/product-item';

@Component({
  selector: 'app-menu',
  imports: [
    CommonModule
  ],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css'
})
export class MenuComponent implements OnInit {
  userMenuOpen = false;
  showDropdown = false;
  user?: User;
  cartProduct?: ProductItem[];


  constructor(
    private router: Router,
    private authService: AuthService
  ) {

  }

  ngOnInit(): void {
    const userJson = localStorage.getItem('user');
    console.log("naquele pique");
    
    this.cartProduct = JSON.parse(localStorage.getItem('cart-product')!!)
    console.log(this.cartProduct);
    
    if (userJson) {
      this.user = JSON.parse(userJson);
    }
  }

  isCartOpen = false;
cartProducts = [
  { name: 'Produto 1', price: 50 },
  { name: 'Produto 2', price: 30 }
];

get totalPrice() {
  return this.cartProducts.reduce((sum, p) => sum + p.price, 0);
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
