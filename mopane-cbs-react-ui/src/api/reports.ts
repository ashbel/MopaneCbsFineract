import { apiClient } from './client';

export interface ReportSummary {
  id: number;
  reportName?: string;
  reportType?: string;
  reportSubType?: string;
  reportCategory?: string;
  description?: string;
  reportParameters?: Array<{ parameterName: string; parameterType: string; [key: string]: unknown }>;
  [key: string]: unknown;
}

/** Report list: GET /reports returns array of report metadata */
export function fetchReports(): Promise<ReportSummary[]> {
  return apiClient
    .get<ReportSummary[]>('/reports')
    .then((r) => (Array.isArray(r.data) ? r.data : []));
}

/** Build R_* query params for Fineract run report API */
function toReportParams(params: Record<string, string>): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [k, v] of Object.entries(params)) {
    if (v === '' || v == null) continue;
    out[`R_${k}`] = v;
  }
  return out;
}

/** Run report: GET /runreports/{reportName} with optional R_paramName=value query params. Returns JSON by default. */
export function runReport(
  reportName: string,
  params?: Record<string, string>,
  options?: { exportCsv?: boolean; exportPdf?: boolean }
): Promise<unknown> {
  const query: Record<string, string> = params ? toReportParams(params) : {};
  if (options?.exportCsv) query.exportCsv = 'true';
  if (options?.exportPdf) query.exportPdf = 'true';
  return apiClient
    .get<unknown>(`/runreports/${encodeURIComponent(reportName)}`, { params: query })
    .then((r) => r.data);
}

/** Run report and return blob for download (CSV/PDF). */
export function runReportDownload(
  reportName: string,
  params: Record<string, string>,
  format: 'csv' | 'pdf'
): Promise<Blob> {
  const query = params ? toReportParams(params) : {};
  if (format === 'csv') query.exportCsv = 'true';
  if (format === 'pdf') query.exportPdf = 'true';
  return apiClient
    .get<Blob>(`/runreports/${encodeURIComponent(reportName)}`, {
      params: query,
      responseType: 'blob',
    })
    .then((r) => r.data);
}
