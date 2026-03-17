import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import {
  fetchOffice,
  fetchOfficeTemplate,
  createOffice,
  updateOffice,
} from '../../api/offices';
import './OfficeForm.css';

function formatDateArr(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

export default function OfficeForm() {
  const { id } = useParams<{ id: string }>();
  const officeId = id ? Number(id) : null;
  const isEdit = Number.isFinite(officeId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [parentId, setParentId] = useState('');
  const [openingDate, setOpeningDate] = useState('');
  const [externalId, setExternalId] = useState('');
  const [submitError, setSubmitError] = useState('');

  const { data: template } = useQuery({
    queryKey: ['officeTemplate'],
    queryFn: fetchOfficeTemplate,
    enabled: !isEdit,
  });

  const { data: existing } = useQuery({
    queryKey: ['office', officeId!],
    queryFn: () => fetchOffice(officeId!),
    enabled: isEdit && Number.isFinite(officeId!),
  });

  useEffect(() => {
    if (existing) {
      setName(existing.name ?? '');
      setParentId(String(existing.parentId ?? ''));
      setOpeningDate(formatDateArr(existing.openingDate));
      setExternalId(existing.externalId ?? '');
    }
  }, [existing]);

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createOffice(body),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['offices'] });
      navigate(`/offices/${data.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create office. Please try again.'),
  });

  const updateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => updateOffice(officeId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['offices'] });
      queryClient.invalidateQueries({ queryKey: ['office', officeId] });
      navigate(`/offices/${officeId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update office. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const dateParts = openingDate ? openingDate.split('-').map(Number) : undefined;
    if (isEdit) {
      updateMutation.mutate({
        name: name.trim(),
        openingDate: dateParts,
        externalId: externalId.trim() || undefined,
        locale: 'en',
        dateFormat: 'yyyy-MM-dd',
      });
    } else {
      const pid = parentId ? Number(parentId) : undefined;
      createMutation.mutate({
        name: name.trim(),
        parentId: pid ?? template?.allowedParents?.[0]?.id,
        openingDate: dateParts ?? [new Date().getFullYear(), new Date().getMonth() + 1, new Date().getDate()],
        externalId: externalId.trim() || undefined,
        locale: 'en',
        dateFormat: 'yyyy-MM-dd',
      });
    }
  }

  const mutating = createMutation.isPending || updateMutation.isPending;
  const parentOptions = template?.allowedParents ?? [];

  return (
    <div className="office-form-page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit office' : 'Add office'}</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="office-form">
        {submitError && <div className="form-error" role="alert">{submitError}</div>}
        <label>
          <span>Name *</span>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        {!isEdit && parentOptions.length > 0 && (
          <label>
            <span>Parent office</span>
            <select
              value={parentId}
              onChange={(e) => setParentId(e.target.value)}
              disabled={mutating}
            >
              <option value="">None</option>
              {parentOptions.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </label>
        )}
        <label>
          <span>Opening date *</span>
          <input
            type="date"
            value={openingDate}
            onChange={(e) => setOpeningDate(e.target.value)}
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
            {mutating ? 'Saving…' : isEdit ? 'Save changes' : 'Create office'}
          </button>
        </div>
      </form>
    </div>
  );
}
