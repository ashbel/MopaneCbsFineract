import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchRole, createRole, updateRole } from '../../api/roles';
import './RoleForm.css';

export default function RoleForm() {
  const { id } = useParams<{ id: string }>();
  const roleId = id ? Number(id) : null;
  const isEdit = Number.isFinite(roleId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [submitError, setSubmitError] = useState('');

  const { data: existing } = useQuery({
    queryKey: ['role', roleId!],
    queryFn: () => fetchRole(roleId!),
    enabled: isEdit && Number.isFinite(roleId!),
  });

  useEffect(() => {
    if (existing) {
      setName(existing.name ?? '');
      setDescription(existing.description ?? '');
    }
  }, [existing]);

  const createMutation = useMutation({
    mutationFn: (body: { name: string; description?: string }) => createRole(body),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['roles'] });
      navigate(`/roles/${data.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create role. Please try again.'),
  });

  const updateMutation = useMutation({
    mutationFn: (body: { name?: string; description?: string }) => updateRole(roleId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['roles'] });
      queryClient.invalidateQueries({ queryKey: ['role', roleId] });
      navigate(`/roles/${roleId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update role. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const n = name.trim();
    if (!n) {
      setSubmitError('Name is required.');
      return;
    }
    if (isEdit) {
      updateMutation.mutate({ name: n, description: description.trim() || undefined });
    } else {
      createMutation.mutate({ name: n, description: description.trim() || undefined });
    }
  }

  const mutating = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="role-form-page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit role' : 'Add role'}</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="role-form">
        {submitError && <div className="form-error" role="alert">{submitError}</div>}
        <label>
          <span>Name *</span>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        <label>
          <span>Description</span>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            disabled={mutating}
          />
        </label>
        <div className="form-actions">
          <button type="submit" disabled={mutating} className="btn-primary">
            {mutating ? (isEdit ? 'Saving…' : 'Creating…') : (isEdit ? 'Save changes' : 'Create role')}
          </button>
        </div>
      </form>
    </div>
  );
}
