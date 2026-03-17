import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchSavingsAccounts, type SavingsSummary } from '../../api/savings';
import './SavingsList.css';

export default function SavingsList() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['savingsaccounts'],
    queryFn: () => fetchSavingsAccounts({ limit: 50 }),
  });

  if (isLoading) return <div className="page-loading">Loading savings accounts…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load savings accounts. Please try again.
      </div>
    );
  }

  const items = data?.pageItems ?? [];

  return (
    <div className="savings-list-page">
      <div className="page-header">
        <h1>Savings</h1>
        <Link to="/savings/new" className="btn-primary">Add savings account</Link>
      </div>
      <div className="savings-table-wrap">
        <table className="savings-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Account #</th>
              <th>Client</th>
              <th>Product</th>
              <th>Balance</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={7}>No savings accounts found.</td>
              </tr>
            ) : (
              items.map((s: SavingsSummary) => (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td>{s.accountNo ?? '—'}</td>
                  <td>{s.clientName ?? '—'}</td>
                  <td>{s.savingsProductName ?? '—'}</td>
                  <td>{s.accountBalance != null ? Number(s.accountBalance).toLocaleString() : '—'}</td>
                  <td>{s.status?.value ?? '—'}</td>
                  <td>
                    <Link to={`/savings/${s.id}`} className="link-view">View</Link>
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
