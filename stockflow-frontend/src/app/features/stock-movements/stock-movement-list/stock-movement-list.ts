
import {
  Component,
  OnInit,
  ChangeDetectorRef,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { StockMovementApi } from '../stock-movement-api';
import {
  MovementType,
  StockMovement,
  StockMovementRequest
} from '../stock-movement';

import { ProductApi } from '../../products/product-api';
import { Product } from '../../products/product';

import { PaginationComponent } from '../../../shared/components/pagination/pagination';
import { downloadCsv } from '../../../shared/utils/csv';

@Component({
  selector: 'app-stock-movement-list',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './stock-movement-list.html',
  styleUrl: './stock-movement-list.scss'
})
export class StockMovementList implements OnInit {
  private readonly stockMovementApi = inject(StockMovementApi);
  private readonly productApi = inject(ProductApi);
  private readonly cdr = inject(ChangeDetectorRef);

  movements: StockMovement[] = [];
  products: Product[] = [];

  loading = false;
  submitting = false;
  errorMessage = '';
  successMessage = '';

  searchTerm = '';
  selectedType: MovementType | '' = '';

  currentPage = 1;
  pageSize = 10;

  showForm = false;
  selectedMovement: StockMovement | null = null;

  form: StockMovementRequest = {
    productId: 0,
    type: 'IN',
    quantity: 1,
    reason: '',
    purchaseOrderLineId: null
  };

  readonly movementTypes: { value: MovementType; label: string }[] = [
    { value: 'IN', label: 'Entrée' },
    { value: 'OUT', label: 'Sortie' },
    { value: 'ADJUSTMENT', label: 'Ajustement' }
  ];

  ngOnInit(): void {
    this.loadProducts();
    this.loadMovements();
  }


  
  loadProducts(): void {
    this.productApi.getAll().subscribe({
      next: (products) => {
        this.products = products.filter(product => product.active);
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les produits.';
        this.cdr.markForCheck();
      }
    });
  }

  loadMovements(): void {
    this.loading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();

    this.stockMovementApi.getAll().subscribe({
      next: (movements) => {
        this.movements = movements;
        this.currentPage = 1;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les mouvements de stock.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }


  get filteredMovements(): StockMovement[] {
    const term = this.searchTerm.trim().toLowerCase();

    return this.movements.filter(movement => {
      const matchesSearch =
        !term ||
        movement.productName.toLowerCase().includes(term) ||
        movement.productReference.toLowerCase().includes(term) ||
        movement.reason.toLowerCase().includes(term);

      const matchesType =
        !this.selectedType || movement.type === this.selectedType;

      return matchesSearch && matchesType;
    });
  }

  get paginatedMovements(): StockMovement[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;

    return this.filteredMovements.slice(
      startIndex,
      startIndex + this.pageSize
    );
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  openForm(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.form = {
      productId: 0,
      type: 'IN',
      quantity: 1,
      reason: '',
      purchaseOrderLineId: null
    };
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  viewMovement(movement: StockMovement): void {
    this.selectedMovement = movement;
  }

  closeMovementDetails(): void {
    this.selectedMovement = null;
  }

  
  submitMovement(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (
      !this.form.productId ||
      !this.form.type ||
      !Number.isInteger(this.form.quantity) ||
      this.form.quantity <= 0 ||
      !this.form.reason.trim()
    ) {
      this.errorMessage = 'Vérifie le produit, la quantité et le motif.';
      return;
    }

    this.submitting = true;
    this.cdr.markForCheck();

    this.stockMovementApi.create({
      ...this.form,
      reason: this.form.reason.trim()
    }).subscribe({
      next: () => {
        this.submitting = false;
        this.showForm = false;
        this.successMessage = 'Le mouvement de stock a été enregistré.';
        this.cdr.markForCheck();

        this.loadMovements();

        this.productApi.getAll().subscribe({
          next: (products) => {
            this.products = products.filter(product => product.active);
            this.cdr.markForCheck();
          },
          error: () => {
            this.errorMessage =
              'Le mouvement est enregistré, mais la liste des produits n’a pas pu être actualisée.';
            this.cdr.markForCheck();
          }
        });
      },
      error: (error) => {
        this.submitting = false;
        this.errorMessage =
          error?.error?.message ??
          'Impossible d’enregistrer le mouvement de stock.';
        this.cdr.markForCheck();
      }
    });
  }


  exportCsv(): void {
    downloadCsv(
      `mouvements-stock-${new Date().toISOString().slice(0, 10)}.csv`,
      ['Date', 'Référence', 'Produit', 'Type', 'Avant', 'Après', 'Variation', 'Motif'],
      this.filteredMovements.map(movement => [
        new Date(movement.createdAt).toLocaleString('fr-FR'),
        movement.productReference,
        movement.productName,
        this.getMovementLabel(movement.type),
        movement.quantityBefore,
        movement.quantityAfter,
        movement.quantityAfter - movement.quantityBefore,
        movement.reason
      ])
    );
  }

  getMovementLabel(type: MovementType): string {
    return this.movementTypes.find(item => item.value === type)?.label ?? type;
  }

  trackByMovementId(_index: number, movement: StockMovement): number {
    return movement.id;
  }
}
