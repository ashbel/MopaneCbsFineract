import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import {
  fetchSavingsProduct,
  fetchSavingsProductTemplate,
  createSavingsProduct,
  updateSavingsProduct,
  type OptionItem,
} from '../../api/savingsProducts';
import './SavingsProductForm.css';

export default function SavingsProductForm() {
  const { id } = useParams<{ id: string }>();
  const productId = id ? Number(id) : null;
  const isEdit = Number.isFinite(productId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [shortName, setShortName] = useState('');
  const [description, setDescription] = useState('');
  const [currencyCode, setCurrencyCode] = useState('');
  const [nominalAnnualInterestRate, setNominalAnnualInterestRate] = useState('');
  const [interestCompoundingPeriodTypeId, setInterestCompoundingPeriodTypeId] = useState('');
  const [interestPostingPeriodTypeId, setInterestPostingPeriodTypeId] = useState('');
  const [interestCalculationTypeId, setInterestCalculationTypeId] = useState('');
  const [minRequiredOpeningBalance, setMinRequiredOpeningBalance] = useState('');
  const [submitError, setSubmitError] = useState('');

  const { data: template } = useQuery({
    queryKey: ['savingsProductTemplate'],
    queryFn: fetchSavingsProductTemplate,
    enabled: !isEdit,
  });

  const { data: existing } = useQuery({
    queryKey: ['savingsProduct', productId!],
    queryFn: () => fetchSavingsProduct(productId!, { template: true }),
    enabled: isEdit && Number.isFinite(productId!),
  });

  const data = isEdit ? existing : template;

  useEffect(() => {
    if (existing) {
      setName(existing.name ?? '');
      setShortName(existing.shortName ?? '');
      setDescription((existing.description as string) ?? '');
      setCurrencyCode(existing.currency?.code ?? '');
      setNominalAnnualInterestRate(String(existing.nominalAnnualInterestRate ?? ''));
      setInterestCompoundingPeriodTypeId(String(existing.interestCompoundingPeriodType?.id ?? ''));
      setInterestPostingPeriodTypeId(String(existing.interestPostingPeriodType?.id ?? ''));
      setInterestCalculationTypeId(String(existing.interestCalculationType?.id ?? ''));
      setMinRequiredOpeningBalance(String(existing.minRequiredOpeningBalance ?? ''));
    }
  }, [existing]);

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createSavingsProduct(body),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['savingsProducts'] });
      navigate(`/savingsproducts/${res.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create savings product. Please try again.'),
  });

  const updateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => updateSavingsProduct(productId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['savingsProducts'] });
      queryClient.invalidateQueries({ queryKey: ['savingsProduct', productId] });
      navigate(`/savingsproducts/${productId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update savings product. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const body: Record<string, unknown> = {
      name: name.trim(),
      shortName: shortName.trim(),
      description: description.trim() || undefined,
      currencyCode: currencyCode || (data?.currencyOptions?.[0]?.code),
      nominalAnnualInterestRate: nominalAnnualInterestRate ? Number(nominalAnnualInterestRate) : undefined,
      interestCompoundingPeriodTypeId: interestCompoundingPeriodTypeId ? Number(interestCompoundingPeriodTypeId) : undefined,
      interestPostingPeriodTypeId: interestPostingPeriodTypeId ? Number(interestPostingPeriodTypeId) : undefined,
      interestCalculationTypeId: interestCalculationTypeId ? Number(interestCalculationTypeId) : undefined,
      minRequiredOpeningBalance: minRequiredOpeningBalance ? Number(minRequiredOpeningBalance) : undefined,
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
  const currencyOptions = data?.currencyOptions ?? [];
  const compoundingOptions = (data?.interestCompoundingPeriodTypeOptions ?? []) as OptionItem[];
  const postingOptions = (data?.interestPostingPeriodTypeOptions ?? []) as OptionItem[];
  const calculationOptions = (data?.interestCalculationTypeOptions ?? []) as OptionItem[];

  return (
    <div className="savings-product-form-page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit savings product' : 'Add savings product'}</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="savings-product-form">
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
          <span>Nominal annual interest rate (%)</span>
          <input type="number" min="0" step="any" value={nominalAnnualInterestRate} onChange={(e) => setNominalAnnualInterestRate(e.target.value)} disabled={mutating} />
        </label>
        {compoundingOptions.length > 0 && (
          <label>
            <span>Interest compounding period</span>
            <select value={interestCompoundingPeriodTypeId} onChange={(e) => setInterestCompoundingPeriodTypeId(e.target.value)} disabled={mutating}>
              <option value="">Select</option>
              {compoundingOptions.map((o) => (
                <option key={o.id} value={o.id}>{o.value ?? o.code ?? o.id}</option>
              ))}
            </select>
          </label>
        )}
        {postingOptions.length > 0 && (
          <label>
            <span>Interest posting period</span>
            <select value={interestPostingPeriodTypeId} onChange={(e) => setInterestPostingPeriodTypeId(e.target.value)} disabled={mutating}>
              <option value="">Select</option>
              {postingOptions.map((o) => (
                <option key={o.id} value={o.id}>{o.value ?? o.code ?? o.id}</option>
              ))}
            </select>
          </label>
        )}
        {calculationOptions.length > 0 && (
          <label>
            <span>Interest calculation type</span>
            <select value={interestCalculationTypeId} onChange={(e) => setInterestCalculationTypeId(e.target.value)} disabled={mutating}>
              <option value="">Select</option>
              {calculationOptions.map((o) => (
                <option key={o.id} value={o.id}>{o.value ?? o.code ?? o.id}</option>
              ))}
            </select>
          </label>
        )}
        <label>
          <span>Min required opening balance</span>
          <input type="number" min="0" step="any" value={minRequiredOpeningBalance} onChange={(e) => setMinRequiredOpeningBalance(e.target.value)} disabled={mutating} />
        </label>
        <div className="form-actions">
          <button type="submit" disabled={mutating} className="btn-primary">
            {mutating ? 'Saving…' : isEdit ? 'Save changes' : 'Create savings product'}
          </button>
        </div>
      </form>
    </div>
  );
}
