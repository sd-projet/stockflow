import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Auth } from '../../core/auth';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss'
})
export class MainLayout {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  readonly user = this.auth.getUser();
  readonly isAdmin = this.auth.isAdmin();

  readonly roleLabels: Record<string, string> = {
    ADMIN: 'Administrateur',
    STOCK_MANAGER: 'Gestionnaire de stock',
    MANAGER: 'Responsable'
  };

  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/login']);
  }
}
