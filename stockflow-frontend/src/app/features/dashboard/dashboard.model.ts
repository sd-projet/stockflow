import { StockMovement } from '../stock-movements/stock-movement';

export interface LowStockItem {
  id: number;
  reference: string;
  name: string;
  quantityInStock: number;
  minimumStock: number;
  unit: string;
}

export interface DashboardData {
  activeProducts: number;
  stockValue: number;
  lowStockCount: number;
  outOfStockCount: number;
  draftOrders: number;
  openOrders: number;
  ordersByStatus: Record<string, number>;
  lowStockProducts: LowStockItem[];
  recentMovements: StockMovement[];
}
