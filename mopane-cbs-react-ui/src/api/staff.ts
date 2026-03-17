import { apiClient } from './client';

export interface StaffSummary {
  id: number;
  firstname?: string;
  lastname?: string;
  displayName?: string;
  officeId?: number;
  officeName?: string;
  isLoanOfficer?: boolean;
  externalId?: string;
  isActive?: boolean;
  joiningDate?: number[];
  mobileNo?: string;
  [key: string]: unknown;
}

export interface StaffDetail extends StaffSummary {
  allowedOffices?: Array<{ id: number; name: string }>;
  [key: string]: unknown;
}

export function fetchStaff(params?: {
  officeId?: number;
  status?: string;
  loanOfficersOnly?: boolean;
}): Promise<StaffSummary[]> {
  return apiClient
    .get<StaffSummary[]>('/staff', { params: params ?? {} })
    .then((r) => (Array.isArray(r.data) ? r.data : []));
}

export function fetchStaffMember(
  id: number,
  options?: { template?: boolean }
): Promise<StaffDetail> {
  const params = options?.template ? { template: 'true' } : {};
  return apiClient.get<StaffDetail>(`/staff/${id}`, { params }).then((r) => r.data);
}

export function createStaff(body: Record<string, unknown>): Promise<{ resourceId: number }> {
  return apiClient.post<{ resourceId: number }>('/staff', body).then((r) => r.data);
}

export function updateStaff(
  id: number,
  body: Record<string, unknown>
): Promise<{ resourceId: number }> {
  return apiClient.put<{ resourceId: number }>(`/staff/${id}`, body).then((r) => r.data);
}
