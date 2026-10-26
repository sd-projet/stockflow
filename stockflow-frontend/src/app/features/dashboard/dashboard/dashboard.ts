import {
  CurrencyPipe,
  DatePipe,
  DecimalPipe,
  registerLocaleData
} from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { Component, LOCALE_ID, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { DashboardApi } from '../dashboard-api';
import { DashboardData } from '../dashboard.model';
import { MovementType } from '../../stock-movements/stock-movement';

registerLocaleData(localeFr);

@Component({
  imports: [DecimalPipe, CurrencyPipe, DatePipe, RouterLink],
  providers: [{ provide: LOCALE_ID, useValue: 'fr-FR' }],
  selector: 'app-dashboard',
  styleUrl: './dashboard.scss',
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private readonly dashboardApi = inject(DashboardApi);
  private readonly router = inject(Router);

  readonly data = signal<DashboardData | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal('');

  readonly orderStatuses: { key: string; label: string }[] = [
    { key: 'DRAFT', label: 'Brouillons' },
    { key: 'ORDERED', label: 'Commandées' },
    { key: 'PARTIALLY_RECEIVED', label: 'Partiellement reçues' },
    { key: 'RECEIVED', label: 'Réceptionnées' },
    { key: 'CANCELLED', label: 'Annulées' }
  ];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMessage.set('');

    this.dashboardApi.get().subscribe({
      next: (data) => {
        this.data.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(
          'Impossible de charger le tableau de bord. Vérifiez que le serveur est démarré.'
        );
        this.loading.set(false);
      }
    });
  }

  movementLabel(type: MovementType): string {
    const labels: Record<MovementType, string> = {
      IN: 'Entrée de stock',
      OUT: 'Sortie de stock',
      ADJUSTMENT: 'Ajustement'
    };

    return labels[type];
  }

  movementDelta(before: number, after: number): string {
    const delta = after - before;

    return delta > 0 ? `+${delta}` : `${delta}`;
  }

  movementClass(before: number, after: number): string {
    return after >= before ? 'entry' : 'exit';
  }

  orderCount(key: string): number {
    return this.data()?.ordersByStatus?.[key] ?? 0;
  }

  navigateTo(path: string, queryParams?: Record<string, unknown>): void {
    void this.router.navigate([path], { queryParams });
  }

  openProduct(id: number): void {
    void this.router.navigate(['/products', id]);
  }
}
