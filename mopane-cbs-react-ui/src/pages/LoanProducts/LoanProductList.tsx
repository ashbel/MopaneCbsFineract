import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchLoanProducts, type LoanProductSummary } from '../../api/loanProducts';
import './LoanProductList.css';

export default function LoanProductList() {
  const { data: products, isLoading, error } = useQuery({
    queryKey: ['loanProducts'],
    queryFn: () => fetchLoanProducts(),
  });

  if (isLoading) return <div className="page-loading">Loading loan products…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load loan products. Please try again.
      </div>
    );
  }

  const items = products ?? [];

  return (
    <div className="loan-product-list-page">
      <div className="page-header">
        <h1>Loan products</h1>
        <Link to="/loanproducts/new" className="btn-primary">Add loan product</Link>
      </div>
      <div className="loan-product-table-wrap">
        <table className="loan-product-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Short name</th>
              <th>Description</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={5}>No loan products found.</td>
              </tr>
            ) : (
              items.map((p: LoanProductSummary) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.name ?? '—'}</td>
                  <td>{p.shortName ?? '—'}</td>
                  <td>{(p.description as string) ? String(p.description).slice(0, 60) + ((p.description as string).length > 60 ? '…' : '') : '—'}</td>
                  <td>
                    <Link to={`/loanproducts/${p.id}`} className="link-view">View</Link>
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
