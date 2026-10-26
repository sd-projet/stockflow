
import {
  Component,
  OnInit,
  ChangeDetectorRef,
  ApplicationRef,
  inject
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Supplier } from '../supplier';
import { SupplierApi } from '../supplier-api';
import { Product } from '../../products/product';
import { ProductApi } from '../../products/product-api';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupplierRequest } from '../supplier';

@Component({
  selector: 'app-supplier-detail',
  standalone: true,
  imports: [DatePipe, FormsModule],
  templateUrl: './supplier-detail.html',
  styleUrl: './supplier-detail.scss'
})
export class SupplierDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly supplierApi = inject(SupplierApi);
  private readonly productApi = inject(ProductApi);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly appRef = inject(ApplicationRef);

  supplier: Supplier | null = null;
  supplierProducts: Product[] = [];

  loadingSupplier = false;
  loadingProducts = false;

  errorMessage = '';
  productsErrorMessage = '';
  showEditForm = false;
  saving = false;
  formError = '';
  editForm: SupplierRequest = this.emptyForm();

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isInteger(id) || id <= 0) {
      this.errorMessage = 'Identifiant de fournisseur invalide.';
      return;
    }

    this.loadSupplier(id);
    this.loadSupplierProducts(id);
  }

  private loadSupplier(id: number): void {
    this.loadingSupplier = true;
    this.errorMessage = '';

    this.supplierApi.getById(id).subscribe({
      next: (supplier) => {
        this.supplier = supplier;
        this.loadingSupplier = false;
        this.refreshView();
      },
      error: () => {
        this.errorMessage =
          'Impossible de charger les informations du fournisseur.';
        this.loadingSupplier = false;
        this.refreshView();
      }
    });
  }

  private loadSupplierProducts(supplierId: number): void {
    this.loadingProducts = true;
    this.productsErrorMessage = '';

    this.productApi.getAll().subscribe({
      next: (products) => {
        this.supplierProducts = products.filter((product) =>
          product.suppliers?.some(
            (association) => association.supplierId === supplierId
          )
        );

        this.loadingProducts = false;
        this.refreshView();
      },
      error: () => {
        this.productsErrorMessage =
          'Impossible de charger les produits associés à ce fournisseur.';
        this.loadingProducts = false;
        this.refreshView();
      }
    });
  }

  private emptyForm(): SupplierRequest {
    return { name: '', contactName: null, email: null, phone: null, address: null, city: null, postalCode: null, country: null };
  }

  openEditForm(): void {
    if (!this.supplier) return;
    this.editForm = { name: this.supplier.name, contactName: this.supplier.contactName, email: this.supplier.email, phone: this.supplier.phone, address: this.supplier.address, city: this.supplier.city, postalCode: this.supplier.postalCode, country: this.supplier.country };
    this.formError = '';
    this.showEditForm = true;
  }

  saveSupplier(): void {
    if (!this.supplier || !this.editForm.name.trim()) { this.formError = 'Le nom du fournisseur est obligatoire.'; return; }
    this.saving = true;
    this.supplierApi.update(this.supplier.id, { ...this.editForm, name: this.editForm.name.trim() }).subscribe({
      next: (updated) => { this.supplier = updated; this.showEditForm = false; this.saving = false; this.refreshView(); },
      error: () => { this.formError = 'Impossible d’enregistrer les modifications.'; this.saving = false; this.refreshView(); }
    });
  }

  toggleSupplierStatus(): void {
    if (!this.supplier) return;
    const current = this.supplier;
    const action = current.active ? this.supplierApi.deactivate(current.id) : this.supplierApi.reactivate(current.id);
    action.subscribe({
      next: () => { this.supplier = { ...current, active: !current.active }; this.refreshView(); },
      error: () => { this.errorMessage = `Impossible de ${current.active ? 'désactiver' : 'réactiver'} ce fournisseur.`; this.refreshView(); }
    });
  }

  goBack(): void {
    this.router.navigate(['/suppliers']);
  }

  private refreshView(): void {
    this.cdr.detectChanges();
    this.appRef.tick();
  }
}
