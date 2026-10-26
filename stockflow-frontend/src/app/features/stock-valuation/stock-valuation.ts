import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Product } from '../products/product';
import { ProductApi } from '../products/product-api';

@Component({
  selector: 'app-stock-valuation',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, FormsModule, RouterLink],
  templateUrl: './stock-valuation.html',
  styleUrl: './stock-valuation.scss'
})
export class StockValuation implements OnInit {
  private readonly productApi = inject(ProductApi);
  private readonly router = inject(Router);

  readonly products = signal<Product[]>([]);
  readonly searchTerm = signal('');
  readonly loading = signal(false);
  readonly errorMessage = signal('');

  readonly filteredProducts = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('fr');
    const products = this.products();

    if (!term) {
      return products;
    }

    return products.filter(product =>
      product.reference.toLocaleLowerCase('fr').includes(term) ||
      product.name.toLocaleLowerCase('fr').includes(term) ||
      (product.categoryName ?? '').toLocaleLowerCase('fr').includes(term)
    );
  });

  readonly totalStockValue = computed(() =>
    this.products().reduce(
      (totalCents, product) => totalCents + this.productStockValueCents(product),
      0
    ) / 100
  );

  readonly displayedProductsValue = computed(() =>
    this.filteredProducts().reduce(
      (totalCents, product) => totalCents + this.productStockValueCents(product),
      0
    ) / 100
  );

  readonly productsWithPrice = computed(() =>
    this.products().filter(product => product.unitPrice > 0).length
  );

  readonly productsWithoutPrice = computed(() =>
    this.products().filter(product => product.unitPrice <= 0).length
  );

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.productApi.getAll().subscribe({
      next: (products) => {
        this.products.set(products.filter(product => product.active));
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la valorisation du stock :', error);
        this.errorMessage.set('Impossible de charger les données de valorisation du stock.');
        this.loading.set(false);
      }
    });
  }

  productStockValue(product: Product): number {
    return this.productStockValueCents(product) / 100;
  }

  private productStockValueCents(product: Product): number {
    return Math.round((product.unitPrice ?? 0) * 100) * (product.quantityInStock ?? 0);
  }

  openProduct(id: number): void {
    void this.router.navigate(['/products', id]);
  }
}
