import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchOffices } from '../../api/offices';
import { downloadRbzFormMfi1 } from '../../api/rbzReports';
import './RunReport.css';

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function yearStartIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-01-01`;
}

export default function RbzFormMfi1() {
  const [startDate, setStartDate] = useState(yearStartIso());
  const [endDate, setEndDate] = useState(todayIso());
  const [officeId, setOfficeId] = useState('');
  const [downloading, setDownloading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { data: offices } = useQuery({
    queryKey: ['offices'],
    queryFn: () => fetchOffices(),
  });

  async function handleDownload() {
    setError(null);
    if (!startDate || !endDate) {
      setError('Start date and end date are required.');
      return;
    }
    setDownloading(true);
    try {
      const blob = await downloadRbzFormMfi1({
        startDate,
        endDate,
        officeId: officeId || undefined,
      });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `FORM_MFI1_${endDate.replace(/-/g, '')}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      setError('Failed to download FORM_MFI1. Check permissions and try again.');
    } finally {
      setDownloading(false);
    }
  }

  return (
    <div className="run-report-page">
      <div className="page-header">
        <Link to="/reports" className="back-link">
          ← Reports
        </Link>
        <h1>RBZ FORM_MFI1</h1>
      </div>

      <div className="params-card">
        <h2>Reporting period</h2>
        <p style={{ marginTop: 0, color: '#64748b', fontSize: '0.9rem' }}>
          Generates the Reserve Bank of Zimbabwe MFI return (FORM_MFI1) as Excel for the selected
          period. Portfolio schedules use loan and client data; institution, insider, debt and
          channel sheets use the rbz_* datatables when present.
        </p>
        <div className="params-grid">
          <label>
            <span>Start date</span>
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
            />
          </label>
          <label>
            <span>End date</span>
            <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
          </label>
          <label>
            <span>Office</span>
            <select value={officeId} onChange={(e) => setOfficeId(e.target.value)}>
              <option value="">Head office (default)</option>
              {(offices ?? []).map((o) => (
                <option key={o.id} value={o.id}>
                  {o.nameDecorated ?? o.name ?? o.id}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="actions">
        <button
          type="button"
          className="btn-primary"
          onClick={() => void handleDownload()}
          disabled={downloading}
        >
          {downloading ? 'Generating…' : 'Download FORM_MFI1.xlsx'}
        </button>
      </div>

      {error && (
        <div className="result-error" role="alert">
          {error}
        </div>
      )}
    </div>
  );
}
