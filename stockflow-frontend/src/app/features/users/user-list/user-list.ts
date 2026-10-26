import {
  DatePipe,
  registerLocaleData
} from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { Component, LOCALE_ID, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Auth } from '../../../core/auth';
import { UserRole } from '../../../core/auth.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination';
import { User } from '../user';
import { UserApi } from '../user-api';

registerLocaleData(localeFr);

type FormMode = 'create' | 'edit' | 'password';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [DatePipe, FormsModule, PaginationComponent],
  providers: [{ provide: LOCALE_ID, useValue: 'fr-FR' }],
  templateUrl: './user-list.html',
  styleUrl: './user-list.scss'
})
export class UserList implements OnInit {
  private readonly userApi = inject(UserApi);
  private readonly auth = inject(Auth);

  readonly currentUserId = this.auth.getUser()?.userId ?? null;

  readonly roles: { value: UserRole; label: string }[] = [
    { value: 'ADMIN', label: 'Administrateur' },
    { value: 'STOCK_MANAGER', label: 'Gestionnaire de stock' },
    { value: 'MANAGER', label: 'Responsable' }
  ];

  readonly users = signal<User[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly formError = signal('');

  readonly searchTerm = signal('');
  readonly roleFilter = signal<'ALL' | UserRole>('ALL');
  readonly statusFilter = signal<'ALL' | 'ACTIVE' | 'DISABLED'>('ALL');
  readonly currentPage = signal(1);
  readonly pageSize = 10;

  formMode: FormMode | null = null;
  editingUserId: number | null = null;
  form = this.emptyForm();

  readonly filteredUsers = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('fr');
    const role = this.roleFilter();
    const status = this.statusFilter();

    return this.users().filter(user => {
      const matchesTerm =
        !term ||
        user.fullName.toLocaleLowerCase('fr').includes(term) ||
        user.email.toLocaleLowerCase('fr').includes(term);

      const matchesRole = role === 'ALL' || user.role === role;

      const matchesStatus =
        status === 'ALL' ||
        (status === 'ACTIVE' ? user.enabled : !user.enabled);

      return matchesTerm && matchesRole && matchesStatus;
    });
  });

  readonly paginatedUsers = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;

    return this.filteredUsers().slice(start, start + this.pageSize);
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  private emptyForm() {
    return {
      fullName: '',
      email: '',
      password: '',
      role: 'STOCK_MANAGER' as UserRole
    };
  }

  loadUsers(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.userApi.getAll().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Impossible de charger les utilisateurs.');
        this.loading.set(false);
      }
    });
  }

  onFilterChange(): void {
    this.currentPage.set(1);
  }

  roleLabel(role: UserRole): string {
    return this.roles.find(item => item.value === role)?.label ?? role;
  }

  roleClass(role: UserRole): string {
    return role === 'ADMIN' ? 'admin' : role === 'MANAGER' ? 'manager' : '';
  }

  isCurrentUser(user: User): boolean {
    return user.id === this.currentUserId;
  }

  // ----- Formulaires -----

  openCreateForm(): void {
    this.form = this.emptyForm();
    this.editingUserId = null;
    this.formMode = 'create';
    this.formError.set('');
    this.successMessage.set('');
  }

  openEditForm(user: User): void {
    this.form = {
      fullName: user.fullName,
      email: user.email,
      password: '',
      role: user.role
    };
    this.editingUserId = user.id;
    this.formMode = 'edit';
    this.formError.set('');
    this.successMessage.set('');
  }

  openPasswordForm(user: User): void {
    this.form = { ...this.emptyForm(), fullName: user.fullName, email: user.email };
    this.editingUserId = user.id;
    this.formMode = 'password';
    this.formError.set('');
    this.successMessage.set('');
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formMode = null;
    this.editingUserId = null;
    this.formError.set('');
  }

  get formTitle(): string {
    switch (this.formMode) {
      case 'create':
        return 'Nouvel utilisateur';
      case 'edit':
        return 'Modifier l’utilisateur';
      case 'password':
        return 'Changer le mot de passe';
      default:
        return '';
    }
  }

  submitForm(): void {
    if (this.saving() || this.formMode === null) {
      return;
    }

    this.formError.set('');

    const fullName = this.form.fullName.trim();
    const email = this.form.email.trim();

    if (this.formMode !== 'password' && (!fullName || !email)) {
      this.formError.set('Le nom complet et l’adresse e-mail sont obligatoires.');
      return;
    }

    if (
      this.formMode !== 'edit' &&
      (this.form.password.length < 12 || this.form.password.length > 128)
    ) {
      this.formError.set(
        'Le mot de passe doit contenir entre 12 et 128 caractères.'
      );
      return;
    }

    this.saving.set(true);

    const request$ =
      this.formMode === 'create'
        ? this.userApi.create({
            fullName,
            email,
            password: this.form.password,
            role: this.form.role
          })
        : this.formMode === 'edit'
          ? this.userApi.update(this.editingUserId!, {
              fullName,
              email,
              role: this.form.role
            })
          : this.userApi.updatePassword(
              this.editingUserId!,
              this.form.password
            );

    const mode = this.formMode;

    request$.subscribe({
      next: (saved) => {
        if (mode === 'create') {
          this.users.update(users => [...users, saved]);
          this.successMessage.set(`L’utilisateur ${saved.fullName} a été créé.`);
        } else {
          this.replaceUser(saved);
          this.successMessage.set(
            mode === 'edit'
              ? `L’utilisateur ${saved.fullName} a été mis à jour.`
              : `Le mot de passe de ${saved.fullName} a été modifié.`
          );
        }

        this.saving.set(false);
        this.formMode = null;
        this.editingUserId = null;
        this.form = this.emptyForm();
      },
      error: (error) => {
        this.saving.set(false);
        this.formError.set(
          error?.error?.message ?? 'L’opération a échoué. Veuillez réessayer.'
        );
      }
    });
  }

  // ----- Activation / désactivation -----

  toggleEnabled(user: User): void {
    if (this.isCurrentUser(user)) {
      return;
    }

    const action = user.enabled ? 'Désactiver' : 'Réactiver';

    if (!confirm(`${action} le compte de ${user.fullName} ?`)) {
      return;
    }

    this.errorMessage.set('');
    this.successMessage.set('');

    const request$ = user.enabled
      ? this.userApi.disable(user.id)
      : this.userApi.enable(user.id);

    request$.subscribe({
      next: (saved) => {
        this.replaceUser(saved);
        this.successMessage.set(
          saved.enabled
            ? `Le compte de ${saved.fullName} a été réactivé.`
            : `Le compte de ${saved.fullName} a été désactivé.`
        );
      },
      error: (error) => {
        this.errorMessage.set(
          error?.error?.message ?? 'Impossible de modifier le statut du compte.'
        );
      }
    });
  }

  private replaceUser(saved: User): void {
    this.users.update(users =>
      users.map(user => (user.id === saved.id ? saved : user))
    );
  }
}
