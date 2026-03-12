// ── Types matching the Spring Boot API responses ─────────────────────────────

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
}

export type FileStatus =
  | 'PENDING_UPLOAD'
  | 'UPLOADED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED';

export interface FileRecord {
  id: string;
  originalName: string;
  contentType: string;
  sizeBytes: number | null;
  outputSizeBytes: number | null;
  status: FileStatus;
  uploadUrl?: string;
  uploadUrlExpiresInMinutes?: number;
  createdAt: string;
  updatedAt: string;
}

export interface DownloadResponse {
  fileId: string;
  downloadUrl: string;
  expiresInMinutes: number;
  isCompressedOutput: boolean;
}

export interface CreateFileRequest {
  originalName: string;
  contentType: string;
  sizeBytes: number;
}

// ── Core fetch wrapper ────────────────────────────────────────────────────────

const BASE = '/api/v1';

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(BASE + path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(),
      ...(init?.headers ?? {}),
    },
  });

  if (res.status === 401) {
    localStorage.removeItem('token');
    window.location.href = '/';
    throw new Error('Session expired');
  }

  if (res.status === 204) return undefined as T;

  const body = await res.json();
  if (!body.success) throw new Error(body.message ?? 'Request failed');
  return body.data as T;
}

// ── API client ────────────────────────────────────────────────────────────────

export const api = {
  auth: {
    register: (email: string, password: string) =>
      request<AuthResponse>('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),

    login: (email: string, password: string) =>
      request<AuthResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),
  },

  files: {
    create: (req: CreateFileRequest) =>
      request<FileRecord>('/files', {
        method: 'POST',
        body: JSON.stringify(req),
      }),

    list: () => request<FileRecord[]>('/files'),

    get: (id: string) => request<FileRecord>(`/files/${id}`),

    confirmUpload: (id: string) =>
      request<FileRecord>(`/files/${id}/confirm-upload`, { method: 'POST' }),

    getDownloadUrl: (id: string) =>
      request<DownloadResponse>(`/files/${id}/download`),

    delete: (id: string) =>
      request<void>(`/files/${id}`, { method: 'DELETE' }),
  },
};

// ── Direct S3 upload (presigned PUT — no auth header) ─────────────────────────

export async function uploadToS3(
  presignedUrl: string,
  file: File
): Promise<void> {
  const res = await fetch(presignedUrl, {
    method: 'PUT',
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    body: file,
  });
  if (!res.ok) throw new Error(`S3 upload failed: ${res.status}`);
}
