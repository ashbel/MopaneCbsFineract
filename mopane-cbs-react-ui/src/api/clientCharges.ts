import { apiClient } from './client';
import type { PageResponse } from './clients';

export interface ClientChargeSummary {
  id: number;
  clientId?: number;
  chargeId?: number;
  name?: string;
  amount?: number;
  amountPaid?: number;
  amountWaived?: number;
  amountOutstanding?: number;
  dueDate?: number[];
  chargeTimeType?: { id: number; code?: string; value?: string };
  [key: string]: unknown;
}

export interface ClientChargeTemplate {
  chargeOptions?: Array<{ id: number; name: string; amount?: number }>;
  [key: string]: unknown;
}

export function fetchClientCharges(
  clientId: number,
  params?: { status?: string; limit?: number; offset?: number }
): Promise<PageResponse<ClientChargeSummary>> {
  return apiClient
    .get<PageResponse<ClientChargeSummary>>(`/clients/${clientId}/charges`, {
      params: { limit: 50, ...params },
    })
    .then((r) => r.data);
}

export function fetchClientChargeTemplate(clientId: number): Promise<ClientChargeTemplate> {
  return apiClient
    .get<ClientChargeTemplate>(`/clients/${clientId}/charges/template`)
    .then((r) => r.data);
}

export function addClientCharge(
  clientId: number,
  body: { chargeId: number; amount: number; dueDate?: string }
): Promise<{ resourceId: number }> {
  const payload = { ...body, locale: 'en', dateFormat: 'yyyy-MM-dd' };
  return apiClient
    .post<{ resourceId: number }>(`/clients/${clientId}/charges`, payload)
    .then((r) => r.data);
}

export function waiveClientCharge(
  clientId: number,
  chargeId: number
): Promise<{ resourceId: number }> {
  return apiClient
    .post<{ resourceId: number }>(`/clients/${clientId}/charges/${chargeId}`, {}, { params: { command: 'waive' } })
    .then((r) => r.data);
}

export function payClientCharge(
  clientId: number,
  chargeId: number,
  body: { amount: number; paymentDate: string }
): Promise<{ resourceId: number }> {
  const payload = { ...body, locale: 'en', dateFormat: 'yyyy-MM-dd' };
  return apiClient
    .post<{ resourceId: number }>(`/clients/${clientId}/charges/${chargeId}`, payload, {
      params: { command: 'paycharge' },
    })
    .then((r) => r.data);
}
