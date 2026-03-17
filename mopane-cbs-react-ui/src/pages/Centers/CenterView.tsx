import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchCenter } from '../../api/centers';
import './CenterView.css';

function formatDate(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '—';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

export default function CenterView() {
  const { id } = useParams<{ id: string }>();
  const centerId = Number(id);

  const { data: center, isLoading, error } = useQuery({
    queryKey: ['center', centerId],
    queryFn: () => fetchCenter(centerId, { associations: 'groupMembers' }),
    enabled: Number.isFinite(centerId),
  });

  if (!Number.isFinite(centerId)) {
    return (
      <div className="page-error">
        Invalid center ID. <Link to="/centers">Back to centers</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading center…</div>;
  if (error || !center) {
    return (
      <div className="page-error">
        Failed to load center. <Link to="/centers">Back to centers</Link>
      </div>
    );
  }

  const groups = center.groupMembers ?? [];

  return (
    <div className="center-view-page">
      <div className="page-header">
        <div>
          <Link to="/centers" className="back-link">← Centers</Link>
          <h1>{center.name ?? `Center ${center.id}`}</h1>
        </div>
        <Link to={`/centers/${center.id}/edit`} className="btn-primary">Edit</Link>
      </div>
      <div className="center-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{center.id}</dd></div>
          <div><dt>Name</dt><dd>{center.name ?? '—'}</dd></div>
          <div><dt>Office</dt><dd>{center.officeName ?? '—'}</dd></div>
          <div><dt>Status</dt><dd>{center.status?.value ?? (center.active ? 'Active' : 'Inactive')}</dd></div>
          <div><dt>External ID</dt><dd>{center.externalId ?? '—'}</dd></div>
          <div><dt>Submitted on</dt><dd>{formatDate(center.submittedOnDate)}</dd></div>
        </dl>
      </div>
      {groups.length > 0 && (
        <div className="center-groups-card">
          <h2>Groups</h2>
          <ul className="groups-list">
            {groups.map((g) => (
              <li key={g.id}>
                <Link to={`/groups/${g.id}`}>{g.name ?? `Group ${g.id}`}</Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
