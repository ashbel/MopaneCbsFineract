import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchLoans, type LoanSummary } from '../../api/loans';
import './LoanList.css';

export default function LoanList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['loans'],
    queryFn: () => fetchLoans({ limit: 50 }),
  });

  if (isLoading) return <div className="page-loading">Loading loans…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load loans. Please try again.
      </div>
    );
  }

  const items = data?.pageItems ?? [];

  return (
    <div className="loan-list-page">
      <div className="page-header">
        <h1>Loans</h1>
        <Link to="/loans/new" className="btn-primary">Add loan</Link>
      </div>
      <div className="loan-table-wrap">
        <table className="loan-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Account #</th>
              <th>Client</th>
              <th>Product</th>
              <th>Status</th>
              <th>Principal</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={7}>No loans found.</td>
              </tr>
            ) : (
              items.map((loan: LoanSummary) => (
                <tr key={loan.id}>
                  <td>{loan.id}</td>
                  <td>{loan.accountNo ?? '—'}</td>
                  <td>{loan.clientName ?? '—'}</td>
                  <td>{loan.loanProductName ?? '—'}</td>
                  <td>{loan.status?.value ?? '—'}</td>
                  <td>{loan.principal != null ? Number(loan.principal).toLocaleString() : '—'}</td>
                  <td>
                    <Link to={`/loans/${loan.id}`} className="link-view">View</Link>
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
