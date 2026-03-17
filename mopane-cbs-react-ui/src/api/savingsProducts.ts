import { apiClient } from './client';

export interface SavingsProductSummary {
  id: number;
  name?: string;
  shortName?: string;
  description?: string;
  [key: string]: unknown;
}

export interface SavingsProductDetail extends SavingsProductSummary {
  currency?: { code: string; [key: string]: unknown };
  nominalAnnualInterestRate?: number;
  interestCompoundingPeriodType?: { id: number; code?: string; value?: string };
  interestPostingPeriodType?: { id: number; code?: string; value?: string };
  interestCalculationType?: { id: number; code?: string; value?: string };
  minRequiredOpeningBalance?: number;
  [key: string]: unknown;
}

export interface OptionItem {
  id: number;
  code?: string;
  value?: string;
  [key: string]: unknown;
}

export interface SavingsProductTemplate extends SavingsProductDetail {
  currencyOptions?: Array<{ code: string; name?: string }>;
  interestCompoundingPeriodTypeOptions?: OptionItem[];
  interestPostingPeriodTypeOptions?: OptionItem[];
  interestCalculationTypeOptions?: OptionItem[];
  [key: string]: unknown;
}

export function fetchSavingsProducts(): Promise<SavingsProductSummary[]> {
  return apiClient
    .get<SavingsProductSummary[]>('/savingsproducts')
    .then((r) => (Array.isArray(r.data) ? r.data : []));
}

export function fetchSavingsProduct(
  id: number,
  options?: { template?: boolean }
): Promise<SavingsProductDetail & SavingsProductTemplate> {
  const params = options?.template ? { template: 'true' } : {};
  return apiClient
    .get<SavingsProductDetail & SavingsProductTemplate>(`/savingsproducts/${id}`, { params })
    .then((r) => r.data);
}

export function fetchSavingsProductTemplate(): Promise<SavingsProductTemplate> {
  return apiClient.get<SavingsProductTemplate>('/savingsproducts/template').then((r) => r.data);
}

export function createSavingsProduct(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/savingsproducts', body).then((r) => r.data);
}

export function updateSavingsProduct(
  id: number,
  body: Record<string, unknown>
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/savingsproducts/${id}`, body).then((r) => r.data);
}
