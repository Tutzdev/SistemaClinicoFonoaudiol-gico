# Contrato da API

Base local: `http://127.0.0.1:8081/api/v1`. No frontend e no Compose, use a mesma origem com o prefixo `/api/v1`. O contrato exportado da aplicação está em [openapi.json](openapi.json); a versão servida está em `GET /api/v1/openapi`, autenticada. A interface Swagger está em `/api/v1/docs`.

Consulte a [arquitetura e matriz de permissões](architecture.md). Todas as entradas e saídas são DTOs JSON: entidades JPA, hashes, informações de pacientes e contatos privados não são enviados pela API pública.

## Convenções

- IDs são UUIDs em texto. Datas civis usam `YYYY-MM-DD`; agendamentos e bloqueios exigem ISO-8601 com offset, como `2026-10-05T09:00:00-03:00`. A clínica usa `America/Sao_Paulo`, independentemente do fuso do servidor.
- `POST` de cadastro responde `201`; consultas e atualizações, `200`; exclusão, logout e alteração de senha, `204` sem corpo. Login responde `200`.
- `PUT` substitui os campos editáveis do recurso; envie todos os campos obrigatórios. Em atualizações de cadastros, envie a `version` obtida na última leitura. Não reenvie o DTO inteiro: campos como `id`, `active`, timestamps e propriedades desconhecidas são recusados na entrada.
- Listagens paginadas aceitam `page` a partir de `0` e `size` entre `1` e `100`, padrão `20`. Retorno: `{ "content": [], "totalElements": 0, "totalPages": 0, "number": 0, "size": 20 }`.
- Cadastros retornam `active`, `version`, `createdAt` e `updatedAt`. Timestamps de criação e atualização são instantes ISO-8601. Não há endpoint para prontuário, triagem, pagamento ou cadastro público de usuário.

## Sessão, CSRF e autenticação

1. Faça `GET /auth/csrf` para receber `{ "token": "…", "headerName": "X-CSRF-TOKEN" }` e conservar o cookie de sessão.
2. Envie o token no nome de cabeçalho retornado, inclusive em `POST /auth/login`. Corpo do login: `{ "email": "…", "password": "…" }`.
3. Após login, consulte `/auth/csrf` novamente: o token anterior é invalidado. Preserve o cookie nas próximas requisições.
4. Todas as escritas exigem CSRF. Para integração no navegador, use a mesma origem e credenciais de sessão. Não há token JWT nem armazenamento de senha no navegador.

O cookie é `HttpOnly`, `SameSite=Lax` e `Secure` quando `APP_COOKIE_SECURE=true`. A sessão expira após 30 minutos de inatividade. Logout a invalida. Desativação, troca de papel ou de e-mail e alteração de senha invalidam sessões anteriores; a troca da própria senha também encerra a sessão atual. O servidor verifica o usuário ativo e a versão da autenticação em cada requisição.

| Método e rota | Entrada / retorno | Acesso |
|---|---|---|
| `GET /auth/csrf` | Token e nome do cabeçalho | Público |
| `POST /auth/login` | `email`, `password` → `id`, `name`, `email`, `role` | Público, com CSRF |
| `GET /auth/me` | `id`, `name`, `email`, `role` | Autenticado |
| `POST /auth/logout` | Sem corpo → `204` | Autenticado |
| `PUT /auth/password` | `currentPassword`, `newPassword` → `204` | Próprio usuário |

Senhas novas precisam ter pelo menos 12 caracteres e no máximo 72 bytes UTF-8. São armazenadas com BCrypt. O login limita tentativas por e-mail e endereço remoto em uma janela de 15 minutos, respondendo `429` ao exceder o limite. O controle é em memória, adequado à instância única deste MVP. O último administrador ativo não pode ser removido, desativado nem rebaixado.

## Cadastros básicos

Para cada grupo `/patients`, `/professionals`, `/services` e `/users`, existem:

| Método | Rota | Resultado |
|---|---|---|
| `GET` | `/grupo?search=&page=0&size=20` | Página ordenada por nome |
| `GET` | `/grupo/{id}` | Um cadastro |
| `POST` | `/grupo` | Criação com os campos da tabela abaixo |
| `PUT` | `/grupo/{id}` | Atualização com campos editáveis e `version` |
| `PATCH` | `/grupo/{id}/active` | `{ "active": false, "version": 0 }`; também permite reativar |
| `DELETE` | `/grupo/{id}` | Exclusão física somente para ADMIN e sem vínculos impeditivos |

Não existe filtro `active` nesta versão; a listagem retorna ativos e inativos. A busca procura nome ou telefone em pacientes, nome ou e-mail em usuários, e nome em serviços/profissionais.

| Grupo | Campos editáveis do corpo | Validação e acesso |
|---|---|---|
| `/patients` | Obrigatórios: `name`, `birthDate`, `phone`. Opcionais: `email`, `guardianName`, `guardianRelationship`, `guardianPhone`. | ADMIN e RECEPCAO gerenciam; só ADMIN exclui. Menores de 18 anos exigem os três campos do responsável. Data futura é recusada. Telefones têm de 10 a 15 dígitos, com DDD; formatação é aceita. Contatos podem ser compartilhados entre familiares. |
| `/services` | Obrigatórios: `name`, `durationMinutes`. Opcionais: `description`, `published` (padrão `false`). | ADMIN escreve; RECEPCAO consulta. Duração de 1 a 480 minutos. Mudanças não alteram consultas existentes. |
| `/professionals` | Obrigatórios: `name`, `serviceIds` (array, inclusive vazio). Opcionais: `email`, `phone`, `registration`, `region`, `bio`, `published` (padrão `false`). | ADMIN escreve; RECEPCAO consulta. IDs de serviço precisam existir. Publicação exige CRFa e região preenchidos e confirmados pelo administrador; o sistema não verifica credenciais em conselho profissional. |
| `/users` | Obrigatórios: `name`, `email`, `role` (`ADMIN` ou `RECEPCAO`). `password` obrigatório na criação; deve ser omitido na edição. | Somente ADMIN. E-mail único sem distinguir maiúsculas. Senhas são alteradas por `/auth/password`, com a senha atual. Não há recuperação por e-mail nesta versão. |

Nomes têm até 160 caracteres; e-mails, 254; telefones, 32; descrições e biografias, 4.000. Os limites completos de cada DTO estão no OpenAPI. `version` é obrigatório no `PUT` e no `PATCH /active`, não na criação.

Cadastros com histórico de consultas devem ser inativados. Consultas abertas ainda não encerradas impedem inativação; o erro identifica as consultas a resolver. Exclusão também respeita vínculos de disponibilidade, serviços e auditoria. Inativação não significa anonimização. Profissionais são cadastros administrativos, independentes de contas de usuário.

## Disponibilidade e bloqueios

| Método e rota | Corpo / resposta | Acesso |
|---|---|---|
| `GET /professionals/{id}/availability` | `{ "periods": [{ "dayOfWeek": 1, "startTime": "08:00:00", "endTime": "18:00:00" }] }` | ADMIN / RECEPCAO |
| `PUT /professionals/{id}/availability` | Substitui o conjunto usando o mesmo formato; `periods: []` limpa o conjunto quando não afeta consultas | ADMIN |
| `GET /professionals/{id}/blocks` | Array de `{ id, start, end }`, ordenado por início | ADMIN / RECEPCAO |
| `POST /professionals/{id}/blocks` | `{ "start": "…-03:00", "end": "…-03:00" }` → bloqueio criado | ADMIN |
| `DELETE /professionals/{id}/blocks/{blockId}` | `204` | ADMIN |

`dayOfWeek` usa ISO: segunda-feira `1`, domingo `7`. Horários têm precisão de minutos (`HH:mm` ou `HH:mm:00`), início anterior ao fim, sem sobreposição; máximo de 35 períodos por profissional. Um bloqueio precisa terminar no futuro, e bloqueios sobrepostos são recusados. Alterações que prejudiquem consultas abertas ainda não encerradas retornam `409`, sem cancelamento automático. Esses sub-recursos não recebem `version`: a alteração de disponibilidade substitui explicitamente o conjunto, dentro da transação de escrita.

## Agendamentos

ADMIN e RECEPCAO têm acesso a todos os endpoints deste grupo.

| Método e rota | Corpo / parâmetros | Resposta |
|---|---|---|
| `GET /appointments` | Opcionais: `from`, `to`, `professionalId`, `patientId`, `status`, `page`, `size` | Página ordenada por início |
| `GET /appointments/{id}` | — | Um agendamento |
| `POST /appointments` | `patientId`, `professionalId`, `serviceId`, `start` | Agendamento em `AGENDADO` |
| `PATCH /appointments/{id}/reschedule` | `start`, `version` | Reagendamento em `AGENDADO` |
| `PATCH /appointments/{id}/status` | `status`, `version` | Agendamento atualizado |
| `GET /appointments/{id}/history` | — | Array ordenado por criação: `id`, `action`, `actorName`, `createdAt`, `previousStart`, `newStart`, `previousStatus`, `newStatus` |

O filtro de período considera o início da consulta: `from ≤ start < to`. Passe os limites do dia no fuso da clínica. O histórico de um paciente é consultado por `/appointments?patientId={id}`; não existe um endpoint separado de prontuário.

O DTO de consulta contém `id`, `patientId`, `patientName`, `professionalId`, `professionalName`, `serviceId`, `serviceName`, `start`, `end`, `durationMinutes`, `status`, `version`, `createdAt` e `updatedAt`. O servidor calcula `end` e registra uma cópia da duração. Reagendar preserva essa duração, mesmo que o serviço tenha sido alterado depois.

Início deve ser futuro; os três cadastros devem estar ativos e o profissional deve oferecer o serviço. A consulta inteira cabe em um período disponível do mesmo dia, sem bloqueio nem sobreposição do profissional ou paciente. Intervalos são `[início, fim)`: uma consulta pode começar no instante em que outra termina.

Transições permitidas:

- `AGENDADO → CONFIRMADO` ou `CANCELADO`.
- `CONFIRMADO → CANCELADO`.
- `AGENDADO` ou `CONFIRMADO → CONCLUIDO` ou `NAO_COMPARECEU`, somente depois do término.
- Reagendamento de `AGENDADO` ou `CONFIRMADO` volta a `AGENDADO`, com histórico.
- `CONCLUIDO`, `CANCELADO` e `NAO_COMPARECEU` são finais. Consultas não são apagadas; cancelamento libera o intervalo.

## Clínica, conteúdo público e auditoria

| Método e rota | Corpo / resposta | Acesso |
|---|---|---|
| `GET /clinic-settings` | `displayName`, `description`, `phone`, `email`, `whatsapp`, `publicAddress`, `addressConfirmed`, `version` | ADMIN |
| `PUT /clinic-settings` | Mesmo formato; todos obrigatórios salvo `publicAddress` | ADMIN |
| `GET /public/clinic` | Informações publicáveis e constantes `legalName`, `cnpj`; sem versão interna | Público |
| `GET /public/services` | Array de `id`, `name`, `description` | Público |
| `GET /public/professionals` | Array de `id`, `name`, `registration`, `region`, `bio` | Público |
| `GET /audit-events?page=0&size=20` | Página decrescente por horário: `id`, `actorId`, `action`, `entityType`, `entityId`, `createdAt` | ADMIN |
| `GET /health` | `{ "status": "UP" }` | Público |

`addressConfirmed=true` exige endereço preenchido. Quando `false`, o endereço completo nunca aparece na API pública. Serviços e profissionais aparecem somente se ativos e explicitamente publicados. Telefones/e-mails privados de profissionais e qualquer informação de paciente ficam fora desses DTOs. A auditoria registra ator, ação e referência, sem copiar conteúdo clínico.

## Conflitos, transações e erros

Todas as escritas administrativas adquirem `pg_advisory_xact_lock(65952229)` na mesma transação PostgreSQL. Isso serializa as escritas de uma clínica e mantém consultas de leitura concorrentes. Disponibilidade, inativação, vínculos de serviço e agendamento compartilham esse limite, evitando que uma validação fique inválida por outra alteração simultânea.

Além do lock, duas constraints de exclusão GiST no PostgreSQL impedem sobreposição de intervalos para profissional e paciente, inclusive se outra integração escrever diretamente no banco. São aplicadas a todos os estados exceto `CANCELADO`. `@Version` e a comparação explícita da versão enviada recusam edições obsoletas com `409`. Trata-se de uma escolha simples para uma única clínica, não de uma arquitetura multiempresa ou de alta taxa de escrita. Para crescer, seria necessário reprojetar granularidade dos locks, autenticação compartilhada e limitação de login.

| HTTP | Significado |
|---|---|
| `400` | Dados/formatos inválidos, campos desconhecidos, falta de campos ou regras de entrada |
| `401` | Credenciais inválidas, sessão ausente, expirada ou revogada |
| `403` | Papel sem permissão ou token CSRF ausente/inválido |
| `404` | Recurso não encontrado |
| `409` | Concorrência, sobreposição, versão antiga, vínculos impeditivos ou transição inválida |
| `429` | Muitas tentativas de login |

Erros usam `application/problem+json`. Validação de campos acrescenta `errors`; conflitos de disponibilidade/inativação/sobreposição identificados pela camada de serviço acrescentam `conflictingAppointmentIds`. Não é garantido que todos os `409` tragam IDs — uma constraint de banco pode produzir mensagem genérica. Exemplo:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Resolva os agendamentos futuros antes de inativar este cadastro.",
  "conflictingAppointmentIds": ["11111111-2222-4333-8444-555555555555"]
}
```

## Exemplo com curl

Exemplo Bash com `curl` e `jq`, usando uma conta local configurada. No PowerShell, prefira os scripts de execução do projeto ou `curl.exe`, adaptando as aspas. Nenhuma senha de demonstração está embutida.

```bash
api='http://127.0.0.1:8081/api/v1'
cookie_jar=$(mktemp)
curl --fail-with-body "$api/public/clinic"

# Uma sessão conserva os cookies; o token retornado usa X-CSRF-TOKEN.
csrf=$(curl --fail-with-body -sS -c "$cookie_jar" -b "$cookie_jar" "$api/auth/csrf" | jq -r '.token')
read -r -p 'E-mail: ' SINAPSE_EMAIL
read -r -s -p 'Senha: ' SINAPSE_PASSWORD
export SINAPSE_EMAIL SINAPSE_PASSWORD
jq -n '{email: env.SINAPSE_EMAIL, password: env.SINAPSE_PASSWORD}' |
  curl --fail-with-body -sS -c "$cookie_jar" -b "$cookie_jar" \
    -H 'Content-Type: application/json' -H "X-CSRF-TOKEN: $csrf" \
    --data-binary @- "$api/auth/login"
unset SINAPSE_EMAIL SINAPSE_PASSWORD

# Renovar após login; consultar e cadastrar um paciente fictício.
csrf=$(curl --fail-with-body -sS -c "$cookie_jar" -b "$cookie_jar" "$api/auth/csrf" | jq -r '.token')
curl --fail-with-body -sS -b "$cookie_jar" "$api/patients?search=&page=0&size=20"
curl --fail-with-body -sS -b "$cookie_jar" \
  -H 'Content-Type: application/json' -H "X-CSRF-TOKEN: $csrf" \
  --data '{"name":"DEMONSTRAÇÃO · Paciente fictício","birthDate":"1990-01-01","phone":"61999990000"}' \
  "$api/patients"

curl --fail-with-body -sS -b "$cookie_jar" -X POST \
  -H "X-CSRF-TOKEN: $csrf" "$api/auth/logout"
rm -- "$cookie_jar"
```

Para editar, use `PUT /patients/{id}` com os campos editáveis completos e a `version` devolvida na leitura. Para criar consulta, utilize IDs reais dos cadastros de demonstração e um horário futuro dentro da disponibilidade configurada; consulte o schema de `AppointmentInput` no OpenAPI. O roteiro executável [api-smoke.mjs](../qa/api-smoke.mjs) demonstra autenticação, CRUD, bloqueios, concorrência, histórico e revogação de sessão contra a aplicação local.
