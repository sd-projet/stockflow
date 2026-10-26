
import {
  Component,
  OnInit,
  ChangeDetectorRef,
  ApplicationRef,
  inject,
  LOCALE_ID
} from '@angular/core';
import { CurrencyPipe, DatePipe, registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ActivatedRoute, Router } from '@angular/router';

import { Product } from '../product';
import { ProductApi } from '../product-api';
import { Supplier } from '../../suppliers/supplier';
import { SupplierApi } from '../../suppliers/supplier-api';
import { PurchaseOrder, PurchaseOrderStatus } from '../../purchase-orders/purchase-order';
import { PurchaseOrderApi } from '../../purchase-orders/purchase-order-api';

registerLocaleData(localeFr);

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  providers: [{ provide: LOCALE_ID, useValue: 'fr-FR' }],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.scss'
})
export class ProductDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productApi = inject(ProductApi);
  private readonly supplierApi = inject(SupplierApi);
  private readonly purchaseOrderApi = inject(PurchaseOrderApi);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly appRef = inject(ApplicationRef);

  product: Product | null = null;
  suppliers: Supplier[] = [];
  purchaseOrders: PurchaseOrder[] = [];

  loadingProduct = true;
  loadingSuppliers = true;
  loadingOrders = true;

  productError = '';
  suppliersError = '';
  ordersError = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isInteger(id) || id <= 0) {
      this.productError = 'Identifiant de produit invalide.';
      this.loadingProduct = false;
      this.loadingSuppliers = false;
      this.loadingOrders = false;
      return;
    }

    this.productApi.getById(id).subscribe({
      next: product => {
        this.product = product;
        this.loadingProduct = false;
        this.refreshView();
      },
      error: error => {
        this.productError = error.status === 404
          ? 'Ce produit est introuvable.'
          : 'Impossible de charger les informations du produit.';
        this.loadingProduct = false;
        this.refreshView();
      }
    });

    this.supplierApi.getAll().subscribe({
      next: suppliers => {
        this.suppliers = suppliers;
        this.loadingSuppliers = false;
        this.refreshView();
      },
      error: () => {
        this.suppliersError = 'Impossible de charger la liste des fournisseurs.';
        this.loadingSuppliers = false;
        this.refreshView();
      }
    });

    this.purchaseOrderApi.getAll().subscribe({
      next: orders => {
        this.purchaseOrders = orders
          .filter(order => order.lines?.some(line => line.productId === id))
          .sort((a, b) => {
            const dateA = new Date(a.orderDate ?? a.createdAt).getTime();
            const dateB = new Date(b.orderDate ?? b.createdAt).getTime();
            return dateB - dateA;
          })
          .slice(0, 5);

        this.loadingOrders = false;
        this.refreshView();
      },
      error: () => {
        this.ordersError = 'Impossible de charger les commandes associées à ce produit.';
        this.loadingOrders = false;
        this.refreshView();
      }
    });
  }

  get recentOrders(): PurchaseOrder[] {
    return this.purchaseOrders;
  }

  getSupplierName(supplierId: number): string {
    return this.suppliers.find(supplier => supplier.id === supplierId)?.name
      ?? 'Fournisseur inconnu';
  }

  getQuantityInOrder(order: PurchaseOrder): number {
    return order.lines
      .filter(line => line.productId === this.product?.id)
      .reduce((total, line) => total + line.quantityOrdered, 0);
  }

  getOrderStatusLabel(status: PurchaseOrderStatus): string {
    const labels: Record<PurchaseOrderStatus, string> = {
      DRAFT: 'Brouillon',
      ORDERED: 'Commandée',
      PARTIALLY_RECEIVED: 'Partiellement reçue',
      RECEIVED: 'Reçue',
      CANCELLED: 'Annulée'
    };

    return labels[status];
  }

  goBack(): void {
    this.router.navigate(['/products']);
  }

  editProduct(): void {
    if (this.product) {
      this.router.navigate(['/products'], {
        queryParams: { edit: this.product.id }
      });
    }
  }

  deactivateProduct(): void {
    if (!this.product || !this.product.active) {
      return;
    }

    const confirmed = confirm(
      `Désactiver le produit « ${this.product.name} » ?\n\n` +
      'Son historique sera conservé.'
    );

    if (!confirmed) {
      return;
    }

    this.productApi.delete(this.product.id).subscribe({
      next: () => {
        if (this.product) {
          this.product = { ...this.product, active: false };
        }
        this.refreshView();
      },
      error: error => {
        console.error('Erreur lors de la désactivation du produit :', error);
        this.productError = 'Impossible de désactiver le produit. Veuillez réessayer.';
        this.refreshView();
      }
    });
  }

  reactivateProduct(): void {
    if (!this.product || this.product.active) return;
    this.productApi.reactivate(this.product.id).subscribe({
      next: (updated) => { this.product = updated; this.refreshView(); },
      error: () => { this.productError = "Impossible de réactiver le produit. Veuillez réessayer."; this.refreshView(); }
    });
  }

  openSupplier(id: number): void { void this.router.navigate(["/suppliers", id]); }

  openOrder(id: number): void { void this.router.navigate(['/purchase-orders', id]); }

  private refreshView(): void {
    this.cdr.detectChanges();
    this.appRef.tick();
  }
}
