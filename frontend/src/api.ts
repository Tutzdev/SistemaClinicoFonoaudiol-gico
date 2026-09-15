import { useCallback, useEffect, useState } from "react";
import type { Page } from "./types";

type Problem = {
  detail?: string;
  title?: string;
  errors?: Record<string, string> | { field: string; message: string }[];
  fieldErrors?: Record<string, string>;
  conflictingAppointmentIds?: string[];
};
export class ApiError extends Error {
  status: number;
  fields: Record<string, string>;
  conflictingAppointmentIds: string[];
  constructor(status: number, problem: Problem) {
    super(
      problem.detail ||
        problem.title ||
        "Não foi possível concluir. Tente novamente.",
    );
    this.status = status;
    this.conflictingAppointmentIds = problem.conflictingAppointmentIds || [];
    this.fields = Array.isArray(problem.errors)
      ? Object.fromEntries(problem.errors.map((e) => [e.field, e.message]))
      : problem.fieldErrors || problem.errors || {};
  }
}
let csrf: { token: string; headerName: string } | null = null;
let csrfPromise: Promise<void> | null = null;
export function resetCsrf() {
  csrf = null;
  csrfPromise = null;
}
async function ensureCsrf() {
  if (csrf) return;
  if (!csrfPromise)
    csrfPromise = fetch("/api/v1/auth/csrf", { credentials: "same-origin" })
      .then(async (response) => {
        if (!response.ok)
          throw new Error(
            "Não foi possível iniciar uma conexão segura. Tente novamente.",
          );
        csrf = await response.json();
      })
      .finally(() => {
        csrfPromise = null;
      });
  return csrfPromise;
}
export async function api<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    await ensureCsrf();
    if (csrf) headers[csrf.headerName] = csrf.token;
  }
  let response: Response;
  try {
    response = await fetch(`/api/v1${path}`, {
      method,
      headers,
      credentials: "same-origin",
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new Error(
      "Não foi possível conectar. Verifique sua conexão e tente novamente.",
    );
  }
  if (!response.ok) {
    const problem: Problem = await response.json().catch(() => ({
      detail: "O serviço está temporariamente indisponível. Tente novamente.",
    }));
    if (response.status === 401 && !path.startsWith("/auth/"))
      window.dispatchEvent(new Event("session-expired"));
    if (response.status === 403) resetCsrf();
    throw new ApiError(response.status, problem);
  }
  if (response.status === 204 || response.headers.get("content-length") === "0")
    return undefined as T;
  const content = await response.text();
  return content ? (JSON.parse(content) as T) : (undefined as T);
}
export function useResource<T>(path: string | null) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(Boolean(path));
  const [error, setError] = useState<Error | null>(null);
  const [revision, setRevision] = useState(0);
  const reload = useCallback(() => setRevision((v) => v + 1), []);
  useEffect(() => {
    let current = true;
    if (!path) {
      setData(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    api<T>(path)
      .then((value) => {
        if (current) setData(value);
      })
      .catch((err) => {
        if (current) setError(err as Error);
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => {
      current = false;
    };
  }, [path, revision]);
  return { data, loading, error, reload };
}
export async function allRecords<T>(path: string): Promise<T[]> {
  const separator = path.includes("?") ? "&" : "?";
  const first = await api<Page<T>>(`${path}${separator}size=100&page=0`);
  const rest = await Promise.all(
    Array.from({ length: Math.max(0, first.totalPages - 1) }, (_, i) =>
      api<Page<T>>(`${path}${separator}size=100&page=${i + 1}`),
    ),
  );
  return first.content.concat(...rest.map((p) => p.content));
}
export function useCollection<T>(path: string) {
  const [data, setData] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [revision, setRevision] = useState(0);
  const reload = useCallback(() => setRevision((v) => v + 1), []);
  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(null);
    allRecords<T>(path)
      .then((rows) => {
        if (current) setData(rows);
      })
      .catch((err) => {
        if (current) setError(err as Error);
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => {
      current = false;
    };
  }, [path, revision]);
  return { data, loading, error, reload };
}
