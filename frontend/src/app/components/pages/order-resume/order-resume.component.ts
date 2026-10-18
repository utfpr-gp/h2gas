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
import { DeliveryMode, OrderPreview, OrderRequest } from '../../../models/order.model';
import { OrderItemRequest } from '../../../models/order-item.model';
import { OrderService } from '../../../services/order.service';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { fadeIn, listItem, popIn, stepSlide } from '../../../animations/checkout.animations';
import { LocationContextService } from '../../../services/location-context.service';
import { SupplierService } from '../../../services/supplier.service';
import { Supplier } from '../../../models/supplier';

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
    private addressService: AddressService,
    private userService: UserService,
    private toastr: ToastrService,
    private fb: FormBuilder, 
    private orderService: OrderService,
    private router: Router,
    private locationContext: LocationContextService,
    private supplierService: SupplierService,
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
  addresses: Address[] = [];
  selectedAddressId?: number;
  isLoading: boolean = false;
  deliveryMode: DeliveryMode = 'DELIVERY';
  preview?: OrderPreview;
  step: 1 | 2 | 3 = 1;
  previewLoading = false;
  orderPlaced = false;
  bumpedProductId?: number;
  private previewRequestId = 0;
  readonly steps = ['Itens', 'Entrega', 'Pagamento'];
  pickupStores: Supplier[] = [];
  pickupStoresLoading = false;

  readonly paymentMethods = [
    { value: 'DINHEIRO', label: 'Dinheiro', hint: 'Pagamento na entrega', icon: 'payments' },
    { value: 'PIX', label: 'PIX', hint: 'Pagamento instantâneo na entrega', icon: 'pix' },
    { value: 'CREDIT_CARD', label: 'Cartão de Crédito', hint: 'Visa, Mastercard, Elo', icon: 'credit_card' },
  ];

  ngOnInit(): void {
    this.productsCarts = this.cartService.getCart();
    this.userLogado = this.userService.getUserLocalStorage();
    this.deliveryMode = this.cartService.getDeliveryMode();
    this.loadAddresses();
    this.loadPreview();
  }

  nextStep() {
    if (this.step === 1 && !this.productsCarts?.length) {
      this.toastr.warning('Adicione ao menos um item para continuar.');
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

  trackByProductId(_: number, product: ProductItem) {
    return product.id;
  }

  increaseQuantity(product: ProductItem) {
    this.changeQuantity(product, (product.quantitySelect ?? 1) + 1);
  }

  decreaseQuantity(product: ProductItem) {
    this.changeQuantity(product, (product.quantitySelect ?? 1) - 1);
  }

  removeItem(product: ProductItem) {
    this.cartService.removeProduct(product.id!);
    this.reloadCart();
  }

  private changeQuantity(product: ProductItem, quantity: number) {
    if (quantity < 1) {
      return;
    }
    this.cartService.updateQuantity(product.id!, quantity);
    this.bumpedProductId = product.id;
    setTimeout(() => this.bumpedProductId = undefined, 150);
    this.reloadCart();
  }

  private reloadCart() {
    this.productsCarts = this.cartService.getCart();
    this.loadPreview();
  }

  // só as lojas dos fornecedores que ainda estão no carrinho
  get visiblePickupStores(): Supplier[] {
    const supplierIds = (this.productsCarts ?? []).map((p) => p.supplierId);
    return this.pickupStores.filter((s) => supplierIds.includes(s.id!));
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
    const supplierIds = [...new Set((this.productsCarts ?? []).map((p) => p.supplierId).filter(Boolean))];
    const jaCarregadas = this.pickupStores.map((s) => s.id);
    if (!supplierIds.length || supplierIds.every((id) => jaCarregadas.includes(id))) {
      return;
    }
    this.pickupStoresLoading = true;
    forkJoin(supplierIds.map((id) => this.supplierService.findById(id))).subscribe({
      next: (stores) => {
        this.pickupStores = stores;
        this.pickupStoresLoading = false;
      },
      error: (err) => {
        this.pickupStoresLoading = false;
        this.toastr.error(`Erro ao buscar o endereço da loja: ${this.backendMessage(err)}`);
      },
    });
  }

  // RN01: preço unitário, total por item e subtotal vêm do backend
  loadPreview() {
    if (!this.productsCarts?.length) {
      this.previewRequestId++;
      this.preview = undefined;
      this.previewLoading = false;
      return;
    }
    // cliques rápidos no +/− disparam vários previews; só a resposta mais recente vale
    const requestId = ++this.previewRequestId;
    this.previewLoading = true;
    this.orderService.previewOrder({
      items: this.toOrderItemRequests(this.productsCarts),
      deliveryMode: this.deliveryMode,
    }).subscribe({
      next: (preview) => {
        if (requestId === this.previewRequestId) {
          this.preview = preview;
          this.previewLoading = false;
        }
      },
      error: (err) => {
        if (requestId === this.previewRequestId) {
          this.previewLoading = false;
        }
        this.toastr.error(`Erro ao calcular o subtotal: ${this.backendMessage(err)}`);
      },
    });
  }

  previewItem(product: ProductItem) {
    return this.preview?.items.find((item) => item.productItemRef === product.id);
  }

  private toOrderItemRequests(products: ProductItem[]): OrderItemRequest[] {
    return products.map(p => ({
      productItemRef: p.id ?? 0,
      quantity: p.quantitySelect ?? 0
    }));
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
        this.toastr.error(`Erro ao buscar endereços: ${this.backendMessage(err)}`);
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
      this.toastr.error("Por favor selecione um meio de pagamento!!");
      return;
    }

    if (this.deliveryMode === 'DELIVERY' && this.selectedAddressId == null) {
      this.toastr.error("Selecione o endereço de entrega.");
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
        const order: OrderRequest = {
          customerUuid: this.userLogado!.id,
          supplierRef: supplierId,
          paymentMethod: this.formOrder.get('paymentMethod')?.value,
          note: this.formOrder.get('observation')?.value,
          addressId: this.deliveryMode === 'DELIVERY' ? this.selectedAddressId : undefined,
          deliveryMode: this.deliveryMode,
          items: this.toOrderItemRequests(products!)
        };
  
        return this.orderService.createOrder(order);
      }
    );
  
    forkJoin(pedidosObservables).subscribe({
      next: results => {
        this.cartService.setCart([]);
        this.isLoading = false;
        this.orderPlaced = true;
        setTimeout(() => this.navigate('/meus-pedidos'), 1500);
      },
      error: err => {
        this.toastr.error(`Falha ao realizar o pedido: ${this.backendMessage(err)}`);
        this.isLoading = false;
      }
    });
  }

  // o GlobalExceptionHandler devolve { error } para regra de negócio e { message } para os demais erros
  private backendMessage(err: any): string {
    return err?.error?.error || err?.error?.message || err?.message || 'erro inesperado';
  }

  navigate(route: string) {
    this.router.navigate([route])
  }
}
