import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchLoan, loanRepayment, type LoanSchedulePeriod } from '../../api/loans';
import {
  fetchLoanCharges,
  fetchLoanChargeTemplate,
  addLoanCharge,
  waiveLoanCharge,
  type LoanChargeSummary,
} from '../../api/loanCharges';
import {
  fetchLoanCollaterals,
  fetchLoanCollateralTemplate,
  addLoanCollateral,
  deleteLoanCollateral,
  type LoanCollateralSummary,
} from '../../api/loanCollaterals';
import './LoanView.css';

function formatDate(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '—';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

function formatNum(n: number | undefined): string {
  if (n == null) return '—';
  return Number(n).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

export default function LoanView() {
  const { id } = useParams<{ id: string }>();
  const loanId = Number(id);
  const queryClient = useQueryClient();
  const [repayOpen, setRepayOpen] = useState(false);
  const [repayAmount, setRepayAmount] = useState('');
  const [repayDate, setRepayDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [repayError, setRepayError] = useState('');
  const [chargeModalOpen, setChargeModalOpen] = useState(false);
  const [chargeId, setChargeId] = useState('');
  const [chargeAmount, setChargeAmount] = useState('');
  const [chargeDueDate, setChargeDueDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [collateralModalOpen, setCollateralModalOpen] = useState(false);
  const [collateralTypeId, setCollateralTypeId] = useState('');
  const [collateralValue, setCollateralValue] = useState('');
  const [collateralDesc, setCollateralDesc] = useState('');
  const [sectionError, setSectionError] = useState('');

  const { data: loan, isLoading, error } = useQuery({
    queryKey: ['loan', loanId, 'repaymentSchedule'],
    queryFn: () => fetchLoan(loanId, { associations: 'repaymentSchedule' }),
    enabled: Number.isFinite(loanId),
  });

  const { data: charges } = useQuery({
    queryKey: ['loanCharges', loanId],
    queryFn: () => fetchLoanCharges(loanId),
    enabled: Number.isFinite(loanId),
  });

  const { data: chargeTemplate } = useQuery({
    queryKey: ['loanChargeTemplate', loanId],
    queryFn: () => fetchLoanChargeTemplate(loanId),
    enabled: Number.isFinite(loanId) && chargeModalOpen,
  });

  const { data: collaterals } = useQuery({
    queryKey: ['loanCollaterals', loanId],
    queryFn: () => fetchLoanCollaterals(loanId),
    enabled: Number.isFinite(loanId),
  });

  const { data: collateralTemplate } = useQuery({
    queryKey: ['loanCollateralTemplate', loanId],
    queryFn: () => fetchLoanCollateralTemplate(loanId),
    enabled: Number.isFinite(loanId) && collateralModalOpen,
  });

  const repayMutation = useMutation({
    mutationFn: (body: { transactionDate: string; transactionAmount: number; note?: string }) =>
      loanRepayment(loanId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loan', loanId] });
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      setRepayOpen(false);
      setRepayAmount('');
      setRepayError('');
    },
    onError: () => setRepayError('Repayment failed. Please try again.'),
  });

  const addChargeMutation = useMutation({
    mutationFn: (body: { chargeId: number; amount: number; dueDate?: string }) =>
      addLoanCharge(loanId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loanCharges', loanId] });
      setChargeModalOpen(false);
      setChargeId('');
      setChargeAmount('');
      setSectionError('');
    },
    onError: () => setSectionError('Failed to add charge.'),
  });

  const waiveChargeMutation = useMutation({
    mutationFn: (cId: number) => waiveLoanCharge(loanId, cId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loanCharges', loanId] });
      setSectionError('');
    },
    onError: () => setSectionError('Failed to waive charge.'),
  });

  const addCollateralMutation = useMutation({
    mutationFn: (body: { type: number; value: number; description: string }) =>
      addLoanCollateral(loanId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loanCollaterals', loanId] });
      setCollateralModalOpen(false);
      setCollateralTypeId('');
      setCollateralValue('');
      setCollateralDesc('');
      setSectionError('');
    },
    onError: () => setSectionError('Failed to add collateral.'),
  });

  const deleteCollateralMutation = useMutation({
    mutationFn: (cId: number) => deleteLoanCollateral(loanId, cId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loanCollaterals', loanId] });
      setSectionError('');
    },
    onError: () => setSectionError('Failed to delete collateral.'),
  });

  function handleRepaySubmit(e: React.FormEvent) {
    e.preventDefault();
    setRepayError('');
    const amount = Number(repayAmount);
    if (!Number.isFinite(amount) || amount <= 0) {
      setRepayError('Enter a valid amount.');
      return;
    }
    repayMutation.mutate({
      transactionDate: repayDate,
      transactionAmount: amount,
    });
  }

  if (!Number.isFinite(loanId)) {
    return (
      <div className="page-error">
        Invalid loan ID. <Link to="/loans">Back to loans</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading loan…</div>;
  if (error || !loan) {
    return (
      <div className="page-error">
        Failed to load loan. <Link to="/loans">Back to loans</Link>
      </div>
    );
  }

  return (
    <div className="loan-view-page">
      <div className="page-header">
        <div>
          <Link to="/loans" className="back-link">← Loans</Link>
          <h1>Loan {loan.accountNo ?? loan.id}</h1>
        </div>
        <div className="page-header-actions">
          {(loan.status?.code === 'submittedAndPendingApproval' || loan.status?.code === 'approved') && (
            <Link to={`/loans/${loan.id}/edit`} className="btn-secondary">Edit</Link>
          )}
          {loan.status?.code === 'active' && (
            <button type="button" className="btn-primary" onClick={() => setRepayOpen(true)}>
              Repay
            </button>
          )}
        </div>
      </div>
      <div className="loan-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{loan.id}</dd></div>
          <div><dt>Account #</dt><dd>{loan.accountNo ?? '—'}</dd></div>
          <div><dt>Client</dt><dd>{loan.clientName ?? '—'}</dd></div>
          <div><dt>Product</dt><dd>{loan.loanProductName ?? '—'}</dd></div>
          <div><dt>Status</dt><dd>{loan.status?.value ?? '—'}</dd></div>
          <div><dt>Principal</dt><dd>{loan.principal != null ? Number(loan.principal).toLocaleString() : '—'}</dd></div>
          <div><dt>Balance</dt><dd>{loan.loanBalance != null ? Number(loan.loanBalance).toLocaleString() : '—'}</dd></div>
          <div><dt>Currency</dt><dd>{loan.currency?.code ?? '—'}</dd></div>
        </dl>
      </div>

      {loan.repaymentSchedule?.periods && loan.repaymentSchedule.periods.length > 0 && (
        <div className="loan-schedule-card">
          <h2>Repayment schedule</h2>
          <div className="loan-schedule-table-wrap">
            <table className="loan-schedule-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Due date</th>
                  <th>Principal</th>
                  <th>Interest</th>
                  <th>Fees</th>
                  <th>Total due</th>
                  <th>Paid</th>
                  <th>Outstanding</th>
                </tr>
              </thead>
              <tbody>
                {(loan.repaymentSchedule.periods as LoanSchedulePeriod[]).map((p, i) => (
                  <tr key={p.period ?? i}>
                    <td>{p.period ?? '—'}</td>
                    <td>{formatDate(p.dueDate)}</td>
                    <td>{formatNum(p.principalDue)}</td>
                    <td>{formatNum(p.interestDue)}</td>
                    <td>{formatNum(p.feeChargesDue)}</td>
                    <td>{formatNum(p.totalDueForPeriod)}</td>
                    <td>{formatNum(p.totalPaidForPeriod)}</td>
                    <td>{formatNum(p.totalOutstandingForPeriod ?? p.principalOutstanding)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="loan-section-card">
        <h2>Charges</h2>
        {sectionError && <div className="form-error" role="alert">{sectionError}</div>}
        <div className="section-actions">
          <button type="button" className="btn-primary" onClick={() => { setChargeModalOpen(true); setSectionError(''); }}>
            Add charge
          </button>
        </div>
        <table className="loan-charges-table">
          <thead>
            <tr>
              <th>Charge</th>
              <th>Amount</th>
              <th>Outstanding</th>
              <th>Due date</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {(charges ?? []).length === 0 ? (
              <tr><td colSpan={5}>No charges.</td></tr>
            ) : (
              (charges ?? []).map((c: LoanChargeSummary) => (
                <tr key={c.id}>
                  <td>{c.name ?? '—'}</td>
                  <td>{c.amount != null ? formatNum(c.amount) : '—'}</td>
                  <td>{c.amountOutstanding != null ? formatNum(c.amountOutstanding) : '—'}</td>
                  <td>{formatDate(c.dueAsOfDate as number[])}</td>
                  <td>
                    {(c.amountOutstanding ?? 0) > 0 && (
                      <button type="button" className="link-button" onClick={() => waiveChargeMutation.mutate(c.id)} disabled={waiveChargeMutation.isPending}>Waive</button>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="loan-section-card">
        <h2>Collateral</h2>
        <div className="section-actions">
          <button type="button" className="btn-primary" onClick={() => { setCollateralModalOpen(true); setSectionError(''); }}>
            Add collateral
          </button>
        </div>
        <table className="loan-collaterals-table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Value</th>
              <th>Description</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {(collaterals ?? []).length === 0 ? (
              <tr><td colSpan={4}>No collateral.</td></tr>
            ) : (
              (collaterals ?? []).map((c: LoanCollateralSummary) => (
                <tr key={c.id}>
                  <td>{c.type?.name ?? c.type?.value ?? '—'}</td>
                  <td>{c.value != null ? formatNum(c.value) : '—'}</td>
                  <td>{c.description ?? '—'}</td>
                  <td>
                    <button type="button" className="link-button" onClick={() => window.confirm('Delete this collateral?') && deleteCollateralMutation.mutate(c.id)} disabled={deleteCollateralMutation.isPending}>Delete</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {chargeModalOpen && (
        <div className="modal-overlay" onClick={() => !addChargeMutation.isPending && setChargeModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Add charge</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setSectionError('');
              const cId = Number(chargeId);
              const amount = Number(chargeAmount);
              if (!Number.isFinite(cId) || cId <= 0) { setSectionError('Select a charge.'); return; }
              if (!Number.isFinite(amount) || amount <= 0) { setSectionError('Enter a valid amount.'); return; }
              addChargeMutation.mutate({ chargeId: cId, amount, dueDate: chargeDueDate });
            }}>
              <label>
                <span>Charge</span>
                <select value={chargeId} onChange={(e) => setChargeId(e.target.value)} required disabled={addChargeMutation.isPending}>
                  <option value="">Select charge</option>
                  {(chargeTemplate?.chargeOptions ?? []).map((o: { id: number; name?: string }) => (
                    <option key={o.id} value={o.id}>{o.name ?? `Charge ${o.id}`}</option>
                  ))}
                </select>
              </label>
              <label>
                <span>Amount</span>
                <input type="number" step="any" min="0" value={chargeAmount} onChange={(e) => setChargeAmount(e.target.value)} required disabled={addChargeMutation.isPending} />
              </label>
              <label>
                <span>Due date</span>
                <input type="date" value={chargeDueDate} onChange={(e) => setChargeDueDate(e.target.value)} disabled={addChargeMutation.isPending} />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setChargeModalOpen(false)} disabled={addChargeMutation.isPending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={addChargeMutation.isPending}>Add</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {collateralModalOpen && (
        <div className="modal-overlay" onClick={() => !addCollateralMutation.isPending && setCollateralModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Add collateral</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setSectionError('');
              const typeId = Number(collateralTypeId);
              const value = Number(collateralValue);
              const desc = collateralDesc.trim();
              if (!Number.isFinite(typeId) || typeId <= 0) { setSectionError('Select a type.'); return; }
              if (!Number.isFinite(value) || value <= 0) { setSectionError('Enter a valid value.'); return; }
              if (!desc) { setSectionError('Enter a description.'); return; }
              addCollateralMutation.mutate({ type: typeId, value, description: desc });
            }}>
              <label>
                <span>Type</span>
                <select value={collateralTypeId} onChange={(e) => setCollateralTypeId(e.target.value)} required disabled={addCollateralMutation.isPending}>
                  <option value="">Select type</option>
                  {(collateralTemplate?.allowedCollateralTypes ?? []).map((t: { id: number; name?: string; value?: string }) => (
                    <option key={t.id} value={t.id}>{t.name ?? t.value ?? `Type ${t.id}`}</option>
                  ))}
                </select>
              </label>
              <label>
                <span>Value</span>
                <input type="number" step="any" min="0" value={collateralValue} onChange={(e) => setCollateralValue(e.target.value)} required disabled={addCollateralMutation.isPending} />
              </label>
              <label>
                <span>Description</span>
                <input value={collateralDesc} onChange={(e) => setCollateralDesc(e.target.value)} required disabled={addCollateralMutation.isPending} maxLength={500} />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setCollateralModalOpen(false)} disabled={addCollateralMutation.isPending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={addCollateralMutation.isPending}>Add</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {repayOpen && (
        <div className="modal-overlay" onClick={() => !repayMutation.isPending && setRepayOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Make repayment</h3>
            <form onSubmit={handleRepaySubmit}>
              {repayError && <div className="form-error" role="alert">{repayError}</div>}
              <label>
                <span>Date</span>
                <input
                  type="date"
                  value={repayDate}
                  onChange={(e) => setRepayDate(e.target.value)}
                  required
                  disabled={repayMutation.isPending}
                />
              </label>
              <label>
                <span>Amount</span>
                <input
                  type="number"
                  step="any"
                  min="0"
                  value={repayAmount}
                  onChange={(e) => setRepayAmount(e.target.value)}
                  placeholder="0.00"
                  required
                  disabled={repayMutation.isPending}
                />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setRepayOpen(false)} disabled={repayMutation.isPending}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary" disabled={repayMutation.isPending}>
                  {repayMutation.isPending ? 'Submitting…' : 'Submit repayment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
