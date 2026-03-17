import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import {
  fetchClient,
  fetchClientTemplate,
  createClient,
  updateClient,
} from '../../api/clients';
import './ClientForm.css';

type FormState = {
  firstname: string;
  lastname: string;
  officeId: string;
  externalId: string;
  mobileNo: string;
  dateOfBirth: string;
};

const emptyForm: FormState = {
  firstname: '',
  lastname: '',
  officeId: '',
  externalId: '',
  mobileNo: '',
  dateOfBirth: '',
};

export default function ClientForm() {
  const { id } = useParams<{ id: string }>();
  const clientId = id ? Number(id) : null;
  const isEdit = Number.isFinite(clientId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [form, setForm] = useState<FormState>(emptyForm);
  const [submitError, setSubmitError] = useState('');

  const { data: template } = useQuery({
    queryKey: ['clientTemplate'],
    queryFn: fetchClientTemplate,
    enabled: !isEdit,
  });

  const { data: existing } = useQuery({
    queryKey: ['client', clientId!],
    queryFn: () => fetchClient(clientId!),
    enabled: isEdit && Number.isFinite(clientId!),
  });

  useEffect(() => {
    if (existing) {
      setForm({
        firstname: existing.firstname ?? '',
        lastname: existing.lastname ?? '',
        officeId: String(existing.officeId ?? ''),
        externalId: existing.externalId ?? '',
        mobileNo: existing.mobileNo ?? '',
        dateOfBirth: existing.dateOfBirth?.length
          ? `${existing.dateOfBirth[0]}-${String(existing.dateOfBirth[1]).padStart(2, '0')}-${String(existing.dateOfBirth[2]).padStart(2, '0')}`
          : '',
      });
    }
  }, [existing]);

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createClient(body),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['clients'] });
      navigate(`/clients/${data.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create client. Please try again.'),
  });

  const updateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) =>
      updateClient(clientId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clients'] });
      queryClient.invalidateQueries({ queryKey: ['client', clientId] });
      navigate(`/clients/${clientId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update client. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const officeId = form.officeId ? Number(form.officeId) : undefined;
    if (isEdit) {
      updateMutation.mutate({
        firstname: form.firstname,
        lastname: form.lastname,
        externalId: form.externalId || undefined,
        mobileNo: form.mobileNo || undefined,
        dateOfBirth: form.dateOfBirth
          ? form.dateOfBirth.split('-').map(Number)
          : undefined,
      });
    } else {
      if (!officeId && template?.officeOptions?.length) {
        setSubmitError('Please select an office.');
        return;
      }
      createMutation.mutate({
        firstname: form.firstname,
        lastname: form.lastname,
        officeId: officeId ?? template?.officeOptions?.[0]?.id,
        externalId: form.externalId || undefined,
        mobileNo: form.mobileNo || undefined,
        dateOfBirth: form.dateOfBirth
          ? form.dateOfBirth.split('-').map(Number)
          : undefined,
      });
    }
  }

  const mutating = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="client-form-page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit client' : 'Add client'}</h1>
        <button
          type="button"
          className="btn-secondary"
          onClick={() => navigate(-1)}
        >
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="client-form">
        {submitError && (
          <div className="form-error" role="alert">
            {submitError}
          </div>
        )}
        {!isEdit && template?.officeOptions && template.officeOptions.length > 0 && (
          <label>
            <span>Office *</span>
            <select
              value={form.officeId}
              onChange={(e) => setForm((s) => ({ ...s, officeId: e.target.value }))}
              required
              disabled={mutating}
            >
              <option value="">Select office</option>
              {template.officeOptions.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.name}
                </option>
              ))}
            </select>
          </label>
        )}
        <label>
          <span>First name *</span>
          <input
            value={form.firstname}
            onChange={(e) => setForm((s) => ({ ...s, firstname: e.target.value }))}
            required
            disabled={mutating}
          />
        </label>
        <label>
          <span>Last name *</span>
          <input
            value={form.lastname}
            onChange={(e) => setForm((s) => ({ ...s, lastname: e.target.value }))}
            required
            disabled={mutating}
          />
        </label>
        <label>
          <span>Mobile</span>
          <input
            type="tel"
            value={form.mobileNo}
            onChange={(e) => setForm((s) => ({ ...s, mobileNo: e.target.value }))}
            disabled={mutating}
          />
        </label>
        <label>
          <span>External ID</span>
          <input
            value={form.externalId}
            onChange={(e) => setForm((s) => ({ ...s, externalId: e.target.value }))}
            disabled={mutating}
          />
        </label>
        <label>
          <span>Date of birth</span>
          <input
            type="date"
            value={form.dateOfBirth}
            onChange={(e) => setForm((s) => ({ ...s, dateOfBirth: e.target.value }))}
            disabled={mutating}
          />
        </label>
        <div className="form-actions">
          <button type="submit" disabled={mutating} className="btn-primary">
            {mutating ? 'Saving…' : isEdit ? 'Save changes' : 'Create client'}
          </button>
        </div>
      </form>
    </div>
  );
}

