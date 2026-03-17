import { apiClient } from './client';

export interface ClientSummary {
  id: number;
  accountNo?: string;
  displayName?: string;
  fullName?: string;
  officeId?: number;
  officeName?: string;
  status?: { id: number; code: string; value: string };
  active?: boolean;
}

export interface ClientDetail extends ClientSummary {
  firstname?: string;
  lastname?: string;
  mobileNo?: string;
  externalId?: string;
  submittedOnDate?: number[];
  activationDate?: number[];
  dateOfBirth?: number[];
  gender?: { id: number; name: string };
  clientType?: { id: number; name: string };
  clientClassification?: { id: number; name: string };
  legalForm?: { id: number; name: string };
  [key: string]: unknown;
}

export interface ClientTemplate {
  officeOptions?: Array<{ id: number; name: string }>;
  staffOptions?: Array<{ id: number; displayName: string; officeId: number }>;
  clientClassificationOptions?: Array<{ id: number; name: string }>;
  clientTypeOptions?: Array<{ id: number; name: string }>;
  genderOptions?: Array<{ id: number; name: string }>;
  legalFormOptions?: Array<{ id: number; name: string }>;
  [key: string]: unknown;
}

export interface PageResponse<T> {
  pageItems: T[];
  totalFilteredRecords: number;
  totalRecords?: number;
}

export function fetchClients(params?: {
  limit?: number;
  offset?: number;
  orderBy?: string;
  sortOrder?: 'ASC' | 'DESC';
  sqlSearch?: string;
}): Promise<PageResponse<ClientSummary>> {
  return apiClient
    .get<PageResponse<ClientSummary>>('/clients', { params: { limit: 20, ...params } })
    .then((r) => r.data);
}

export function fetchClient(id: number): Promise<ClientDetail> {
  return apiClient.get<ClientDetail>(`/clients/${id}`).then((r) => r.data);
}

export function fetchClientTemplate(): Promise<ClientTemplate> {
  return apiClient.get<ClientTemplate>('/clients/template').then((r) => r.data);
}

export function createClient(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/clients', body).then((r) => r.data);
}

export function updateClient(
  id: number,
  body: Record<string, unknown>
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/clients/${id}`, body).then((r) => r.data);
}

export function transferClient(
  clientId: number,
  body: { destinationOfficeId: number }
): Promise<{ resourceId: number }> {
  const payload = { ...body, locale: 'en', dateFormat: 'yyyy-MM-dd' };
  return apiClient
    .post<{ resourceId: number }>(`/clients/${clientId}`, payload, {
      params: { command: 'proposeAndAcceptTransfer' },
    })
    .then((r) => r.data);
}
