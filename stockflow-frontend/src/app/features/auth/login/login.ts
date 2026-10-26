
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { Auth } from '../../../core/auth';
import { LoginRequest } from '../../../core/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  credentials: LoginRequest = {
    email: '',
    password: ''
  };

  loading = false;
  errorMessage = '';

  submit(): void {
    if (this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.auth.login(this.credentials).subscribe({
      next: () => {
        this.loading = false;
        void this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading = false;

        if (error.status === 401 || error.status === 403) {
          this.errorMessage = 'Adresse e-mail ou mot de passe incorrect.';
        } else {
          this.errorMessage =
            'Impossible de se connecter au serveur. Réessayez plus tard.';
        }
      }
    });
  }
}
