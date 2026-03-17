import { apiClient } from './client';

export interface LoanProductSummary {
  id: number;
  name?: string;
  shortName?: string;
  description?: string;
  [key: string]: unknown;
}

export interface LoanProductDetail extends LoanProductSummary {
  fundId?: number;
  fundName?: string;
  currency?: { code: string; [key: string]: unknown };
  principal?: number;
  numberOfRepayments?: number;
  repaymentEvery?: number;
  repaymentFrequencyType?: { id: number; code?: string; value?: string };
  interestRatePerPeriod?: number;
  amortizationType?: { id: number; code?: string; value?: string };
  interestType?: { id: number; code?: string; value?: string };
  transactionProcessingStrategyId?: number;
  transactionProcessingStrategyName?: string;
  [key: string]: unknown;
}

export interface OptionItem {
  id: number;
  code?: string;
  value?: string;
  name?: string;
  [key: string]: unknown;
}

export interface LoanProductTemplate extends LoanProductDetail {
  fundOptions?: Array<{ id: number; name?: string }>;
  currencyOptions?: Array<{ code: string; name?: string }>;
  repaymentFrequencyTypeOptions?: OptionItem[];
  amortizationTypeOptions?: OptionItem[];
  interestTypeOptions?: OptionItem[];
  transactionProcessingStrategyOptions?: Array<{ id: number; name?: string }>;
  [key: string]: unknown;
}

export function fetchLoanProducts(): Promise<LoanProductSummary[]> {
  return apiClient
    .get<LoanProductSummary[]>('/loanproducts')
    .then((r) => (Array.isArray(r.data) ? r.data : []));
}

export function fetchLoanProduct(
  id: number,
  options?: { template?: boolean }
): Promise<LoanProductDetail & LoanProductTemplate> {
  const params = options?.template ? { template: 'true' } : {};
  return apiClient
    .get<LoanProductDetail & LoanProductTemplate>(`/loanproducts/${id}`, { params })
    .then((r) => r.data);
}

export function fetchLoanProductTemplate(): Promise<LoanProductTemplate> {
  return apiClient.get<LoanProductTemplate>('/loanproducts/template').then((r) => r.data);
}

export function createLoanProduct(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/loanproducts', body).then((r) => r.data);
}

export function updateLoanProduct(
  id: number,
  body: Record<string, unknown>
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/loanproducts/${id}`, body).then((r) => r.data);
}
