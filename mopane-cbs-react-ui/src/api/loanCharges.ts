import { apiClient } from './client';

export interface LoanChargeSummary {
  id: number;
  chargeId?: number;
  name?: string;
  amount?: number;
  amountPaid?: number;
  amountWaived?: number;
  amountOutstanding?: number;
  dueAsOfDate?: number[];
  chargeTimeType?: { id: number; code?: string; value?: string };
  [key: string]: unknown;
}

export interface LoanChargeTemplate {
  chargeOptions?: Array<{ id: number; name: string; amount?: number }>;
  [key: string]: unknown;
}

export function fetchLoanCharges(loanId: number): Promise<LoanChargeSummary[]> {
  return apiClient
    .get<LoanChargeSummary[] | { pageItems?: LoanChargeSummary[] }>(`/loans/${loanId}/charges`)
    .then((r) => {
      const d = r.data;
      if (Array.isArray(d)) return d;
      if (d && typeof d === 'object' && Array.isArray((d as { pageItems?: LoanChargeSummary[] }).pageItems))
        return (d as { pageItems: LoanChargeSummary[] }).pageItems;
      return [];
    });
}

export function fetchLoanChargeTemplate(loanId: number): Promise<LoanChargeTemplate> {
  return apiClient
    .get<LoanChargeTemplate>(`/loans/${loanId}/charges/template`)
    .then((r) => r.data);
}

export function addLoanCharge(
  loanId: number,
  body: { chargeId: number; amount: number; dueDate?: string }
): Promise<{ resourceId: number }> {
  const payload = { ...body, locale: 'en', dateFormat: 'yyyy-MM-dd' };
  return apiClient
    .post<{ resourceId: number }>(`/loans/${loanId}/charges`, payload)
    .then((r) => r.data);
}

export function waiveLoanCharge(loanId: number, chargeId: number): Promise<{ resourceId: number }> {
  return apiClient
    .post<{ resourceId: number }>(`/loans/${loanId}/charges/${chargeId}`, {}, { params: { command: 'waive' } })
    .then((r) => r.data);
}

export function payLoanCharge(
  loanId: number,
  chargeId: number,
  body: { amount: number; transactionDate: string }
): Promise<{ resourceId: number }> {
  const payload = { ...body, locale: 'en', dateFormat: 'yyyy-MM-dd' };
  return apiClient
    .post<{ resourceId: number }>(`/loans/${loanId}/charges/${chargeId}`, payload, {
      params: { command: 'pay' },
    })
    .then((r) => r.data);
}
