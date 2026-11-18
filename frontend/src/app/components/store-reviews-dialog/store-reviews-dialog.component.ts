import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, Input, OnInit, Output, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SupplierService } from '../../services/supplier.service';
import { StoreReview } from '../../models/store-review';
import { StoreRatingComponent } from '../store-rating/store-rating.component';
import { fadeIn } from '../../animations/checkout.animations';
import { backendMessage } from '../../utils/backend-message';

// F7 §5.27: avaliações da loja (primeiro nome, nota, data e comentário), aberta pelas estrelas
@Component({
  selector: 'app-store-reviews-dialog',
  standalone: true,
  imports: [CommonModule, StoreRatingComponent],
  templateUrl: './store-reviews-dialog.component.html',
  animations: [fadeIn],
})
export class StoreReviewsDialogComponent implements OnInit, AfterViewInit {
  @Input() supplierId!: string;
  @Input() storeName?: string;
  @Input() average?: number | null;
  @Input() count?: number | null;
  @Output() closed = new EventEmitter<void>();
  @ViewChild('closeButton') closeButton?: ElementRef<HTMLButtonElement>;

  readonly pageSize = 10;
  readonly stars = [1, 2, 3, 4, 5];
  reviews: StoreReview[] = [];
  page = 0;
  hasNext = false;
  loading = false;
  error = '';

  constructor(private supplierService: SupplierService) {}

  ngOnInit(): void {
    this.load(0);
  }

  ngAfterViewInit(): void {
    this.closeButton?.nativeElement.focus();
  }

  @HostListener('document:keydown.escape')
  close() {
    this.closed.emit();
  }

  load(page: number) {
    this.loading = true;
    this.error = '';
    this.supplierService.getReviews(this.supplierId, page, this.pageSize).subscribe({
      next: (response) => {
        this.reviews = page === 0 ? response.data : [...this.reviews, ...response.data];
        this.page = response.page;
        this.hasNext = response.hasNext;
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.error = `Não foi possível carregar as avaliações: ${backendMessage(err)}`;
      },
    });
  }

  loadMore() {
    this.load(this.page + 1);
  }

  retry() {
    this.load(this.reviews.length ? this.page + 1 : 0);
  }

  trackById(_: number, review: StoreReview) {
    return review.id;
  }
}
