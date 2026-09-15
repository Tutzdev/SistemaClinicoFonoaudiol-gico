import {
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from "react";
import {
  AlertCircle,
  ArrowRight,
  ChevronLeft,
  ChevronRight,
  LoaderCircle,
  X,
} from "lucide-react";
import { api, ApiError } from "../api";
import { dateTime } from "../format";
import type { Appointment, Page } from "../types";

function ConflictingAppointments({ ids }: { ids: string[] }) {
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  useEffect(() => {
    let current = true;
    Promise.all(ids.map((id) => api<Appointment>(`/appointments/${id}`)))
      .then((rows) => {
        if (current) setAppointments(rows);
      })
      .catch(() => {
        if (current) setFailed(true);
      })
      .finally(() => {
        if (current) setLoading(false);
      });
    return () => {
      current = false;
    };
  }, [ids]);
  if (loading) return <p>Consultando agendamentos afetados…</p>;
  return (
    <div className="conflicting-appointments">
      <p>Resolva estes agendamentos antes de continuar:</p>
      {appointments.length ? (
        <ul>
          {appointments.map((a) => (
            <li key={a.id}>
              <strong>{a.patientName}</strong> · {dateTime(a.start)} ·{" "}
              {a.professionalName}
            </li>
          ))}
        </ul>
      ) : null}
      {failed ? (
        <p>
          Não foi possível carregar os detalhes. Consulte a agenda para revisar
          as consultas futuras.
        </p>
      ) : null}
      <a className="text-link" href="/admin/agenda">
        Abrir agenda <ArrowRight size={16} aria-hidden />
      </a>
    </div>
  );
}

export function Button({
  variant = "primary",
  className = "",
  busy,
  children,
  disabled,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
  busy?: boolean;
}) {
  return (
    <button
      className={`button button-${variant} ${className}`}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      {...props}
    >
      {busy ? <LoaderCircle className="spin" size={17} aria-hidden /> : null}
      {children}
    </button>
  );
}
export function Brand({
  compact = false,
  light = false,
  name = "Espaço Sinapse",
}: {
  compact?: boolean;
  light?: boolean;
  name?: string;
}) {
  return (
    <span className={`brand ${light ? "brand-light" : ""}`}>
      <svg
        className="brand-symbol"
        width="40"
        height="40"
        viewBox="0 0 64 64"
        aria-hidden="true"
      >
        <path
          d="M9 35C9 19 20 9 35 9h18v15H35c-7 0-11 4-11 11v20H9Z"
          fill="currentColor"
        />
        <path
          d="M55 29c0 16-11 26-26 26H11V40h18c7 0 11-4 11-11V9h15Z"
          fill="var(--accent)"
        />
      </svg>
      <span className="brand-words">
        <span>
          {name === "Espaço Sinapse" ? (
            <>
              espaço <strong>sinapse</strong>
            </>
          ) : (
            name
          )}
        </span>
        {!compact ? <small>FONOAUDIOLOGIA</small> : null}
      </span>
    </span>
  );
}
export function Loading({ full = false }: { full?: boolean }) {
  return (
    <div className={`loading ${full ? "loading-full" : ""}`} role="status">
      <LoaderCircle className="spin" size={22} aria-hidden />
      <span>Carregando…</span>
    </div>
  );
}
export function ErrorNotice({
  error,
  onRetry,
}: {
  error: Error | string | null;
  onRetry?: () => void;
}) {
  if (!error) return null;
  const message = typeof error === "string" ? error : error.message;
  const fields = error instanceof ApiError ? Object.entries(error.fields) : [];
  return (
    <div className="error-notice" role="alert">
      <AlertCircle size={19} aria-hidden />
      <div>
        <p>{message}</p>
        {fields.length ? (
          <ul>
            {fields.map(([key, value]) => (
              <li key={key}>{value}</li>
            ))}
          </ul>
        ) : null}
        {error instanceof ApiError && error.conflictingAppointmentIds.length ? (
          <ConflictingAppointments ids={error.conflictingAppointmentIds} />
        ) : null}
        {onRetry ? (
          <Button variant="secondary" onClick={onRetry}>
            Tentar novamente
          </Button>
        ) : null}
      </div>
    </div>
  );
}
export function Notice({ children }: { children: ReactNode }) {
  return (
    <div className="notice" role="status">
      {children}
    </div>
  );
}
export function Empty({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty">
      <span className="empty-mark" aria-hidden="true">
        <ArrowRight size={22} />
      </span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  );
}
export function Modal({
  title,
  description,
  children,
  onClose,
  wide = false,
}: {
  title: string;
  description?: string;
  children: ReactNode;
  onClose: () => void;
  wide?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const titleId = useId();
  const descriptionId = useId();
  useLayoutEffect(() => {
    const opener = document.activeElement as HTMLElement | null;
    const dialog = ref.current;
    dialog?.showModal();
    return () => {
      dialog?.close();
      queueMicrotask(() => {
        if (opener?.isConnected) opener.focus();
      });
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className={`modal ${wide ? "modal-wide" : ""}`}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      tabIndex={-1}
      onKeyDown={(event) => {
        if (event.key !== "Tab") return;
        const focusable = Array.from(
          event.currentTarget.querySelectorAll<HTMLElement>(
            'button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
          ),
        ).filter((element) => element.getClientRects().length > 0);
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (!first) {
          event.preventDefault();
          event.currentTarget.focus();
          return;
        }
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }}
      onCancel={(e) => {
        e.preventDefault();
        onClose();
      }}
    >
      <header className="modal-header">
        <div>
          <h2 id={titleId}>{title}</h2>
          {description ? <p id={descriptionId}>{description}</p> : null}
        </div>
        <Button
          variant="ghost"
          className="icon-button"
          onClick={onClose}
          aria-label="Fechar janela"
        >
          <X size={20} aria-hidden />
        </Button>
      </header>
      <div className="modal-body">{children}</div>
    </dialog>
  );
}
type FieldProps = { label: string; hint?: string; error?: string };
export function Field({
  label,
  hint,
  error,
  id: givenId,
  className = "",
  required,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & FieldProps) {
  const generatedId = useId();
  const id = givenId || generatedId;
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>
        {label}
        {required ? <span className="required"> *</span> : null}
      </label>
      <input
        id={id}
        required={required}
        aria-invalid={Boolean(error)}
        aria-describedby={
          error ? `${id}-error` : hint ? `${id}-hint` : undefined
        }
        {...props}
      />
      {hint ? <small id={`${id}-hint`}>{hint}</small> : null}
      {error ? (
        <small id={`${id}-error`} className="field-error">
          {error}
        </small>
      ) : null}
    </div>
  );
}
export function SelectField({
  label,
  hint,
  error,
  children,
  id: givenId,
  className = "",
  required,
  ...props
}: SelectHTMLAttributes<HTMLSelectElement> & FieldProps) {
  const generatedId = useId();
  const id = givenId || generatedId;
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>
        {label}
        {required ? <span className="required"> *</span> : null}
      </label>
      <select
        id={id}
        required={required}
        aria-invalid={Boolean(error)}
        aria-describedby={
          error ? `${id}-error` : hint ? `${id}-hint` : undefined
        }
        {...props}
      >
        {children}
      </select>
      {hint ? <small id={`${id}-hint`}>{hint}</small> : null}
      {error ? (
        <small id={`${id}-error`} className="field-error">
          {error}
        </small>
      ) : null}
    </div>
  );
}
export function TextareaField({
  label,
  hint,
  error,
  id: givenId,
  className = "",
  ...props
}: TextareaHTMLAttributes<HTMLTextAreaElement> & FieldProps) {
  const generatedId = useId();
  const id = givenId || generatedId;
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>{label}</label>
      <textarea
        id={id}
        rows={4}
        aria-invalid={Boolean(error)}
        aria-describedby={
          error ? `${id}-error` : hint ? `${id}-hint` : undefined
        }
        {...props}
      />
      {hint ? <small id={`${id}-hint`}>{hint}</small> : null}
      {error ? (
        <small id={`${id}-error`} className="field-error">
          {error}
        </small>
      ) : null}
    </div>
  );
}
export function Check({
  label,
  hint,
  ...props
}: Omit<InputHTMLAttributes<HTMLInputElement>, "type"> & FieldProps) {
  const id = useId();
  return (
    <div className="checkbox-field">
      <input
        id={id}
        type="checkbox"
        aria-describedby={hint ? `${id}-hint` : undefined}
        {...props}
      />
      <div>
        <label htmlFor={id}>{label}</label>
        {hint ? <small id={`${id}-hint`}>{hint}</small> : null}
      </div>
    </div>
  );
}
export function Pagination({
  page,
  onChange,
}: {
  page: Page<unknown>;
  onChange: (page: number) => void;
}) {
  return (
    <nav className="pagination" aria-label="Paginação">
      <span>
        {page.totalElements} registro{page.totalElements !== 1 ? "s" : ""} ·
        Página {page.number + 1} de {Math.max(1, page.totalPages)}
      </span>
      <div>
        <Button
          variant="secondary"
          disabled={page.number === 0}
          onClick={() => onChange(page.number - 1)}
          aria-label="Página anterior"
        >
          <ChevronLeft size={17} aria-hidden />
        </Button>
        <Button
          variant="secondary"
          disabled={page.number + 1 >= page.totalPages}
          onClick={() => onChange(page.number + 1)}
          aria-label="Próxima página"
        >
          <ChevronRight size={17} aria-hidden />
        </Button>
      </div>
    </nav>
  );
}
export function PageHeader({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow?: string;
  title: string;
  description: string;
  children?: ReactNode;
}) {
  return (
    <header className="page-heading">
      <div>
        {eyebrow ? <p className="eyebrow">{eyebrow}</p> : null}
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {children ? <div className="page-actions">{children}</div> : null}
    </header>
  );
}
export function Status({ active }: { active: boolean }) {
  return (
    <span className={`badge ${active ? "badge-success" : "badge-neutral"}`}>
      {active ? "Ativo" : "Inativo"}
    </span>
  );
}
