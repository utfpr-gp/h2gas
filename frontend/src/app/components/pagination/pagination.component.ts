import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  imports: [
    CommonModule,
  ],
  templateUrl: './pagination.component.html',
  styleUrl: './pagination.component.css'
})
export class PaginationComponent {
  @Input() currentPage: number = 0;
  @Input() totalPages: number = 0;

  @Output() pageChange: EventEmitter<number> = new EventEmitter<number>();

  // Gera os números de página com elipses
  getPageNumbers(): number[] {
    const totalNumbers = 5;
    const pages: number[] = [];

    if (this.totalPages <= totalNumbers) {
      for (let i = 0; i < this.totalPages; i++) pages.push(i);
    } else {
      let start = Math.max(0, this.currentPage - 2);
      let end = Math.min(this.totalPages - 1, this.currentPage + 2);

      if (start > 0) pages.push(0);
      if (start > 1) pages.push(-1);

      for (let i = start; i <= end; i++) pages.push(i);

      if (end < this.totalPages - 2) pages.push(-1);
      if (end < this.totalPages - 1) pages.push(this.totalPages - 1);
    }

    return pages;
  }

  goToPage(page: number) {
    debugger
    if (page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }

  hasPrevious(): boolean {
    return this.currentPage > 0;
  }

  hasNext(): boolean {
    return this.currentPage < this.totalPages - 1;
  }
}
