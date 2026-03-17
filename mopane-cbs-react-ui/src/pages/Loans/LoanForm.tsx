import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { fetchClients } from '../../api/clients';
import {
  fetchLoanTemplate,
  fetchLoanProducts,
  createLoan,
} from '../../api/loans';
import './LoanForm.css';

export default function LoanForm() {
  const [searchParams] = useSearchParams();
  const preselectedClientId = searchParams.get('clientId');
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [clientId, setClientId] = useState(preselectedClientId ?? '');
  const [productId, setProductId] = useState('');
  const [principal, setPrincipal] = useState('');
  const [numberOfRepayments, setNumberOfRepayments] = useState('');
  const [transactionDate, setTransactionDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [submitError, setSubmitError] = useState('');

  const { data: clientsData } = useQuery({
    queryKey: ['clients'],
    queryFn: () => fetchClients({ limit: 500 }),
  });

  const { data: products } = useQuery({
    queryKey: ['loanProducts'],
    queryFn: fetchLoanProducts,
  });

  const cId = clientId ? Number(clientId) : 0;
  const { data: template } = useQuery({
    queryKey: ['loanTemplate', cId, productId || undefined],
    queryFn: () => fetchLoanTemplate(cId, productId ? Number(productId) : undefined),
    enabled: Number.isFinite(cId) && cId > 0,
  });

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createLoan(body),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      navigate(`/loans/${data.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create loan. Please try again.'),
  });

  useEffect(() => {
    if (preselectedClientId && !clientId) setClientId(preselectedClientId);
  }, [preselectedClientId, clientId]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const c = Number(clientId);
    const p = productId ? Number(productId) : undefined;
    const prin = Number(principal);
    const numRep = Number(numberOfRepayments);
    if (!Number.isFinite(c) || c <= 0) {
      setSubmitError('Please select a client.');
      return;
    }
    if (!p && (!template?.productOptions?.length && !products?.length)) {
      setSubmitError('Please select a loan product.');
      return;
    }
    if (!Number.isFinite(prin) || prin <= 0) {
      setSubmitError('Please enter a valid principal.');
      return;
    }
    if (!Number.isFinite(numRep) || numRep <= 0) {
      setSubmitError('Please enter number of repayments.');
      return;
    }
    const product = p ?? template?.productOptions?.[0]?.id ?? products?.[0]?.id;
    if (!product) {
      setSubmitError('Please select a loan product.');
      return;
    }
    const dateParts = transactionDate.split('-').map(Number);
    createMutation.mutate({
      clientId: c,
      productId: product,
      principal,
      numberOfRepayments: numRep,
      transactionDate: dateParts,
      locale: 'en',
      dateFormat: 'yyyy-MM-dd',
    });
  }

  const productOptions = template?.productOptions?.length ? template.productOptions : (products ?? []).map((p) => ({ id: p.id, name: p.name }));
  const mutating = createMutation.isPending;

  return (
    <div className="loan-form-page">
      <div className="page-header">
        <h1>Add loan</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="loan-form">
        {submitError && <div className="form-error" role="alert">{submitError}</div>}
        <label>
          <span>Client *</span>
          <select
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            required
            disabled={mutating}
          >
            <option value="">Select client</option>
            {(clientsData?.pageItems ?? []).map((c) => (
              <option key={c.id} value={c.id}>
                {c.displayName ?? c.fullName ?? `Client ${c.id}`}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Loan product *</span>
          <select
            value={productId}
            onChange={(e) => setProductId(e.target.value)}
            required
            disabled={mutating || !clientId}
          >
            <option value="">Select product</option>
            {productOptions.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          <span>Principal *</span>
          <input
            type="number"
            step="any"
            min="0"
            value={principal}
            onChange={(e) => setPrincipal(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        <label>
          <span>Number of repayments *</span>
          <input
            type="number"
            min="1"
            value={numberOfRepayments}
            onChange={(e) => setNumberOfRepayments(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        <label>
          <span>Disbursement date *</span>
          <input
            type="date"
            value={transactionDate}
            onChange={(e) => setTransactionDate(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        <div className="form-actions">
          <button type="submit" disabled={mutating} className="btn-primary">
            {mutating ? 'Creating…' : 'Create loan'}
          </button>
        </div>
      </form>
    </div>
  );
}
