
export interface Supplier {
  id: number;
  name: string;
  contactName: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  city: string | null;
  postalCode: string | null;
  country: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierRequest {
  name: string;
  contactName: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  city: string | null;
  postalCode: string | null;
  country: string | null;
}
