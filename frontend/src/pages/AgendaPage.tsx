import { useEffect, useState, type FormEvent } from "react";
import {
  ArrowRight,
  CalendarDays,
  CalendarPlus,
  Check,
  ChevronLeft,
  ChevronRight,
  Clock3,
  List,
  Plus,
  UserPlus,
} from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { allRecords, api, ApiError, useCollection, useResource } from "../api";
import {
  Button,
  Empty,
  ErrorNotice,
  Field,
  Loading,
  Modal,
  Notice,
  PageHeader,
  Pagination,
  SelectField,
} from "../components/ui";
import {
  dateTime,
  localDate,
  localInput,
  nextDay,
  statuses,
  time,
  timezone,
  withOffset,
} from "../format";
import type {
  Appointment,
  AppointmentHistory,
  AppointmentStatus,
  Page,
  Patient,
  Professional,
  Service,
} from "../types";
import { useAuth } from "./AdminApp";

function rangeQuery(from: string, to: string) {
  return `from=${encodeURIComponent(withOffset(`${from}T00:00`))}&to=${encodeURIComponent(withOffset(`${nextDay(to)}T00:00`))}`;
}
function dayLabel(day: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    weekday: "long",
    day: "numeric",
    month: "long",
    timeZone: "UTC",
  }).format(new Date(`${day}T12:00:00Z`));
}
function StatusBadge({ status }: { status: AppointmentStatus }) {
  return (
    <span className={`badge status-${status.toLowerCase()}`}>
      {statuses[status]}
    </span>
  );
}

function AppointmentForm({
  onClose,
  onSaved,
  appointment,
}: {
  onClose: () => void;
  onSaved: () => void;
  appointment?: Appointment;
}) {
  const [patients, setPatients] = useState<Patient[]>([]);
  const [professionals, setProfessionals] = useState<Professional[]>([]);
  const [services, setServices] = useState<Service[]>([]);
  const [patientId, setPatientId] = useState(appointment?.patientId || "");
  const [professionalId, setProfessionalId] = useState(
    appointment?.professionalId || "",
  );
  const [serviceId, setServiceId] = useState(appointment?.serviceId || "");
  const [start, setStart] = useState(
    appointment ? localInput(appointment.start) : "",
  );
  const [loading, setLoading] = useState(!appointment);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [catalogError, setCatalogError] = useState<Error | null>(null);
  async function load() {
    setLoading(true);
    setCatalogError(null);
    try {
      const [p, pro, s] = await Promise.all([
        allRecords<Patient>("/patients"),
        allRecords<Professional>("/professionals"),
        allRecords<Service>("/services"),
      ]);
      setPatients(p.filter((item) => item.active));
      setProfessionals(pro.filter((item) => item.active));
      setServices(s.filter((item) => item.active));
    } catch (err) {
      setCatalogError(err as Error);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    if (!appointment) void load();
  }, [appointment]);
  const linkedServices = services.filter((service) =>
    professionals
      .find((pro) => pro.id === professionalId)
      ?.serviceIds.includes(service.id),
  );
  const fieldError = (key: string) =>
    error instanceof ApiError ? error.fields[key] : undefined;
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (appointment)
        await api(`/appointments/${appointment.id}/reschedule`, "PATCH", {
          start: withOffset(start),
          version: appointment.version,
        });
      else
        await api("/appointments", "POST", {
          patientId,
          professionalId,
          serviceId,
          start: withOffset(start),
        });
      onSaved();
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  const missingCatalog =
    !appointment &&
    (!patients.length || !professionals.length || !services.length);
  return (
    <Modal
      title={appointment ? "Reagendar consulta" : "Novo agendamento"}
      description="Horários de Brasília. A disponibilidade será verificada ao salvar."
      onClose={onClose}
    >
      {loading ? (
        <Loading />
      ) : catalogError ? (
        <ErrorNotice error={catalogError} onRetry={() => void load()} />
      ) : (
        <form onSubmit={submit} className="form-stack">
          {appointment ? (
            <div className="appointment-summary">
              <strong>{appointment.patientName}</strong>
              <p>
                {appointment.serviceName} · {appointment.professionalName}
              </p>
              <small>Horário atual: {dateTime(appointment.start)}</small>
            </div>
          ) : (
            <>
              <SelectField
                label="Paciente"
                required
                value={patientId}
                onChange={(e) => setPatientId(e.target.value)}
                error={fieldError("patientId")}
              >
                <option value="">Selecione o paciente</option>
                {patients.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} · {p.phone}
                  </option>
                ))}
              </SelectField>
              <SelectField
                label="Profissional"
                required
                value={professionalId}
                onChange={(e) => {
                  setProfessionalId(e.target.value);
                  setServiceId("");
                }}
                error={fieldError("professionalId")}
              >
                <option value="">Selecione o profissional</option>
                {professionals.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </SelectField>
              <SelectField
                label="Serviço"
                required
                value={serviceId}
                disabled={!professionalId}
                onChange={(e) => setServiceId(e.target.value)}
                error={fieldError("serviceId")}
                hint={
                  professionalId && !linkedServices.length
                    ? "Este profissional não tem serviços ativos vinculados. A administração pode atualizar seu cadastro."
                    : undefined
                }
              >
                <option value="">Selecione o serviço</option>
                {linkedServices.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} · {s.durationMinutes} min
                  </option>
                ))}
              </SelectField>
            </>
          )}
          {missingCatalog ? (
            <Notice>
              Cadastre ao menos um paciente, um profissional e um serviço ativo
              antes de agendar.
            </Notice>
          ) : null}
          <Field
            label={appointment ? "Nova data e horário" : "Data e horário"}
            type="datetime-local"
            required
            value={start}
            min={`${localDate()}T00:00`}
            onChange={(e) => setStart(e.target.value)}
            error={fieldError("start")}
          />
          <p className="form-hint">
            O horário de término é calculado pela duração do serviço. Consultas
            confirmadas retornam ao status “Agendado” quando reagendadas.
          </p>
          <ErrorNotice error={error} />
          <div className="form-actions">
            <Button
              type="button"
              variant="secondary"
              disabled={busy}
              onClick={onClose}
            >
              Cancelar
            </Button>
            <Button busy={busy} disabled={missingCatalog}>
              {appointment ? "Salvar novo horário" : "Criar agendamento"}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
function AppointmentDetails({
  appointment,
  onClose,
  onChanged,
  onReschedule,
}: {
  appointment: Appointment;
  onClose: () => void;
  onChanged: () => void;
  onReschedule: (appointment: Appointment) => void;
}) {
  const [current, setCurrent] = useState(appointment);
  const history = useResource<AppointmentHistory[]>(
    `/appointments/${appointment.id}/history`,
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [selected, setSelected] = useState<AppointmentStatus | null>(null);
  const [notice, setNotice] = useState("");
  const active =
    current.status === "AGENDADO" || current.status === "CONFIRMADO";
  const ended = new Date(current.end).getTime() <= Date.now();
  async function changeStatus() {
    if (!selected) return;
    setBusy(true);
    setError(null);
    try {
      const updated = await api<Appointment>(
        `/appointments/${current.id}/status`,
        "PATCH",
        { status: selected, version: current.version },
      );
      setCurrent(
        updated || (await api<Appointment>(`/appointments/${current.id}`)),
      );
      setNotice("Status atualizado.");
      setSelected(null);
      history.reload();
      onChanged();
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  const actionLabels: Record<string, string> = {
    CREATED: "Agendamento criado",
    RESCHEDULED: "Consulta reagendada",
    STATUS_CHANGED: "Status alterado",
    CREATE: "Agendamento criado",
    RESCHEDULE: "Consulta reagendada",
    STATUS_CHANGE: "Status alterado",
  };
  return (
    <Modal
      title="Detalhes do agendamento"
      description={current.patientName}
      onClose={onClose}
      wide
    >
      {notice ? <Notice>{notice}</Notice> : null}
      <dl className="detail-grid">
        <div>
          <dt>Serviço</dt>
          <dd>{current.serviceName}</dd>
        </div>
        <div>
          <dt>Profissional</dt>
          <dd>{current.professionalName}</dd>
        </div>
        <div>
          <dt>Início</dt>
          <dd>{dateTime(current.start)}</dd>
        </div>
        <div>
          <dt>Término</dt>
          <dd>
            {dateTime(current.end)} · {current.durationMinutes} min
          </dd>
        </div>
        <div>
          <dt>Situação</dt>
          <dd>
            <StatusBadge status={current.status} />
          </dd>
        </div>
      </dl>
      {active ? (
        <div className="appointment-actions">
          {current.status === "AGENDADO" ? (
            <Button
              variant="secondary"
              onClick={() => setSelected("CONFIRMADO")}
            >
              <Check size={17} aria-hidden />
              Confirmar consulta
            </Button>
          ) : null}
          <Button variant="secondary" onClick={() => onReschedule(current)}>
            Reagendar
          </Button>
          {ended ? (
            <>
              <Button
                variant="secondary"
                onClick={() => setSelected("CONCLUIDO")}
              >
                Marcar como concluído
              </Button>
              <Button
                variant="secondary"
                onClick={() => setSelected("NAO_COMPARECEU")}
              >
                Não compareceu
              </Button>
            </>
          ) : null}
          <Button
            variant="ghost"
            className="delete-action"
            onClick={() => setSelected("CANCELADO")}
          >
            Cancelar consulta
          </Button>
        </div>
      ) : (
        <p className="muted">
          Este agendamento foi finalizado e não pode ser editado.
        </p>
      )}
      {selected ? (
        <div className="inline-confirm">
          <p>
            Alterar o status para <strong>{statuses[selected]}</strong>?
          </p>
          {selected === "CANCELADO" ? (
            <p>
              O horário será liberado. O registro e o histórico serão
              preservados.
            </p>
          ) : null}
          <ErrorNotice error={error} />
          <div className="row-actions">
            <Button
              variant="secondary"
              disabled={busy}
              onClick={() => setSelected(null)}
            >
              Voltar
            </Button>
            <Button
              variant={selected === "CANCELADO" ? "danger" : "primary"}
              busy={busy}
              onClick={() => void changeStatus()}
            >
              Confirmar alteração
            </Button>
          </div>
        </div>
      ) : null}
      <section className="appointment-history">
        <h3>Histórico do agendamento</h3>
        {history.loading ? (
          <Loading />
        ) : history.error ? (
          <ErrorNotice error={history.error} onRetry={history.reload} />
        ) : history.data?.length ? (
          <ol className="timeline">
            {history.data.map((item) => (
              <li key={item.id}>
                <strong>
                  {actionLabels[item.action] || "Agendamento atualizado"}
                </strong>
                <p>
                  {item.previousStart &&
                  item.newStart &&
                  item.previousStart !== item.newStart
                    ? `${dateTime(item.previousStart)} → ${dateTime(item.newStart)}`
                    : item.newStatus
                      ? `${item.previousStatus ? `${statuses[item.previousStatus]} → ` : ""}${statuses[item.newStatus]}`
                      : item.newStart
                        ? dateTime(item.newStart)
                        : ""}
                </p>
                <small>
                  {dateTime(item.createdAt)} · {item.actorName}
                </small>
              </li>
            ))}
          </ol>
        ) : (
          <p className="muted">Nenhuma alteração registrada.</p>
        )}
      </section>
    </Modal>
  );
}
function AppointmentRows({
  rows,
  onSelect,
  compact = false,
}: {
  rows: Appointment[];
  onSelect: (a: Appointment) => void;
  compact?: boolean;
}) {
  return (
    <div
      className={`appointment-list ${compact ? "compact-appointments" : ""}`}
    >
      {rows.map((a) => (
        <article
          key={a.id}
          className={`appointment-row appointment-${a.status.toLowerCase()}`}
        >
          <div className="appointment-time">
            <strong>{time(a.start)}</strong>
            <span>{time(a.end)}</span>
          </div>
          <div className="appointment-patient">
            <button onClick={() => onSelect(a)}>{a.patientName}</button>
            <span>{a.serviceName}</span>
          </div>
          <div className="appointment-professional">
            {a.professionalName}
            <small>{a.durationMinutes} min</small>
          </div>
          <StatusBadge status={a.status} />
          <Button
            variant="ghost"
            aria-label={`Ver agendamento de ${a.patientName} às ${time(a.start)}`}
            onClick={() => onSelect(a)}
          >
            <ArrowRight size={18} aria-hidden />
            <span className="mobile-action-label">Detalhes</span>
          </Button>
        </article>
      ))}
    </div>
  );
}

export function Dashboard() {
  const { user } = useAuth();
  const today = localDate();
  const resource = useCollection<Appointment>(
    `/appointments?${rangeQuery(today, today)}`,
  );
  const [details, setDetails] = useState<Appointment | null>(null);
  const [editing, setEditing] = useState<Appointment | null>(null);
  useEffect(() => {
    document.title = "Visão geral · Espaço Sinapse";
  }, []);
  const now = Date.now();
  const upcoming = resource.data
    .filter(
      (a) =>
        (a.status === "AGENDADO" || a.status === "CONFIRMADO") &&
        new Date(a.end).getTime() > now,
    )
    .sort((a, b) => a.start.localeCompare(b.start));
  const counts = Object.keys(statuses).map((key) => ({
    key,
    label: statuses[key as AppointmentStatus],
    count: resource.data.filter((a) => a.status === key).length,
  }));
  return (
    <>
      <PageHeader
        eyebrow="VISÃO GERAL"
        title={`Olá, ${user.name.split(" ")[0]}.`}
        description="Veja o dia da clínica e organize os próximos atendimentos."
      >
        <Link to="/admin/agenda?novo=1" className="button button-primary">
          <Plus size={18} aria-hidden />
          Novo agendamento
        </Link>
      </PageHeader>
      <div className="dashboard-date">
        <CalendarDays size={19} aria-hidden />
        <span>{dayLabel(today)}</span>
        <span>Horário de Brasília</span>
      </div>
      {resource.error ? (
        <ErrorNotice error={resource.error} onRetry={resource.reload} />
      ) : resource.loading ? (
        <Loading />
      ) : (
        <>
          <section
            className="daily-overview"
            aria-label="Agendamentos de hoje por status"
          >
            <div className="daily-total">
              <span>Agendamentos hoje</span>
              <strong>{resource.data.length}</strong>
            </div>
            <div className="daily-counts">
              {counts.map((c) => (
                <div key={c.key}>
                  <span>{c.label}</span>
                  <strong>{c.count}</strong>
                </div>
              ))}
            </div>
          </section>
          <div className="dashboard-columns">
            <section className="data-panel upcoming-panel">
              <header className="panel-heading">
                <div>
                  <h2>Próximos atendimentos</h2>
                  <p>Agenda de hoje</p>
                </div>
                <Link to="/admin/agenda" className="text-link">
                  Ver agenda <ArrowRight size={16} aria-hidden />
                </Link>
              </header>
              {upcoming.length ? (
                <AppointmentRows
                  rows={upcoming}
                  onSelect={setDetails}
                  compact
                />
              ) : (
                <Empty
                  title="Nenhum atendimento por vir hoje"
                  description="Consulte a agenda completa ou crie um novo agendamento."
                  action={
                    <Link
                      className="button button-secondary"
                      to="/admin/agenda"
                    >
                      Abrir agenda
                    </Link>
                  }
                />
              )}
            </section>
            <aside
              className="quick-actions"
              aria-label="Atalhos para organizar o atendimento"
            >
              <h2>Comece por aqui</h2>
              <Link to="/admin/pacientes?novo=1">
                <span className="quick-icon">
                  <UserPlus size={21} aria-hidden />
                </span>
                <span>
                  <strong>Cadastrar paciente</strong>
                  <small>Contato e responsável</small>
                </span>
                <ArrowRight size={17} aria-hidden />
              </Link>
              <Link to="/admin/agenda?novo=1">
                <span className="quick-icon">
                  <CalendarPlus size={21} aria-hidden />
                </span>
                <span>
                  <strong>Agendar atendimento</strong>
                  <small>Paciente, serviço e horário</small>
                </span>
                <ArrowRight size={17} aria-hidden />
              </Link>
              <div className="quick-note">
                <Clock3 size={20} aria-hidden />
                <p>
                  Os horários da agenda seguem o fuso de Brasília, mesmo ao
                  acessar de outra região.
                </p>
              </div>
            </aside>
          </div>
        </>
      )}
      {details ? (
        <AppointmentDetails
          appointment={details}
          onClose={() => setDetails(null)}
          onChanged={resource.reload}
          onReschedule={(current) => {
            setEditing(current);
            setDetails(null);
          }}
        />
      ) : null}
      {editing ? (
        <AppointmentForm
          appointment={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            resource.reload();
          }}
        />
      ) : null}
    </>
  );
}

export function AgendaPage() {
  const today = localDate();
  const [params, setParams] = useSearchParams();
  const [day, setDay] = useState(today);
  const [from, setFrom] = useState(today);
  const [to, setTo] = useState(today);
  const [view, setView] = useState<"day" | "list">("day");
  const [status, setStatus] = useState("");
  const [professionalId, setProfessionalId] = useState("");
  const [patientId, setPatientId] = useState("");
  const [page, setPage] = useState(0);
  const [create, setCreate] = useState(false);
  const [editing, setEditing] = useState<Appointment | null>(null);
  const [details, setDetails] = useState<Appointment | null>(null);
  const [notice, setNotice] = useState("");
  const professionals = useCollection<Professional>("/professionals");
  const patients = useCollection<Patient>("/patients");
  const resource = useResource<Page<Appointment>>(
    `/appointments?${rangeQuery(view === "day" ? day : from, view === "day" ? day : to)}${status ? `&status=${status}` : ""}${professionalId ? `&professionalId=${professionalId}` : ""}${patientId ? `&patientId=${patientId}` : ""}&page=${page}&size=20`,
  );
  useEffect(() => {
    document.title = "Agenda · Espaço Sinapse";
  }, []);
  useEffect(() => {
    if (params.has("novo")) {
      setCreate(true);
      setParams({}, { replace: true });
    }
  }, [params, setParams]);
  const changeDay = (newDay: string) => {
    setDay(newDay);
    setPage(0);
  };
  const sorted =
    resource.data?.content
      .slice()
      .sort((a, b) => a.start.localeCompare(b.start)) || [];
  return (
    <>
      <PageHeader
        eyebrow="ATENDIMENTOS"
        title="Agenda"
        description="Organize consultas, confirme presenças e acompanhe alterações."
      >
        <Button onClick={() => setCreate(true)}>
          <Plus size={18} aria-hidden />
          Novo agendamento
        </Button>
      </PageHeader>
      {notice ? <Notice>{notice}</Notice> : null}
      <section className="data-panel agenda-panel">
        <div className="agenda-toolbar">
          <div
            className="view-switch"
            role="group"
            aria-label="Visualização da agenda"
          >
            <Button
              variant="ghost"
              aria-pressed={view === "day"}
              onClick={() => {
                setView("day");
                setPage(0);
              }}
            >
              <CalendarDays size={17} aria-hidden />
              Dia
            </Button>
            <Button
              variant="ghost"
              aria-pressed={view === "list"}
              onClick={() => {
                setView("list");
                setPage(0);
              }}
            >
              <List size={17} aria-hidden />
              Lista
            </Button>
          </div>
          {view === "day" ? (
            <div className="date-navigation">
              <Button
                variant="secondary"
                aria-label="Dia anterior"
                onClick={() => changeDay(nextDay(day, -1))}
              >
                <ChevronLeft size={17} aria-hidden />
              </Button>
              <input
                type="date"
                aria-label="Data da agenda"
                value={day}
                required
                onChange={(e) => {
                  if (e.target.value) changeDay(e.target.value);
                }}
              />
              <Button
                variant="secondary"
                aria-label="Próximo dia"
                onClick={() => changeDay(nextDay(day))}
              >
                <ChevronRight size={17} aria-hidden />
              </Button>
              <Button variant="ghost" onClick={() => changeDay(today)}>
                Hoje
              </Button>
            </div>
          ) : (
            <div className="range-filter">
              <Field
                label="De"
                type="date"
                required
                value={from}
                onChange={(e) => {
                  if (e.target.value) {
                    setFrom(e.target.value);
                    if (e.target.value > to) setTo(e.target.value);
                    setPage(0);
                  }
                }}
              />
              <Field
                label="Até"
                type="date"
                required
                min={from}
                value={to}
                onChange={(e) => {
                  if (e.target.value) {
                    setTo(e.target.value);
                    setPage(0);
                  }
                }}
              />
            </div>
          )}
        </div>
        <div className="agenda-filters">
          <SelectField
            label="Profissional"
            value={professionalId}
            onChange={(e) => {
              setProfessionalId(e.target.value);
              setPage(0);
            }}
          >
            <option value="">Todos os profissionais</option>
            {professionals.data.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </SelectField>
          <SelectField
            label="Paciente"
            value={patientId}
            onChange={(e) => {
              setPatientId(e.target.value);
              setPage(0);
            }}
          >
            <option value="">Todos os pacientes</option>
            {patients.data.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </SelectField>
          <SelectField
            label="Situação"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">Todas as situações</option>
            {Object.entries(statuses).map(([key, name]) => (
              <option key={key} value={key}>
                {name}
              </option>
            ))}
          </SelectField>
        </div>
        {professionals.error ? (
          <ErrorNotice
            error={professionals.error}
            onRetry={professionals.reload}
          />
        ) : null}
        {patients.error ? (
          <ErrorNotice error={patients.error} onRetry={patients.reload} />
        ) : null}
        <header className="agenda-date-heading">
          <h2>{view === "day" ? dayLabel(day) : "Agendamentos no período"}</h2>
          <span>
            {resource.data?.totalElements || 0} agendamentos · Brasília
          </span>
        </header>
        {resource.loading ? (
          <Loading />
        ) : resource.error ? (
          <ErrorNotice error={resource.error} onRetry={resource.reload} />
        ) : sorted.length ? (
          <>
            {view === "day" ? (
              <AppointmentRows rows={sorted} onSelect={setDetails} />
            ) : (
              <div className="agenda-list-view">
                {sorted.map((a) => (
                  <div key={a.id}>
                    <span className="list-date">
                      {new Intl.DateTimeFormat("pt-BR", {
                        day: "2-digit",
                        month: "short",
                        timeZone: timezone,
                      }).format(new Date(a.start))}
                    </span>
                    <AppointmentRows rows={[a]} onSelect={setDetails} />
                  </div>
                ))}
              </div>
            )}
            {resource.data ? (
              <Pagination page={resource.data} onChange={setPage} />
            ) : null}
          </>
        ) : (
          <Empty
            title="Nenhum agendamento neste período"
            description="Escolha outra data, ajuste os filtros ou agende um atendimento."
            action={
              <Button variant="secondary" onClick={() => setCreate(true)}>
                <Plus size={16} aria-hidden />
                Novo agendamento
              </Button>
            }
          />
        )}
      </section>
      {create || editing ? (
        <AppointmentForm
          appointment={editing || undefined}
          onClose={() => {
            setCreate(false);
            setEditing(null);
          }}
          onSaved={() => {
            setNotice(editing ? "Consulta reagendada." : "Agendamento criado.");
            setCreate(false);
            setEditing(null);
            resource.reload();
          }}
        />
      ) : null}
      {details ? (
        <AppointmentDetails
          appointment={details}
          onClose={() => setDetails(null)}
          onChanged={resource.reload}
          onReschedule={(current) => {
            setEditing(current);
            setDetails(null);
          }}
        />
      ) : null}
    </>
  );
}
