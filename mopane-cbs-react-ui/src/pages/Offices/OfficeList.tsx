import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchOffices, type OfficeSummary } from '../../api/offices';
import './OfficeList.css';

function formatDate(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '—';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

export default function OfficeList() {
  const { data: offices, isLoading, error } = useQuery({
    queryKey: ['offices'],
    queryFn: () => fetchOffices(),
  });

  if (isLoading) return <div className="page-loading">Loading offices…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load offices. Please try again.
      </div>
    );
  }

  const items = offices ?? [];

  return (
    <div className="office-list-page">
      <div className="page-header">
        <h1>Offices</h1>
        <Link to="/offices/new" className="btn-primary">Add office</Link>
      </div>
      <div className="office-table-wrap">
        <table className="office-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Parent</th>
              <th>Opening date</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={5}>No offices found.</td>
              </tr>
            ) : (
              items.map((o: OfficeSummary) => (
                <tr key={o.id}>
                  <td>{o.id}</td>
                  <td>{o.name ?? '—'}</td>
                  <td>{o.parentName ?? '—'}</td>
                  <td>{formatDate(o.openingDate)}</td>
                  <td>
                    <Link to={`/offices/${o.id}`} className="link-view">View</Link>
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
