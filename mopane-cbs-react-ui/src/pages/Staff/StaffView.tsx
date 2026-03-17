import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchStaffMember } from '../../api/staff';
import './StaffView.css';

function formatDate(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '—';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

export default function StaffView() {
  const { id } = useParams<{ id: string }>();
  const staffId = Number(id);

  const { data: staff, isLoading, error } = useQuery({
    queryKey: ['staff', staffId],
    queryFn: () => fetchStaffMember(staffId),
    enabled: Number.isFinite(staffId),
  });

  if (!Number.isFinite(staffId)) {
    return (
      <div className="page-error">
        Invalid staff ID. <Link to="/staff">Back to staff</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading staff…</div>;
  if (error || !staff) {
    return (
      <div className="page-error">
        Failed to load staff. <Link to="/staff">Back to staff</Link>
      </div>
    );
  }

  const displayName = (staff.displayName ?? [staff.firstname, staff.lastname].filter(Boolean).join(' ')) || '—';

  return (
    <div className="staff-view-page">
      <div className="page-header">
        <div>
          <Link to="/staff" className="back-link">← Staff</Link>
          <h1>{displayName}</h1>
        </div>
        <Link to={`/staff/${staff.id}/edit`} className="btn-primary">Edit</Link>
      </div>
      <div className="staff-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{staff.id}</dd></div>
          <div><dt>First name</dt><dd>{staff.firstname ?? '—'}</dd></div>
          <div><dt>Last name</dt><dd>{staff.lastname ?? '—'}</dd></div>
          <div><dt>Office</dt><dd>{staff.officeName ?? '—'}</dd></div>
          <div><dt>Loan officer</dt><dd>{staff.isLoanOfficer ? 'Yes' : 'No'}</dd></div>
          <div><dt>Joining date</dt><dd>{formatDate(staff.joiningDate)}</dd></div>
          <div><dt>Mobile</dt><dd>{staff.mobileNo ?? '—'}</dd></div>
          <div><dt>External ID</dt><dd>{staff.externalId ?? '—'}</dd></div>
          <div><dt>Status</dt><dd>{staff.isActive !== false ? 'Active' : 'Inactive'}</dd></div>
        </dl>
      </div>
    </div>
  );
}
