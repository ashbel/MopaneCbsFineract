import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchUsers, type UserSummary } from '../../api/users';
import './UserList.css';

function rolesLabel(selectedRoles: { name: string }[] | undefined): string {
  if (!selectedRoles?.length) return '—';
  return selectedRoles.map((r) => r.name).join(', ');
}

export default function UserList() {
  const { data: users, isLoading, error } = useQuery({
    queryKey: ['users'],
    queryFn: () => fetchUsers(),
  });

  if (isLoading) return <div className="page-loading">Loading users…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load users. Please try again.
      </div>
    );
  }

  const items = users ?? [];

  return (
    <div className="user-list-page">
      <div className="page-header">
        <h1>Users</h1>
        <Link to="/users/new" className="btn-primary">Add user</Link>
      </div>
      <div className="user-table-wrap">
        <table className="user-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Username</th>
              <th>Name</th>
              <th>Email</th>
              <th>Office</th>
              <th>Roles</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={7}>No users found.</td>
              </tr>
            ) : (
              items.map((u: UserSummary) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.username ?? '—'}</td>
                  <td>{[u.firstname, u.lastname].filter(Boolean).join(' ') || '—'}</td>
                  <td>{u.email ?? '—'}</td>
                  <td>{u.officeName ?? '—'}</td>
                  <td>{rolesLabel(u.selectedRoles)}</td>
                  <td>
                    <Link to={`/users/${u.id}`} className="link-view">View</Link>
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
