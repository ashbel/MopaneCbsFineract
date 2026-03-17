import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchUser } from '../../api/users';
import './UserView.css';

function rolesLabel(selectedRoles: { name: string }[] | undefined): string {
  if (!selectedRoles?.length) return '—';
  return selectedRoles.map((r) => r.name).join(', ');
}

export default function UserView() {
  const { id } = useParams<{ id: string }>();
  const userId = Number(id);

  const { data: user, isLoading, error } = useQuery({
    queryKey: ['user', userId],
    queryFn: () => fetchUser(userId),
    enabled: Number.isFinite(userId),
  });

  if (!Number.isFinite(userId)) {
    return (
      <div className="page-error">
        Invalid user ID. <Link to="/users">Back to users</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading user…</div>;
  if (error || !user) {
    return (
      <div className="page-error">
        Failed to load user. <Link to="/users">Back to users</Link>
      </div>
    );
  }

  return (
    <div className="user-view-page">
      <div className="page-header">
        <div>
          <Link to="/users" className="back-link">← Users</Link>
          <h1>{user.username ?? `User ${user.id}`}</h1>
        </div>
        <Link to={`/users/${user.id}/edit`} className="btn-primary">Edit</Link>
      </div>
      <div className="user-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{user.id}</dd></div>
          <div><dt>Username</dt><dd>{user.username ?? '—'}</dd></div>
          <div><dt>First name</dt><dd>{user.firstname ?? '—'}</dd></div>
          <div><dt>Last name</dt><dd>{user.lastname ?? '—'}</dd></div>
          <div><dt>Email</dt><dd>{user.email ?? '—'}</dd></div>
          <div><dt>Office</dt><dd>{user.officeName ?? '—'}</dd></div>
          <div><dt>Roles</dt><dd>{rolesLabel(user.selectedRoles)}</dd></div>
        </dl>
      </div>
    </div>
  );
}
