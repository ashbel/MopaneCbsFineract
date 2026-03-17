import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import {
  fetchGroup,
  fetchGroupTemplate,
  createGroup,
  updateGroup,
} from '../../api/groups';
import './GroupForm.css';

export default function GroupForm() {
  const { id } = useParams<{ id: string }>();
  const groupId = id ? Number(id) : null;
  const isEdit = Number.isFinite(groupId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [officeId, setOfficeId] = useState('');
  const [externalId, setExternalId] = useState('');
  const [submitError, setSubmitError] = useState('');

  const { data: template } = useQuery({
    queryKey: ['groupTemplate'],
    queryFn: () => fetchGroupTemplate(),
    enabled: !isEdit,
  });

  const { data: existing } = useQuery({
    queryKey: ['group', groupId!],
    queryFn: () => fetchGroup(groupId!),
    enabled: isEdit && Number.isFinite(groupId!),
  });

  useEffect(() => {
    if (existing) {
      setName(existing.name ?? '');
      setOfficeId(String(existing.officeId ?? ''));
      setExternalId(existing.externalId ?? '');
    }
  }, [existing]);

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createGroup(body),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      navigate(`/groups/${data.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create group. Please try again.'),
  });

  const updateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => updateGroup(groupId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['groups'] });
      queryClient.invalidateQueries({ queryKey: ['group', groupId] });
      navigate(`/groups/${groupId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update group. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const oid = officeId ? Number(officeId) : undefined;
    if (isEdit) {
      updateMutation.mutate({
        name: name.trim(),
        externalId: externalId.trim() || undefined,
      });
    } else {
      const defaultOffice = template?.officeOptions?.[0]?.id;
      if (!oid && !defaultOffice) {
        setSubmitError('Please select an office.');
        return;
      }
      createMutation.mutate({
        name: name.trim(),
        officeId: oid ?? defaultOffice,
        externalId: externalId.trim() || undefined,
        locale: 'en',
        dateFormat: 'yyyy-MM-dd',
      });
    }
  }

  const mutating = createMutation.isPending || updateMutation.isPending;
  const officeOptions = template?.officeOptions ?? [];

  return (
    <div className="group-form-page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit group' : 'Add group'}</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="group-form">
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
            {mutating ? 'Saving…' : isEdit ? 'Save changes' : 'Create group'}
          </button>
        </div>
      </form>
    </div>
  );
}
