
import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Category, CategoryRequest } from '../category';
import { CategoryApi } from '../category-api';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';
import { Router } from '@angular/router';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [FormsModule, PaginationComponent],
  templateUrl: './category-list.html',
  styleUrl: './category-list.scss'
})
export class CategoryList implements OnInit {
  private readonly categoryApi = inject(CategoryApi);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);

  categories: Category[] = [];
  searchTerm = '';

  currentPage = 1;
  pageSize = 10;
  loading = false;
  errorMessage = '';
  showForm = false;
  saving = false;
  formError = '';

  formData: CategoryRequest = {
    name: '',
    description: ''
  };

  selectedCategory: Category | null = null;
  isEditing = false;
  showDetails = false;
  deletingCategoryId: number | null = null;

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading = true;
    this.errorMessage = '';

    this.categoryApi.getAll().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.currentPage = 1;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les catégories.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get filteredCategories(): Category[] {
    const term = this.searchTerm.trim().toLocaleLowerCase('fr');

    if (!term) {
      return this.categories;
    }

    return this.categories.filter(category =>
      category.name.toLocaleLowerCase('fr').includes(term)
    );
  }

  get paginatedCategories(): Category[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;

    return this.filteredCategories.slice(
      startIndex,
      startIndex + this.pageSize
    );
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  openForm(category?: Category): void {
    this.isEditing = !!category;
    this.formData = category
      ? {
          name: category.name,
          description: category.description
        }
      : {
          name: '',
          description: ''
        };

    this.selectedCategory = category ?? null;
    this.formError = '';
    this.showForm = true;
  }

  closeForm(): void {
    if (this.saving) return;
    this.showForm = false;
  }

  createCategory(): void {
    if (this.saving) return;

    const request: CategoryRequest = {
      name: this.formData.name.trim(),
      description: this.formData.description?.trim() || null
    };

    if (!request.name) {
      this.formError = 'Le nom de la catégorie est obligatoire.';
      return;
    }

    this.saving = true;
    this.formError = '';

    const request$ = this.isEditing && this.selectedCategory
      ? this.categoryApi.update(this.selectedCategory.id, request)
      : this.categoryApi.create(request);

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.selectedCategory = null;
        this.isEditing = false;
        this.loadCategories();
      },
      error: (error) => {
        this.saving = false;

        if (error.status === 409) {
          this.formError =
            'Une catégorie portant ce nom existe déjà, ou cette catégorie ne peut pas être modifiée en raison de ses relations.';
        } else if (error.status === 400) {
          this.formError =
            'Les données saisies sont invalides. Vérifiez le nom et la description.';
        } else if (error.status === 404) {
          this.formError =
            'Cette catégorie n’existe plus. Actualisez la liste et réessayez.';
        } else {
          this.formError =
            'Impossible d’enregistrer la catégorie. Veuillez réessayer.';
        }

        this.cdr.detectChanges();
      }
    });
  }

  viewCategory(category: Category): void {
    this.router.navigate(['/categories', category.id]);
  }

  editCategory(category: Category): void {
    this.openForm(category);
  }

  deleteCategory(category: Category): void {
    const confirmed = window.confirm(
      `Voulez-vous vraiment supprimer la catégorie « ${category.name} » ?`
    );

    if (!confirmed || this.deletingCategoryId !== null) {
      return;
    }

    this.deletingCategoryId = category.id;
    this.errorMessage = '';

    this.categoryApi.delete(category.id).subscribe({
      next: () => {
        this.deletingCategoryId = null;
        this.loadCategories();
      },
      error: (error) => {
        this.deletingCategoryId = null;

        if (error.status === 409) {
          this.errorMessage =
            'Cette catégorie est utilisée par un ou plusieurs produits et ne peut pas être supprimée.';
        } else if (error.status === 404) {
          this.errorMessage =
            'Cette catégorie n’existe plus. La liste va être actualisée.';
          this.loadCategories();
        } else {
          this.errorMessage =
            'Impossible de supprimer la catégorie. Veuillez réessayer.';
        }

        this.cdr.detectChanges();
      }
    });
  }
}
