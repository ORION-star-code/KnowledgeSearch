import type { ApiResponse } from '../types';

export class ApiError extends Error {
  code: string;

  constructor(code: string, message: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
  }
}

type QueryValue = string | number | boolean | null | undefined | Array<string | number | boolean>;

const API_BASE = '/api';

function buildQuery(params?: Record<string, QueryValue>) {
  const search = new URLSearchParams();
  Object.entries(params ?? {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    if (Array.isArray(value)) {
      value.forEach((item) => search.append(key, String(item)));
      return;
    }
    search.append(key, String(value));
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: {
        Accept: 'application/json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers
      },
      ...init
    });
  } catch (err) {
    throw new ApiError('NETWORK_ERROR', err instanceof Error ? err.message : '网络请求失败');
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (!contentType.includes('application/json')) {
    if (!response.ok) {
      const text = await response.text();
      const proxyFailed = text.includes('Error occurred while trying to proxy') || response.status === 500;
      throw new ApiError(
        `HTTP_${response.status}`,
        proxyFailed ? '后端未响应，请启动 8080 端口' : text || response.statusText || '接口请求失败'
      );
    }
    return undefined as T;
  }

  const envelope = (await response.json()) as ApiResponse<T>;
  if (!response.ok) {
    throw new ApiError(`HTTP_${response.status}`, envelope.message || response.statusText);
  }
  if (!envelope.success) {
    throw new ApiError(envelope.code || 'API_ERROR', envelope.message || '接口返回失败');
  }
  return envelope.data;
}

export const apiClient = {
  get<T>(path: string, params?: Record<string, QueryValue>) {
    return request<T>(`${path}${buildQuery(params)}`, { method: 'GET' });
  },
  post<T>(path: string, body?: unknown) {
    return request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) });
  },
  put<T>(path: string, body?: unknown) {
    return request<T>(path, { method: 'PUT', body: body === undefined ? undefined : JSON.stringify(body) });
  },
  delete<T>(path: string) {
    return request<T>(path, { method: 'DELETE' });
  }
};
