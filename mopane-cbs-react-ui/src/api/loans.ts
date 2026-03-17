import { apiClient } from './client';
import type { PageResponse } from './clients';

export interface LoanSummary {
  id: number;
  accountNo?: string;
  clientId?: number;
  clientName?: string;
  clientOfficeId?: number;
  loanProductId?: number;
  loanProductName?: string;
  loanProductDescription?: string;
  status?: { id: number; code: string; value: string };
  principal?: number;
  loanBalance?: number;
  [key: string]: unknown;
}

export interface LoanSchedulePeriod {
  period?: number;
  dueDate?: number[];
  principalDue?: number;
  interestDue?: number;
  feeChargesDue?: number;
  penaltyChargesDue?: number;
  totalDueForPeriod?: number;
  totalPaidForPeriod?: number;
  totalOutstandingForPeriod?: number;
  principalOutstanding?: number;
  [key: string]: unknown;
}

export interface LoanRepaymentSchedule {
  periods?: LoanSchedulePeriod[];
  currency?: { code: string; [key: string]: unknown };
  [key: string]: unknown;
}

export interface LoanDetail extends LoanSummary {
  timeline?: { submittedOnDate?: number[]; approvedOnDate?: number[]; disbursedOnDate?: number[] };
  termFrequency?: { id: number; code: string; value: string };
  termPeriod?: number;
  repaymentEvery?: number;
  interestRatePerPeriod?: number;
  interestType?: { id: number; code: string; value: string };
  currency?: { code: string; displayLabel?: string };
  repaymentSchedule?: LoanRepaymentSchedule;
  [key: string]: unknown;
}

export interface LoanTemplate {
  productOptions?: Array<{ id: number; name: string }>;
  loanOfficerOptions?: Array<{ id: number; displayName?: string; name?: string }>;
  clientId?: number;
  clientName?: string;
  [key: string]: unknown;
}

export interface LoanProduct {
  id: number;
  name: string;
  shortName?: string;
  [key: string]: unknown;
}

export function fetchLoans(params?: {
  limit?: number;
  offset?: number;
  clientId?: number;
  sqlSearch?: string;
}): Promise<PageResponse<LoanSummary>> {
  return apiClient
    .get<PageResponse<LoanSummary>>('/loans', { params: { limit: 20, ...params } })
    .then((r) => r.data);
}

export function fetchLoan(
  id: number,
  options?: { associations?: string }
): Promise<LoanDetail> {
  const params = options?.associations ? { associations: options.associations } : {};
  return apiClient.get<LoanDetail>(`/loans/${id}`, { params }).then((r) => r.data);
}

export function fetchLoanTemplate(clientId: number, productId?: number): Promise<LoanTemplate> {
  const params: Record<string, string | number> = {
    templateType: 'individual',
    clientId,
  };
  if (productId != null) params.productId = productId;
  return apiClient.get<LoanTemplate>('/loans/template', { params }).then((r) => r.data);
}

export function fetchLoanProducts(): Promise<LoanProduct[]> {
  return apiClient.get<LoanProduct[]>('/loanproducts').then((r) => r.data);
}

export function createLoan(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/loans', body).then((r) => r.data);
}

export function updateLoan(
  id: number,
  body: Record<string, unknown>
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/loans/${id}`, body).then((r) => r.data);
}

export function loanRepayment(
  loanId: number,
  body: { transactionDate: string; transactionAmount: number; note?: string }
): Promise<{ resourceId: number }> {
  return apiClient
    .post<{ resourceId: number }>(`/loans/${loanId}/transactions`, body, {
      params: { command: 'repayment' },
    })
    .then((r) => r.data);
}
