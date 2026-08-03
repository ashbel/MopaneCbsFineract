import { apiClient } from './client';

export interface RbzFormMfi1Params {
  startDate: string;
  endDate: string;
  officeId?: number | string;
}

/** Download RBZ FORM_MFI1 Excel workbook. */
export function downloadRbzFormMfi1(params: RbzFormMfi1Params): Promise<Blob> {
  const query: Record<string, string> = {
    R_startDate: params.startDate,
    R_endDate: params.endDate,
  };
  if (params.officeId != null && params.officeId !== '') {
    query.R_officeId = String(params.officeId);
  }
  return apiClient
    .get<Blob>('/rbz/form-mfi1', {
      params: query,
      responseType: 'blob',
    })
    .then((r) => r.data);
}
