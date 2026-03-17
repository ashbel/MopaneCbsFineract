import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchRole } from '../../api/roles';
import './RoleView.css';

export default function RoleView() {
  const { id } = useParams<{ id: string }>();
  const roleId = Number(id);

  const { data: role, isLoading, error } = useQuery({
    queryKey: ['role', roleId],
    queryFn: () => fetchRole(roleId),
    enabled: Number.isFinite(roleId),
  });

  if (!Number.isFinite(roleId)) {
    return (
      <div className="page-error">
        Invalid role ID. <Link to="/roles">Back to roles</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading role…</div>;
  if (error || !role) {
    return (
      <div className="page-error">
        Failed to load role. <Link to="/roles">Back to roles</Link>
      </div>
    );
  }

  const permissions = (role.selectedPermissions ?? []) as Array<{ id?: number; name?: string; code?: string }>;

  return (
    <div className="role-view-page">
      <div className="page-header">
        <div>
          <Link to="/roles" className="back-link">← Roles</Link>
          <h1>{role.name ?? `Role ${role.id}`}</h1>
        </div>
        <Link to={`/roles/${role.id}/edit`} className="btn-primary">Edit</Link>
      </div>
      <div className="role-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{role.id}</dd></div>
          <div><dt>Name</dt><dd>{role.name ?? '—'}</dd></div>
          <div><dt>Description</dt><dd>{role.description ?? '—'}</dd></div>
        </dl>
      </div>
      {permissions.length > 0 && (
        <div className="role-permissions-card">
          <h2>Permissions</h2>
          <ul className="permissions-list">
            {permissions.map((p, i) => (
              <li key={p.id ?? i}>{p.name ?? p.code ?? `Permission ${p.id ?? i}`}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
