import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchGroups, type GroupSummary } from '../../api/groups';
import './GroupList.css';

export default function GroupList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['groups'],
    queryFn: () => fetchGroups({ limit: 50 }),
  });

  if (isLoading) return <div className="page-loading">Loading groups…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load groups. Please try again.
      </div>
    );
  }

  const items = data?.pageItems ?? [];

  return (
    <div className="group-list-page">
      <div className="page-header">
        <h1>Groups</h1>
        <Link to="/groups/new" className="btn-primary">Add group</Link>
      </div>
      <div className="group-table-wrap">
        <table className="group-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Office</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={5}>No groups found.</td>
              </tr>
            ) : (
              items.map((g: GroupSummary) => (
                <tr key={g.id}>
                  <td>{g.id}</td>
                  <td>{g.name ?? '—'}</td>
                  <td>{g.officeName ?? '—'}</td>
                  <td>{g.status?.value ?? (g.active ? 'Active' : 'Inactive')}</td>
                  <td>
                    <Link to={`/groups/${g.id}`} className="link-view">View</Link>
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
