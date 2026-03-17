import { apiClient } from './client';
import type { PageResponse } from './clients';

export interface SavingsSummary {
  id: number;
  accountNo?: string;
  clientId?: number;
  clientName?: string;
  savingsProductId?: number;
  savingsProductName?: string;
  status?: { id: number; code: string; value: string };
  accountBalance?: number;
  [key: string]: unknown;
}

export interface SavingsDetail extends SavingsSummary {
  currency?: { code: string; displayLabel?: string };
  summary?: { accountBalance?: number };
  [key: string]: unknown;
}

export interface SavingsTemplate {
  productOptions?: Array<{ id: number; name: string }>;
  clientId?: number;
  clientName?: string;
  [key: string]: unknown;
}

export interface SavingsProduct {
  id: number;
  name: string;
  shortName?: string;
  [key: string]: unknown;
}

export interface SavingsTransaction {
  id: number;
  transactionType?: { id: number; code: string; value: string };
  amount?: number;
  date?: number[];
  [key: string]: unknown;
}

export function fetchSavingsAccounts(params?: {
  limit?: number;
  offset?: number;
  clientId?: number;
}): Promise<PageResponse<SavingsSummary>> {
  return apiClient
    .get<PageResponse<SavingsSummary>>('/savingsaccounts', { params: { limit: 20, ...params } })
    .then((r) => r.data);
}

export function fetchSavingsAccount(id: number): Promise<SavingsDetail> {
  return apiClient.get<SavingsDetail>(`/savingsaccounts/${id}`).then((r) => r.data);
}

export function fetchSavingsTemplate(clientId: number, productId?: number): Promise<SavingsTemplate> {
  const params: Record<string, string | number> = { clientId };
  if (productId != null) params.productId = productId;
  return apiClient.get<SavingsTemplate>('/savingsaccounts/template', { params }).then((r) => r.data);
}

export function fetchSavingsProducts(): Promise<SavingsProduct[]> {
  return apiClient.get<SavingsProduct[]>('/savingsproducts').then((r) => r.data);
}

export function createSavingsAccount(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/savingsaccounts', body).then((r) => r.data);
}

export function fetchSavingsTransactions(savingsId: number): Promise<SavingsTransaction[]> {
  return apiClient
    .get<{ pageItems?: SavingsTransaction[] }>(`/savingsaccounts/${savingsId}/transactions`, {
      params: { limit: 100 },
    })
    .then((r) => r.data.pageItems ?? []);
}

export function savingsDeposit(
  savingsId: number,
  body: { transactionDate: string; transactionAmount: number; paymentTypeId?: number; note?: string }
): Promise<{ resourceId: number }> {
  const payload = { ...body, locale: 'en', dateFormat: 'yyyy-MM-dd' };
  return apiClient
    .post<{ resourceId: number }>(`/savingsaccounts/${savingsId}/transactions`, payload, {
      params: { command: 'deposit' },
    })
    .then((r) => r.data);
}

export function savingsWithdrawal(
  savingsId: number,
  body: { transactionDate: string; transactionAmount: number; paymentTypeId?: number; note?: string }
): Promise<{ resourceId: number }> {
  const payload = { ...body, locale: 'en', dateFormat: 'yyyy-MM-dd' };
  return apiClient
    .post<{ resourceId: number }>(`/savingsaccounts/${savingsId}/transactions`, payload, {
      params: { command: 'withdrawal' },
    })
    .then((r) => r.data);
}

export function savingsHoldAmount(
  savingsId: number,
  body: { transactionDate: string; transactionAmount: number }
): Promise<{ resourceId: number }> {
  const payload = { ...body, locale: 'en', dateFormat: 'yyyy-MM-dd' };
  return apiClient
    .post<{ resourceId: number }>(`/savingsaccounts/${savingsId}/transactions`, payload, {
      params: { command: 'holdAmount' },
    })
    .then((r) => r.data);
}
