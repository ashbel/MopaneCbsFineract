import { apiClient } from './client';

export interface ClientDocumentSummary {
  id: number;
  parentEntityType?: string;
  parentEntityId?: number;
  name?: string;
  fileName?: string;
  size?: number;
  type?: string;
  description?: string;
  [key: string]: unknown;
}

const ENTITY_TYPE = 'clients';

export function fetchClientDocuments(clientId: number): Promise<ClientDocumentSummary[]> {
  return apiClient
    .get<ClientDocumentSummary[] | { pageItems?: ClientDocumentSummary[] }>(`/${ENTITY_TYPE}/${clientId}/documents`)
    .then((r) => {
      const d = r.data;
      if (Array.isArray(d)) return d;
      if (d && typeof d === 'object' && Array.isArray((d as { pageItems?: ClientDocumentSummary[] }).pageItems))
        return (d as { pageItems: ClientDocumentSummary[] }).pageItems;
      return [];
    });
}

export function uploadClientDocument(
  clientId: number,
  formData: FormData
): Promise<{ resourceId: number }> {
  return apiClient
    .post<{ resourceId: number }>(`/${ENTITY_TYPE}/${clientId}/documents`, formData)
    .then((r) => {
      const data = r.data as { resourceId?: number };
      return { resourceId: data?.resourceId ?? 0 };
    });
}

export function downloadClientDocument(
  clientId: number,
  documentId: number
): Promise<Blob> {
  return apiClient
    .get<Blob>(`/${ENTITY_TYPE}/${clientId}/documents/${documentId}/attachment`, {
      responseType: 'blob',
    })
    .then((r) => r.data);
}

export function deleteClientDocument(
  clientId: number,
  documentId: number
): Promise<void> {
  return apiClient
    .delete(`/${ENTITY_TYPE}/${clientId}/documents/${documentId}`)
    .then(() => undefined);
}
