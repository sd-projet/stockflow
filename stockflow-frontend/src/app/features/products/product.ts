
export interface SupplierProduct {
  supplierId: number;
  supplierName: string;
  supplierReference: string | null;
  unitPrice: number;
}

export interface SupplierProductRequest {
  supplierId: number;
  supplierReference: string | null;
  unitPrice: number;
}

export interface Product {
  id: number;
  reference: string;
  name: string;
  description: string | null;
  categoryId: number;
  categoryName: string;
  quantityInStock: number;
  minimumStock: number;
  unit: string;
  unitPrice: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  suppliers: SupplierProduct[];
}

export interface ProductRequest {
  reference: string;
  name: string;
  description: string | null;
  categoryId: number;
  minimumStock: number;
  unit: string;
  unitPrice: number;
  suppliers: SupplierProductRequest[];
}
