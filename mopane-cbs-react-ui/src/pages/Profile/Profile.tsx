import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../contexts/AuthContext';
import { fetchUser, updateUser } from '../../api/users';
import './Profile.css';

export default function Profile() {
  const { user } = useAuth();
  const userId = user?.userId ?? 0;
  const queryClient = useQueryClient();
  const [firstname, setFirstname] = useState('');
  const [lastname, setLastname] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [repeatPassword, setRepeatPassword] = useState('');
  const [profileError, setProfileError] = useState('');
  const [passwordError, setPasswordError] = useState('');

  const { data: profile, isLoading, error } = useQuery({
    queryKey: ['user', userId],
    queryFn: () => fetchUser(userId),
    enabled: Number.isFinite(userId) && userId > 0,
  });

  useEffect(() => {
    if (profile) {
      setFirstname(profile.firstname ?? '');
      setLastname(profile.lastname ?? '');
      setEmail(profile.email ?? '');
    }
  }, [profile]);

  const updateProfileMutation = useMutation({
    mutationFn: (body: Record<string, unknown>) => updateUser(userId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['user', userId] });
      setProfileError('');
    },
    onError: () => setProfileError('Failed to update profile.'),
  });

  const changePasswordMutation = useMutation({
    mutationFn: (body: { password: string; repeatPassword: string }) =>
      updateUser(userId, body),
    onSuccess: () => {
      setPassword('');
      setRepeatPassword('');
      setPasswordError('');
    },
    onError: () => setPasswordError('Failed to change password.'),
  });

  function handleProfileSubmit(e: React.FormEvent) {
    e.preventDefault();
    setProfileError('');
    updateProfileMutation.mutate({
      firstname: firstname.trim(),
      lastname: lastname.trim(),
      email: email.trim() || undefined,
    });
  }

  function handlePasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPasswordError('');
    if (password.length < 1) {
      setPasswordError('Enter a new password.');
      return;
    }
    if (password !== repeatPassword) {
      setPasswordError('Passwords do not match.');
      return;
    }
    changePasswordMutation.mutate({ password, repeatPassword });
  }

  if (!Number.isFinite(userId) || userId <= 0) {
    return (
      <div className="profile-page">
        <div className="page-error">
          You must be logged in to view profile. Your session may not have a user ID.
        </div>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading profile…</div>;
  if (error || !profile) {
    return (
      <div className="profile-page">
        <div className="page-error">Failed to load profile.</div>
      </div>
    );
  }

  return (
    <div className="profile-page">
      <div className="page-header">
        <h1>Profile</h1>
      </div>
      <div className="profile-card">
        <h2>Account</h2>
        <dl className="detail-list">
          <div><dt>Username</dt><dd>{profile.username ?? '—'}</dd></div>
          <div><dt>Office</dt><dd>{profile.officeName ?? '—'}</dd></div>
        </dl>
      </div>
      <div className="profile-card">
        <h2>Update profile</h2>
        <form onSubmit={handleProfileSubmit} className="profile-form">
          {profileError && <div className="form-error" role="alert">{profileError}</div>}
          <label>
            <span>First name</span>
            <input value={firstname} onChange={(e) => setFirstname(e.target.value)} disabled={updateProfileMutation.isPending} />
          </label>
          <label>
            <span>Last name</span>
            <input value={lastname} onChange={(e) => setLastname(e.target.value)} disabled={updateProfileMutation.isPending} />
          </label>
          <label>
            <span>Email</span>
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} disabled={updateProfileMutation.isPending} />
          </label>
          <div className="form-actions">
            <button type="submit" className="btn-primary" disabled={updateProfileMutation.isPending}>
              {updateProfileMutation.isPending ? 'Saving…' : 'Save profile'}
            </button>
          </div>
        </form>
      </div>
      <div className="profile-card">
        <h2>Change password</h2>
        <form onSubmit={handlePasswordSubmit} className="profile-form">
          {passwordError && <div className="form-error" role="alert">{passwordError}</div>}
          <label>
            <span>New password</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} disabled={changePasswordMutation.isPending} autoComplete="new-password" />
          </label>
          <label>
            <span>Repeat password</span>
            <input type="password" value={repeatPassword} onChange={(e) => setRepeatPassword(e.target.value)} disabled={changePasswordMutation.isPending} autoComplete="new-password" />
          </label>
          <div className="form-actions">
            <button type="submit" className="btn-primary" disabled={changePasswordMutation.isPending || !password.trim()}>
              {changePasswordMutation.isPending ? 'Updating…' : 'Change password'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
