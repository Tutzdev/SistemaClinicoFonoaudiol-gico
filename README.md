# Espaço Sinapse

MVP administrativo para uma clínica: site público, painel React/TypeScript e API Java 21/Spring Boot com PostgreSQL. O site inicia contato; a recepção cria e confirma agendamentos no painel. Não inclui prontuário nem faturamento.

## Executar com Docker

Requisitos: Docker Engine/Desktop e Docker Compose v2.

1. Copie `.env.example` para `.env`.
2. Defina `DB_ADMIN_PASSWORD`, `DB_PASSWORD` e `BOOTSTRAP_ADMIN_PASSWORD` com senhas diferentes e fortes; ajuste e-mail/nome do administrador.
3. Execute:

```sh
docker compose up --build -d
```

Acesse http://localhost:8088 e use a área interna com as credenciais configuradas. O banco e a API ficam na rede interna do Compose; somente o frontend é exposto, em loopback. O bootstrap é idempotente e não redefine a senha de uma conta existente.

Para parar sem remover o banco:

```sh
docker compose stop
```

O volume `database` preserva os dados. Não use `down -v` se quiser mantê-los.

A senha administrativa do PostgreSQL é usada só na inicialização; o backend usa uma role sem privilégios de superusuário. O script de criação dessa role só roda ao inicializar um volume novo. Atualizar variáveis do `.env` não troca automaticamente senhas em um banco já existente. Não apague volumes para resolver essa diferença: migre as credenciais de forma planejada.

## Executar localmente no Windows

Requisitos: Node.js/npm, JDK 21 e PostgreSQL. Scripts em `scripts/` usam um cluster isolado no workspace, porta **55432**, e não alteram bancos existentes. O assistente de setup prepara JDK portátil quando necessário.

```powershell
.\scripts\setup-local-runtime.ps1
.\scripts\start-local-db.ps1
. .\scripts\use-local-env.ps1
.\scripts\start-backend.ps1 -Demo
```

Em outro terminal:

```powershell
$env:API_PROXY_TARGET='http://127.0.0.1:8081'
npm.cmd --prefix frontend ci
npm.cmd --prefix frontend run dev -- --host 127.0.0.1
```

Frontend: http://localhost:5173. API local: http://127.0.0.1:8081/api/v1. O Vite encaminha `/api` para o backend, preservando a mesma origem no navegador. A porta 8080 não é usada no modo Windows para evitar conflito com serviços já instalados.

Configurações e credenciais locais ficam em `.runtime/`, fora do versionamento. Para consultar o acesso gerado, execute `./scripts/show-local-access.ps1` no seu terminal privado. Não envie esse diretório para clientes nem para o Git.

`-Demo` habilita cadastros explicitamente fictícios e não publicados; omita a opção para iniciar sem inserir exemplos. Desabilitar o perfil depois não apaga dados já inseridos. Use bancos separados para demonstração e operação. No Compose, o equivalente é `SPRING_PROFILES_ACTIVE=demo` no `.env`.

Em redes que usam certificados confiáveis do Windows, Node 24 pode precisar de `$env:NODE_USE_SYSTEM_CA='1'` antes de `npm.cmd ci`. Os scripts Java usam o repositório de certificados do Windows, mantendo a validação TLS ativa.

## Organização

| Diretório | Responsabilidade |
|---|---|
| `frontend/` | Site, painel, formulários e consumo da API |
| `backend/` | Autenticação, permissões, regras, persistência e migrations |
| `docker/` | Imagens e proxy da execução local com Compose |
| `scripts/` | Ambiente local Windows isolado |
| `qa/` | Verificações integradas e visuais |
| `docs/` | Modelagem, origem dos dados e validação |

## Segurança e dados

- Dois perfis: ADMIN e RECEPCAO; autorização no servidor.
- Autenticação por sessão HttpOnly e CSRF, sem token persistido em localStorage.
- Senhas com hash; credenciais via ambiente.
- API pública com DTOs separados, sem pacientes ou agenda interna.
- Endereço cadastral não é publicado como endereço de atendimento.
- Serviços e profissionais só aparecem publicamente quando autorizados.
- Exclusões limitadas a cadastros sem vínculos; cancelamentos preservam histórico.
- Inativar não anonimiza dados. Política de retenção e uso real precisam ser definidos pela clínica.

Use dados fictícios para avaliação. A operação com dados reais depende de confirmar cadastros, registros, local, conteúdo e procedimentos de acesso. Não foi solicitada publicação em produção.

## Testes

```powershell
npm.cmd --prefix frontend run build
.\scripts\test-backend.ps1
```

Os testes de banco usam PostgreSQL (Testcontainers ou banco de teste dedicado configurável). Nunca aponte a suíte para um banco de operação: fixtures de teste podem limpar tabelas.

As verificações de navegador ficam em `qa/`; instale suas dependências na raiz com `npm.cmd ci`. Credenciais de teste são recebidas por ambiente, não embutidas nos scripts.

Com a API e o frontend locais já em execução, e Chrome instalado:

```powershell
. .\scripts\use-local-env.ps1
npm.cmd ci
npm.cmd run test:api
npm.cmd run test:e2e
npm.cmd run test:visual
```

Esses três comandos criam fixtures fictícias identificadas na demonstração local. Não execute contra uma base com dados reais. Relatórios e capturas ficam em `output/playwright/`, ignorado pelo Git. O teste Java usa exclusivamente `sinapse_test`; a suíte também pode criar PostgreSQL com Testcontainers quando executada sem `TEST_DB_URL` em ambiente com Docker.

Contrato exportado: `docs/openapi.json`. A versão ao vivo fica em `/api/v1/openapi`, após login; a documentação interativa em `/api/v1/docs`. Detalhes do contrato: `docs/api.md`.

Resultados efetivamente executados e limitações ficam em `docs/verification.md`.

## Backup e restauração (Compose local)

Criar cópia dentro do container e trazê-la para o host evita corrupção de binários por redirecionamento do PowerShell:

```sh
docker compose exec -T database sh -c 'pg_dump -U "$POSTGRES_USER" -d sinapse -Fc -f /tmp/sinapse.backup'
docker compose cp database:/tmp/sinapse.backup ./sinapse.backup
```

Guarde o backup fora do repositório, com acesso restrito. Para verificar a recuperação, restaure em outro banco, sem sobrescrever `sinapse`:

```sh
docker compose cp ./sinapse.backup database:/tmp/sinapse.backup
docker compose exec -T database sh -c 'createdb -U "$POSTGRES_USER" sinapse_restore'
docker compose exec -T database sh -c 'pg_restore -U "$POSTGRES_USER" -d sinapse_restore /tmp/sinapse.backup'
```

Confira os dados restaurados antes de qualquer substituição do banco original. A periodicidade, retenção e proteção dos backups devem ser definidas antes da operação real.
"# SistemaClinicoFonoaudiol-gico" 
