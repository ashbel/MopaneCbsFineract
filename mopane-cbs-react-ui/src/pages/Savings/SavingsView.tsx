import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import {
  fetchSavingsAccount,
  fetchSavingsTransactions,
  savingsDeposit,
  savingsWithdrawal,
  savingsHoldAmount,
} from '../../api/savings';
import './SavingsView.css';

function formatDate(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '—';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

export default function SavingsView() {
  const { id } = useParams<{ id: string }>();
  const savingsId = Number(id);
  const queryClient = useQueryClient();
  const [depositOpen, setDepositOpen] = useState(false);
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [depositAmount, setDepositAmount] = useState('');
  const [depositDate, setDepositDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [withdrawAmount, setWithdrawAmount] = useState('');
  const [withdrawDate, setWithdrawDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [holdOpen, setHoldOpen] = useState(false);
  const [holdAmount, setHoldAmount] = useState('');
  const [holdDate, setHoldDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [actionError, setActionError] = useState('');

  const { data: account, isLoading, error } = useQuery({
    queryKey: ['savingsaccount', savingsId],
    queryFn: () => fetchSavingsAccount(savingsId),
    enabled: Number.isFinite(savingsId),
  });

  const { data: transactions } = useQuery({
    queryKey: ['savingsTransactions', savingsId],
    queryFn: () => fetchSavingsTransactions(savingsId),
    enabled: Number.isFinite(savingsId) && !!account,
  });

  const depositMutation = useMutation({
    mutationFn: (body: { transactionDate: string; transactionAmount: number }) =>
      savingsDeposit(savingsId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['savingsaccount', savingsId] });
      queryClient.invalidateQueries({ queryKey: ['savingsTransactions', savingsId] });
      setDepositOpen(false);
      setDepositAmount('');
      setActionError('');
    },
    onError: () => setActionError('Deposit failed. Please try again.'),
  });

  const withdrawMutation = useMutation({
    mutationFn: (body: { transactionDate: string; transactionAmount: number }) =>
      savingsWithdrawal(savingsId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['savingsaccount', savingsId] });
      queryClient.invalidateQueries({ queryKey: ['savingsTransactions', savingsId] });
      setWithdrawOpen(false);
      setWithdrawAmount('');
      setActionError('');
    },
    onError: () => setActionError('Withdrawal failed. Please try again.'),
  });

  const holdMutation = useMutation({
    mutationFn: (body: { transactionDate: string; transactionAmount: number }) =>
      savingsHoldAmount(savingsId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['savingsaccount', savingsId] });
      queryClient.invalidateQueries({ queryKey: ['savingsTransactions', savingsId] });
      setHoldOpen(false);
      setHoldAmount('');
      setActionError('');
    },
    onError: () => setActionError('Hold amount failed. Please try again.'),
  });

  if (!Number.isFinite(savingsId)) {
    return (
      <div className="page-error">
        Invalid account ID. <Link to="/savings">Back to savings</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading account…</div>;
  if (error || !account) {
    return (
      <div className="page-error">
        Failed to load account. <Link to="/savings">Back to savings</Link>
      </div>
    );
  }

  const balance = account.summary?.accountBalance ?? account.accountBalance;

  const canTransact = account?.status?.code === 'active';
  const pending = depositMutation.isPending || withdrawMutation.isPending || holdMutation.isPending;

  return (
    <div className="savings-view-page">
      <div className="page-header">
        <div>
          <Link to="/savings" className="back-link">← Savings</Link>
          <h1>Savings {account.accountNo ?? account.id}</h1>
        </div>
        {canTransact && (
          <div className="page-header-actions">
            <button type="button" className="btn-primary" onClick={() => { setWithdrawOpen(false); setHoldOpen(false); setActionError(''); setDepositOpen(true); }}>
              Deposit
            </button>
            <button type="button" className="btn-secondary" onClick={() => { setDepositOpen(false); setHoldOpen(false); setActionError(''); setWithdrawOpen(true); }}>
              Withdraw
            </button>
            <button type="button" className="btn-secondary" onClick={() => { setDepositOpen(false); setWithdrawOpen(false); setActionError(''); setHoldOpen(true); }}>
              Hold amount
            </button>
          </div>
        )}
      </div>
      <div className="savings-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{account.id}</dd></div>
          <div><dt>Account #</dt><dd>{account.accountNo ?? '—'}</dd></div>
          <div><dt>Client</dt><dd>{account.clientName ?? '—'}</dd></div>
          <div><dt>Product</dt><dd>{account.savingsProductName ?? '—'}</dd></div>
          <div><dt>Status</dt><dd>{account.status?.value ?? '—'}</dd></div>
          <div><dt>Balance</dt><dd>{balance != null ? Number(balance).toLocaleString() : '—'}</dd></div>
          <div><dt>Currency</dt><dd>{account.currency?.code ?? '—'}</dd></div>
        </dl>
      </div>
      <div className="savings-transactions-card">
        <h2>Transactions</h2>
        <table className="transactions-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Type</th>
              <th>Amount</th>
            </tr>
          </thead>
          <tbody>
            {(transactions ?? []).length === 0 ? (
              <tr>
                <td colSpan={3}>No transactions.</td>
              </tr>
            ) : (
              (transactions ?? []).map((t) => (
                <tr key={t.id}>
                  <td>{formatDate(t.date)}</td>
                  <td>{t.transactionType?.value ?? '—'}</td>
                  <td>{t.amount != null ? Number(t.amount).toLocaleString() : '—'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {depositOpen && (
        <div className="modal-overlay" onClick={() => !pending && setDepositOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Deposit</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setActionError('');
              const amount = Number(depositAmount);
              if (!Number.isFinite(amount) || amount <= 0) { setActionError('Enter a valid amount.'); return; }
              depositMutation.mutate({ transactionDate: depositDate, transactionAmount: amount });
            }}>
              {actionError && <div className="form-error" role="alert">{actionError}</div>}
              <label>
                <span>Date</span>
                <input type="date" value={depositDate} onChange={(e) => setDepositDate(e.target.value)} required disabled={pending} />
              </label>
              <label>
                <span>Amount</span>
                <input type="number" step="any" min="0" value={depositAmount} onChange={(e) => setDepositAmount(e.target.value)} placeholder="0.00" required disabled={pending} />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setDepositOpen(false)} disabled={pending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={pending}>{pending ? 'Submitting…' : 'Submit deposit'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {holdOpen && (
        <div className="modal-overlay" onClick={() => !pending && setHoldOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Hold amount</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setActionError('');
              const amount = Number(holdAmount);
              if (!Number.isFinite(amount) || amount <= 0) { setActionError('Enter a valid amount.'); return; }
              holdMutation.mutate({ transactionDate: holdDate, transactionAmount: amount });
            }}>
              {actionError && <div className="form-error" role="alert">{actionError}</div>}
              <label>
                <span>Date</span>
                <input type="date" value={holdDate} onChange={(e) => setHoldDate(e.target.value)} required disabled={pending} />
              </label>
              <label>
                <span>Amount</span>
                <input type="number" step="any" min="0" value={holdAmount} onChange={(e) => setHoldAmount(e.target.value)} placeholder="0.00" required disabled={pending} />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setHoldOpen(false)} disabled={pending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={pending}>{pending ? 'Submitting…' : 'Hold amount'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {withdrawOpen && (
        <div className="modal-overlay" onClick={() => !pending && setWithdrawOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Withdraw</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setActionError('');
              const amount = Number(withdrawAmount);
              if (!Number.isFinite(amount) || amount <= 0) { setActionError('Enter a valid amount.'); return; }
              withdrawMutation.mutate({ transactionDate: withdrawDate, transactionAmount: amount });
            }}>
              {actionError && <div className="form-error" role="alert">{actionError}</div>}
              <label>
                <span>Date</span>
                <input type="date" value={withdrawDate} onChange={(e) => setWithdrawDate(e.target.value)} required disabled={pending} />
              </label>
              <label>
                <span>Amount</span>
                <input type="number" step="any" min="0" value={withdrawAmount} onChange={(e) => setWithdrawAmount(e.target.value)} placeholder="0.00" required disabled={pending} />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setWithdrawOpen(false)} disabled={pending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={pending}>{pending ? 'Submitting…' : 'Submit withdrawal'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
