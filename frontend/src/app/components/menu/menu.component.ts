import { CommonModule } from '@angular/common';
import { navigateKeepingReturnUrl } from '../../utils/return-url';
import { Component, ElementRef, HostListener, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { User } from '../../models/user';
import { DeliveryMode } from '../../models/order.model';
import { CartService } from '../../services/cart.service';
import { CartApiService } from '../../services/cart-api.service';
import { CartLine, CartPreview, CartPreviewItem, UNAVAILABLE_REASON_LABELS } from '../../models/cart';
import { backendMessage } from '../../utils/backend-message';
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
    private cartApi: CartApiService,
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
  // HU20: o navegador só tem id + quantidade; o drawer mostra o que o backend devolve
  cartLines: CartLine[] = [];
  cartCount = 0;
  cartPreview?: CartPreview;
  cartPreviewLoading = false;
  private cartPreviewRequestId = 0;
  isCartOpen = false;
  deliveryMode: DeliveryMode = 'DELIVERY';

  min: number = 1;

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

  ngOnInit(): void {
    const userJson = localStorage.getItem('user');
    this.cartService.cartProducts$.subscribe(lines => {
      this.cartLines = lines;
      if (this.isCartOpen) this.loadCartPreview();
    });
    this.cartService.count$.subscribe(count => this.cartCount = count);
    this.cartService.deliveryMode$.subscribe(mode => {
      this.deliveryMode = mode;
      if (this.isCartOpen) this.loadCartPreview();
    });

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
      .subscribe(() => {
        this.closeNavMenus();
        // HU13: a guarda pode ter descartado uma sessão vencida — o menu acompanha o que está salvo
        this.user = this.userService.getUserLocalStorage() ?? undefined;
      });
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
          { id: 'minha-loja', label: 'Minha loja', icon: 'storefront', route: '/minha-loja' },
          { id: 'dashboard', label: 'Dashboard', icon: 'insights', route: '/minha-loja/painel' },
          { id: 'pedidos-recebidos', label: 'Pedidos recebidos', icon: 'inbox', route: '/minha-loja/pedidos' },
          { id: 'configuracao', label: 'Configurações', icon: 'settings', route: '/minha-loja/configuracoes' },
        ];
      case 'ADMIN':
        return [
          meusDados,
          { id: 'painel', label: 'Painel', icon: 'space_dashboard', route: '/admin' },
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

  // HU13: sem login, a vitrine trata o visitante como quem compra (barra clara, cidade, carrinho) e oferece "Entrar"
  get isVisitor(): boolean {
    return !this.user;
  }

  get isShopper(): boolean {
    return this.isCustomer || this.isVisitor;
  }

  visitorLogin() {
    navigateKeepingReturnUrl(this.router, '/login', this.router.url);
  }

  // F7 §5.22: a barra do cliente é clara (fundo branco, texto neutro, ativo em azul suave); fornecedor, admin e
  // entregador mantêm a barra escura. Classes literais aqui para o Tailwind gerar.
  get t(): { bar: string; text: string; hover: string; active: string; ring: string; brand: string } {
    return this.isShopper
      ? { bar: 'bg-white border-b border-gray-200', text: 'text-gray-700', hover: 'hover:bg-primary/5',
          active: 'bg-primary/10 text-primary', ring: 'focus-visible:ring-primary', brand: 'text-primary' }
      : { bar: 'bg-primary', text: 'text-white', hover: 'hover:bg-white/10',
          active: 'bg-white/15', ring: 'focus-visible:ring-white', brand: 'text-white' };
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

  // HU20: ao abrir o drawer, os dados, o preço (RN01) e a disponibilidade vêm do backend
  onCartToggle(open: boolean) {
    this.isCartOpen = open;
    if (open) this.loadCartPreview();
  }

  loadCartPreview() {
    const requestId = ++this.cartPreviewRequestId;
    if (!this.cartLines.length) {
      this.cartPreview = undefined;
      this.cartPreviewLoading = false;
      return;
    }
    this.cartPreviewLoading = true;
    this.cartApi.preview(this.cartLines, this.deliveryMode).subscribe({
      next: (preview) => {
        if (requestId !== this.cartPreviewRequestId) return; // cliques rápidos: só a resposta mais nova vale
        this.cartPreview = preview;
        this.cartPreviewLoading = false;
      },
      error: (err) => {
        if (requestId !== this.cartPreviewRequestId) return;
        this.cartPreviewLoading = false;
        this.toastr.error(`Erro ao carregar o carrinho: ${backendMessage(err)}`);
      },
    });
  }

  get cartHasUnavailable(): boolean {
    return !!this.cartPreview?.items.some((item) => !item.available);
  }

  unavailableLabel(item: CartPreviewItem): string {
    return item.unavailableReason ? UNAVAILABLE_REASON_LABELS[item.unavailableReason] : '';
  }

  trackByCartItem(_: number, item: CartPreviewItem) {
    return item.productId;
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
    if (this.isVisitor) {
      navigateKeepingReturnUrl(this.router, '/onde-voce-esta', this.router.url); // HU13: o visitante troca o CEP
      return;
    }
    this.router.navigate(['/meus-enderecos']);
  }

  @HostListener('document:click', ['$event'])
  clickOutside(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('[data-user-menu]')) {
      this.userMenuOpen = false;
    }
  }

  minusClickCart(item: CartPreviewItem) {
    if (item.quantity > this.min) this.cartService.updateQuantity(item.productId, item.quantity - 1);
  }

  deleteItemCart(item: CartPreviewItem) {
    this.cartService.removeProduct(item.productId);
  }

  plusClickCart(item: CartPreviewItem) {
    if (item.quantity < (item.stockQuantity ?? 0)) this.cartService.updateQuantity(item.productId, item.quantity + 1);
  }

  checkout() {
    if (this.cartHasUnavailable) return;
    const toggle = this.host.nativeElement.querySelector<HTMLInputElement>('#cart-drawer');
    if (toggle) toggle.checked = false;
    this.isCartOpen = false;
    this.navigate('resumo-pedido');
  }

  logout() {
    this.authService.logout();
    this.navigate('/login');
  }

}
