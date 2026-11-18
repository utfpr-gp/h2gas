import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastrService } from 'ngx-toastr';
import { OrderService } from '../../services/order.service';
import { OrderResponse, OrderReview } from '../../models/order.model';
import { fadeIn } from '../../animations/checkout.animations';
import { backendMessage } from '../../utils/backend-message';

const STAR_LABELS = ['', 'Muito ruim', 'Ruim', 'Regular', 'Bom', 'Ótimo'];

// HU24: o cliente dá de 1 a 5 estrelas para a loja e, se houve entregador, para ele
@Component({
  selector: 'app-order-review-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-review-dialog.component.html',
  animations: [fadeIn],
})
export class OrderReviewDialogComponent implements AfterViewInit {
  @Input() order!: OrderResponse;
  @Output() saved = new EventEmitter<OrderReview>();
  @Output() closed = new EventEmitter<void>();

  readonly stars = [1, 2, 3, 4, 5];
  readonly maxComment = 500;
  storeRating = 0;
  delivererRating = 0;
  comment = '';
  sending = false;
  error = '';

  constructor(private orderService: OrderService, private toastr: ToastrService, private host: ElementRef<HTMLElement>) {}

  ngAfterViewInit(): void {
    this.host.nativeElement.querySelector<HTMLInputElement>('[data-testid="store-star-1"]')?.focus();
  }

  get hasDeliverer(): boolean {
    return !!this.order.delivererName && this.order.deliveryMode !== 'PICKUP';
  }

  label(rating: number): string {
    return STAR_LABELS[rating] ?? '';
  }

  setComment(value: string) {
    this.comment = value.slice(0, this.maxComment);
  }

  @HostListener('document:keydown.escape')
  close() {
    if (!this.sending) this.closed.emit();
  }

  submit() {
    if (!this.storeRating || this.sending) return;
    this.sending = true;
    this.error = '';
    const review: OrderReview = {
      storeRating: this.storeRating,
      delivererRating: this.hasDeliverer && this.delivererRating ? this.delivererRating : null,
      comment: this.comment.trim() || null,
    };
    this.orderService.reviewOrder(this.order.id, review).subscribe({
      next: (saved) => {
        this.sending = false;
        this.toastr.success('Obrigado pela avaliação!');
        this.saved.emit(saved);
      },
      error: (err) => {
        this.sending = false;
        this.error = `Não foi possível enviar: ${backendMessage(err)}`;
      },
    });
  }
}
