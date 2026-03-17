import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { fetchSavingsProducts, type SavingsProductSummary } from '../../api/savingsProducts';
import './SavingsProductList.css';

export default function SavingsProductList() {
  const { data: products, isLoading, error } = useQuery({
    queryKey: ['savingsProducts'],
    queryFn: () => fetchSavingsProducts(),
  });

  if (isLoading) return <div className="page-loading">Loading savings products…</div>;
  if (error) {
    return (
      <div className="page-error" role="alert">
        Failed to load savings products. Please try again.
      </div>
    );
  }

  const items = products ?? [];

  return (
    <div className="savings-product-list-page">
      <div className="page-header">
        <h1>Savings products</h1>
        <Link to="/savingsproducts/new" className="btn-primary">Add savings product</Link>
      </div>
      <div className="savings-product-table-wrap">
        <table className="savings-product-table">
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
                <td colSpan={5}>No savings products found.</td>
              </tr>
            ) : (
              items.map((p: SavingsProductSummary) => (
                <tr key={p.id}>
                  <td>{p.id}</td>
                  <td>{p.name ?? '—'}</td>
                  <td>{p.shortName ?? '—'}</td>
                  <td>{(p.description as string) ? String(p.description).slice(0, 60) + ((p.description as string).length > 60 ? '…' : '') : '—'}</td>
                  <td>
                    <Link to={`/savingsproducts/${p.id}`} className="link-view">View</Link>
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
