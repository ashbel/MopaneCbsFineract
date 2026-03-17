import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchOffice } from '../../api/offices';
import './OfficeView.css';

function formatDate(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '—';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

export default function OfficeView() {
  const { id } = useParams<{ id: string }>();
  const officeId = Number(id);

  const { data: office, isLoading, error } = useQuery({
    queryKey: ['office', officeId],
    queryFn: () => fetchOffice(officeId),
    enabled: Number.isFinite(officeId),
  });

  if (!Number.isFinite(officeId)) {
    return (
      <div className="page-error">
        Invalid office ID. <Link to="/offices">Back to offices</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading office…</div>;
  if (error || !office) {
    return (
      <div className="page-error">
        Failed to load office. <Link to="/offices">Back to offices</Link>
      </div>
    );
  }

  return (
    <div className="office-view-page">
      <div className="page-header">
        <div>
          <Link to="/offices" className="back-link">← Offices</Link>
          <h1>{office.name ?? `Office ${office.id}`}</h1>
        </div>
        <Link to={`/offices/${office.id}/edit`} className="btn-primary">Edit</Link>
      </div>
      <div className="office-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{office.id}</dd></div>
          <div><dt>Name</dt><dd>{office.name ?? '—'}</dd></div>
          <div><dt>Parent</dt><dd>{office.parentName ?? '—'}</dd></div>
          <div><dt>Opening date</dt><dd>{formatDate(office.openingDate)}</dd></div>
          <div><dt>External ID</dt><dd>{office.externalId ?? '—'}</dd></div>
        </dl>
      </div>
    </div>
  );
}
