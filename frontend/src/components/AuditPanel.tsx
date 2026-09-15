import { useState } from "react";
import { RefreshCw } from "lucide-react";
import { useCollection, useResource } from "../api";
import { dateTime } from "../format";
import type { Page, User } from "../types";
import { Button, Empty, ErrorNotice, Loading, Pagination } from "./ui";

type AuditEvent = {
  id: string;
  actorId: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  createdAt: string;
};
const actions: Record<string, string> = {
  CREATED: "Cadastro criado",
  UPDATED: "Cadastro atualizado",
  DELETED: "Cadastro excluído",
  ACTIVATED: "Cadastro reativado",
  INACTIVATED: "Cadastro inativado",
  RESCHEDULED: "Consulta reagendada",
  STATUS_CHANGED: "Status alterado",
  PASSWORD_CHANGED: "Senha alterada",
  BOOTSTRAPPED: "Conta inicial criada",
  AVAILABILITY_UPDATED: "Disponibilidade atualizada",
  BLOCK_CREATED: "Bloqueio criado",
  BLOCK_DELETED: "Bloqueio removido",
};
const entities: Record<string, string> = {
  USER: "Equipe",
  PATIENT: "Paciente",
  SERVICE: "Serviço",
  PROFESSIONAL: "Profissional",
  APPOINTMENT: "Agendamento",
  CLINIC_SETTINGS: "Configurações",
  AVAILABILITY: "Disponibilidade",
  BLOCK: "Bloqueio",
};

export function AuditPanel() {
  const [page, setPage] = useState(0);
  const events = useResource<Page<AuditEvent>>(
    `/audit-events?page=${page}&size=10`,
  );
  const users = useCollection<User>("/users");
  const names = new Map(users.data.map((user) => [user.id, user.name]));
  return (
    <section className="data-panel audit-panel" aria-labelledby="audit-heading">
      <header className="panel-heading">
        <div>
          <h2 id="audit-heading">Histórico administrativo</h2>
          <p>
            Alterações registradas pela equipe, das mais recentes para as
            anteriores.
          </p>
        </div>
        <Button
          variant="secondary"
          aria-label="Atualizar histórico administrativo"
          onClick={() => {
            events.reload();
            users.reload();
          }}
        >
          <RefreshCw size={16} aria-hidden />
        </Button>
      </header>
      {events.loading ? (
        <Loading />
      ) : events.error ? (
        <ErrorNotice error={events.error} onRetry={events.reload} />
      ) : events.data?.content.length ? (
        <>
          <ol className="audit-list">
            {events.data.content.map((event) => (
              <li key={event.id}>
                <div>
                  <strong>
                    {actions[event.action] || "Alteração registrada"}
                  </strong>
                  <span>{entities[event.entityType] || "Administração"}</span>
                </div>
                <p>
                  <time dateTime={event.createdAt}>
                    {dateTime(event.createdAt)}
                  </time>{" "}
                  ·{" "}
                  {event.actorId
                    ? names.get(event.actorId) || "Conta indisponível"
                    : "Sistema"}
                </p>
                <details>
                  <summary>Ver referências do registro</summary>
                  <dl>
                    <div>
                      <dt>Registro alterado</dt>
                      <dd>{event.entityId || "Não se aplica"}</dd>
                    </div>
                    <div>
                      <dt>Identificador do evento</dt>
                      <dd>{event.id}</dd>
                    </div>
                    {event.actorId ? (
                      <div>
                        <dt>Identificador da conta</dt>
                        <dd>{event.actorId}</dd>
                      </div>
                    ) : null}
                  </dl>
                </details>
              </li>
            ))}
          </ol>
          <Pagination page={events.data} onChange={setPage} />
        </>
      ) : (
        <Empty
          title="Nenhuma alteração registrada"
          description="As operações administrativas realizadas pela equipe aparecerão aqui."
        />
      )}
      {users.error ? (
        <ErrorNotice
          error="Não foi possível carregar os nomes das contas. As referências dos eventos continuam disponíveis."
          onRetry={users.reload}
        />
      ) : null}
    </section>
  );
}
