import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import { fetchStaffMember, createStaff, updateStaff } from '../../api/staff';
import { fetchOffices } from '../../api/offices';
import './StaffForm.css';

function formatDateArr(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

export default function StaffForm() {
  const { id } = useParams<{ id: string }>();
  const staffId = id ? Number(id) : null;
  const isEdit = Number.isFinite(staffId);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [firstname, setFirstname] = useState('');
  const [lastname, setLastname] = useState('');
  const [officeId, setOfficeId] = useState('');
  const [isLoanOfficer, setIsLoanOfficer] = useState(false);
  const [externalId, setExternalId] = useState('');
  const [joiningDate, setJoiningDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [mobileNo, setMobileNo] = useState('');
  const [submitError, setSubmitError] = useState('');

  const { data: offices } = useQuery({
    queryKey: ['offices'],
    queryFn: () => fetchOffices(),
    enabled: !isEdit,
  });

  const { data: existing } = useQuery({
    queryKey: ['staff', staffId!],
    queryFn: () => fetchStaffMember(staffId!, { template: true }),
    enabled: isEdit && Number.isFinite(staffId!),
  });

  useEffect(() => {
    if (existing) {
      setFirstname(existing.firstname ?? '');
      setLastname(existing.lastname ?? '');
      setOfficeId(String(existing.officeId ?? ''));
      setIsLoanOfficer(existing.isLoanOfficer ?? false);
      setExternalId(existing.externalId ?? '');
      setJoiningDate(formatDateArr(existing.joiningDate) || new Date().toISOString().slice(0, 10));
      setMobileNo(existing.mobileNo ?? '');
    }
  }, [existing]);

  const createMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => createStaff(body),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['staff'] });
      navigate(`/staff/${data.resourceId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to create staff. Please try again.'),
  });

  const updateMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => updateStaff(staffId!, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['staff'] });
      queryClient.invalidateQueries({ queryKey: ['staff', staffId] });
      navigate(`/staff/${staffId}`, { replace: true });
    },
    onError: () => setSubmitError('Failed to update staff. Please try again.'),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError('');
    const dateParts = joiningDate ? joiningDate.split('-').map(Number) : undefined;
    const body: Record<string, unknown> = {
      firstname: firstname.trim(),
      lastname: lastname.trim(),
      officeId: officeId ? Number(officeId) : (existing?.officeId ?? offices?.[0]?.id),
      isLoanOfficer,
      externalId: externalId.trim() || undefined,
      joiningDate: dateParts,
      mobileNo: mobileNo.trim() || undefined,
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
  const officeOptions = existing?.allowedOffices ?? offices ?? [];

  return (
    <div className="staff-form-page">
      <div className="page-header">
        <h1>{isEdit ? 'Edit staff' : 'Add staff'}</h1>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>
          Cancel
        </button>
      </div>
      <form onSubmit={handleSubmit} className="staff-form">
        {submitError && <div className="form-error" role="alert">{submitError}</div>}
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
          <span>Office *</span>
          <select
            value={officeId}
            onChange={(e) => setOfficeId(e.target.value)}
            required
            disabled={mutating}
          >
            <option value="">Select office</option>
            {Array.isArray(officeOptions) && officeOptions.map((o: { id: number; name?: string }) => (
              <option key={o.id} value={o.id}>
                {o.name ?? o.id}
              </option>
            ))}
          </select>
        </label>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={isLoanOfficer}
            onChange={(e) => setIsLoanOfficer(e.target.checked)}
            disabled={mutating}
          />
          Loan officer
        </label>
        <label>
          <span>Joining date *</span>
          <input
            type="date"
            value={joiningDate}
            onChange={(e) => setJoiningDate(e.target.value)}
            required
            disabled={mutating}
          />
        </label>
        <label>
          <span>Mobile number</span>
          <input
            value={mobileNo}
            onChange={(e) => setMobileNo(e.target.value)}
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
            {mutating ? 'Saving…' : isEdit ? 'Save changes' : 'Create staff'}
          </button>
        </div>
      </form>
    </div>
  );
}
