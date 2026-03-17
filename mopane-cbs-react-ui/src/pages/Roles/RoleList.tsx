import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchRoles, type RoleSummary } from '../../api/roles';
import './RoleList.css';

export default function RoleList() {
  const { data: roles, isLoading, error } = useQuery({
    queryKey: ['roles'],
    queryFn: () => fetchRoles(),
  });

  if (isLoading) return <div className="page-loading">Loading roles…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load roles. Please try again.
      </div>
    );
  }

  const items = roles ?? [];

  return (
    <div className="role-list-page">
      <div className="page-header">
        <h1>Roles</h1>
        <Link to="/roles/new" className="btn-primary">Add role</Link>
      </div>
      <div className="role-table-wrap">
        <table className="role-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Description</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={4}>No roles found.</td>
              </tr>
            ) : (
              items.map((r: RoleSummary) => (
                <tr key={r.id}>
                  <td>{r.id}</td>
                  <td>{r.name ?? '—'}</td>
                  <td>{r.description ?? '—'}</td>
                  <td>
                    <Link to={`/roles/${r.id}`} className="link-view">View</Link>
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
