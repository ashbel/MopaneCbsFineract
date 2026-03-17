import { apiClient } from './client';

export interface RoleOption {
  id: number;
  name: string;
  [key: string]: unknown;
}

export interface UserSummary {
  id: number;
  username?: string;
  firstname?: string;
  lastname?: string;
  email?: string;
  officeId?: number;
  officeName?: string;
  selectedRoles?: RoleOption[];
  [key: string]: unknown;
}

export interface UserTemplate {
  allowedOffices?: Array<{ id: number; name: string }>;
  availableRoles?: RoleOption[];
  [key: string]: unknown;
}

export function fetchUsers(): Promise<UserSummary[]> {
  return apiClient
    .get<UserSummary[]>('/users')
    .then((r) => (Array.isArray(r.data) ? r.data : []));
}

export function fetchUser(id: number): Promise<UserSummary & UserTemplate> {
  return apiClient
    .get<UserSummary & UserTemplate>(`/users/${id}`, { params: { template: 'true' } })
    .then((r) => r.data);
}

export function fetchUserTemplate(): Promise<UserTemplate> {
  return apiClient.get<UserTemplate>('/users/template').then((r) => r.data);
}

export function createUser(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/users', body).then((r) => r.data);
}

export function updateUser(
  id: number,
  body: Record<string, unknown>
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/users/${id}`, body).then((r) => r.data);
}
