import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user';
import { ProductItem } from '../../models/product-item';
import { DeliveryMode } from '../../models/order.model';
import { CartService } from '../../services/cart.service';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import { LocationContextService } from '../../services/location-context.service';
import { UserService } from '../../services/user.service';
import { initials } from '../../utils/initials';
import { homeFor } from '../../utils/home-route';
import { expandCollapse, fadeIn } from '../../animations/checkout.animations';
import { Subscription, filter, skip } from 'rxjs';
import { BottomTabBarComponent } from '../bottom-tab-bar/bottom-tab-bar.component';
import { DelivererTabBarComponent } from '../deliverer-tab-bar/deliverer-tab-bar.component';

@Component({
  selector: 'app-menu',
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    RouterLinkActive,
    BottomTabBarComponent,
    DelivererTabBarComponent
  ],
  templateUrl: './menu.component.html',
  animations: [fadeIn, expandCollapse]
})
export class MenuComponent implements OnInit, OnDestroy {

  constructor(
    private router: Router,
    private authService: AuthService,
    private cartService: CartService,
    private toastr: ToastrService,
    public locationContext: LocationContextService,
    private userService: UserService,
    private host: ElementRef<HTMLElement>,
  ) {}

  private userSub?: Subscription;
  private navigationSub?: Subscription;

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
      this.toastr.warning('Informe um valor positivo.');
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
    // o avatar acompanha os dados salvos em "Meus Dados". skip(1): o valor inicial do fluxo é o do início
    // do app e pode ser de outra sessão; ao abrir o menu vale o que está no navegador (lido acima)
    this.userSub = this.userService.currentUser$.pipe(skip(1)).subscribe((user) => {
      if (user) this.user = user;
    });
    this.navigationSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.closeNavMenus());
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
    this.navigationSub?.unsubscribe();
  }

  private closeNavMenus() {
    this.mobileMenuOpen = false;
    this.submenuOpen = false;
    this.productsMenuOpen = false;
  }

  get homeRoute(): string {
    return homeFor(this.user?.userType);
  }

  get userInitials(): string {
    return initials(this.user?.name);
  }

  // itens do dropdown por perfil; "Sair" fica separado no fim
  get userMenuItems(): { id: string; label: string; icon: string; route: string }[] {
    const meusDados = { id: 'meus-dados', label: 'Meus Dados', icon: 'person', route: '/meus-dados' };
    switch (this.user?.userType) {
      case 'CUSTOMER':
        return [
          meusDados,
          { id: 'meus-pedidos', label: 'Meus Pedidos', icon: 'receipt_long', route: '/meus-pedidos' },
          { id: 'meus-enderecos', label: 'Meus Endereços', icon: 'location_on', route: '/meus-enderecos' },
        ];
      case 'SUPPLIER':
        return [
          meusDados,
          { id: 'dashboard', label: 'Dashboard', icon: 'insights', route: '/dashboard' },
          { id: 'pedidos-recebidos', label: 'Pedidos recebidos', icon: 'inbox', route: '/dashboard-fornecedor' },
          { id: 'configuracao', label: 'Configurações', icon: 'settings', route: '/configuracao' },
        ];
      case 'ADMIN':
        return [
          meusDados,
          { id: 'pedidos-recebidos', label: 'Pedidos recebidos', icon: 'inbox', route: '/dashboard-fornecedor' },
          { id: 'configuracao', label: 'Configurações', icon: 'settings', route: '/configuracao' },
        ];
      default:
        return [meusDados];
    }
  }

  // o getter acima devolve objetos novos a cada ciclo; sem trackBy o ngFor recria os botões e o foco se perde
  trackByItemId(_: number, item: { id: string }) {
    return item.id;
  }

  openUserMenuItem(route: string) {
    this.closeUserMenu();
    this.navigate(route);
  }


  // HU07: o entregador só vê a marca e "Sair" (sem catálogo, carrinho ou links do cliente)
  get isDeliverer(): boolean {
    return this.user?.userType === 'DELIVERER';
  }

  get isCustomer(): boolean {
    return this.user?.userType === 'CUSTOMER';
  }

  get isSupplier(): boolean {
    return this.user?.userType === 'SUPPLIER';
  }

  get isAdmin(): boolean {
    return this.user?.userType === 'ADMIN';
  }

  get hasMobileMenu(): boolean {
    return this.isSupplier || this.isAdmin;
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
    if (this.userMenuOpen) {
      // teclado/leitor de tela: o foco vai para o primeiro item assim que o dropdown aparece
      setTimeout(() => this.host.nativeElement.querySelector<HTMLElement>('[data-testid=user-menu] [role=menuitem]')?.focus());
    }
  }

  closeUserMenu(returnFocus = false) {
    this.userMenuOpen = false;
    if (returnFocus) {
      this.host.nativeElement.querySelector<HTMLElement>('[data-testid=user-menu-button]')?.focus();
    }
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    if (this.userMenuOpen) this.closeUserMenu(true);
    if (this.mobileMenuOpen) {
      this.closeNavMenus();
      this.host.nativeElement.querySelector<HTMLElement>('[data-testid=mobile-menu-button]')?.focus();
    }
    this.productsMenuOpen = false;
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
    if (!target.closest('[data-user-menu]')) {
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
