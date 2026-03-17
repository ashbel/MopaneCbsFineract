import axios, { type AxiosInstance } from 'axios';

const baseURL =
  import.meta.env.VITE_BASE_API_URL ?? '/fineract-provider/api/v1';
const tenantIdentifier =
  import.meta.env.VITE_TENANT_IDENTIFIER ?? 'default';

let authKey: string | null = null;

export function setAuthKey(key: string | null): void {
  authKey = key;
}

export function getAuthKey(): string | null {
  return authKey;
}

function createClient(): AxiosInstance {
  const client = axios.create({
    baseURL,
    headers: { 'Content-Type': 'application/json' },
  });

  client.interceptors.request.use((config) => {
    const params = { ...config.params, tenantIdentifier };
    config.params = params;
    if (authKey) {
      config.headers.Authorization = `Basic ${authKey}`;
    }
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }
    return config;
  });

  client.interceptors.response.use(
    (r) => r,
    (err) => {
      if (err.response?.status === 401) {
        setAuthKey(null);
        window.dispatchEvent(new CustomEvent('auth:logout'));
      }
      return Promise.reject(err);
    }
  );

  return client;
}

export const apiClient = createClient();
export { tenantIdentifier };
