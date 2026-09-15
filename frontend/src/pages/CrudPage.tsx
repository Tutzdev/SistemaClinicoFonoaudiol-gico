import { useEffect, useState, type FormEvent } from "react";
import {
  CalendarDays,
  Clock3,
  History,
  Pencil,
  Plus,
  Search,
  Trash2,
} from "lucide-react";
import { useSearchParams } from "react-router-dom";
import { allRecords, api, ApiError, useResource } from "../api";
import {
  Button,
  Check,
  Empty,
  ErrorNotice,
  Field,
  Loading,
  Modal,
  Notice,
  PageHeader,
  Pagination,
  SelectField,
  Status,
  TextareaField,
} from "../components/ui";
import {
  date,
  dateTime,
  isMinor,
  localDate,
  statuses,
  withOffset,
} from "../format";
import type {
  Appointment,
  BaseRecord,
  Block,
  Page,
  Patient,
  Period,
  Professional,
  RecordKind,
  Service,
  User,
} from "../types";
import { useAuth } from "./AdminApp";

type EditableRecord = BaseRecord &
  Partial<
    Omit<Patient, keyof BaseRecord> &
      Omit<Professional, keyof BaseRecord> &
      Omit<Service, keyof BaseRecord> &
      Omit<User, keyof BaseRecord>
  >;
const descriptions: Record<
  RecordKind,
  { title: string; singular: string; action: string; description: string }
> = {
  patients: {
    title: "Pacientes",
    singular: "paciente",
    action: "Novo paciente",
    description: "Dados de contato, responsáveis e histórico de agendamentos.",
  },
  professionals: {
    title: "Profissionais",
    singular: "profissional",
    action: "Novo profissional",
    description:
      "Organize os profissionais, serviços e horários de atendimento.",
  },
  services: {
    title: "Serviços",
    singular: "serviço",
    action: "Novo serviço",
    description: "Configure a duração dos atendimentos e o conteúdo publicado.",
  },
  users: {
    title: "Equipe e acessos",
    singular: "usuário",
    action: "Novo usuário",
    description: "Gerencie quem pode acessar a clínica e suas permissões.",
  },
};
type FormValues = {
  name: string;
  birthDate: string;
  phone: string;
  email: string;
  guardianName: string;
  guardianRelationship: string;
  guardianPhone: string;
  registration: string;
  region: string;
  bio: string;
  description: string;
  durationMinutes: string;
  role: string;
  password: string;
  published: boolean;
  serviceIds: string[];
};
function values(record?: EditableRecord): FormValues {
  return {
    name: record?.name || "",
    birthDate: record?.birthDate || "",
    phone: record?.phone || "",
    email: record?.email || "",
    guardianName: record?.guardianName || "",
    guardianRelationship: record?.guardianRelationship || "",
    guardianPhone: record?.guardianPhone || "",
    registration: record?.registration || "",
    region: record?.region || "",
    bio: record?.bio || "",
    description: record?.description || "",
    durationMinutes: String(record?.durationMinutes || ""),
    role: record?.role || "RECEPCAO",
    password: "",
    published: record?.published || false,
    serviceIds: record?.serviceIds || [],
  };
}

function RecordForm({
  kind,
  record,
  onClose,
  onSaved,
}: {
  kind: RecordKind;
  record?: EditableRecord;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form, setForm] = useState(() => values(record));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [services, setServices] = useState<Service[]>([]);
  const [servicesLoading, setServicesLoading] = useState(
    kind === "professionals",
  );
  const [servicesError, setServicesError] = useState<Error | null>(null);
  const set = (key: keyof FormValues, value: string | boolean | string[]) =>
    setForm((prev) => ({ ...prev, [key]: value }));
  const fieldError = (name: string) =>
    error instanceof ApiError ? error.fields[name] : undefined;
  const input = (key: keyof FormValues) => ({
    value: String(form[key]),
    onChange: (e: React.ChangeEvent<HTMLInputElement>) =>
      set(key, e.target.value),
    error: fieldError(key),
  });
  async function loadServices() {
    setServicesLoading(true);
    setServicesError(null);
    try {
      setServices(await allRecords<Service>("/services"));
    } catch (err) {
      setServicesError(err as Error);
    } finally {
      setServicesLoading(false);
    }
  }
  useEffect(() => {
    if (kind === "professionals") void loadServices();
  }, [kind]);
  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    let payload: object = { name: form.name };
    if (kind === "patients")
      payload = {
        ...payload,
        birthDate: form.birthDate,
        phone: form.phone,
        email: form.email || null,
        guardianName: form.guardianName || null,
        guardianRelationship: form.guardianRelationship || null,
        guardianPhone: form.guardianPhone || null,
      };
    if (kind === "professionals")
      payload = {
        ...payload,
        phone: form.phone || null,
        email: form.email || null,
        registration: form.registration || null,
        region: form.region || null,
        bio: form.bio || null,
        serviceIds: form.serviceIds,
        published: form.published,
      };
    if (kind === "services")
      payload = {
        ...payload,
        description: form.description,
        durationMinutes: Number(form.durationMinutes),
        published: form.published,
      };
    if (kind === "users")
      payload = {
        ...payload,
        email: form.email,
        role: form.role,
        ...(!record ? { password: form.password } : {}),
      };
    payload = { ...payload, ...(record ? { version: record.version } : {}) };
    try {
      await api(
        `/${kind}${record ? `/${record.id}` : ""}`,
        record ? "PUT" : "POST",
        payload,
      );
      onSaved();
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  const minor = isMinor(form.birthDate);
  return (
    <Modal
      title={`${record ? "Editar" : "Cadastrar"} ${descriptions[kind].singular}`}
      description="Campos com * são obrigatórios."
      onClose={onClose}
      wide
    >
      <form onSubmit={submit} className="form-stack">
        <div className="form-grid">
          <Field
            className="span-2"
            label={kind === "services" ? "Nome do serviço" : "Nome completo"}
            required
            maxLength={160}
            autoComplete="off"
            {...input("name")}
          />
          {kind === "patients" ? (
            <>
              <Field
                label="Data de nascimento"
                type="date"
                required
                max={localDate()}
                {...input("birthDate")}
              />
              <Field
                label="Telefone de contato"
                type="tel"
                required
                maxLength={25}
                {...input("phone")}
              />
              <Field
                className="span-2"
                label="E-mail (opcional)"
                type="email"
                maxLength={180}
                {...input("email")}
              />
              <fieldset className="fieldset span-2">
                <legend>
                  Responsável{" "}
                  {minor
                    ? "· obrigatório para menores de 18 anos"
                    : "· se aplicável"}
                </legend>
                <div className="form-grid">
                  <Field
                    className="span-2"
                    label="Nome do responsável"
                    required={minor}
                    maxLength={160}
                    {...input("guardianName")}
                  />
                  <Field
                    label="Vínculo com o paciente"
                    required={minor}
                    maxLength={80}
                    {...input("guardianRelationship")}
                  />
                  <Field
                    label="Telefone do responsável"
                    type="tel"
                    required={minor}
                    maxLength={25}
                    {...input("guardianPhone")}
                  />
                </div>
              </fieldset>
            </>
          ) : null}
          {kind === "professionals" ? (
            <>
              <Field
                label="Telefone (opcional)"
                type="tel"
                maxLength={25}
                {...input("phone")}
              />
              <Field
                label="E-mail (opcional)"
                type="email"
                maxLength={180}
                {...input("email")}
              />
              <Field
                label="Registro CRFa"
                required={form.published}
                hint="Informe somente um registro confirmado."
                {...input("registration")}
              />
              <Field
                label="Região do CRFa"
                required={form.published}
                {...input("region")}
              />
              <TextareaField
                className="span-2"
                label="Apresentação profissional"
                value={form.bio}
                onChange={(e) => set("bio", e.target.value)}
                error={fieldError("bio")}
              />
              <fieldset className="fieldset span-2">
                <legend>Serviços que realiza</legend>
                {servicesLoading ? (
                  <Loading />
                ) : servicesError ? (
                  <ErrorNotice
                    error={servicesError}
                    onRetry={() => void loadServices()}
                  />
                ) : services.length ? (
                  <div className="check-list">
                    {services
                      .filter((s) => s.active || form.serviceIds.includes(s.id))
                      .map((s) => (
                        <Check
                          key={s.id}
                          label={`${s.name}${s.active ? "" : " (inativo)"}`}
                          checked={form.serviceIds.includes(s.id)}
                          onChange={(e) =>
                            set(
                              "serviceIds",
                              e.target.checked
                                ? [...form.serviceIds, s.id]
                                : form.serviceIds.filter((id) => id !== s.id),
                            )
                          }
                        />
                      ))}
                  </div>
                ) : (
                  <p className="muted">
                    Cadastre um serviço para vincular a este profissional.
                  </p>
                )}
              </fieldset>
            </>
          ) : null}
          {kind === "services" ? (
            <>
              <Field
                label="Duração em minutos"
                type="number"
                min={1}
                max={480}
                required
                {...input("durationMinutes")}
              />
              <TextareaField
                className="span-2"
                label="Descrição do serviço"
                value={form.description}
                onChange={(e) => set("description", e.target.value)}
                error={fieldError("description")}
              />
            </>
          ) : null}
          {kind === "services" || kind === "professionals" ? (
            <div className="span-2">
              <Check
                label="Autorizar publicação no site"
                hint="Marque somente após revisar e aprovar o conteúdo público."
                checked={form.published}
                onChange={(e) => set("published", e.target.checked)}
              />
            </div>
          ) : null}
          {kind === "users" ? (
            <>
              <Field
                label="E-mail de acesso"
                type="email"
                required
                autoComplete="off"
                {...input("email")}
              />
              <SelectField
                label="Permissão"
                value={form.role}
                onChange={(e) => set("role", e.target.value)}
              >
                <option value="RECEPCAO">Recepção</option>
                <option value="ADMIN">Administrador</option>
              </SelectField>
              {!record ? (
                <Field
                  className="span-2"
                  label="Senha inicial"
                  type="password"
                  autoComplete="new-password"
                  required
                  minLength={12}
                  hint="Use ao menos 12 caracteres. Compartilhe por um canal seguro."
                  {...input("password")}
                />
              ) : null}
              <p className="muted span-2">
                A recepção gerencia pacientes e agendamentos. Administradores
                também gerenciam cadastros, configurações e acessos.
              </p>
            </>
          ) : null}
        </div>
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
          <Button
            busy={busy}
            disabled={
              kind === "professionals" &&
              (servicesLoading || Boolean(servicesError))
            }
          >
            Salvar {descriptions[kind].singular}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
function PatientHistory({
  patient,
  onClose,
}: {
  patient: EditableRecord;
  onClose: () => void;
}) {
  const [page, setPage] = useState(0);
  const history = useResource<Page<Appointment>>(
    `/appointments?patientId=${patient.id}&page=${page}&size=10`,
  );
  return (
    <Modal
      title="Agendamentos do paciente"
      description={patient.name}
      onClose={onClose}
      wide
    >
      {history.loading ? (
        <Loading />
      ) : history.error ? (
        <ErrorNotice error={history.error} onRetry={history.reload} />
      ) : history.data?.content.length ? (
        <>
          <div className="history-list">
            {history.data.content.map((a) => (
              <article key={a.id}>
                <div>
                  <strong>{dateTime(a.start)}</strong>
                  <p>
                    {a.serviceName} · {a.professionalName}
                  </p>
                </div>
                <span className={`badge status-${a.status.toLowerCase()}`}>
                  {statuses[a.status]}
                </span>
              </article>
            ))}
          </div>
          <Pagination page={history.data} onChange={setPage} />
        </>
      ) : (
        <Empty
          title="Nenhum agendamento"
          description="As consultas deste paciente aparecerão aqui."
        />
      )}
    </Modal>
  );
}
function AvailabilityModal({
  person,
  onClose,
  editable,
}: {
  person: EditableRecord;
  onClose: () => void;
  editable: boolean;
}) {
  const availability = useResource<{ periods: Period[] }>(
    `/professionals/${person.id}/availability`,
  );
  const blocks = useResource<Block[]>(`/professionals/${person.id}/blocks`);
  const [periods, setPeriods] = useState<Period[]>([]);
  const [start, setStart] = useState("");
  const [end, setEnd] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [notice, setNotice] = useState("");
  const [deleteBlock, setDeleteBlock] = useState<Block | null>(null);
  useEffect(() => {
    if (availability.data) setPeriods(availability.data.periods);
  }, [availability.data]);
  const days = [
    "Segunda-feira",
    "Terça-feira",
    "Quarta-feira",
    "Quinta-feira",
    "Sexta-feira",
    "Sábado",
    "Domingo",
  ];
  async function save(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setNotice("");
    try {
      await api(`/professionals/${person.id}/availability`, "PUT", { periods });
      setNotice("Disponibilidade atualizada.");
      availability.reload();
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  async function addBlock(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    setNotice("");
    try {
      await api(`/professionals/${person.id}/blocks`, "POST", {
        start: withOffset(start),
        end: withOffset(end),
      });
      setNotice("Bloqueio adicionado.");
      setStart("");
      setEnd("");
      blocks.reload();
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  async function removeBlock() {
    if (!deleteBlock) return;
    setBusy(true);
    setError(null);
    try {
      await api(
        `/professionals/${person.id}/blocks/${deleteBlock.id}`,
        "DELETE",
      );
      setDeleteBlock(null);
      setNotice("Bloqueio removido.");
      blocks.reload();
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  function updatePeriod(
    index: number,
    field: keyof Period,
    value: number | string,
  ) {
    setPeriods((old) =>
      old.map((period, i) =>
        i === index ? { ...period, [field]: value } : period,
      ),
    );
  }
  return (
    <Modal
      title="Disponibilidade e bloqueios"
      description={`${person.name} · Horários de Brasília`}
      onClose={onClose}
      wide
    >
      {notice ? <Notice>{notice}</Notice> : null}
      <ErrorNotice error={error} />
      <section className="availability-section">
        <h3>Horários semanais</h3>
        <p className="muted">
          Defina os períodos em que o profissional atende.
        </p>
        {availability.loading ? (
          <Loading />
        ) : availability.error ? (
          <ErrorNotice
            error={availability.error}
            onRetry={availability.reload}
          />
        ) : (
          <form onSubmit={save}>
            <div className="period-list">
              {periods.map((p, i) => (
                <div className="period-row" key={i}>
                  <SelectField
                    label="Dia da semana"
                    value={p.dayOfWeek}
                    disabled={!editable}
                    onChange={(e) =>
                      updatePeriod(i, "dayOfWeek", Number(e.target.value))
                    }
                  >
                    {days.map((d, j) => (
                      <option key={d} value={j + 1}>
                        {d}
                      </option>
                    ))}
                  </SelectField>
                  <Field
                    label="Início"
                    type="time"
                    required
                    value={p.startTime}
                    disabled={!editable}
                    onChange={(e) =>
                      updatePeriod(i, "startTime", e.target.value)
                    }
                  />
                  <Field
                    label="Fim"
                    type="time"
                    required
                    value={p.endTime}
                    disabled={!editable}
                    onChange={(e) => updatePeriod(i, "endTime", e.target.value)}
                  />
                  {editable ? (
                    <Button
                      type="button"
                      variant="ghost"
                      className="icon-button"
                      aria-label={`Remover período ${i + 1}`}
                      onClick={() =>
                        setPeriods((old) => old.filter((_, j) => j !== i))
                      }
                    >
                      <Trash2 size={18} aria-hidden />
                    </Button>
                  ) : null}
                </div>
              ))}
            </div>
            {!periods.length ? (
              <p className="muted empty-inline">
                Nenhum período de atendimento cadastrado.
              </p>
            ) : null}
            {editable ? (
              <div className="availability-actions">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() =>
                    setPeriods((old) => [
                      ...old,
                      { dayOfWeek: 1, startTime: "", endTime: "" },
                    ])
                  }
                >
                  <Plus size={16} aria-hidden />
                  Adicionar período
                </Button>
                <Button busy={busy}>Salvar disponibilidade</Button>
              </div>
            ) : null}
          </form>
        )}
      </section>
      <section className="availability-section">
        <h3>Bloqueios de agenda</h3>
        <p className="muted">Reserve períodos em que não haverá atendimento.</p>
        {blocks.loading ? (
          <Loading />
        ) : blocks.error ? (
          <ErrorNotice error={blocks.error} onRetry={blocks.reload} />
        ) : blocks.data?.length ? (
          <div className="block-list">
            {blocks.data.map((block) => (
              <div key={block.id}>
                <span>
                  {dateTime(block.start)} → {dateTime(block.end)}
                </span>
                {editable ? (
                  <Button
                    variant="ghost"
                    aria-label={`Remover bloqueio de ${dateTime(block.start)}`}
                    onClick={() => setDeleteBlock(block)}
                  >
                    <Trash2 size={17} aria-hidden />
                  </Button>
                ) : null}
              </div>
            ))}
          </div>
        ) : (
          <p className="muted empty-inline">Nenhum bloqueio cadastrado.</p>
        )}
        {deleteBlock ? (
          <div className="inline-confirm">
            <p>Remover o bloqueio de {dateTime(deleteBlock.start)}?</p>
            <div className="row-actions">
              <Button variant="secondary" onClick={() => setDeleteBlock(null)}>
                Manter bloqueio
              </Button>
              <Button
                variant="danger"
                busy={busy}
                onClick={() => void removeBlock()}
              >
                Remover bloqueio
              </Button>
            </div>
          </div>
        ) : null}
        {editable ? (
          <form onSubmit={addBlock} className="form-stack">
            <div className="form-grid">
              <Field
                label="Início do bloqueio"
                type="datetime-local"
                required
                value={start}
                onChange={(e) => setStart(e.target.value)}
              />
              <Field
                label="Fim do bloqueio"
                type="datetime-local"
                required
                min={start}
                value={end}
                onChange={(e) => setEnd(e.target.value)}
              />
            </div>
            <div className="form-actions">
              <Button busy={busy} variant="secondary">
                Adicionar bloqueio
              </Button>
            </div>
          </form>
        ) : null}
      </section>
    </Modal>
  );
}
export function CrudPage({ kind }: { kind: RecordKind }) {
  const { user } = useAuth();
  const config = descriptions[kind];
  const editable = kind === "patients" || user.role === "ADMIN";
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [params, setParams] = useSearchParams();
  const [editor, setEditor] = useState<EditableRecord | "new" | null>(
    params.has("novo") ? "new" : null,
  );
  const resource = useResource<Page<EditableRecord>>(
    `/${kind}?search=${encodeURIComponent(query)}&page=${page}&size=12`,
  );
  const [action, setAction] = useState<{
    record: EditableRecord;
    type: "delete" | "active";
  } | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [notice, setNotice] = useState("");
  const [history, setHistory] = useState<EditableRecord | null>(null);
  const [availability, setAvailability] = useState<EditableRecord | null>(null);
  useEffect(() => {
    document.title = `${config.title} · Espaço Sinapse`;
    setSearch("");
    setQuery("");
    setPage(0);
    setEditor(null);
    setNotice("");
  }, [kind, config.title]);
  useEffect(() => {
    if (params.has("novo")) {
      setEditor("new");
      setParams({}, { replace: true });
    }
  }, [params, setParams]);
  function searchRecords(e: FormEvent) {
    e.preventDefault();
    setPage(0);
    setQuery(search);
  }
  async function confirmAction() {
    if (!action) return;
    setBusy(true);
    setError(null);
    try {
      await api(
        `/${kind}/${action.record.id}${action.type === "active" ? "/active" : ""}`,
        action.type === "active" ? "PATCH" : "DELETE",
        action.type === "active"
          ? { active: !action.record.active, version: action.record.version }
          : undefined,
      );
      setNotice(
        action.type === "delete"
          ? "Cadastro excluído."
          : action.record.active
            ? "Cadastro inativado."
            : "Cadastro reativado.",
      );
      setAction(null);
      resource.reload();
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <PageHeader
        eyebrow="CADASTROS"
        title={config.title}
        description={config.description}
      >
        {editable ? (
          <Button onClick={() => setEditor("new")}>
            <Plus size={18} aria-hidden />
            {config.action}
          </Button>
        ) : (
          <span className="badge badge-neutral">Somente consulta</span>
        )}
      </PageHeader>
      {notice ? <Notice>{notice}</Notice> : null}
      <section className="data-panel" aria-label={config.title}>
        <form className="table-toolbar" onSubmit={searchRecords}>
          <div className="search-field">
            <Search size={18} aria-hidden />
            <input
              aria-label={
                kind === "patients"
                  ? "Buscar por nome ou telefone"
                  : "Buscar por nome"
              }
              placeholder={
                kind === "patients"
                  ? "Buscar por nome ou telefone"
                  : "Buscar por nome"
              }
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
          <Button type="submit" variant="secondary">
            Buscar
          </Button>
          <span className="toolbar-count">
            {resource.data ? `${resource.data.totalElements} cadastros` : ""}
          </span>
        </form>
        {resource.loading ? (
          <Loading />
        ) : resource.error ? (
          <ErrorNotice error={resource.error} onRetry={resource.reload} />
        ) : resource.data?.content.length ? (
          <>
            <div className="table-wrapper">
              <table className="records-table">
                <thead>
                  <tr>
                    <th scope="col">
                      {kind === "services" ? "Serviço" : "Nome"}
                    </th>
                    <th scope="col">
                      {kind === "services"
                        ? "Duração"
                        : kind === "users"
                          ? "Permissão"
                          : "Contato"}
                    </th>
                    <th scope="col">Situação</th>
                    <th scope="col" className="table-action-header">
                      Ações
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {resource.data.content.map((record) => (
                    <tr key={record.id}>
                      <td data-label={kind === "services" ? "Serviço" : "Nome"}>
                        <strong>{record.name}</strong>
                        <small>
                          {kind === "patients"
                            ? `Nascimento: ${date(record.birthDate || "")}`
                            : kind === "professionals"
                              ? record.registration
                                ? `CRFa ${record.region || ""} · ${record.registration}`
                                : "Registro não informado"
                              : kind === "users"
                                ? record.email
                                : record.description}
                        </small>
                      </td>
                      <td
                        data-label={
                          kind === "services"
                            ? "Duração"
                            : kind === "users"
                              ? "Permissão"
                              : "Contato"
                        }
                      >
                        {kind === "services" ? (
                          <span className="inline-icon">
                            <Clock3 size={15} aria-hidden />
                            {record.durationMinutes} min
                          </span>
                        ) : kind === "users" ? (
                          record.role === "ADMIN" ? (
                            "Administrador"
                          ) : (
                            "Recepção"
                          )
                        ) : (
                          <>
                            <span>{record.phone || "—"}</span>
                            <small>{record.email}</small>
                          </>
                        )}
                      </td>
                      <td data-label="Situação">
                        <Status active={record.active} />
                        {record.published ? (
                          <small>Publicado no site</small>
                        ) : null}
                      </td>
                      <td className="record-actions">
                        <div className="row-actions">
                          {kind === "patients" ? (
                            <Button
                              variant="ghost"
                              aria-label={`Ver agendamentos de ${record.name}`}
                              onClick={() => setHistory(record)}
                            >
                              <History size={17} aria-hidden />
                              <span className="mobile-action-label">
                                Histórico
                              </span>
                            </Button>
                          ) : null}
                          {kind === "professionals" ? (
                            <Button
                              variant="ghost"
                              aria-label={`Disponibilidade de ${record.name}`}
                              onClick={() => setAvailability(record)}
                            >
                              <CalendarDays size={17} aria-hidden />
                              <span className="mobile-action-label">
                                Horários
                              </span>
                            </Button>
                          ) : null}
                          {editable ? (
                            <>
                              <Button
                                variant="ghost"
                                aria-label={`Editar ${record.name}`}
                                onClick={() => setEditor(record)}
                              >
                                <Pencil size={16} aria-hidden />
                                <span className="mobile-action-label">
                                  Editar
                                </span>
                              </Button>
                              <Button
                                variant="ghost"
                                onClick={() => {
                                  setAction({ record, type: "active" });
                                  setError(null);
                                }}
                              >
                                {record.active ? "Inativar" : "Reativar"}
                              </Button>
                              {user.role === "ADMIN" ? (
                                <Button
                                  variant="ghost"
                                  className="delete-action"
                                  aria-label={`Excluir ${record.name}`}
                                  onClick={() => {
                                    setAction({ record, type: "delete" });
                                    setError(null);
                                  }}
                                >
                                  <Trash2 size={16} aria-hidden />
                                </Button>
                              ) : null}
                            </>
                          ) : null}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={resource.data} onChange={setPage} />
          </>
        ) : (
          <Empty
            title={
              query
                ? "Nenhum resultado encontrado"
                : `Nenhum ${config.singular} cadastrado`
            }
            description={
              query
                ? "Tente outro nome ou ajuste os termos da busca."
                : `Os cadastros aparecerão aqui. ${editable ? "Comece adicionando o primeiro." : "A administração pode criar um novo cadastro."}`
            }
            action={
              editable && !query ? (
                <Button onClick={() => setEditor("new")}>
                  {config.action}
                </Button>
              ) : undefined
            }
          />
        )}
      </section>
      {editor ? (
        <RecordForm
          key={`${kind}-${editor === "new" ? "new" : editor.id}`}
          kind={kind}
          record={editor === "new" ? undefined : editor}
          onClose={() => setEditor(null)}
          onSaved={() => {
            setEditor(null);
            setNotice("Cadastro salvo com sucesso.");
            resource.reload();
          }}
        />
      ) : null}
      {action ? (
        <Modal
          title={`${action.type === "delete" ? "Excluir" : action.record.active ? "Inativar" : "Reativar"} ${config.singular}`}
          description={action.record.name}
          onClose={() => {
            if (!busy) setAction(null);
          }}
        >
          <p>
            {action.type === "delete"
              ? "A exclusão é permanente e só é permitida para cadastros sem vínculos. Para preservar o cadastro, utilize a inativação."
              : action.record.active
                ? "O cadastro deixará de estar disponível para novos agendamentos. Consultas futuras precisam ser resolvidas antes da inativação."
                : "O cadastro voltará a estar disponível para uso."}
          </p>
          <ErrorNotice error={error} />
          <div className="form-actions">
            <Button
              variant="secondary"
              disabled={busy}
              onClick={() => setAction(null)}
            >
              Voltar
            </Button>
            <Button
              variant={action.type === "delete" ? "danger" : "primary"}
              busy={busy}
              onClick={() => void confirmAction()}
            >
              {action.type === "delete"
                ? "Excluir cadastro"
                : action.record.active
                  ? "Inativar cadastro"
                  : "Reativar cadastro"}
            </Button>
          </div>
        </Modal>
      ) : null}
      {history ? (
        <PatientHistory patient={history} onClose={() => setHistory(null)} />
      ) : null}
      {availability ? (
        <AvailabilityModal
          person={availability}
          editable={user.role === "ADMIN"}
          onClose={() => setAvailability(null)}
        />
      ) : null}
    </>
  );
}
