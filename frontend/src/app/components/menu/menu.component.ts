import { CommonModule } from '@angular/common';
import { Component, HostListener, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user';
import { ProductItem } from '../../models/product-item';
import { DeliveryMode } from '../../models/order.model';
import { CartService } from '../../services/cart.service';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { LocationContextService } from '../../services/location-context.service';
import { BottomTabBarComponent } from '../bottom-tab-bar/bottom-tab-bar.component';
import { DelivererTabBarComponent } from '../deliverer-tab-bar/deliverer-tab-bar.component';

@Component({
  selector: 'app-menu',
  imports: [
    CommonModule,
    FormsModule,
    BottomTabBarComponent,
    DelivererTabBarComponent
  ],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css'
})
export class MenuComponent implements OnInit {

  constructor(
    private router: Router,
    private authService: AuthService,
    private cartService: CartService,
    private toastr: ToastrService,
    public locationContext: LocationContextService,
  ) {}

  userMenuOpen = false;
  showDropdown = false;
  user?: User;
  cartProducts?: ProductItem[];
  isCartOpen = false;
  deliveryMode: DeliveryMode = 'DELIVERY';

  quantity: number = 1;
  min: number = 1;
  max: number = 10;

  mobileMenuOpen = false;
  submenuOpen = false;

  productsMenuOpen = false;
  toggleProductsMenu() {
    this.productsMenuOpen = !this.productsMenuOpen;
  }


  toggleMobileMenu() {
    this.mobileMenuOpen = !this.mobileMenuOpen;
    if (!this.mobileMenuOpen) this.submenuOpen = false;
  }

  toggleSubmenu() {
    this.submenuOpen = !this.submenuOpen;
  }

  onQuantityInput(cartProduct?: ProductItem) {
    if (cartProduct!.quantitySelect! < this.min) {
      cartProduct!.quantitySelect = this.min;
      this.toastr.warning("Por favor coloque um valor positivo!!");
    }
  }

  ngOnInit(): void {
    const userJson = localStorage.getItem('user');
    this.cartService.cartProducts$.subscribe(cart => {
      this.cartProducts = cart;
      console.log('Carrinho atualizado:', this.cartProducts);
    });
    this.cartService.deliveryMode$.subscribe(mode => this.deliveryMode = mode);

    if (userJson) {
      this.user = JSON.parse(userJson);
    }
  }


  // HU07: o entregador só vê a marca e "Sair" (sem catálogo, carrinho ou links do cliente)
  get isDeliverer(): boolean {
    return this.user?.userType === 'DELIVERER';
  }

  unitPriceForMode(product: ProductItem): number | undefined {
    return this.deliveryMode === 'PICKUP' ? product.pricePickup : product.priceDelivery;
  }

  // estimativa do drawer; o valor cobrado vem do preview do backend no checkout (RN01)
  get totalPrice() {
    return this.cartProducts!.reduce((sum, item) => sum + (this.unitPriceForMode(item) ?? 0) * item.quantitySelect!, 0);
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

  editAddress() {
    this.router.navigate(['/meus-enderecos']);
  }

  @HostListener('document:click', ['$event'])
  clickOutside(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.relative')) {
      this.userMenuOpen = false;
    }
  }

  minusClickCart(cartProduct: ProductItem) {
    cartProduct!.quantitySelect = (cartProduct!.quantitySelect ?? this.min) > this.min
      ? (cartProduct!.quantitySelect ?? this.min) - 1
      : (cartProduct!.quantitySelect ?? this.min);
  }

  deleteItemCart(cartProduct: ProductItem) {
    this.cartService.setCart(this.cartService.getCart().filter(item => item.id !== cartProduct.id));
  }

  plusClickCart(cartProduct: ProductItem) {
    cartProduct!.quantitySelect = (cartProduct!.quantitySelect ?? 0) + 1
  }

  logout() {
    this.authService.logout();
    this.navigate('/login');
  }

}
