import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import {
  fetchLoanProduct,
  fetchLoanProductTemplate,
  createLoanProduct,
  updateLoanProduct,
  type OptionItem,
} from '../../api/loanProducts';
import './LoanProductForm.css';

export default function LoanProductForm() {
  const { id } = useParams<{ id: string }>();
  const productId = id ? Number(id) : null;
  const isEdit = Number.isFinite(productId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [shortName, setShortName] = useState('');
  const [description, setDescription] = useState('');
  const [fundId, setFundId] = useState('');
  const [currencyCode, setCurrencyCode] = useState('');
  const [principal, setPrincipal] = useState('');
  const [numberOfRepayments, setNumberOfRepayments] = useState('');
  const [repaymentEvery, setRepaymentEvery] = useState('');
  const [repaymentFrequencyTypeId, setRepaymentFrequencyTypeId] = useState('');
  const [interestRatePerPeriod, setInterestRatePerPeriod] = useState('');
  const [amortizationTypeId, setAmortizationTypeId] = useState('');
  const [interestTypeId, setInterestTypeId] = useState('');
  const [transactionProcessingStrategyId, setTransactionProcessingStrategyId] = useState('');
  const [submitError, setSubmitError] = useState('');

  const { data: template } = useQuery({
    queryKey: ['loanProductTemplate'],
    queryFn: fetchLoanProductTemplate,
    enabled: !isEdit,
  });

  const { data: existing } = useQuery({
    queryKey: ['loanProduct', productId!],
    queryFn: () => fetchLoanProduct(productId!, { template: true }),
    enabled: isEdit && Number.isFinite(productId!),
  });

  const data = isEdit ? existing : template;

  useEffect(() => {
    if (existing) {
      setName(existing.name ?? '');
      setShortName(existing.shortName ?? '');
      setDescription((existing.description as string) ?? '');
      setFundId(String(existing.fundId ?? ''));
      setCurrencyCode(existing.currency?.code ?? '');
      setPrincipal(String(existing.principal ?? ''));
      setNumberOfRepayments(String(existing.numberOfRepayments ?? ''));
      setRepaymentEvery(String(existing.repaymentEvery ?? ''));
      setRepaymentFrequencyTypeId(String(existing.repaymentFrequencyType?.id ?? ''));
      setInterestRatePerPeriod(String(existing.interestRatePerPeriod ?? ''));
      setAmortizationTypeId(String(existing.amortizationType?.id ?? ''));
      setInterestTypeId(String(existing.interestType?.id ?? ''));
      setTransactionProcessingStrategyId(String(existing.transactionProcessingStrategyId ?? ''));
    }
  }, [existing]);

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createLoanProduct(body),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['loanProducts'] });
      navigate(`/loanproducts/${res.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create loan product. Please try again.'),
  });

  const updateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => updateLoanProduct(productId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['loanProducts'] });
      queryClient.invalidateQueries({ queryKey: ['loanProduct', productId] });
      navigate(`/loanproducts/${productId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update loan product. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const body: Record<string, unknown> = {
      name: name.trim(),
      shortName: shortName.trim(),
      description: description.trim() || undefined,
      fundId: fundId ? Number(fundId) : (data?.fundOptions?.[0]?.id),
      currencyCode: currencyCode || (data?.currencyOptions?.[0]?.code),
      principal: principal ? Number(principal) : undefined,
      numberOfRepayments: numberOfRepayments ? Number(numberOfRepayments) : undefined,
      repaymentEvery: repaymentEvery ? Number(repaymentEvery) : undefined,
      repaymentFrequencyTypeId: repaymentFrequencyTypeId ? Number(repaymentFrequencyTypeId) : undefined,
      interestRatePerPeriod: interestRatePerPeriod ? Number(interestRatePerPeriod) : undefined,
      amortizationTypeId: amortizationTypeId ? Number(amortizationTypeId) : undefined,
      interestTypeId: interestTypeId ? Number(interestTypeId) : undefined,
      transactionProcessingStrategyId: transactionProcessingStrategyId ? Number(transactionProcessingStrategyId) : (data?.transactionProcessingStrategyOptions?.[0]?.id),
      locale: 'en',
      dateFormat: 'yyyy-MM-dd',
    };
    if (isEdit) {
      updateMutation.mutate(body);
    } else {
      createMutation.mutate(body);
    }
  }

  const mutating = createMutation.isPending || updateMutation.isPending;
  const fundOptions = data?.fundOptions ?? [];
  const currencyOptions = data?.currencyOptions ?? [];
  const repaymentFreqOptions = (data?.repaymentFrequencyTypeOptions ?? []) as OptionItem[];
  const amortizationOptions = (data?.amortizationTypeOptions ?? []) as OptionItem[];
  const interestTypeOptions = (data?.interestTypeOptions ?? []) as OptionItem[];
  const strategyOptions = data?.transactionProcessingStrategyOptions ?? [];

  return (
    <div className="loan-product-form-page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit loan product' : 'Add loan product'}</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="loan-product-form">
        {submitError && <div className="form-error" role="alert">{submitError}</div>}
        <label>
          <span>Name *</span>
          <input value={name} onChange={(e) => setName(e.target.value)} required disabled={mutating} />
        </label>
        <label>
          <span>Short name *</span>
          <input value={shortName} onChange={(e) => setShortName(e.target.value)} required disabled={mutating} />
        </label>
        <label>
          <span>Description</span>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} disabled={mutating} rows={2} />
        </label>
        {fundOptions.length > 0 && (
          <label>
            <span>Fund</span>
            <select value={fundId} onChange={(e) => setFundId(e.target.value)} disabled={mutating}>
              <option value="">Select fund</option>
              {fundOptions.map((f) => (
                <option key={f.id} value={f.id}>{f.name ?? f.id}</option>
              ))}
            </select>
          </label>
        )}
        {currencyOptions.length > 0 && (
          <label>
            <span>Currency *</span>
            <select value={currencyCode} onChange={(e) => setCurrencyCode(e.target.value)} required disabled={mutating}>
              <option value="">Select currency</option>
              {currencyOptions.map((c: { code: string; name?: string }) => (
                <option key={c.code} value={c.code}>{c.code} {c.name ?? ''}</option>
              ))}
            </select>
          </label>
        )}
        <label>
          <span>Principal</span>
          <input type="number" min="0" step="any" value={principal} onChange={(e) => setPrincipal(e.target.value)} disabled={mutating} />
        </label>
        <label>
          <span>Number of repayments</span>
          <input type="number" min="1" value={numberOfRepayments} onChange={(e) => setNumberOfRepayments(e.target.value)} disabled={mutating} />
        </label>
        <label>
          <span>Repayment every</span>
          <input type="number" min="1" value={repaymentEvery} onChange={(e) => setRepaymentEvery(e.target.value)} disabled={mutating} />
        </label>
        {repaymentFreqOptions.length > 0 && (
          <label>
            <span>Repayment frequency</span>
            <select value={repaymentFrequencyTypeId} onChange={(e) => setRepaymentFrequencyTypeId(e.target.value)} disabled={mutating}>
              <option value="">Select</option>
              {repaymentFreqOptions.map((o) => (
                <option key={o.id} value={o.id}>{o.value ?? o.code ?? o.id}</option>
              ))}
            </select>
          </label>
        )}
        <label>
          <span>Interest rate per period (%)</span>
          <input type="number" min="0" step="any" value={interestRatePerPeriod} onChange={(e) => setInterestRatePerPeriod(e.target.value)} disabled={mutating} />
        </label>
        {amortizationOptions.length > 0 && (
          <label>
            <span>Amortization type</span>
            <select value={amortizationTypeId} onChange={(e) => setAmortizationTypeId(e.target.value)} disabled={mutating}>
              <option value="">Select</option>
              {amortizationOptions.map((o) => (
                <option key={o.id} value={o.id}>{o.value ?? o.code ?? o.id}</option>
              ))}
            </select>
          </label>
        )}
        {interestTypeOptions.length > 0 && (
          <label>
            <span>Interest type</span>
            <select value={interestTypeId} onChange={(e) => setInterestTypeId(e.target.value)} disabled={mutating}>
              <option value="">Select</option>
              {interestTypeOptions.map((o) => (
                <option key={o.id} value={o.id}>{o.value ?? o.code ?? o.id}</option>
              ))}
            </select>
          </label>
        )}
        {strategyOptions.length > 0 && (
          <label>
            <span>Transaction processing strategy</span>
            <select value={transactionProcessingStrategyId} onChange={(e) => setTransactionProcessingStrategyId(e.target.value)} disabled={mutating}>
              <option value="">Select</option>
              {strategyOptions.map((s: { id: number; name?: string }) => (
                <option key={s.id} value={s.id}>{s.name ?? s.id}</option>
              ))}
            </select>
          </label>
        )}
        <div className="form-actions">
          <button type="submit" disabled={mutating} className="btn-primary">
            {mutating ? 'Saving…' : isEdit ? 'Save changes' : 'Create loan product'}
          </button>
        </div>
      </form>
    </div>
  );
}
