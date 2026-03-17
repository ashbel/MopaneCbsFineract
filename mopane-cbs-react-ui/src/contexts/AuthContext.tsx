import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { getAuthKey } from '../api/client';
import { login as apiLogin, logout as apiLogout } from '../api/auth';

interface User {
  userId: number;
  username: string;
}

interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  isLoading: boolean;
}

interface AuthContextValue extends AuthState {
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    isAuthenticated: !!getAuthKey(),
    user: null,
    isLoading: true,
  });

  const logout = useCallback(() => {
    apiLogout();
    setState({ isAuthenticated: false, user: null, isLoading: false });
  }, []);

  useEffect(() => {
    const key = getAuthKey();
    if (!key) {
      setState((s) => ({ ...s, isLoading: false }));
      return;
    }
    // Optionally validate by fetching user (e.g. /users/me or similar). For now we trust stored key.
    setState((s) => ({
      ...s,
      isAuthenticated: true,
      user: { userId: 0, username: 'User' },
      isLoading: false,
    }));
  }, []);

  useEffect(() => {
    const handler = () => logout();
    window.addEventListener('auth:logout', handler);
    return () => window.removeEventListener('auth:logout', handler);
  }, [logout]);

  const login = useCallback(async (username: string, password: string) => {
    setState((s) => ({ ...s, isLoading: true }));
    try {
      const res = await apiLogin(username, password);
      setState({
        isAuthenticated: true,
        user: {
          userId: res.userId,
          username: res.username ?? username,
        },
        isLoading: false,
      });
    } catch {
      setState((s) => ({ ...s, isLoading: false }));
      throw new Error('Login failed');
    }
  }, []);

  const value: AuthContextValue = {
    ...state,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
