export interface CategoryResponse {
  id: number;
  name: string;
  slug: string;
  description: string;
  parentId: number | null;
}

export interface MediaResponse {
  id: number;
  fullUrl: string;
  isPrimary: boolean;
  sortOrder: number;
}

export interface ProductResponse {
  id: number;
  sku: string;
  title: string;
  description: string;
  price: number;     
  currency: string;
  categoryId: number;
  sellerId: number;
  attributes: Record<string, any>;  
  media: MediaResponse[];
}

export interface InventoryResponse {
  productId: number;
  currentQuantity: number;
  status: string;          
  message: string;
}