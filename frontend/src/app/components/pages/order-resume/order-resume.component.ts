import { Component, OnInit } from '@angular/core';
import { CartService } from '../../../services/cart.service';
import { CartApiService } from '../../../services/cart-api.service';
import { CartLine, CartPreview, CartPreviewItem, UNAVAILABLE_REASON_LABELS } from '../../../models/cart';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AddressService } from '../../../services/address.service';
import { User } from '../../../models/user';
import { UserService } from '../../../services/user.service';
import { Address } from '../../../models/address';
import { ToastrService } from 'ngx-toastr';
import { DeliveryMode, OrderRequest } from '../../../models/order.model';
import { OrderService } from '../../../services/order.service';
import { ActivatedRoute, Router } from '@angular/router';
import { fadeIn, listItem, popIn, stepSlide } from '../../../animations/checkout.animations';
import { LocationContextService } from '../../../services/location-context.service';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';
import { backendMessage } from '../../../utils/backend-message';

@Component({
  selector: 'app-order-resume',
  imports: [
    FormsModule,
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './order-resume.component.html',
  animations: [stepSlide, listItem, fadeIn, popIn],
})
export class OrderResumeComponent implements OnInit {

  constructor(
    private cartService: CartService,
    private cartApi: CartApiService,
    private addressService: AddressService,
    private userService: UserService,
    private toastr: ToastrService,
    private fb: FormBuilder, 
    private orderService: OrderService,
    private router: Router,
    private route: ActivatedRoute,
    private locationContext: LocationContextService,
    private supplierService: SupplierService,
  ) {
    this.formOrder = this.fb.group({
      observation: [''],
      paymentMethod: ['', Validators.required]
    })
  }

  // HU20: o navegador tem só id + quantidade (lines); o que aparece vem do preview do backend
  private lines: CartLine[] = [];
  private fallbackSupplierId: string | null = null;
  productsCarts: CartPreviewItem[] = [];
  formOrder!: FormGroup;
  order?: OrderRequest;
  userLogado?: User;
  addresses: Address[] = [];
  selectedAddressId?: number;
  isLoading: boolean = false;
  deliveryMode: DeliveryMode = 'DELIVERY';
  preview?: CartPreview;
  step: 1 | 2 | 3 = 1;
  previewLoading = false;
  orderPlaced = false;
  bumpedProductId?: number;
  private previewRequestId = 0;
  readonly steps = ['Itens', 'Entrega', 'Pagamento'];
  pickupStores: Supplier[] = [];
  pickupStoresLoading = false;
  // F7 §5.23: compra direta usa um item avulso e deixa o carrinho intacto
  buyNow = false;

  readonly paymentMethods = [
    { value: 'DINHEIRO', label: 'Dinheiro', hint: 'Pagamento na entrega', icon: 'payments' },
    { value: 'PIX', label: 'PIX', hint: 'Pagamento instantâneo na entrega', icon: 'pix' },
    { value: 'CREDIT_CARD', label: 'Cartão de Crédito', hint: 'Visa, Mastercard, Elo', icon: 'credit_card' },
  ];

  ngOnInit(): void {
    this.buyNow = this.route.snapshot.queryParamMap.get('modo') === 'compra-direta';
    if (this.buyNow) {
      const item = this.cartService.getBuyNow();
      if (!item) {
        this.toastr.warning('Escolha um produto para comprar agora.');
        this.navigate('/inicio');
        return;
      }
      this.step = 2;
    } else {
      this.cartService.clearBuyNow();
    }
    this.loadLines();
    this.userLogado = this.userService.getUserLocalStorage();
    this.deliveryMode = this.cartService.getDeliveryMode();
    this.loadAddresses();
    this.loadPreview();
  }

  nextStep() {
    if (this.step === 1 && !this.productsCarts.length) {
      this.toastr.warning('Adicione ao menos um item para continuar.');
      return;
    }
    if (this.hasUnavailable) {
      this.toastr.warning('Remova os itens indisponíveis para continuar.');
      return;
    }
    if (this.step === 2 && this.deliveryMode === 'DELIVERY' && this.selectedAddressId == null) {
      this.toastr.warning('Selecione o endereço de entrega.');
      return;
    }
    if (this.step < 3) {
      this.changeStep((this.step + 1) as 2 | 3);
      if (this.step === 2 && this.deliveryMode === 'PICKUP') {
        this.loadPickupStores();
      }
    }
  }

  previousStep() {
    if (this.step > 1) {
      this.changeStep((this.step - 1) as 1 | 2);
    }
  }

  // indicador de etapas: só volta para etapas já concluídas
  goToStep(target: number) {
    if (target >= 1 && target < this.step) {
      this.changeStep(target as 1 | 2);
    }
  }

  private changeStep(target: 1 | 2 | 3) {
    this.step = target;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  trackByProductId(_: number, product: CartPreviewItem) {
    return product.productId;
  }

  // HU20 Cenário 4: item indisponível bloqueia o checkout até sair do carrinho
  get hasUnavailable(): boolean {
    return this.productsCarts.some((item) => !item.available);
  }

  unavailableLabel(item: CartPreviewItem): string {
    return item.unavailableReason ? UNAVAILABLE_REASON_LABELS[item.unavailableReason] : '';
  }

  canIncrease(item: CartPreviewItem): boolean {
    return item.available && (item.stockQuantity == null || item.quantity < item.stockQuantity);
  }

  increaseQuantity(product: CartPreviewItem) {
    this.changeQuantity(product, product.quantity + 1);
  }

  decreaseQuantity(product: CartPreviewItem) {
    this.changeQuantity(product, product.quantity - 1);
  }

  removeItem(product: CartPreviewItem) {
    if (this.buyNow) {
      this.cartService.clearBuyNow();
      this.navigate('/inicio');
      return;
    }
    this.cartService.removeProduct(product.productId);
    this.reloadCart();
  }

  private changeQuantity(product: CartPreviewItem, quantity: number) {
    if (quantity < 1) {
      return;
    }
    if (this.buyNow) {
      this.cartService.updateBuyNowQuantity(quantity);
    } else {
      this.cartService.updateQuantity(product.productId, quantity);
    }
    this.bumpedProductId = product.productId;
    setTimeout(() => this.bumpedProductId = undefined, 150);
    this.reloadCart();
  }

  private loadLines() {
    if (!this.buyNow) {
      this.lines = this.cartService.getCart();
      this.fallbackSupplierId = this.cartService.getCartSupplierId();
    } else {
      const item = this.cartService.getBuyNow();
      this.lines = item ? [{ productId: item.productId, quantity: item.quantity }] : [];
      this.fallbackSupplierId = item?.supplierId ?? null;
    }
    this.rebuildItems();
  }

  // a quantidade vale a do navegador (muda na hora); dados e preço, os do último preview
  private rebuildItems() {
    this.productsCarts = this.lines.map((line) => {
      const fromPreview = this.preview?.items.find((item) => item.productId === line.productId);
      return { available: true, ...fromPreview, productId: line.productId, quantity: line.quantity };
    });
  }

  private reloadCart() {
    this.loadLines();
    this.loadPreview();
  }

  get orderSupplierId(): string | null {
    return this.preview?.supplierId ?? this.fallbackSupplierId;
  }

  // só as lojas dos fornecedores que ainda estão no carrinho
  get visiblePickupStores(): Supplier[] {
    return this.pickupStores.filter((s) => s.id === this.orderSupplierId);
  }

  get selectedAddress(): Address | undefined {
    return this.addresses.find((a) => a.id === this.selectedAddressId);
  }

  setDeliveryMode(mode: DeliveryMode) {
    this.deliveryMode = mode;
    this.cartService.setDeliveryMode(mode);
    this.loadPreview();
    if (mode === 'PICKUP') {
      this.loadPickupStores();
    }
  }

  // HU05: na retirada o cliente vê onde retirar — a loja de cada fornecedor do carrinho
  loadPickupStores() {
    const supplierId = this.orderSupplierId;
    if (!supplierId || this.pickupStores.some((s) => s.id === supplierId)) {
      return;
    }
    this.pickupStoresLoading = true;
    this.supplierService.findById(supplierId).subscribe({
      next: (store) => {
        this.pickupStores = [store];
        this.pickupStoresLoading = false;
      },
      error: (err) => {
        this.pickupStoresLoading = false;
        this.toastr.error(`Erro ao buscar o endereço da loja: ${backendMessage(err)}`);
      },
    });
  }

  // RN01 + HU20: dados, preço, total e disponibilidade vêm do backend
  loadPreview() {
    if (!this.lines.length) {
      this.previewRequestId++;
      this.preview = undefined;
      this.previewLoading = false;
      return;
    }
    // cliques rápidos no +/− disparam vários previews; só a resposta mais recente vale
    const requestId = ++this.previewRequestId;
    this.previewLoading = true;
    this.cartApi.preview(this.lines, this.deliveryMode).subscribe({
      next: (preview) => {
        if (requestId === this.previewRequestId) {
          this.preview = preview;
          this.previewLoading = false;
          this.rebuildItems();
          if (this.step === 2 && this.deliveryMode === 'PICKUP') this.loadPickupStores();
        }
      },
      error: (err) => {
        if (requestId === this.previewRequestId) {
          this.previewLoading = false;
        }
        this.toastr.error(`Erro ao calcular o subtotal: ${backendMessage(err)}`);
      },
    });
  }



  loadAddresses() {
    this.addressService.findAllByUser(this.userLogado?.id!).subscribe({
      next: (response) => {
        this.addresses = response;
        const activeId = this.locationContext.current()?.addressId;
        const ativo = response.find((a) => a.id === activeId) ?? response[0];
        this.selectedAddressId = ativo?.id;
      },
      error: (err) => {
        this.toastr.error(`Erro ao buscar endereços: ${backendMessage(err)}`);
      },
    });
  }

  selectAddress(address: Address) {
    this.selectedAddressId = address.id;
    this.locationContext.selectAddress(address);
  }

  createOrder() {
    if (this.step !== 3) {
      return;
    }

    if (!this.formOrder.valid) {
      this.toastr.warning('Selecione a forma de pagamento.');
      return;
    }

    if (this.deliveryMode === 'DELIVERY' && this.selectedAddressId == null) {
      this.toastr.warning('Selecione o endereço de entrega.');
      return;
    }
  
    if (this.hasUnavailable) {
      this.toastr.warning('Remova os itens indisponíveis para finalizar.');
      return;
    }

    this.isLoading = true;

    // HU14 + HU20: um pedido só, da loja do carrinho, com os ids e quantidades do navegador
    const order: OrderRequest = {
      customerUuid: this.userLogado!.id,
      supplierRef: this.orderSupplierId ?? '',
      paymentMethod: this.formOrder.get('paymentMethod')?.value,
      note: this.formOrder.get('observation')?.value,
      addressId: this.deliveryMode === 'DELIVERY' ? this.selectedAddressId : undefined,
      deliveryMode: this.deliveryMode,
      items: this.lines.map((line) => ({ productItemRef: line.productId, quantity: line.quantity })),
    };

    this.orderService.createOrder(order).subscribe({
      next: () => {
        if (this.buyNow) {
          this.cartService.clearBuyNow();
        } else {
          this.cartService.clear();
        }
        this.isLoading = false;
        this.orderPlaced = true;
        setTimeout(() => this.navigate('/meus-pedidos'), 1500);
      },
      error: err => {
        this.toastr.error(`Não foi possível finalizar o pedido: ${backendMessage(err)}`);
        this.isLoading = false;
      }
    });
  }

  navigate(route: string) {
    this.router.navigate([route])
  }
}
