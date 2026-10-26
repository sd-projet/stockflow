
import { Component, computed, input, output } from '@angular/core';

@Component({
  selector: 'app-pagination',
  standalone: true,
  templateUrl: './pagination.html',
  styleUrl: './pagination.scss'
})
export class PaginationComponent {
  totalItems = input.required<number>();
  currentPage = input.required<number>();
  pageSize = input(10);

  pageChange = output<number>();

  totalPages = computed(() =>
    Math.max(1, Math.ceil(this.totalItems() / this.pageSize()))
  );

  firstItem = computed(() =>
    this.totalItems() === 0
      ? 0
      : (this.currentPage() - 1) * this.pageSize() + 1
  );

  lastItem = computed(() =>
    Math.min(
      this.currentPage() * this.pageSize(),
      this.totalItems()
    )
  );

  previousPage(): void {
    if (this.currentPage() > 1) {
      this.pageChange.emit(this.currentPage() - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages()) {
      this.pageChange.emit(this.currentPage() + 1);
    }
  }
}
