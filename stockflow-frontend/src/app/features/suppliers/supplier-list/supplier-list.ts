
import { Component, OnInit, ChangeDetectorRef, ApplicationRef, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Supplier, SupplierRequest } from '../supplier';
import { SupplierApi } from '../supplier-api';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';
import { Router } from '@angular/router';

@Component({
  selector: 'app-supplier-list',
  standalone: true,
  imports: [FormsModule, PaginationComponent],
  templateUrl: './supplier-list.html',
  styleUrl: './supplier-list.scss'
})
export class SupplierList implements OnInit {
  private readonly supplierApi = inject(SupplierApi);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly appRef = inject(ApplicationRef);
  private readonly router = inject(Router);

  suppliers: Supplier[] = [];
  loading = false;
  errorMessage = '';
  showForm = false;
  editingSupplierId: number | null = null;
  saving = false;
  formError = '';

  formData: SupplierRequest = this.emptyForm();

  searchTerm = '';
  statusFilter: 'all' | 'active' | 'inactive' = 'all';

  currentPage = 1;
  pageSize = 10;

  get filteredSuppliers(): Supplier[] {
    const term = this.searchTerm.trim().toLocaleLowerCase('fr');

    return this.suppliers.filter((supplier) => {
      const matchesSearch =
        !term ||
        [
          supplier.name,
          supplier.contactName,
          supplier.email,
          supplier.phone,
          supplier.city,
          supplier.postalCode,
          supplier.country
        ].some((value) => value?.toLocaleLowerCase('fr').includes(term));

      const matchesStatus =
        this.statusFilter === 'all' ||
        (this.statusFilter === 'active' && supplier.active) ||
        (this.statusFilter === 'inactive' && !supplier.active);

      return matchesSearch && matchesStatus;
    });
  }

  get paginatedSuppliers(): Supplier[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;

    return this.filteredSuppliers.slice(
      startIndex,
      startIndex + this.pageSize
    );
  }

  openSupplierDetails(supplier: Supplier): void {
    this.router.navigate(['/suppliers', supplier.id]);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }


  ngOnInit(): void {
    this.loadSuppliers();
  }

  private emptyForm(): SupplierRequest {
    return {
      name: '',
      contactName: '',
      email: '',
      phone: '',
      address: '',
      city: '',
      postalCode: '',
      country: 'France'
    };
  }

  loadSuppliers(): void {
    this.loading = true;
    this.errorMessage = '';

    this.supplierApi.getAll().subscribe({
      next: (suppliers) => {
        this.suppliers = suppliers;
        this.currentPage = 1;
        this.loading = false;
        this.cdr.detectChanges();
        this.appRef.tick();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les fournisseurs.';
        this.loading = false;
        this.cdr.detectChanges();
        this.appRef.tick();
      }
    });
  }

  openForm(): void {
    this.editingSupplierId = null;
    this.formData = this.emptyForm();
    this.formError = '';
    this.showForm = true;
  }

  openEditForm(supplier: Supplier): void {
    this.editingSupplierId = supplier.id;
    this.formError = '';

    this.formData = {
      name: supplier.name,
      contactName: supplier.contactName,
      email: supplier.email,
      phone: supplier.phone,
      address: supplier.address,
      city: supplier.city,
      postalCode: supplier.postalCode,
      country: supplier.country
    };

    this.showForm = true;
  }

  closeForm(): void {
    if (this.saving) return;
    this.showForm = false;
  }


  createSupplier(): void {
    if (this.saving) return;

    const request: SupplierRequest = {
      name: this.formData.name.trim(),
      contactName: this.formData.contactName?.trim() || null,
      email: this.formData.email?.trim() || null,
      phone: this.formData.phone?.trim() || null,
      address: this.formData.address?.trim() || null,
      city: this.formData.city?.trim() || null,
      postalCode: this.formData.postalCode?.trim() || null,
      country: this.formData.country?.trim() || null
    };

    if (!request.name) {
      this.formError = 'Le nom du fournisseur est obligatoire.';
      return;
    }

    this.saving = true;
    this.formError = '';

    const request$ = this.editingSupplierId === null
      ? this.supplierApi.create(request)
      : this.supplierApi.update(this.editingSupplierId, request);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.closeForm();
        this.loadSuppliers();
      },
      error: (error) => {
        this.saving = false;

        if (error.status === 409) {
          this.formError = 'Un fournisseur avec ce nom existe déjà.';
        } else {
          this.formError = this.editingSupplierId === null
            ? 'Impossible de créer le fournisseur. Vérifiez les champs et réessayez.'
            : 'Impossible de modifier le fournisseur. Vérifiez les champs et réessayez.';
        }

        this.cdr.detectChanges();
        this.appRef.tick();
      }
    });
  }

  
  deactivateSupplier(supplier: Supplier): void {
    const confirmed = window.confirm(
      `Voulez-vous désactiver le fournisseur « ${supplier.name} » ?`
    );

    if (!confirmed) return;

    this.supplierApi.deactivate(supplier.id).subscribe({
      next: () => {
        this.loadSuppliers();
      },
      error: () => {
        this.errorMessage =
          'Impossible de désactiver le fournisseur. Veuillez réessayer.';
        this.cdr.detectChanges();
        this.appRef.tick();
      }
    });
  }

  reactivateSupplier(supplier: Supplier): void {
    const confirmed = window.confirm(
      `Voulez-vous réactiver le fournisseur « ${supplier.name} » ?`
    );

    if (!confirmed) return;

    this.supplierApi.reactivate(supplier.id).subscribe({
      next: () => {
        this.loadSuppliers();
      },
      error: () => {
        this.errorMessage =
          'Impossible de réactiver le fournisseur. Veuillez réessayer.';
        this.cdr.detectChanges();
        this.appRef.tick();
      }
    });
  }

  
  deleteSupplierPermanently(supplier: Supplier): void {
    if (supplier.active) {
      this.errorMessage =
        'Désactivez d’abord le fournisseur avant de le supprimer définitivement.';
      return;
    }

    const confirmed = window.confirm(
      `ATTENTION : voulez-vous supprimer définitivement le fournisseur « ${supplier.name} » ? Cette action est irréversible.`
    );

    if (!confirmed) return;

    this.supplierApi.deletePermanently(supplier.id).subscribe({
      next: () => {
        this.loadSuppliers();
      },
      error: () => {
        this.errorMessage =
          'Impossible de supprimer ce fournisseur. Il est peut-être utilisé par d’autres données.';
        this.cdr.detectChanges();
        this.appRef.tick();
      }
    });
  }

}
