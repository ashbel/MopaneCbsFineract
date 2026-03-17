import { apiClient } from './client';

export interface OfficeSummary {
  id: number;
  name?: string;
  nameDecorated?: string;
  externalId?: string;
  openingDate?: number[];
  parentId?: number;
  parentName?: string;
  [key: string]: unknown;
}

export interface OfficeTemplate {
  allowedParents?: Array<{ id: number; name: string }>;
  [key: string]: unknown;
}

export function fetchOffices(params?: {
  orderBy?: string;
  sortOrder?: 'ASC' | 'DESC';
}): Promise<OfficeSummary[]> {
  return apiClient
    .get<OfficeSummary[]>('/offices', { params })
    .then((r) => (Array.isArray(r.data) ? r.data : []));
}

export function fetchOffice(id: number): Promise<OfficeSummary> {
  return apiClient.get<OfficeSummary>(`/offices/${id}`).then((r) => r.data);
}

export function fetchOfficeTemplate(): Promise<OfficeTemplate> {
  return apiClient.get<OfficeTemplate>('/offices/template').then((r) => r.data);
}

export function createOffice(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/offices', body).then((r) => r.data);
}

export function updateOffice(
  id: number,
  body: Record<string, unknown>
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/offices/${id}`, body).then((r) => r.data);
}
