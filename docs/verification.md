# Verificação da entrega

Ambiente: Windows, Java 21.0.12.1, Spring Boot 3.5.16, Flyway 11.20.3, PostgreSQL 18.3, Node 24.14.1 e Chrome. Execução local em 14/09/2026. Nenhuma publicação em produção.

## Backend

`Maven 3.9.16 -B test`: **12 testes, 0 falhas, 0 erros, 0 ignorados**. A suíte usou o PostgreSQL real `sinapse_test`, porta 55432, separado de `sinapse`. Não usou H2. Evidência: `backend/target/surefire-reports/br.com.espacosinapse.ClinicIntegrationTest.txt`.

Última conclusão JUnit registrada: 19:09. Uma repetição posterior por `scripts/test-backend.ps1` foi interrompida sem relatório final e não entra nessa contagem. Depois da última correção documental do OpenAPI, a compilação/empacotamento final (`package -DskipTests`, 19:16) passou; API e fluxos completos de navegador foram novamente executados contra esse JAR.

Cobertura executada: login/CSRF/logout, desativação de sessão, ADMIN/RECEPCAO, menor com responsável, versões obsoletas, disponibilidade/bloqueios, consultas adjacentes, concorrência, restrição SQL independente da aplicação, mudanças de configuração com consultas futuras, reagendamento/histórico, cancelamento, duração preservada, estados finais, separação público/privado, último administrador, troca de senha e exclusão de cadastros sem vínculo.

`node qa/api-smoke.mjs`: **13 cenários passaram** contra a aplicação em execução. Inclui duas requisições HTTP simultâneas para o mesmo horário: uma resposta 201 e uma 409. O script exporta o OpenAPI real para `docs/openapi.json`. Relatório: `output/playwright/api-smoke.json`.

## Frontend e integração

`npm.cmd --prefix frontend run build`: compilação TypeScript e build Vite executados com sucesso. As dependências instaladas não apresentaram vulnerabilidades no relatório do npm da execução; isso não equivale a uma auditoria de segurança.

`node qa/smoke.mjs`: testa login ADMIN; serviço; profissional e vínculo; disponibilidade; paciente; consulta; recarga/persistência; conflito; confirmação; reagendamento; cancelamento; edição/inativação/reativação; configurações; recepção e HTTP 403; troca de senha e encerramento de sessão. Fixtures são fictícias, identificadas e permanecem apenas no banco local de demonstração. Consultas criadas pelo fluxo bem-sucedido terminam canceladas.

Rodada final: **15 cenários passaram**, sem erros JavaScript capturados. A troca de senha foi testada em uma conta fictícia de recepção, sem alterar a senha do administrador inicial.

Relatório e capturas: `output/playwright/e2e.json`, `agenda-persisted.png`, `appointment-conflict.png`, `appointment-history.png`, `reception-restriction.png`.

## Acessibilidade e revisão visual

`node qa/visual.mjs` verifica site, login e sete telas internas em 360, 390, 768 e 1440 px; diálogo de paciente em 360/1440; reflow a 320 px; espaçamento ampliado; cores forçadas e movimento reduzido. Usa axe-core, medidas do DOM, capturas e teclado real do navegador. Não confundir um viewport de 320 px com teste completo do zoom nativo de 400%.

O relatório `output/playwright/visual.json` registra por estado as violações, checagens inconclusivas, dimensões e capturas. Mede pixels do anel de foco e calcula contraste da combinação observada. As capturas foram revisadas visualmente. Isso não é uma certificação WCAG.

Rodada final: **41 estados, nenhuma violação automática ou rolagem horizontal**. As 18 movimentações de Tab permaneceram no diálogo, e Escape devolveu o foco ao botão de abertura. Anel de foco medido: 3 px, 1.268 pixels da cor prevista na amostra, contraste 6,02:1 com o fundo. Texto branco na superfície escura: 12,17:1. Restam duas verificações de contraste inconclusivas do axe (legenda/botão no diálogo móvel e marca no rodapé em cores forçadas), registradas no relatório para inspeção assistiva/manual adicional.

A aplicação da skill `accessimind-accessible-ui-agent-skill` motivou testes de teclado, foco, contraste, reflow e a tentativa com NVDA. O harness detectou NVDA, mas falhou ao iniciá-lo (`NVDA cannot be started`), sem produzir leitura/announcements válidos. Evidência: `output/playwright/nvda.json`. A aprovação completa por tecnologia assistiva permanece **não verificada**; repetir manualmente com NVDA operacional, incluindo login, formulários, mensagens de erro, diálogos, agenda e mudanças de status.

O navegador integrado do Codex não estava disponível nesta sessão; a verificação foi realizada com Playwright e o Chrome instalado. Nenhum contato por WhatsApp, telefone ou e-mail foi enviado pelos testes.

## Infraestrutura e limitações

- Compose, arquivos de build e inicialização PostgreSQL revisados estaticamente. YAML analisado; `bash -n docker/init-database.sh` passou. Docker não está instalado: imagens, Compose, Nginx e backup/restauração em container **não foram executados**.
- O Maven Wrapper POSIX e suas propriedades estão incluídos. O gerador oficial criou e executou `mvnw.cmd`, mas o arquivo não permaneceu disponível neste ambiente Windows; causa não confirmada. Os scripts Windows resolvem Maven instalado/cache sem desativar proteções. Em outro Windows sem Maven, instale Maven 3.9.x ou regenere o wrapper oficial. Não confundir essa limitação com falha do build Java, que passou.
- O ambiente local usa banco/app distintos de serviços já existentes. A porta 8080 estava ocupada e não foi alterada; a API deste projeto usa 8081.
- Não foram realizados teste de carga, pentest independente, restauração real de backup, entrega de mensagens, validação clínica, revisão jurídica ou homologação com a equipe da clínica.
- O catálogo é carregado em listas completas nos seletores. Adequado à demonstração de uma clínica; grandes volumes exigem busca remota/autocomplete e revisão de desempenho.
- Antes de dados reais: confirmar os itens de `clinic-source.md`, revisar retenção/perfis, configurar HTTPS/cookie Secure e executar homologação e recuperação de backup.
