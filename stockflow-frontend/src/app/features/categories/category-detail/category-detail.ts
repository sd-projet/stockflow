
import { Component, OnInit, ChangeDetectorRef, ApplicationRef, inject, LOCALE_ID } from '@angular/core';
import { CurrencyPipe, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';

import { ActivatedRoute, Router } from '@angular/router';
import { Category } from '../category';
import { CategoryApi } from '../category-api';
import { Product } from '../../products/product';
import { ProductApi } from '../../products/product-api';

registerLocaleData(localeFr);

@Component({
  selector: 'app-category-detail',
  standalone: true,
  imports: [CurrencyPipe],
  providers: [{ provide: LOCALE_ID, useValue: 'fr-FR' }],
  templateUrl: './category-detail.html',
  styleUrl: './category-detail.scss'
})
export class CategoryDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly categoryApi = inject(CategoryApi);
  private readonly productApi = inject(ProductApi);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly appRef = inject(ApplicationRef);

  category: Category | null = null;
  products: Product[] = [];

  loadingCategory = true;
  loadingProducts = true;
  categoryError = '';
  productsError = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isInteger(id) || id <= 0) {
      this.categoryError = 'Identifiant de catégorie invalide.';
      this.loadingCategory = false;
      this.loadingProducts = false;
      return;
    }

    this.categoryApi.getById(id).subscribe({
      next: category => {
        this.category = category;
        this.loadingCategory = false;
        this.refreshView();
      },
      error: error => {
        this.categoryError = error.status === 404
          ? 'Cette catégorie est introuvable.'
          : 'Impossible de charger les informations de la catégorie.';
        this.loadingCategory = false;
        this.refreshView();
      }
    });

    this.productApi.getAll().subscribe({
      next: products => {
        this.products = products.filter(product => product.categoryId === id);
        this.loadingProducts = false;
        this.refreshView();
      },
      error: () => {
        this.productsError = 'Impossible de charger les produits associés.';
        this.loadingProducts = false;
        this.refreshView();
      }
    });
  }

  get lowStockCount(): number {
    return this.products.filter(
      product => product.quantityInStock <= product.minimumStock
    ).length;
  }

  goBack(): void {
    this.router.navigate(['/categories']);
  }

  private refreshView(): void {
    this.cdr.detectChanges();
    this.appRef.tick();
  }
}
