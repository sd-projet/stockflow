import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from './auth';
import { UserRole } from './auth.model';

/** Redirige vers la page de connexion si l'utilisateur n'est pas connecté. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(Auth);
  const router = inject(Router);

  return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

/** Réserve une route à certains rôles ; sinon retour au tableau de bord. */
export const roleGuard = (...roles: UserRole[]): CanActivateFn => () => {
  const auth = inject(Auth);
  const router = inject(Router);

  return auth.hasRole(...roles) ? true : router.createUrlTree(['/dashboard']);
};
