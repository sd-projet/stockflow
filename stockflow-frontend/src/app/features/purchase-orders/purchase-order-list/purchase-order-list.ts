
import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';

import { PurchaseOrderApi } from '../purchase-order-api';
import {
  PurchaseOrder,
  PurchaseOrderCreateRequest,
  PurchaseOrderStatus
} from '../purchase-order';
import { SupplierApi } from '../../suppliers/supplier-api';
import { Supplier } from '../../suppliers/supplier';
import { ProductApi } from '../../products/product-api';
import { Product } from '../../products/product';
import { downloadCsv } from '../../../shared/utils/csv';

interface OrderLineForm {
  productId: number | null;
  quantityOrdered: number;
  unitPrice: number;
}

interface ReceiveLineForm {
  lineId: number;
  productName: string;
  quantityOrdered: number;
  quantityReceived: number;
  quantityToReceive: number;
}

@Component({
  selector: 'app-purchase-order-list',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './purchase-order-list.html',
  styleUrl: './purchase-order-list.scss'
})
export class PurchaseOrderList implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly purchaseOrderApi = inject(PurchaseOrderApi);
  private readonly supplierApi = inject(SupplierApi);
  private readonly productApi = inject(ProductApi);
  private readonly cdr = inject(ChangeDetectorRef);

  orders: PurchaseOrder[] = [];
  suppliers: Supplier[] = [];
  products: Product[] = [];

  loading = false;
  saving = false;
  showForm = false;

  errorMessage = '';
  successMessage = '';

  newOrder = this.createEmptyOrder();
  editingOrderId: number | null = null;

  selectedOrder: PurchaseOrder | null = null;
  loadingDetails = false;
  updatingStatus = false;

  receiving = false;
  showReceiveForm = false;
  receiveLines: ReceiveLineForm[] = [];
  receiveError = '';
  receiveSuccess = '';

  showCancelConfirmation = false;
  cancelling = false;
  cancelSuccess = '';
  cancelError = '';

  searchTerm = '';
  statusFilter: 'ALL' | PurchaseOrderStatus = 'ALL';
  showArchived = false;
  archivingOrderId: number | null = null;
  showArchiveConfirmation = false;
  orderToArchive: PurchaseOrder | null = null;
  archiveError = '';

  currentPage = 1;
  pageSize = 10;

  get filteredOrders(): PurchaseOrder[] {
    const term = this.searchTerm.trim().toLocaleLowerCase('fr');

    return this.orders.filter(order => {
      if (order.archived !== this.showArchived) return false;
      const matchesSearch =
        !term ||
        order.orderNumber?.toLocaleLowerCase('fr').includes(term) ||
        order.supplierName?.toLocaleLowerCase('fr').includes(term) ||
        this.statusLabel(order.status).toLocaleLowerCase('fr').includes(term);

      const matchesStatus =
        this.statusFilter === 'ALL' ||
        order.status === this.statusFilter;

      return matchesSearch && matchesStatus;
    });
  }

  get totalPages(): number {
    return Math.max(
      1,
      Math.ceil(this.filteredOrders.length / this.pageSize)
    );
  }

  get paginatedOrders(): PurchaseOrder[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;

    return this.filteredOrders.slice(
      startIndex,
      startIndex + this.pageSize
    );
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  get productsForSelectedSupplier(): Product[] {
    const supplierId = this.newOrder.supplierId;

    if (supplierId === null) {
      return [];
    }

    return this.products.filter(product =>
      product.suppliers?.some(
        association => association.supplierId === supplierId
      )
    );
  }

  ngOnInit(): void {
    this.loadOrders();
    this.loadReferenceData();
    const orderId = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isInteger(orderId) && orderId > 0) this.loadOrderDetails(orderId);
  }

  private createEmptyOrder() {
    return {
      orderNumber: '',
      supplierId: null as number | null,
      orderDate: new Date().toISOString().slice(0, 10),
      expectedDeliveryDate: '',
      notes: '',
      lines: [this.createEmptyLine()]
    };
  }

  private createEmptyLine(): OrderLineForm {
    return {
      productId: null,
      quantityOrdered: 1,
      unitPrice: 0
    };
  }

  loadOrders(): void {
    this.loading = true;
    this.errorMessage = '';

    this.purchaseOrderApi.getAll().subscribe({
      next: (orders) => {
        this.orders = orders;
        this.currentPage = 1;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les commandes fournisseurs.';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  private loadReferenceData(): void {
    this.supplierApi.getAllActive().subscribe({
      next: (suppliers) => {
        this.suppliers = suppliers;
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les fournisseurs.';
        this.cdr.markForCheck();
      }
    });

    this.productApi.getAll().subscribe({
      next: (products) => {
        this.products = products.filter((product) => product.active);
        this.cdr.markForCheck();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les produits.';
        this.cdr.markForCheck();
      }
    });
  }

  openCreateForm(): void {
    this.editingOrderId = null;
    this.newOrder = this.createEmptyOrder();
    this.errorMessage = '';
    this.successMessage = '';
    this.showForm = true;
  }

  closeCreateForm(): void {
    this.showForm = false;
    this.editingOrderId = null;
    this.errorMessage = '';

    // Si le formulaire a été ouvert depuis la page de détail,
    // revenir à la liste lorsque l'utilisateur le ferme.
    if (this.route.snapshot.paramMap.has('id')) {
      this.router.navigate(['/purchase-orders']);
    }
  }

  startEditOrder(order: PurchaseOrder): void {
    if (order.status !== 'DRAFT') {
      return;
    }

    // Ne pas appeler closeOrderDetails() ici : cette méthode navigue
    // vers la liste et fait perdre l'état du formulaire d'édition.
    // On masque uniquement le détail pour afficher le formulaire modal.
    this.selectedOrder = null;
    this.loadingDetails = false;

    this.newOrder = {
      orderNumber: order.orderNumber,
      supplierId: order.supplierId,
      orderDate: order.orderDate ?? '',
      expectedDeliveryDate: order.expectedDeliveryDate ?? '',
      notes: order.notes ?? '',
      lines: order.lines.map(line => ({
        productId: line.productId,
        quantityOrdered: line.quantityOrdered,
        unitPrice: Number(line.unitPrice)
      }))
    };

    this.editingOrderId = order.id;
    this.errorMessage = '';
    this.successMessage = '';
    this.showForm = true;
  }

  private updateOrder(
    id: number,
    request: PurchaseOrderCreateRequest
  ): void {
    const orderNumber = request.orderNumber?.trim();

    if (!orderNumber) {
      this.saving = false;
      this.errorMessage = 'Le numéro de commande est obligatoire.';
      this.cdr.markForCheck();
      return;
    }

    this.purchaseOrderApi
      .update(id, { ...request, orderNumber })
      .subscribe({
        next: (updatedOrder) => {
          this.orders = this.orders.map(order =>
            order.id === updatedOrder.id ? updatedOrder : order
          );
          this.saving = false;
          this.showForm = false;
          this.editingOrderId = null;
          this.successMessage =
            `La commande ${updatedOrder.orderNumber} a été modifiée.`;
          this.newOrder = this.createEmptyOrder();
          this.cdr.markForCheck();
          if (this.route.snapshot.paramMap.has('id')) {
            this.router.navigate(['/purchase-orders']);
          }
        },
        error: (error) => {
          this.saving = false;
          this.errorMessage =
            error?.error?.message ||
            'La modification de la commande a échoué.';
          this.cdr.markForCheck();
        }
      });
  }

  exportCsv(): void {
    downloadCsv(
      `commandes-fournisseurs-${new Date().toISOString().slice(0, 10)}.csv`,
      [
        'Numéro', 'Fournisseur', 'Statut', 'Date de commande',
        'Livraison prévue', 'Montant (€)', 'Créée par'
      ],
      this.filteredOrders.map(order => [
        order.orderNumber,
        order.supplierName,
        this.statusLabel(order.status),
        order.orderDate ?? '',
        order.expectedDeliveryDate ?? '',
        Number(order.totalAmount).toFixed(2).replace('.', ','),
        order.createdByName
      ])
    );
  }

  addLine(): void {
    this.newOrder.lines.push(this.createEmptyLine());
  }

  removeLine(index: number): void {
    if (this.newOrder.lines.length > 1) {
      this.newOrder.lines.splice(index, 1);
    }
  }

  onProductSelected(line: OrderLineForm): void {
    const product = this.products.find(
      item => item.id === Number(line.productId)
    );

    if (!product || this.newOrder.supplierId === null) {
      line.unitPrice = 0;
      this.cdr.markForCheck();
      return;
    }

    const association = product.suppliers?.find(
      item => item.supplierId === this.newOrder.supplierId
    );

    line.unitPrice = association
      ? Number(association.unitPrice)
      : 0;

    this.cdr.markForCheck();
  }

  getLineTotal(line: OrderLineForm): number {
    return Math.max(0, Number(line.quantityOrdered) || 0)
      * Math.max(0, Number(line.unitPrice) || 0);
  }

  getFormTotal(): number {
    return this.newOrder.lines.reduce(
      (total, line) => total + this.getLineTotal(line),
      0
    );
  }

  createOrder(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (this.newOrder.supplierId === null) {
      this.errorMessage = 'Veuillez sélectionner un fournisseur.';
      return;
    }

    if (this.newOrder.lines.some(
      (line) =>
        line.productId === null ||
        !Number.isInteger(Number(line.quantityOrdered)) ||
        Number(line.quantityOrdered) <= 0 ||
        !Number.isFinite(Number(line.unitPrice)) ||
        Number(line.unitPrice) < 0
    )) {
      this.errorMessage =
        'Vérifiez les produits, les quantités et les prix de chaque ligne.';
      return;
    }

    const request: PurchaseOrderCreateRequest = {
      orderNumber: this.newOrder.orderNumber.trim() || null,
      supplierId: this.newOrder.supplierId,
      orderDate: this.newOrder.orderDate || null,
      expectedDeliveryDate: this.newOrder.expectedDeliveryDate || null,
      notes: this.newOrder.notes.trim() || null,
      lines: this.newOrder.lines.map((line) => ({
        productId: Number(line.productId),
        quantityOrdered: Number(line.quantityOrdered),
        unitPrice: Number(line.unitPrice)
      }))
    };

    this.saving = true;

    if (this.editingOrderId !== null) {
      this.updateOrder(this.editingOrderId, request);
      return;
    }

    this.purchaseOrderApi.create(request).subscribe({
      next: (createdOrder) => {
        this.orders = [createdOrder, ...this.orders];
        this.currentPage = 1;
        this.saving = false;
        this.showForm = false;
        this.successMessage =
          `La commande ${createdOrder.orderNumber} a été créée.`;
        this.newOrder = this.createEmptyOrder();
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.saving = false;
        this.errorMessage =
          error?.error?.message ||
          'La création de la commande a échoué.';
        this.cdr.markForCheck();
      }
    });
  }

  statusLabel(status: PurchaseOrderStatus): string {
    const labels: Record<string, string> = {
      DRAFT: 'Brouillon',
      ORDERED: 'Commandée',
      PARTIALLY_RECEIVED: 'Partiellement reçue',
      RECEIVED: 'Réceptionnée',
      CANCELLED: 'Annulée'
    };

    return labels[status] ?? status;
  }

  statusClass(status: PurchaseOrderStatus): string {
    const classes: Record<string, string> = {
      DRAFT: 'status-draft',
      ORDERED: 'status-ordered',
      PARTIALLY_RECEIVED: 'status-partial',
      RECEIVED: 'status-received',
      CANCELLED: 'status-cancelled'
    };

    return classes[status] ?? '';
  }

  onSupplierSelected(): void {
    // Les produits sélectionnés pour l'ancien fournisseur
    // ne sont pas conservés.
    this.newOrder.lines.forEach(line => {
      line.productId = null;
      line.unitPrice = 0;
    });

    this.cdr.markForCheck();
  }

  archiveOrder(order: PurchaseOrder): void {
    if (order.archived || this.archivingOrderId !== null) return;
    this.orderToArchive = order;
    this.archiveError = '';
    this.showArchiveConfirmation = true;
  }

  closeArchiveConfirmation(): void {
    if (this.archivingOrderId !== null) return;
    this.showArchiveConfirmation = false;
    this.orderToArchive = null;
    this.archiveError = '';
  }

  confirmArchiveOrder(): void {
    const order = this.orderToArchive;
    if (!order || order.archived || this.archivingOrderId !== null) return;

    this.archivingOrderId = order.id;
    this.archiveError = '';
    this.purchaseOrderApi.archive(order.id).subscribe({
      next: (updatedOrder) => {
        this.orders = this.orders.map(item => item.id === updatedOrder.id ? updatedOrder : item);
        this.archivingOrderId = null;
        this.showArchiveConfirmation = false;
        this.orderToArchive = null;
        this.successMessage = `La commande ${updatedOrder.orderNumber} a été archivée.`;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.archivingOrderId = null;
        this.archiveError = error?.error?.message ?? 'Impossible d’archiver cette commande.';
        this.cdr.markForCheck();
      }
    });
  }

  viewOrderDetails(id: number): void {
    this.router.navigate(['/purchase-orders', id]);
    this.loadOrderDetails(id);
  }

  private loadOrderDetails(id: number): void {
    this.selectedOrder = null;
    this.loadingDetails = true;
    this.errorMessage = '';
    this.receiveSuccess = '';
    this.receiveError = '';
    this.cancelSuccess = '';
    this.cancelError = '';
    this.showCancelConfirmation = false;

    this.purchaseOrderApi.getById(id).subscribe({
      next: (order) => {
        this.selectedOrder = order;
        this.loadingDetails = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadingDetails = false;
        this.errorMessage =
          'Impossible de charger le détail de cette commande.';
        this.cdr.markForCheck();
      }
    });
  }

  closeOrderDetails(): void {
    if (this.route.snapshot.paramMap.has('id')) this.router.navigate(['/purchase-orders']);
    this.selectedOrder = null;
    this.loadingDetails = false;
    this.receiveSuccess = '';
    this.receiveError = '';
    this.cancelSuccess = '';
    this.cancelError = '';
    this.showCancelConfirmation = false;
  }

  orderPurchaseOrder(id: number): void {
    if (this.updatingStatus) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';
    this.updatingStatus = true;

    this.purchaseOrderApi.order(id).subscribe({
      next: (updatedOrder) => {
        this.orders = this.orders.map(order =>
          order.id === updatedOrder.id ? updatedOrder : order
        );

        this.selectedOrder = updatedOrder;
        this.updatingStatus = false;
        this.successMessage =
          `La commande ${updatedOrder.orderNumber} a été confirmée.`;

        this.cdr.markForCheck();
      },
      error: (error) => {
        this.updatingStatus = false;
        this.errorMessage =
          error?.error?.message ||
          'Impossible de confirmer cette commande.';

        this.cdr.markForCheck();
      }
    });
  }


  startReceiving(): void {
    if (!this.selectedOrder) {
      return;
    }

    this.receiveLines = this.selectedOrder.lines
      .filter(line => line.quantityReceived < line.quantityOrdered)
      .map(line => ({
        lineId: line.id,
        productName: line.productName,
        quantityOrdered: line.quantityOrdered,
        quantityReceived: line.quantityReceived,
        quantityToReceive: 0
      }));

    this.receiveError = '';
    this.receiveSuccess = '';
    this.showReceiveForm = true;
  }

  getRemainingQuantity(line: ReceiveLineForm): number {
    return line.quantityOrdered - line.quantityReceived;
  }

  submitReceipt(): void {
    const order = this.selectedOrder;

    if (!order || this.receiving) {
      return;
    }

    this.receiveError = '';
    this.receiveSuccess = '';

    const linesToReceive = this.receiveLines
      .filter(line => Number(line.quantityToReceive) > 0);

    if (linesToReceive.length === 0) {
      this.receiveError =
        'Saisissez une quantité à recevoir pour au moins un produit.';
      return;
    }

    const invalidLine = linesToReceive.find(line =>
      !Number.isInteger(Number(line.quantityToReceive)) ||
      Number(line.quantityToReceive) <= 0 ||
      Number(line.quantityToReceive) > this.getRemainingQuantity(line)
    );

    if (invalidLine) {
      this.receiveError =
        'Vérifiez les quantités : elles doivent être entières, positives et ne pas dépasser les quantités restantes.';
      return;
    }

    const request = {
      lines: linesToReceive.map(line => ({
        lineId: line.lineId,
        quantityReceived: Number(line.quantityToReceive)
      }))
    };

    this.receiving = true;

    this.purchaseOrderApi.receive(order.id, request).subscribe({
      next: updatedOrder => {
        this.orders = this.orders.map(item =>
          item.id === updatedOrder.id ? updatedOrder : item
        );

        this.selectedOrder = updatedOrder;
        this.receiving = false;
        this.showReceiveForm = false;
        this.receiveLines = [];
        this.receiveSuccess =
          `La réception de la commande ${updatedOrder.orderNumber} a été enregistrée.`;

        this.cdr.markForCheck();
      },
      error: error => {
        this.receiving = false;
        this.receiveError =
          error?.error?.message ||
          'Impossible d’enregistrer la réception.';

        this.cdr.markForCheck();
      }
    });
  }

  cancelReceiving(): void {
    this.showReceiveForm = false;
    this.receiveLines = [];
    this.receiveError = '';
  }

  confirmCancelOrder(): void {
    const order = this.selectedOrder;

    if (!order || this.cancelling) {
      return;
    }

    if (order.status !== 'DRAFT' && order.status !== 'ORDERED') {
      this.cancelError =
        'Cette commande ne peut pas être annulée.';
      return;
    }

    this.cancelling = true;
    this.cancelError = '';
    this.cancelSuccess = '';

    this.purchaseOrderApi.cancel(order.id).subscribe({
      next: updatedOrder => {
        this.orders = this.orders.map(item =>
          item.id === updatedOrder.id ? updatedOrder : item
        );

        this.selectedOrder = updatedOrder;
        this.cancelling = false;
        this.showCancelConfirmation = false;
        this.cancelSuccess =
          `La commande ${updatedOrder.orderNumber} a été annulée.`;

        this.cdr.markForCheck();
      },
      error: error => {
        this.cancelling = false;
        this.cancelError =
          error?.error?.message ||
          'Impossible d’annuler cette commande.';

        this.cdr.markForCheck();
      }
    });
  }

  openCancelConfirmation(): void {
    this.showCancelConfirmation = true;
    this.cancelError = '';
  }

  closeCancelConfirmation(): void {
    if (this.cancelling) {
      return;
    }

    this.showCancelConfirmation = false;
    this.cancelError = '';
  }

}
