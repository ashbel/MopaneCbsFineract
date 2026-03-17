import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { fetchClients } from '../../api/clients';
import {
  fetchSavingsTemplate,
  fetchSavingsProducts,
  createSavingsAccount,
} from '../../api/savings';
import './SavingsForm.css';

export default function SavingsForm() {
  const [searchParams] = useSearchParams();
  const preselectedClientId = searchParams.get('clientId');
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [clientId, setClientId] = useState(preselectedClientId ?? '');
  const [productId, setProductId] = useState('');
  const [submittedOnDate, setSubmittedOnDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [submitError, setSubmitError] = useState('');

  const { data: clientsData } = useQuery({
    queryKey: ['clients'],
    queryFn: () => fetchClients({ limit: 500 }),
  });

  const { data: products } = useQuery({
    queryKey: ['savingsProducts'],
    queryFn: fetchSavingsProducts,
  });

  const cId = clientId ? Number(clientId) : 0;
  const { data: template } = useQuery({
    queryKey: ['savingsTemplate', cId],
    queryFn: () => fetchSavingsTemplate(cId),
    enabled: Number.isFinite(cId) && cId > 0,
  });

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createSavingsAccount(body),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['savingsaccounts'] });
      navigate(`/savings/${data.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create savings account. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const c = Number(clientId);
    const p = productId ? Number(productId) : template?.productOptions?.[0]?.id ?? products?.[0]?.id;
    if (!Number.isFinite(c) || c <= 0) {
      setSubmitError('Please select a client.');
      return;
    }
    if (!p) {
      setSubmitError('Please select a savings product.');
      return;
    }
    const dateParts = submittedOnDate.split('-').map(Number);
    createMutation.mutate({
      clientId: c,
      productId: p,
      submittedOnDate: dateParts,
      locale: 'en',
      dateFormat: 'yyyy-MM-dd',
    });
  }

  const productOptions = template?.productOptions?.length ? template.productOptions : (products ?? []).map((p) => ({ id: p.id, name: p.name }));
  const mutating = createMutation.isPending;

  return (
    <div className="savings-form-page">
      <div className="page-header">
        <h1>Add savings account</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="savings-form">
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
          <span>Savings product *</span>
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
          <span>Submitted on date *</span>
          <input
            type="date"
            value={submittedOnDate}
            onChange={(e) => setSubmittedOnDate(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        <div className="form-actions">
          <button type="submit" disabled={mutating} className="btn-primary">
            {mutating ? 'Creating…' : 'Create savings account'}
          </button>
        </div>
      </form>
    </div>
  );
}
