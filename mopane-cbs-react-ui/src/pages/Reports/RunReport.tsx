import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchReports, runReport, runReportDownload } from '../../api/reports';
import './RunReport.css';

interface ReportResult {
  columnHeaders?: Array<{ columnName: string; columnType?: string; [key: string]: unknown }>;
  data?: Array<{ row: (string | number | null)[] }>;
  [key: string]: unknown;
}

export default function RunReport() {
  const { reportName: encodedName } = useParams<{ reportName: string }>();
  const reportName = encodedName ? decodeURIComponent(encodedName) : '';
  const [params, setParams] = useState<Record<string, string>>({});
  const [result, setResult] = useState<ReportResult | null>(null);

  const { data: reports } = useQuery({
    queryKey: ['reports'],
    queryFn: () => fetchReports(),
  });

  const report = reports?.find((r) => (r.reportName ?? '') === reportName);
  const paramDefs = report?.reportParameters ?? [];

  const runMutation = useMutation({
    mutationFn: () => runReport(reportName, params),
    onSuccess: (data) => setResult((data as ReportResult) ?? null),
    onError: () => setResult(null),
  });

  async function handleExport(format: 'csv' | 'pdf') {
    try {
      const blob = await runReportDownload(reportName, params, format);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${reportName.replace(/\s/g, '')}.${format}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      // ignore
    }
  }

  const columns = result?.columnHeaders ?? [];
  const rows = result?.data ?? [];
  const hasTable = columns.length > 0 && (rows.length > 0 || runMutation.isSuccess);

  if (!reportName) {
    return (
      <div className="page-error">
        Report not specified. <Link to="/reports">Back to reports</Link>
      </div>
    );
  }

  return (
    <div className="run-report-page">
      <div className="page-header">
        <Link to="/reports" className="back-link">← Reports</Link>
        <h1>Run: {reportName}</h1>
      </div>

      {paramDefs.length > 0 && (
        <div className="params-card">
          <h2>Parameters</h2>
          <div className="params-grid">
            {paramDefs.map((p: { parameterName?: string; parameterType?: string }) => (
              <label key={p.parameterName ?? ''}>
                <span>{p.parameterName ?? ''}</span>
                <input
                  type="text"
                  value={params[p.parameterName ?? ''] ?? ''}
                  onChange={(e) =>
                    setParams((prev) => ({
                      ...prev,
                      [p.parameterName ?? '']: e.target.value,
                    }))
                  }
                  placeholder={p.parameterType ?? 'Value'}
                />
              </label>
            ))}
          </div>
        </div>
      )}

      <div className="actions">
        <button
          type="button"
          className="btn-primary"
          onClick={() => runMutation.mutate()}
          disabled={runMutation.isPending}
        >
          {runMutation.isPending ? 'Running…' : 'Run report'}
        </button>
        {hasTable && (
          <>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => handleExport('csv')}
            >
              Download CSV
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={() => handleExport('pdf')}
            >
              Download PDF
            </button>
          </>
        )}
      </div>

      {runMutation.isError && (
        <div className="result-error" role="alert">
          Failed to run report. You may not have permission or parameters may be invalid.
        </div>
      )}

      {hasTable && (
        <div className="result-card">
          <h2>Result</h2>
          <div className="result-table-wrap">
            <table className="result-table">
              <thead>
                <tr>
                  {columns.map((c) => (
                    <th key={c.columnName ?? ''}>{c.columnName ?? ''}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((r, i) => (
                  <tr key={i}>
                    {(r.row ?? []).map((cell, j) => (
                      <td key={j}>{cell != null ? String(cell) : '—'}</td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
