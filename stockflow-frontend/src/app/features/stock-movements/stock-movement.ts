
export type MovementType = 'IN' | 'OUT' | 'ADJUSTMENT';

export interface StockMovement {
  id: number;
  productId: number;
  productReference: string;
  productName: string;
  type: MovementType;
  quantityBefore: number;
  quantityAfter: number;
  reason: string;
  purchaseOrderLineId: number | null;
  performedBy: number;
  createdAt: string;
}

export interface StockMovementRequest {
  productId: number;
  type: MovementType;
  quantity: number;
  reason: string;
  purchaseOrderLineId?: number | null;
}
