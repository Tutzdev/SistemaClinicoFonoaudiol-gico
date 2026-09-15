import { useEffect, useState, type FormEvent } from "react";
import { ArrowUpRight, Save } from "lucide-react";
import { Link } from "react-router-dom";
import { api, ApiError, useResource } from "../api";
import {
  Button,
  Check,
  ErrorNotice,
  Field,
  Loading,
  Notice,
  PageHeader,
  TextareaField,
} from "../components/ui";
import type { Clinic } from "../types";
import { AuditPanel } from "../components/AuditPanel";

export function SettingsPage() {
  const resource = useResource<Clinic>("/clinic-settings");
  const [form, setForm] = useState<Clinic | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [notice, setNotice] = useState("");
  useEffect(() => {
    document.title = "Configurações · Espaço Sinapse";
    if (resource.data) setForm(resource.data);
  }, [resource.data]);
  function set(key: keyof Clinic, value: string | boolean) {
    setForm((prev) => (prev ? { ...prev, [key]: value } : null));
  }
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!form) return;
    setBusy(true);
    setError(null);
    setNotice("");
    const {
      displayName,
      description,
      phone,
      email,
      whatsapp,
      publicAddress,
      addressConfirmed,
      version,
    } = form;
    try {
      await api("/clinic-settings", "PUT", {
        displayName,
        description,
        phone,
        email,
        whatsapp,
        publicAddress,
        addressConfirmed,
        version,
      });
      setNotice("Informações públicas atualizadas.");
      resource.reload();
    } catch (err) {
      setError(err as Error);
    } finally {
      setBusy(false);
    }
  }
  const fieldError = (key: string) =>
    error instanceof ApiError ? error.fields[key] : undefined;
  return (
    <>
      <PageHeader
        eyebrow="ADMINISTRAÇÃO"
        title="Configurações"
        description="Informações aprovadas que aparecem no site da clínica."
      >
        <Link
          className="button button-secondary"
          to="/"
          target="_blank"
          rel="noopener"
        >
          Ver site <ArrowUpRight size={17} aria-hidden />
        </Link>
      </PageHeader>
      {resource.loading && !form ? (
        <Loading />
      ) : resource.error ? (
        <ErrorNotice error={resource.error} onRetry={resource.reload} />
      ) : form ? (
        <form onSubmit={submit} className="settings-form">
          {notice ? <Notice>{notice}</Notice> : null}
          <section className="settings-section">
            <div>
              <h2>Apresentação</h2>
              <p>Nome de apresentação e texto de abertura.</p>
            </div>
            <div className="form-stack">
              <Field
                label="Nome de apresentação"
                required
                maxLength={160}
                value={form.displayName}
                onChange={(e) => set("displayName", e.target.value)}
                error={fieldError("displayName")}
              />
              <TextareaField
                label="Descrição da clínica"
                required
                maxLength={4000}
                value={form.description}
                onChange={(e) => set("description", e.target.value)}
                error={fieldError("description")}
              />
            </div>
          </section>
          <section className="settings-section">
            <div>
              <h2>Canais de contato</h2>
              <p>Revise os dados antes de disponibilizá-los aos pacientes.</p>
            </div>
            <div className="form-stack">
              <Field
                label="Telefone"
                type="tel"
                required
                maxLength={32}
                value={form.phone}
                onChange={(e) => set("phone", e.target.value)}
                error={fieldError("phone")}
              />
              <Field
                label="WhatsApp"
                type="tel"
                required
                maxLength={32}
                hint="Inclua o código do país e o DDD. Exemplo de formato: 5561999999999."
                value={form.whatsapp}
                onChange={(e) => set("whatsapp", e.target.value)}
                error={fieldError("whatsapp")}
              />
              <Field
                label="E-mail"
                type="email"
                required
                maxLength={254}
                value={form.email}
                onChange={(e) => set("email", e.target.value)}
                error={fieldError("email")}
              />
            </div>
          </section>
          <section className="settings-section">
            <div>
              <h2>Local de atendimento</h2>
              <p>
                O endereço cadastral da empresa não é publicado automaticamente.
              </p>
            </div>
            <div className="form-stack">
              <TextareaField
                label="Endereço de atendimento"
                maxLength={500}
                value={form.publicAddress || ""}
                onChange={(e) => set("publicAddress", e.target.value)}
                error={fieldError("publicAddress")}
                required={form.addressConfirmed}
              />
              <Check
                label="Confirmei que este é o local de atendimento"
                hint="Ao marcar e salvar, o endereço será exibido no site público."
                checked={form.addressConfirmed}
                onChange={(e) => set("addressConfirmed", e.target.checked)}
              />
            </div>
          </section>
          <ErrorNotice error={error} />
          <div className="form-actions">
            <Button busy={busy}>
              <Save size={17} aria-hidden />
              Salvar configurações
            </Button>
          </div>
        </form>
      ) : null}
      <AuditPanel />
    </>
  );
}
