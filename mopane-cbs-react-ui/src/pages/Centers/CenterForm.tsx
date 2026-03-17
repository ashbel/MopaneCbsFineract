import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import {
  fetchCenter,
  fetchCenterTemplate,
  createCenter,
  updateCenter,
} from '../../api/centers';
import './CenterForm.css';

function formatDateArr(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

export default function CenterForm() {
  const { id } = useParams<{ id: string }>();
  const centerId = id ? Number(id) : null;
  const isEdit = Number.isFinite(centerId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [officeId, setOfficeId] = useState('');
  const [staffId, setStaffId] = useState('');
  const [externalId, setExternalId] = useState('');
  const [submittedOnDate, setSubmittedOnDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [submitError, setSubmitError] = useState('');

  const { data: template } = useQuery({
    queryKey: ['centerTemplate'],
    queryFn: () => fetchCenterTemplate(),
    enabled: !isEdit,
  });

  const { data: existing } = useQuery({
    queryKey: ['center', centerId!],
    queryFn: () => fetchCenter(centerId!, { template: true }),
    enabled: isEdit && Number.isFinite(centerId!),
  });

  useEffect(() => {
    if (existing) {
      setName(existing.name ?? '');
      setOfficeId(String(existing.officeId ?? ''));
      setStaffId(String((existing as { staffId?: number }).staffId ?? ''));
      setExternalId(existing.externalId ?? '');
      setSubmittedOnDate(formatDateArr(existing.submittedOnDate) || new Date().toISOString().slice(0, 10));
    }
  }, [existing]);

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createCenter(body),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['centers'] });
      navigate(`/centers/${data.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create center. Please try again.'),
  });

  const updateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => updateCenter(centerId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['centers'] });
      queryClient.invalidateQueries({ queryKey: ['center', centerId] });
      navigate(`/centers/${centerId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update center. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const dateParts = submittedOnDate ? submittedOnDate.split('-').map(Number) : undefined;
    const body: Record<string, unknown> = {
      name: name.trim(),
      officeId: officeId ? Number(officeId) : (template?.officeOptions?.[0]?.id ?? existing?.officeId),
      externalId: externalId.trim() || undefined,
      locale: 'en',
      dateFormat: 'yyyy-MM-dd',
      submittedOnDate: dateParts,
    };
    if (staffId) body.staffId = Number(staffId);
    if (isEdit) {
      delete (body as Record<string, unknown>).officeId;
      updateMutation.mutate(body);
    } else {
      createMutation.mutate(body);
    }
  }

  const mutating = createMutation.isPending || updateMutation.isPending;
  const officeOptions = template?.officeOptions ?? existing?.officeOptions ?? [];
  const staffOptions = template?.staffOptions ?? existing?.staffOptions ?? [];

  return (
    <div className="center-form-page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit center' : 'Add center'}</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="center-form">
        {submitError && <div className="form-error" role="alert">{submitError}</div>}
        {!isEdit && officeOptions.length > 0 && (
          <label>
            <span>Office *</span>
            <select
              value={officeId}
              onChange={(e) => setOfficeId(e.target.value)}
              required
              disabled={mutating}
            >
              <option value="">Select office</option>
              {officeOptions.map((o) => (
                <option key={o.id} value={o.id}>
                  {o.name}
                </option>
              ))}
            </select>
          </label>
        )}
        <label>
          <span>Name *</span>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        {staffOptions.length > 0 && (
          <label>
            <span>Staff</span>
            <select
              value={staffId}
              onChange={(e) => setStaffId(e.target.value)}
              disabled={mutating}
            >
              <option value="">None</option>
              {staffOptions.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.displayName ?? `Staff ${s.id}`}
                </option>
              ))}
            </select>
          </label>
        )}
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
        <label>
          <span>External ID</span>
          <input
            value={externalId}
            onChange={(e) => setExternalId(e.target.value)}
            disabled={mutating}
          />
        </label>
        <div className="form-actions">
          <button type="submit" disabled={mutating} className="btn-primary">
            {mutating ? 'Saving…' : isEdit ? 'Save changes' : 'Create center'}
          </button>
        </div>
      </form>
    </div>
  );
}
