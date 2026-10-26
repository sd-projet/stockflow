import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Auth } from '../../../core/auth';
import { RegisterRequest } from '../../../core/auth.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.scss'
})
export class Register {
  private readonly auth = inject(Auth);
  private readonly router = inject(Router);

  credentials: RegisterRequest & { confirmPassword: string } = {
    fullName: '',
    email: '',
    password: '',
    confirmPassword: ''
  };

  loading = false;
  errorMessage = '';
  passwordVisible = false;

  submit(): void {
    if (this.loading) return;

    const fullName = this.credentials.fullName.trim();
    const email = this.credentials.email.trim();
    const password = this.credentials.password;

    if (password.length < 12 || password.length > 128) {
      this.errorMessage = 'Le mot de passe doit contenir entre 12 et 128 caractères.';
      return;
    }

    if (password !== this.credentials.confirmPassword) {
      this.errorMessage = 'Les deux mots de passe ne correspondent pas.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.auth.register({ fullName, email, password }).subscribe({
      next: () => {
        this.loading = false;
        void this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading = false;
        if (error.status === 409) {
          this.errorMessage = error.error?.detail ?? 'Un compte avec cette adresse e-mail existe déjà.';
        } else if (error.status === 400) {
          this.errorMessage = error.error?.detail ?? 'Vérifiez les informations saisies et réessayez.';
        } else {
          this.errorMessage = 'Impossible de créer votre compte pour le moment. Réessayez plus tard.';
        }
      }
    });
  }
}
