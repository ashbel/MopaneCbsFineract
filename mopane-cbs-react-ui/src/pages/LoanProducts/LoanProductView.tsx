import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchLoanProduct } from '../../api/loanProducts';
import './LoanProductView.css';

export default function LoanProductView() {
  const { id } = useParams<{ id: string }>();
  const productId = Number(id);

  const { data: product, isLoading, error } = useQuery({
    queryKey: ['loanProduct', productId],
    queryFn: () => fetchLoanProduct(productId),
    enabled: Number.isFinite(productId),
  });

  if (!Number.isFinite(productId)) {
    return (
      <div className="page-error">
        Invalid product ID. <Link to="/loanproducts">Back to loan products</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading loan product…</div>;
  if (error || !product) {
    return (
      <div className="page-error">
        Failed to load loan product. <Link to="/loanproducts">Back to loan products</Link>
      </div>
    );
  }

  return (
    <div className="loan-product-view-page">
      <div className="page-header">
        <div>
          <Link to="/loanproducts" className="back-link">← Loan products</Link>
          <h1>{product.name ?? `Loan product ${product.id}`}</h1>
        </div>
        <Link to={`/loanproducts/${product.id}/edit`} className="btn-primary">Edit</Link>
      </div>
      <div className="loan-product-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{product.id}</dd></div>
          <div><dt>Name</dt><dd>{product.name ?? '—'}</dd></div>
          <div><dt>Short name</dt><dd>{product.shortName ?? '—'}</dd></div>
          <div><dt>Description</dt><dd>{product.description ?? '—'}</dd></div>
          <div><dt>Fund</dt><dd>{product.fundName ?? '—'}</dd></div>
          <div><dt>Currency</dt><dd>{product.currency?.code ?? '—'}</dd></div>
          <div><dt>Principal</dt><dd>{product.principal != null ? Number(product.principal).toLocaleString() : '—'}</dd></div>
          <div><dt>Number of repayments</dt><dd>{product.numberOfRepayments ?? '—'}</dd></div>
          <div><dt>Repayment every</dt><dd>{product.repaymentEvery ?? '—'}</dd></div>
          <div><dt>Repayment frequency</dt><dd>{product.repaymentFrequencyType?.value ?? '—'}</dd></div>
          <div><dt>Interest rate per period</dt><dd>{product.interestRatePerPeriod != null ? Number(product.interestRatePerPeriod) + '%' : '—'}</dd></div>
          <div><dt>Amortization type</dt><dd>{product.amortizationType?.value ?? '—'}</dd></div>
          <div><dt>Interest type</dt><dd>{product.interestType?.value ?? '—'}</dd></div>
          <div><dt>Transaction processing</dt><dd>{product.transactionProcessingStrategyName ?? '—'}</dd></div>
        </dl>
      </div>
    </div>
  );
}
