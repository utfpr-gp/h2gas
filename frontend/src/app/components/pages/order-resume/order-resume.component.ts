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
import { LocationContextService } from '../../../services/location-context.service';

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
    private locationContext: LocationContextService,
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

  setDeliveryMode(mode: DeliveryMode) {
    this.deliveryMode = mode;
    this.cartService.setDeliveryMode(mode);
    this.loadPreview();
  }

  // RN01: preço unitário, total por item e subtotal vêm do backend
  loadPreview() {
    if (!this.productsCarts?.length) {
      this.preview = undefined;
      return;
    }
    this.orderService.previewOrder({
      items: this.toOrderItemRequests(this.productsCarts),
      deliveryMode: this.deliveryMode,
    }).subscribe({
      next: (preview) => this.preview = preview,
      error: (err) => this.toastr.error(`Erro ao calcular o subtotal: ${err.error?.message || err.message}`),
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
        this.toastr.error(`Erro ao buscar endereços: ${err.error?.message || err.message}`);
      },
    });
  }

  selectAddress(address: Address) {
    this.selectedAddressId = address.id;
    this.locationContext.selectAddress(address);
  }

  createOrder() {
    if (!this.formOrder.valid) {
      this.toastr.error("Por favor selecione um meio de pagamento!!");
      return;
    }

    if (this.selectedAddressId == null) {
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
          addressId: this.selectedAddressId,
          deliveryMode: this.deliveryMode,
          items: this.toOrderItemRequests(products!)
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
