import { apiClient } from './client';

export interface RoleSummary {
  id: number;
  name?: string;
  description?: string;
  [key: string]: unknown;
}

export interface RoleDetail extends RoleSummary {
  availablePermissions?: Array<{ id: number; name?: string; code?: string }>;
  selectedPermissions?: Array<{ id: number; name?: string; code?: string }>;
  [key: string]: unknown;
}

export function fetchRoles(): Promise<RoleSummary[]> {
  return apiClient
    .get<RoleSummary[]>('/roles')
    .then((r) => (Array.isArray(r.data) ? r.data : []));
}

export function fetchRole(id: number): Promise<RoleDetail> {
  return apiClient.get<RoleDetail>(`/roles/${id}`).then((r) => r.data);
}

export function createRole(body: {
  name: string;
  description?: string;
  permissions?: string[];
}): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/roles', body).then((r) => r.data);
}

export function updateRole(
  id: number,
  body: { name?: string; description?: string; permissions?: string[] }
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/roles/${id}`, body).then((r) => r.data);
}
