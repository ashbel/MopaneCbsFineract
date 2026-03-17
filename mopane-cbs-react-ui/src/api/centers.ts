import { apiClient } from './client';
import type { PageResponse } from './clients';

export interface CenterSummary {
  id: number;
  name?: string;
  officeId?: number;
  officeName?: string;
  status?: { id: number; code: string; value: string };
  active?: boolean;
  [key: string]: unknown;
}

export interface GroupGeneral {
  id: number;
  name?: string;
  [key: string]: unknown;
}

export interface CenterDetail extends CenterSummary {
  externalId?: string;
  submittedOnDate?: number[];
  activationDate?: number[];
  groupMembers?: GroupGeneral[];
  officeOptions?: Array<{ id: number; name: string }>;
  staffOptions?: Array<{ id: number; displayName?: string }>;
  [key: string]: unknown;
}

export interface CenterTemplate {
  officeOptions?: Array<{ id: number; name: string }>;
  staffOptions?: Array<{ id: number; displayName?: string }>;
  [key: string]: unknown;
}

export function fetchCenters(params?: {
  limit?: number;
  offset?: number;
  officeId?: number;
  sqlSearch?: string;
  paged?: boolean;
}): Promise<PageResponse<CenterSummary>> {
  return apiClient
    .get<PageResponse<CenterSummary>>('/centers', {
      params: { limit: 20, paged: true, ...params },
    })
    .then((r) => r.data);
}

export function fetchCenter(
  id: number,
  options?: { associations?: string; template?: boolean }
): Promise<CenterDetail> {
  const params: Record<string, string | boolean> = {};
  if (options?.associations) params.associations = options.associations;
  if (options?.template) params.template = true;
  return apiClient.get<CenterDetail>(`/centers/${id}`, { params }).then((r) => r.data);
}

export function fetchCenterTemplate(officeId?: number): Promise<CenterTemplate> {
  const params = officeId != null ? { officeId } : {};
  return apiClient.get<CenterTemplate>('/centers/template', { params }).then((r) => r.data);
}

export function createCenter(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/centers', body).then((r) => r.data);
}

export function updateCenter(
  id: number,
  body: Record<string, unknown>
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/centers/${id}`, body).then((r) => r.data);
}
