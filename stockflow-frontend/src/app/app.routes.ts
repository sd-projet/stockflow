
import { Routes } from '@angular/router';
import { MainLayout } from './layout/main-layout/main-layout';
import { authGuard, roleGuard } from './core/auth-guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/public/home/home').then(m => m.Home)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register').then(m => m.Register)
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login')
        .then(m => m.Login)
  },
  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard/dashboard')
            .then(m => m.Dashboard)
      },
      {
        path: 'products/:id',
        loadComponent: () =>
          import('./features/products/product-detail/product-detail')
            .then(m => m.ProductDetail)
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./features/products/product-list/product-list')
            .then(m => m.ProductList)
      },
      {
        path: 'categories/:id',
        loadComponent: () =>
          import('./features/categories/category-detail/category-detail')
            .then(m => m.CategoryDetail)
      },
      {
        path: 'categories',
        loadComponent: () =>
          import('./features/categories/category-list/category-list')
            .then(m => m.CategoryList)
      },
      {
        path: 'suppliers/:id',
        loadComponent: () =>
          import('./features/suppliers/supplier-detail/supplier-detail')
            .then(m => m.SupplierDetail)
      },
      {
        path: 'suppliers',
        loadComponent: () =>
          import('./features/suppliers/supplier-list/supplier-list')
            .then(m => m.SupplierList)
      },
      {
        path: 'stock-movements',
        loadComponent: () =>
          import('./features/stock-movements/stock-movement-list/stock-movement-list')
            .then(m => m.StockMovementList)
      },
      {
        path: 'stock-valuation',
        loadComponent: () =>
          import('./features/stock-valuation/stock-valuation')
            .then(m => m.StockValuation)
      },
      {
        path: 'purchase-orders/:id',
        loadComponent: () =>
          import('./features/purchase-orders/purchase-order-list/purchase-order-list')
            .then(m => m.PurchaseOrderList)
      },
      {
        path: 'purchase-orders',
        loadComponent: () =>
          import('./features/purchase-orders/purchase-order-list/purchase-order-list')
            .then(m => m.PurchaseOrderList)
      },
      {
        path: 'users',
        canActivate: [roleGuard('ADMIN')],
        loadComponent: () =>
          import('./features/users/user-list/user-list')
            .then(m => m.UserList)
      }

    ]
  },
  { path: '**', redirectTo: 'login' }
];
