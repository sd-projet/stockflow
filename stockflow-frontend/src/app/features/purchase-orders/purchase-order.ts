
export type PurchaseOrderStatus =
  | 'DRAFT'
  | 'ORDERED'
  | 'PARTIALLY_RECEIVED'
  | 'RECEIVED'
  | 'CANCELLED';

export interface PurchaseOrder {
  id: number;
  orderNumber: string;
  supplierId: number;
  supplierName: string;
  status: PurchaseOrderStatus;
  orderDate: string | null;
  expectedDeliveryDate: string | null;
  receivedDate: string | null;
  notes: string | null;
  archived: boolean;
  createdById: number;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
  lines: PurchaseOrderLine[];
  totalAmount: number;
}


export interface PurchaseOrderLine {
  id: number;
  productId: number;
  productName: string;
  quantityOrdered: number;
  quantityReceived: number;
  unitPrice: number;
  totalPrice: number;
}


export interface PurchaseOrderLineRequest {
  productId: number;
  quantityOrdered: number;
  unitPrice: number;
}

export interface PurchaseOrderCreateRequest {
  orderNumber?: string | null;
  supplierId: number;
  orderDate?: string | null;
  expectedDeliveryDate?: string | null;
  notes?: string | null;
  lines: PurchaseOrderLineRequest[];
}

export interface PurchaseOrderUpdateRequest {
  orderNumber: string;
  supplierId: number;
  orderDate?: string | null;
  expectedDeliveryDate?: string | null;
  notes?: string | null;
  lines: PurchaseOrderLineRequest[];
}

export interface PurchaseOrderReceiveRequest {
  lines: {
    lineId: number;
    quantityReceived: number;
  }[];
}
