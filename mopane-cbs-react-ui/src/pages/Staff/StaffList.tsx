import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchStaff, type StaffSummary } from '../../api/staff';
import './StaffList.css';

export default function StaffList() {
  const { data: staff, isLoading, error } = useQuery({
    queryKey: ['staff'],
    queryFn: () => fetchStaff(),
  });

  if (isLoading) return <div className="page-loading">Loading staff…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load staff. Please try again.
      </div>
    );
  }

  const items = staff ?? [];

  return (
    <div className="staff-list-page">
      <div className="page-header">
        <h1>Staff</h1>
        <Link to="/staff/new" className="btn-primary">Add staff</Link>
      </div>
      <div className="staff-table-wrap">
        <table className="staff-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Office</th>
              <th>Loan officer</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={6}>No staff found.</td>
              </tr>
            ) : (
              items.map((s: StaffSummary) => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td>{(s.displayName ?? [s.firstname, s.lastname].filter(Boolean).join(' ')) || '—'}</td>
                  <td>{s.officeName ?? '—'}</td>
                  <td>{s.isLoanOfficer ? 'Yes' : 'No'}</td>
                  <td>{s.isActive !== false ? 'Active' : 'Inactive'}</td>
                  <td>
                    <Link to={`/staff/${s.id}`} className="link-view">View</Link>
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
