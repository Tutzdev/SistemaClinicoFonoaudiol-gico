# Direção de produto e interface

O site ajuda pacientes e responsáveis a encontrar informações aprovadas e iniciar contato. O painel ajuda a recepção a organizar pacientes e consultas, e o administrador a configurar a operação. O contato público não constitui uma reserva.

## Hierarquia

- Público: identidade → apresentação → serviços/profissionais publicados → contato.
- Painel: situação do dia → agenda → pacientes → cadastros e configurações conforme permissão.

## Sistema visual

Base #F7FAF9, texto #173B3B, ação #176B64, superfície #E6F0EC. O tom #D9A56B serve como acento decorativo e não como cor automática de texto. Manrope para títulos e Source Sans 3 para leitura; fontes locais. Uma composição de duas fitas que se conectam representa diálogo. Não representa equipe, instalações ou evidência clínica.

## Estrutura dos componentes

```text
PublicSite
  Header → Hero → PublishedServices → PublishedProfessionals → Contact
AdminShell
  Sidebar → PageHeader
  Dashboard | Agenda | Patients | Professionals | Services | Users | Settings
Shared
  Dialog → Field → Feedback → Pagination → EmptyState
```

Tabelas tornam-se registros empilhados no celular. Formulários oferecem labels, erros, estado de envio e recuperação. Dialogs contêm o foco e o restauram ao fechar. Estados vazios oferecem a próxima ação permitida sem dados ou métricas inventados.

## Revisão antes da implementação

| Risco | Decisão |
|---|---|
| Aparência repetitiva de cartões | Divisores e linhas onde há informação sequencial |
| Infantilização do público | Tipografia legível e linguagem apropriada a adultos/responsáveis |
| Inventar conteúdo para preencher espaço | Publicação condicional e campos aprovados |
| Dashboard vazio sem orientação | Atalhos úteis e informações reais da API |
| Tabelas e modais maiores que a tela | Uma coluna em formulários móveis e registros adaptados |

As skills frontend-design e frontend-production-shadcn orientam composição e estados. UI UX Pro Max oferece referências, subordinadas ao brief. A verificação de acessibilidade deve identificar exatamente as superfícies testadas; critérios sem evidências não recebem aprovação.
