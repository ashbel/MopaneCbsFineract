import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchSavingsProduct } from '../../api/savingsProducts';
import './SavingsProductView.css';

export default function SavingsProductView() {
  const { id } = useParams<{ id: string }>();
  const productId = Number(id);

  const { data: product, isLoading, error } = useQuery({
    queryKey: ['savingsProduct', productId],
    queryFn: () => fetchSavingsProduct(productId),
    enabled: Number.isFinite(productId),
  });

  if (!Number.isFinite(productId)) {
    return (
      <div className="page-error">
        Invalid product ID. <Link to="/savingsproducts">Back to savings products</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading savings product…</div>;
  if (error || !product) {
    return (
      <div className="page-error">
        Failed to load savings product. <Link to="/savingsproducts">Back to savings products</Link>
      </div>
    );
  }

  return (
    <div className="savings-product-view-page">
      <div className="page-header">
        <div>
          <Link to="/savingsproducts" className="back-link">← Savings products</Link>
          <h1>{product.name ?? `Savings product ${product.id}`}</h1>
        </div>
        <Link to={`/savingsproducts/${product.id}/edit`} className="btn-primary">Edit</Link>
      </div>
      <div className="savings-product-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{product.id}</dd></div>
          <div><dt>Name</dt><dd>{product.name ?? '—'}</dd></div>
          <div><dt>Short name</dt><dd>{product.shortName ?? '—'}</dd></div>
          <div><dt>Description</dt><dd>{product.description ?? '—'}</dd></div>
          <div><dt>Currency</dt><dd>{product.currency?.code ?? '—'}</dd></div>
          <div><dt>Nominal annual interest rate</dt><dd>{product.nominalAnnualInterestRate != null ? Number(product.nominalAnnualInterestRate) + '%' : '—'}</dd></div>
          <div><dt>Interest compounding</dt><dd>{product.interestCompoundingPeriodType?.value ?? '—'}</dd></div>
          <div><dt>Interest posting</dt><dd>{product.interestPostingPeriodType?.value ?? '—'}</dd></div>
          <div><dt>Interest calculation</dt><dd>{product.interestCalculationType?.value ?? '—'}</dd></div>
          <div><dt>Min opening balance</dt><dd>{product.minRequiredOpeningBalance != null ? Number(product.minRequiredOpeningBalance).toLocaleString() : '—'}</dd></div>
        </dl>
      </div>
    </div>
  );
}
