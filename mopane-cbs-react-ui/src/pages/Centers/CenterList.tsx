import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchCenters, type CenterSummary } from '../../api/centers';
import './CenterList.css';

export default function CenterList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['centers'],
    queryFn: () => fetchCenters({ limit: 50, paged: true }),
  });

  if (isLoading) return <div className="page-loading">Loading centers…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load centers. Please try again.
      </div>
    );
  }

  const items = data?.pageItems ?? [];

  return (
    <div className="center-list-page">
      <div className="page-header">
        <h1>Centers</h1>
        <Link to="/centers/new" className="btn-primary">Add center</Link>
      </div>
      <div className="center-table-wrap">
        <table className="center-table">
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
                <td colSpan={5}>No centers found.</td>
              </tr>
            ) : (
              items.map((c: CenterSummary) => (
                <tr key={c.id}>
                  <td>{c.id}</td>
                  <td>{c.name ?? '—'}</td>
                  <td>{c.officeName ?? '—'}</td>
                  <td>{c.status?.value ?? (c.active ? 'Active' : 'Inactive')}</td>
                  <td>
                    <Link to={`/centers/${c.id}`} className="link-view">View</Link>
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
