
import { Component, OnInit, ChangeDetectorRef, NgZone, ApplicationRef, inject } from '@angular/core';
import { Router } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';

import { Product, ProductRequest, SupplierProductRequest } from '../product';
import { ProductApi } from '../product-api';
import { Category } from '../../categories/category';
import { CategoryApi } from '../../categories/category-api';
import { Supplier } from '../../suppliers/supplier';
import { SupplierApi } from '../../suppliers/supplier-api';
import { downloadCsv } from '../../../shared/utils/csv';


@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [FormsModule, DecimalPipe, PaginationComponent],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss'
})

export class ProductList implements OnInit {
  private readonly productApi = inject(ProductApi);
  private readonly categoryApi = inject(CategoryApi);
  private readonly supplierApi = inject(SupplierApi);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly ngZone = inject(NgZone);
  private readonly appRef = inject(ApplicationRef);
  private readonly router = inject(Router);

  products: Product[] = [];
  categories: Category[] = [];
  suppliers: Supplier[] = [];

  searchTerm = '';
  currentPage = 1;
  pageSize = 10;

  get filteredProducts(): Product[] {
    const term = this.searchTerm.trim().toLocaleLowerCase('fr');

    if (!term) {
      return this.products;
    }

    return this.products.filter(product =>
      product.reference.toLocaleLowerCase('fr').includes(term) ||
      product.name.toLocaleLowerCase('fr').includes(term) ||
      product.categoryName.toLocaleLowerCase('fr').includes(term)
    );
  }

  get paginatedProducts(): Product[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;

    return this.filteredProducts.slice(
      startIndex,
      startIndex + this.pageSize
    );
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  showForm = false;
  isEditMode = false;
  editingProductId: number | null = null;

  loading = false;
  errorMessage = '';
  formError = '';
  saving = false;

  formData: ProductRequest = this.createEmptyForm();
  
  selectedProduct: Product | null = null;
  loadingDetails = false;
  detailsError = '';

  supplierActionError = '';
  supplierActionSuccess = '';
  savingSupplier = false;

  newSupplierData: SupplierProductRequest = {
    supplierId: 0,
    supplierReference: null,
    unitPrice: 0
  };

  editingSupplierId: number | null = null;

  editSupplierData: SupplierProductRequest = {
    supplierId: 0,
    supplierReference: null,
    unitPrice: 0
  };

  private createEmptyForm(): ProductRequest {
    return {
      reference: '',
      name: '',
      description: '',
      categoryId: 0,
      minimumStock: 0,
      unit: 'unité',
      unitPrice: 0,
      suppliers: []
    };
  }

  goToProductDetail(id: number): void {
    this.router.navigate(['/products', id]);
  }

  ngOnInit(): void {
    this.loadProducts();
    this.loadCategories();
    this.loadSuppliers();
  }

  loadProducts(): void {
    this.loading = true;
    this.errorMessage = '';

    this.productApi.getAll().subscribe({
      next: (products) => {
        this.products = products;
        this.currentPage = 1;
        this.loading = false;
        this.cdr.detectChanges();
        this.appRef.tick();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les produits.';
        this.loading = false;
        this.cdr.detectChanges();
        this.appRef.tick();
      }
    });
  }

  loadCategories(): void {
    this.categoryApi.getAll().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.cdr.detectChanges();
        this.appRef.tick();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les catégories.';
        this.cdr.detectChanges();
        this.appRef.tick();
      }
    });
  }

  
  loadSuppliers(): void {
    this.supplierApi.getAllActive().subscribe({
      next: (suppliers) => {
        this.suppliers = suppliers;

        // Si le formulaire est déjà ouvert, proposer le premier fournisseur.
        if (this.showForm && this.formData.suppliers.length === 0) {
          this.initializeSupplier();
        }

        this.cdr.detectChanges();
        this.appRef.tick();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les fournisseurs.';
        this.cdr.detectChanges();
        this.appRef.tick();
      }
    });
  }

  get totalProducts(): number {
    return this.products.length;
  }

  get activeProducts(): number {
    return this.products.filter((product) => product.active).length;
  }

  get lowStockProducts(): number {
    return this.products.filter(
      (product) =>
        product.active &&
        product.quantityInStock <= product.minimumStock
    ).length;
  }

  get canSave(): boolean {
    const association = this.formData.suppliers[0];

    return this.formData.suppliers.length === 1
      && association.supplierId > 0
      && Number.isFinite(Number(association.unitPrice))
      && Number(association.unitPrice) >= 0;
  }

  private initializeSupplier(): void {
    if (this.formData.suppliers.length > 0 || this.suppliers.length === 0) {
      return;
    }

    this.formData.suppliers = [{
      supplierId: this.suppliers[0].id,
      supplierReference: null,
      unitPrice: 0
    }];
  }

  availableSuppliers(index: number): Supplier[] {
    const selectedElsewhere = new Set(
      this.formData.suppliers
        .filter((_, i) => i !== index)
        .map((item) => item.supplierId)
        .filter((id) => id > 0)
    );

    return this.suppliers.filter(
      (supplier) =>
        !selectedElsewhere.has(supplier.id) ||
        supplier.id === this.formData.suppliers[index]?.supplierId
    );
  }

  addSupplier(): void {
    const alreadySelected = new Set(
      this.formData.suppliers.map((item) => item.supplierId)
    );

    const available = this.suppliers.find(
      (supplier) => !alreadySelected.has(supplier.id)
    );

    if (!available) {
      return;
    }

    this.formData.suppliers.push({
      supplierId: available.id,
      supplierReference: null,
      unitPrice: 0
    });

    this.formError = '';
  }

  removeSupplier(index: number): void {
    this.formData.suppliers.splice(index, 1);
    this.formError = '';
  }

  openForm(): void {
    this.isEditMode = false;
    this.editingProductId = null;
    this.formData = this.createEmptyForm();
    this.formError = '';
    this.showForm = true;
    this.initializeSupplier();
  }

  editProduct(product: Product): void {
    if (!product.active) {
      return;
    }

    this.editingProductId = product.id;
    this.isEditMode = true;
    this.formError = '';

    this.formData = {
      reference: product.reference,
      name: product.name,
      description: product.description ?? '',
      categoryId: product.categoryId,
      minimumStock: product.minimumStock,
      unit: product.unit,
      unitPrice: product.unitPrice,
      suppliers: product.suppliers.map(supplier => ({
        supplierId: supplier.supplierId,
        supplierReference: supplier.supplierReference,
        unitPrice: supplier.unitPrice
      }))
    };

    this.showForm = true;
  }

  reactivateProduct(product: Product): void {
    if (product.active) {
      return;
    }

    this.errorMessage = '';

    this.productApi.reactivate(product.id).subscribe({
      next: (updated) => {
        this.ngZone.run(() => {
          this.products = this.products.map(item =>
            item.id === updated.id ? updated : item
          );

          this.cdr.detectChanges();
          this.appRef.tick();
        });
      },
      error: (error) => {
        this.ngZone.run(() => {
          this.errorMessage =
            error?.error?.message ??
            'Impossible de réactiver le produit. Veuillez réessayer.';
          this.cdr.detectChanges();
        });
      }
    });
  }

  isLowStock(product: Product): boolean {
    return product.active && product.quantityInStock <= product.minimumStock;
  }

  exportCsv(): void {
    downloadCsv(
      `produits-${new Date().toISOString().slice(0, 10)}.csv`,
      [
        'Référence', 'Nom', 'Catégorie', 'Prix unitaire (€)',
        'Stock', 'Stock minimum', 'Unité', 'Valeur du stock (€)', 'Statut'
      ],
      this.filteredProducts.map(product => [
        product.reference,
        product.name,
        product.categoryName,
        Number(product.unitPrice).toFixed(2).replace('.', ','),
        product.quantityInStock,
        product.minimumStock,
        product.unit,
        (product.quantityInStock * Number(product.unitPrice))
          .toFixed(2).replace('.', ','),
        product.active ? 'Actif' : 'Inactif'
      ])
    );
  }

  deactivateProduct(product: Product): void {
    if (!product.active) {
      return;
    }

    const confirmed = confirm(
      `Désactiver le produit « ${product.name} » ?\n\n` +
      'Il ne sera plus disponible à l’utilisation, mais son historique sera conservé.'
    );

    if (!confirmed) {
      return;
    }

    this.errorMessage = '';

    this.productApi.delete(product.id).subscribe({
      next: () => {
        this.ngZone.run(() => {
          this.products = this.products.map(item =>
            item.id === product.id
              ? { ...item, active: false }
              : item
          );

          if (this.selectedProduct?.id === product.id) {
            this.closeDetails();
          }

          this.loadProducts();
          this.cdr.detectChanges();
          this.appRef.tick();
        });
      },
      error: error => {
        console.error(
          'Erreur lors de la désactivation du produit :',
          error
        );

        this.ngZone.run(() => {
          this.errorMessage =
            'Impossible de désactiver le produit. Veuillez réessayer.';

          this.cdr.detectChanges();
          this.appRef.tick();
        });
      }
    });
  }

  closeForm(): void {
    if (this.saving) {
      return;
    }

    this.showForm = false;
    this.isEditMode = false;
    this.editingProductId = null;
    this.formError = '';
  }

  createProduct(request: ProductRequest): void {
    if (this.saving) {
      return;
    }

    if (!this.canSave) {
      this.formError =
        'Sélectionnez un fournisseur actif et renseignez un prix d’achat valide.';
      return;
    }

    this.saving = true;
    this.formError = '';

    const payload: ProductRequest = {
      ...request,
      reference: request.reference.trim(),
      name: request.name.trim(),
      description: request.description?.trim() || null,
      categoryId: Number(request.categoryId),
      minimumStock: Number(request.minimumStock),
      unit: request.unit.trim(),
      unitPrice: Number(request.unitPrice),
      suppliers: request.suppliers.map(supplier => ({
        supplierId: Number(supplier.supplierId),
        supplierReference: supplier.supplierReference?.trim() || null,
        unitPrice: Number(supplier.unitPrice)
      }))
    };

    const isEditing =
      this.isEditMode && this.editingProductId !== null;

    const request$ = isEditing
      ? this.productApi.update(this.editingProductId!, payload)
      : this.productApi.create(payload);

    request$.subscribe({
      next: product => {
        this.ngZone.run(() => {
          this.saving = false;
          this.showForm = false;
          this.isEditMode = false;
          this.editingProductId = null;

          this.products = isEditing
            ? this.products.map(item =>
                item.id === product.id ? product : item
              )
            : [product, ...this.products];

          this.loadProducts();

          this.cdr.detectChanges();
          this.appRef.tick();
        });
      },
      error: error => {
        console.error(
          isEditing
            ? 'Erreur lors de la modification du produit :'
            : 'Erreur lors de la création du produit :',
          error
        );

        this.ngZone.run(() => {
          this.saving = false;
          this.formError = isEditing
            ? 'Impossible de modifier le produit. Vérifiez les champs et les fournisseurs associés.'
            : 'Impossible de créer le produit. Vérifiez les champs et le fournisseur associé.';

          this.cdr.detectChanges();
          this.appRef.tick();
        });
      }
    });
  }

  viewProduct(id: number): void {
    this.supplierActionError = '';
    this.supplierActionSuccess = '';
    this.editingSupplierId = null;

    this.selectedProduct = null;
    this.loadingDetails = true;
    this.detailsError = '';

    this.productApi.getById(id).subscribe({
      next: (product) => {
        this.ngZone.run(() => {
          this.selectedProduct = product;
          this.loadingDetails = false;
          this.cdr.detectChanges();
          this.appRef.tick();
        });
      },
      error: (error) => {
        console.error('Erreur lors du chargement du produit :', error);

        this.ngZone.run(() => {
          this.loadingDetails = false;
          this.detailsError =
            'Impossible de charger les détails du produit.';
          this.cdr.detectChanges();
          this.appRef.tick();
        });
      }
    });
  }


  closeDetails(): void {
    this.selectedProduct = null;
    this.detailsError = '';
  }


  get availableSuppliersForSelectedProduct(): Supplier[] {
    const associatedIds = new Set(
      (this.selectedProduct?.suppliers ?? []).map(
        supplier => supplier.supplierId
      )
    );

    return this.suppliers.filter(
      supplier => !associatedIds.has(supplier.id)
    );
  }

  addSupplierToProduct(): void {
    if (!this.selectedProduct || this.savingSupplier) {
      return;
    }

    if (
      this.newSupplierData.supplierId <= 0 ||
      !Number.isFinite(Number(this.newSupplierData.unitPrice)) ||
      Number(this.newSupplierData.unitPrice) < 0
    ) {
      this.supplierActionError =
        'Sélectionnez un fournisseur et saisissez un prix d’achat valide.';
      this.supplierActionSuccess = '';
      return;
    }

    this.savingSupplier = true;
    this.supplierActionError = '';
    this.supplierActionSuccess = '';

    const productId = this.selectedProduct.id;
    const request: SupplierProductRequest = {
      supplierId: Number(this.newSupplierData.supplierId),
      supplierReference:
        this.newSupplierData.supplierReference?.trim() || null,
      unitPrice: Number(this.newSupplierData.unitPrice)
    };

    this.productApi.addSupplier(productId, request).subscribe({
      next: product => {
        this.ngZone.run(() => {
          this.selectedProduct = product;
          this.products = this.products.map(item =>
            item.id === product.id ? product : item
          );
          this.newSupplierData = {
            supplierId: 0,
            supplierReference: null,
            unitPrice: 0
          };
          this.supplierActionSuccess = 'Fournisseur ajouté avec succès.';
          this.savingSupplier = false;
          this.cdr.detectChanges();
          this.appRef.tick();
        });
      },
      error: error => {
        console.error('Erreur lors de l’ajout du fournisseur :', error);
        this.ngZone.run(() => {
          this.supplierActionError =
            'Impossible d’ajouter ce fournisseur. Il est peut-être déjà associé au produit.';
          this.savingSupplier = false;
          this.cdr.detectChanges();
          this.appRef.tick();
        });
      }
    });
  }

  startSupplierEdit(supplierId: number): void {
    const supplier = this.selectedProduct?.suppliers.find(
      item => item.supplierId === supplierId
    );

    if (!supplier) {
      return;
    }

    this.editingSupplierId = supplierId;
    this.editSupplierData = {
      supplierId: supplier.supplierId,
      supplierReference: supplier.supplierReference,
      unitPrice: supplier.unitPrice
    };
    this.supplierActionError = '';
    this.supplierActionSuccess = '';
  }

  cancelSupplierEdit(): void {
    this.editingSupplierId = null;
  }

  saveSupplierEdit(): void {
    if (!this.selectedProduct || this.editingSupplierId === null ||
        this.savingSupplier) {
      return;
    }

    if (
      !Number.isFinite(Number(this.editSupplierData.unitPrice)) ||
      Number(this.editSupplierData.unitPrice) < 0
    ) {
      this.supplierActionError = 'Le prix d’achat doit être positif ou nul.';
      this.supplierActionSuccess = '';
      return;
    }

    this.savingSupplier = true;
    this.supplierActionError = '';
    this.supplierActionSuccess = '';

    const productId = this.selectedProduct.id;
    const supplierId = this.editingSupplierId;
    const request: SupplierProductRequest = {
      supplierId,
      supplierReference:
        this.editSupplierData.supplierReference?.trim() || null,
      unitPrice: Number(this.editSupplierData.unitPrice)
    };

    this.productApi.updateSupplier(productId, supplierId, request).subscribe({
      next: product => {
        this.ngZone.run(() => {
          this.selectedProduct = product;
          this.products = this.products.map(item =>
            item.id === product.id ? product : item
          );
          this.editingSupplierId = null;
          this.supplierActionSuccess = 'Association mise à jour.';
          this.savingSupplier = false;
          this.cdr.detectChanges();
          this.appRef.tick();
        });
      },
      error: error => {
        console.error('Erreur lors de la modification du fournisseur :', error);
        this.ngZone.run(() => {
          this.supplierActionError =
            'Impossible de modifier cette association.';
          this.savingSupplier = false;
          this.cdr.detectChanges();
          this.appRef.tick();
        });
      }
    });
  }

  removeSupplierFromProduct(supplierId: number): void {
    if (!this.selectedProduct || this.savingSupplier) {
      return;
    }

    if (this.selectedProduct.suppliers.length <= 1) {
      this.supplierActionError =
        'Un produit doit conserver au moins un fournisseur associé.';
      this.supplierActionSuccess = '';
      return;
    }

    if (!confirm('Retirer ce fournisseur du produit ?')) {
      return;
    }

    this.savingSupplier = true;
    this.supplierActionError = '';
    this.supplierActionSuccess = '';

    const productId = this.selectedProduct.id;

    this.productApi.removeSupplier(productId, supplierId).subscribe({
      next: product => {
        this.ngZone.run(() => {
          this.selectedProduct = product;
          this.products = this.products.map(item =>
            item.id === product.id ? product : item
          );
          this.supplierActionSuccess = 'Fournisseur retiré du produit.';
          this.savingSupplier = false;
          this.cdr.detectChanges();
          this.appRef.tick();
        });
      },
      error: error => {
        console.error('Erreur lors du retrait du fournisseur :', error);
        this.ngZone.run(() => {
          this.supplierActionError =
            'Impossible de retirer ce fournisseur.';
          this.savingSupplier = false;
          this.cdr.detectChanges();
          this.appRef.tick();
        });
      }
    });
  }

}
