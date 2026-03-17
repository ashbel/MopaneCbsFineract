import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchClients, type ClientSummary } from '../../api/clients';
import './ClientList.css';

export default function ClientList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['clients'],
    queryFn: () => fetchClients({ limit: 50 }),
  });

  if (isLoading) return <div className="page-loading">Loading clients…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load clients. Please try again.
      </div>
    );
  }

  const items = data?.pageItems ?? [];

  return (
    <div className="client-list-page">
      <div className="page-header">
        <h1>Clients</h1>
        <Link to="/clients/new" className="btn-primary">Add client</Link>
      </div>
      <div className="client-table-wrap">
        <table className="client-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Account #</th>
              <th>Name</th>
              <th>Office</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={6}>No clients found.</td>
              </tr>
            ) : (
              items.map((c: ClientSummary) => (
                <tr key={c.id}>
                  <td>{c.id}</td>
                  <td>{c.accountNo ?? '—'}</td>
                  <td>{c.displayName ?? c.fullName ?? '—'}</td>
                  <td>{c.officeName ?? '—'}</td>
                  <td>{c.status?.value ?? (c.active ? 'Active' : 'Inactive')}</td>
                  <td>
                    <Link to={`/clients/${c.id}`} className="link-view">View</Link>
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
