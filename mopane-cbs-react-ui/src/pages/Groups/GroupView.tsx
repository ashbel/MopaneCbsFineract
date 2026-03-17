import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchGroup } from '../../api/groups';
import './GroupView.css';

export default function GroupView() {
  const { id } = useParams<{ id: string }>();
  const groupId = Number(id);

  const { data: group, isLoading, error } = useQuery({
    queryKey: ['group', groupId],
    queryFn: () => fetchGroup(groupId, 'clientMembers'),
    enabled: Number.isFinite(groupId),
  });

  if (!Number.isFinite(groupId)) {
    return (
      <div className="page-error">
        Invalid group ID. <Link to="/groups">Back to groups</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading group…</div>;
  if (error || !group) {
    return (
      <div className="page-error">
        Failed to load group. <Link to="/groups">Back to groups</Link>
      </div>
    );
  }

  const members = group.clientMembers ?? [];

  return (
    <div className="group-view-page">
      <div className="page-header">
        <div>
          <Link to="/groups" className="back-link">← Groups</Link>
          <h1>{group.name ?? `Group ${group.id}`}</h1>
        </div>
        <Link to={`/groups/${group.id}/edit`} className="btn-primary">Edit</Link>
      </div>
      <div className="group-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{group.id}</dd></div>
          <div><dt>Name</dt><dd>{group.name ?? '—'}</dd></div>
          <div><dt>Office</dt><dd>{group.officeName ?? '—'}</dd></div>
          <div><dt>Status</dt><dd>{group.status?.value ?? (group.active ? 'Active' : 'Inactive')}</dd></div>
          <div><dt>External ID</dt><dd>{group.externalId ?? '—'}</dd></div>
        </dl>
      </div>
      {members.length > 0 && (
        <div className="group-members-card">
          <h2>Members</h2>
          <ul className="members-list">
            {members.map((m) => (
              <li key={m.id}>
                <Link to={`/clients/${m.id}`}>{m.displayName ?? `Client ${m.id}`}</Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
