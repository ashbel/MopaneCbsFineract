import { apiClient } from './client';
import type { PageResponse } from './clients';

export interface GroupSummary {
  id: number;
  name?: string;
  officeId?: number;
  officeName?: string;
  status?: { id: number; code: string; value: string };
  active?: boolean;
  [key: string]: unknown;
}

export interface GroupDetail extends GroupSummary {
  externalId?: string;
  submittedOnDate?: number[];
  activationDate?: number[];
  clientMembers?: Array<{ id: number; displayName?: string }>;
  [key: string]: unknown;
}

export interface GroupTemplate {
  officeOptions?: Array<{ id: number; name: string }>;
  [key: string]: unknown;
}

export function fetchGroups(params?: {
  limit?: number;
  offset?: number;
  officeId?: number;
}): Promise<PageResponse<GroupSummary>> {
  return apiClient
    .get<PageResponse<GroupSummary>>('/groups', { params: { limit: 20, ...params } })
    .then((r) => r.data);
}

export function fetchGroup(id: number, associations?: string): Promise<GroupDetail> {
  return apiClient
    .get<GroupDetail>(`/groups/${id}`, { params: associations ? { associations } : {} })
    .then((r) => r.data);
}

export function fetchGroupTemplate(officeId?: number): Promise<GroupTemplate> {
  const params = officeId != null ? { officeId } : {};
  return apiClient.get<GroupTemplate>('/groups/template', { params }).then((r) => r.data);
}

export function createGroup(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/groups', body).then((r) => r.data);
}

export function updateGroup(
  id: number,
  body: Record<string, unknown>
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/groups/${id}`, body).then((r) => r.data);
}
