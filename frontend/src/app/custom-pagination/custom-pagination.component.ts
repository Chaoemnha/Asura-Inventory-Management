import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-custom-pagination',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './custom-pagination.component.html',
  styleUrl: './custom-pagination.component.css'
})
export class CustomPaginationComponent {
  @Input() currentPage: number = 1;
  @Input() totalPages: number = 0;
  @Output() pageChange = new EventEmitter<number>();

  gotoPage(page: number): void {
    if (page !== this.currentPage && page >= 1 && page <= this.totalPages) {
      this.pageChange.emit(page);
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }
}
