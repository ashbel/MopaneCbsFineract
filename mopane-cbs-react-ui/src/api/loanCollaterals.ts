import { apiClient } from './client';

export interface LoanCollateralSummary {
  id: number;
  type?: { id: number; name?: string; value?: string };
  value?: number;
  description?: string;
  [key: string]: unknown;
}

export interface LoanCollateralTemplate {
  allowedCollateralTypes?: Array<{ id: number; name?: string; value?: string }>;
  [key: string]: unknown;
}

export function fetchLoanCollaterals(loanId: number): Promise<LoanCollateralSummary[]> {
  return apiClient
    .get<LoanCollateralSummary[] | { pageItems?: LoanCollateralSummary[] }>(`/loans/${loanId}/collaterals`)
    .then((r) => {
      const d = r.data;
      if (Array.isArray(d)) return d;
      if (d && typeof d === 'object' && Array.isArray((d as { pageItems?: LoanCollateralSummary[] }).pageItems))
        return (d as { pageItems: LoanCollateralSummary[] }).pageItems;
      return [];
    });
}

export function fetchLoanCollateralTemplate(loanId: number): Promise<LoanCollateralTemplate> {
  return apiClient
    .get<LoanCollateralTemplate>(`/loans/${loanId}/collaterals/template`)
    .then((r) => r.data);
}

export function addLoanCollateral(
  loanId: number,
  body: { type: number; value: number; description: string }
): Promise<{ resourceId: number }> {
  return apiClient
    .post<{ resourceId: number }>(`/loans/${loanId}/collaterals`, body)
    .then((r) => r.data);
}

export function deleteLoanCollateral(loanId: number, collateralId: number): Promise<void> {
  return apiClient
    .delete(`/loans/${loanId}/collaterals/${collateralId}`)
    .then(() => undefined);
}
