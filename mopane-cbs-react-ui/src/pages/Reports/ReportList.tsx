import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchReports, type ReportSummary } from '../../api/reports';
import './ReportList.css';

export default function ReportList() {
  const { data: reports, isLoading, error } = useQuery({
    queryKey: ['reports'],
    queryFn: () => fetchReports(),
  });

  if (isLoading) return <div className="page-loading">Loading reports…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load reports. Please try again.
      </div>
    );
  }

  const items = reports ?? [];

  return (
    <div className="report-list-page">
      <div className="page-header">
        <h1>Reports</h1>
      </div>
      <div className="report-table-wrap">
        <table className="report-table">
          <thead>
            <tr>
              <th>Report name</th>
              <th>Category</th>
              <th>Type</th>
              <th>Description</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={5}>No reports found.</td>
              </tr>
            ) : (
              items.map((r: ReportSummary) => (
                <tr key={r.id}>
                  <td>{r.reportName ?? '—'}</td>
                  <td>{r.reportCategory ?? '—'}</td>
                  <td>{r.reportType ?? '—'}</td>
                  <td>{r.description ?? '—'}</td>
                  <td>
                    <Link
                      to={`/reports/run/${encodeURIComponent((r.reportName as string) ?? '')}`}
                      className="link-view"
                    >
                      Run
                    </Link>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
