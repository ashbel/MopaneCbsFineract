import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { fetchLoan, fetchLoanTemplate, updateLoan } from '../../api/loans';
import { fetchStaff } from '../../api/staff';
import './LoanForm.css';

export default function LoanEdit() {
  const { id } = useParams<{ id: string }>();
  const loanId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [loanOfficerId, setLoanOfficerId] = useState('');
  const [submitError, setSubmitError] = useState('');

  const { data: loan, isLoading: loanLoading, error: loanError } = useQuery({
    queryKey: ['loan', loanId],
    queryFn: () => fetchLoan(loanId),
    enabled: Number.isFinite(loanId),
  });

  const clientId = loan?.clientId ?? 0;
  const productId = loan?.loanProductId;
  const { data: template } = useQuery({
    queryKey: ['loanTemplate', clientId, productId],
    queryFn: () => fetchLoanTemplate(clientId, productId),
    enabled: Number.isFinite(clientId) && clientId > 0,
  });

  const { data: staffList } = useQuery({
    queryKey: ['staff', { loanOfficersOnly: true }],
    queryFn: () => fetchStaff({ loanOfficersOnly: true }),
  });

  useEffect(() => {
    if (loan != null) {
      const officerId = (loan as { loanOfficerId?: number }).loanOfficerId;
      setLoanOfficerId(officerId != null ? String(officerId) : '');
    }
  }, [loan]);

  const updateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => updateLoan(loanId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loan', loanId] });
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      navigate(`/loans/${loanId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update loan. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const officerId = loanOfficerId ? Number(loanOfficerId) : undefined;
    const body: Record<string, unknown> = {
      locale: 'en',
      dateFormat: 'yyyy-MM-dd',
    };
    if (officerId != null && Number.isFinite(officerId)) body.loanOfficerId = officerId;
    updateMutation.mutate(body);
  }

  if (!Number.isFinite(loanId)) {
    return (
      <div className="page-error">
        Invalid loan ID. <Link to="/loans">Back to loans</Link>
      </div>
    );
  }

  if (loanLoading) return <div className="page-loading">Loading loan…</div>;
  if (loanError || !loan) {
    return (
      <div className="page-error">
        Failed to load loan. <Link to="/loans">Back to loans</Link>
      </div>
    );
  }

  const loanOfficerOptions = template?.loanOfficerOptions?.length
    ? template.loanOfficerOptions
    : (staffList ?? []).map((s) => {
        const label = s.displayName ?? `${s.firstname ?? ''} ${s.lastname ?? ''}`.trim();
        return { id: s.id, displayName: label ? label : `Staff ${s.id}` };
      });

  return (
    <div className="loan-form-page">
      <div className="page-header">
        <h1>Edit loan {loan.accountNo ?? loan.id}</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="loan-form">
        {submitError && <div className="form-error" role="alert">{submitError}</div>}
        <label>
          <span>Loan officer</span>
          <select
            value={loanOfficerId}
            onChange={(e) => setLoanOfficerId(e.target.value)}
            disabled={updateMutation.isPending}
          >
            <option value="">— Select —</option>
            {loanOfficerOptions.map((o: { id: number; displayName?: string; name?: string }) => (
              <option key={o.id} value={o.id}>
                {o.displayName ?? o.name ?? `Staff ${o.id}`}
              </option>
            ))}
          </select>
        </label>
        <div className="form-actions">
          <button type="submit" disabled={updateMutation.isPending} className="btn-primary">
            {updateMutation.isPending ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </form>
    </div>
  );
}
