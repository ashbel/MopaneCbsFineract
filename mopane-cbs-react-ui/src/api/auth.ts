import { apiClient, setAuthKey } from './client';

export interface AuthResponse {
  base64EncodedAuthenticationKey: string;
  authenticated: boolean;
  userId: number;
  username?: string;
}

export async function login(
  username: string,
  password: string
): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/authentication', {}, {
    params: { username, password },
  });
  if (data.authenticated && data.base64EncodedAuthenticationKey) {
    setAuthKey(data.base64EncodedAuthenticationKey);
  }
  return data;
}

export function logout(): void {
  setAuthKey(null);
}
