import { apiClient } from './client';

export interface ClientIdentifierSummary {
  id: number;
  clientId?: number;
  documentType?: { id: number; name?: string };
  documentKey?: string;
  description?: string;
  [key: string]: unknown;
}

export interface ClientIdentifierTemplate {
  allowedDocumentTypes?: Array<{ id: number; name?: string; value?: string }>;
  [key: string]: unknown;
}

export function fetchClientIdentifiers(clientId: number): Promise<ClientIdentifierSummary[]> {
  return apiClient
    .get<ClientIdentifierSummary[]>(`/clients/${clientId}/identifiers`)
    .then((r) => (Array.isArray(r.data) ? r.data : []));
}

export function fetchClientIdentifierTemplate(
  clientId: number
): Promise<ClientIdentifierTemplate> {
  return apiClient
    .get<ClientIdentifierTemplate>(`/clients/${clientId}/identifiers/template`)
    .then((r) => r.data);
}

export function addClientIdentifier(
  clientId: number,
  body: { documentTypeId: number; documentKey: string; description?: string }
): Promise<{ resourceId: number }> {
  return apiClient
    .post<{ resourceId: number }>(`/clients/${clientId}/identifiers`, body)
    .then((r) => r.data);
}

export function updateClientIdentifier(
  clientId: number,
  identifierId: number,
  body: { documentKey?: string; description?: string }
): Promise<{ resourceId: number }> {
  return apiClient
    .put<{ resourceId: number }>(`/clients/${clientId}/identifiers/${identifierId}`, body)
    .then((r) => r.data);
}

export function deleteClientIdentifier(
  clientId: number,
  identifierId: number
): Promise<void> {
  return apiClient
    .delete(`/clients/${clientId}/identifiers/${identifierId}`)
    .then(() => undefined);
}
