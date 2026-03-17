import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import {
  fetchUser,
  fetchUserTemplate,
  createUser,
  updateUser,
  type RoleOption,
} from '../../api/users';
import './UserForm.css';

export default function UserForm() {
  const { id } = useParams<{ id: string }>();
  const userId = id ? Number(id) : null;
  const isEdit = Number.isFinite(userId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [username, setUsername] = useState('');
  const [firstname, setFirstname] = useState('');
  const [lastname, setLastname] = useState('');
  const [email, setEmail] = useState('');
  const [officeId, setOfficeId] = useState('');
  const [roleIds, setRoleIds] = useState<number[]>([]);
  const [sendPasswordToEmail, setSendPasswordToEmail] = useState(false);
  const [submitError, setSubmitError] = useState('');

  const { data: template } = useQuery({
    queryKey: ['userTemplate'],
    queryFn: fetchUserTemplate,
    enabled: !isEdit,
  });

  const { data: existing } = useQuery({
    queryKey: ['user', userId!],
    queryFn: () => fetchUser(userId!),
    enabled: isEdit && Number.isFinite(userId!),
  });

  useEffect(() => {
    if (existing) {
      setUsername(existing.username ?? '');
      setFirstname(existing.firstname ?? '');
      setLastname(existing.lastname ?? '');
      setEmail(existing.email ?? '');
      setOfficeId(String(existing.officeId ?? ''));
      setRoleIds(
        (existing.selectedRoles ?? []).map((r: RoleOption) => r.id)
      );
    }
  }, [existing]);

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createUser(body),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      navigate(`/users/${data.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create user. Please try again.'),
  });

  const updateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => updateUser(userId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      queryClient.invalidateQueries({ queryKey: ['user', userId] });
      navigate(`/users/${userId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update user. Please try again.'),
  });

  function toggleRole(id: number) {
    setRoleIds((prev) =>
      prev.includes(id) ? prev.filter((r) => r !== id) : [...prev, id]
    );
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const body: Record<string, unknown> = {
      username: username.trim(),
      firstname: firstname.trim(),
      lastname: lastname.trim(),
      email: email.trim() || undefined,
      officeId: officeId ? Number(officeId) : (template?.allowedOffices?.[0]?.id ?? 1),
      roles: roleIds,
      locale: 'en',
    };
    if (isEdit) {
      updateMutation.mutate(body);
    } else {
      body.sendPasswordToEmail = sendPasswordToEmail;
      createMutation.mutate(body);
    }
  }

  const mutating = createMutation.isPending || updateMutation.isPending;
  const offices = template?.allowedOffices ?? existing?.allowedOffices ?? [];
  const roles = template?.availableRoles ?? existing?.availableRoles ?? [];

  return (
    <div className="user-form-page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit user' : 'Add user'}</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="user-form">
        {submitError && <div className="form-error" role="alert">{submitError}</div>}
        <label>
          <span>Username *</span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            disabled={mutating || isEdit}
            placeholder="Login name"
          />
        </label>
        <label>
          <span>First name *</span>
          <input
            value={firstname}
            onChange={(e) => setFirstname(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        <label>
          <span>Last name *</span>
          <input
            value={lastname}
            onChange={(e) => setLastname(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        <label>
          <span>Email</span>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={mutating}
          />
        </label>
        <label>
          <span>Office *</span>
          <select
            value={officeId}
            onChange={(e) => setOfficeId(e.target.value)}
            required
            disabled={mutating}
          >
            <option value="">Select office</option>
            {offices.map((o) => (
              <option key={o.id} value={o.id}>
                {o.name}
              </option>
            ))}
          </select>
        </label>
        <div className="form-group">
          <span className="label">Roles</span>
          <div className="roles-checkboxes">
            {roles.map((r) => (
              <label key={r.id} className="checkbox-label">
                <input
                  type="checkbox"
                  checked={roleIds.includes(r.id)}
                  onChange={() => toggleRole(r.id)}
                  disabled={mutating}
                />
                {r.name}
              </label>
            ))}
          </div>
        </div>
        {!isEdit && (
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={sendPasswordToEmail}
              onChange={(e) => setSendPasswordToEmail(e.target.checked)}
              disabled={mutating}
            />
            Send password to user by email
          </label>
        )}
        <div className="form-actions">
          <button type="submit" disabled={mutating} className="btn-primary">
            {mutating ? 'Saving…' : isEdit ? 'Save changes' : 'Create user'}
          </button>
        </div>
      </form>
    </div>
  );
}
