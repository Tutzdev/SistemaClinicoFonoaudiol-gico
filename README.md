# Sistema Clínico Fonoaudiológico

Sistema de gestão para clínicas de fonoaudiologia, com site institucional, painel administrativo e API desenvolvida em **Java 21 e Spring Boot**.

Centraliza cadastros, disponibilidade dos profissionais e agendamentos, com controle de acesso e histórico das alterações.

## Funcionalidades

* Autenticação com perfis de administrador e recepção
* Gerenciamento de pacientes e responsáveis
* Cadastro de profissionais e serviços
* Configuração de disponibilidade e bloqueios de horário
* Agenda com filtros por período, profissional, paciente e status
* Agendamento, confirmação, reagendamento e cancelamento
* Prevenção de conflitos de horário para pacientes e profissionais
* Histórico de consultas e auditoria de operações
* Site institucional com serviços, profissionais e contato

## Tecnologias

* **Backend:** Java 21, Spring Boot, Spring Security e Spring Data JPA
* **Frontend:** React, TypeScript e Vite
* **Banco de dados:** PostgreSQL e Flyway
* **Testes:** JUnit 5, Testcontainers e Playwright
* **Infraestrutura:** Docker e Docker Compose

## Como rodar

Com Docker e Docker Compose instalados:

1. Copie `.env.example` para `.env`.
2. Configure as credenciais do banco e do administrador.
3. Execute:

```bash
docker compose up --build -d
```

Acesse http://localhost:8088 e entre na área interna com as credenciais configuradas.

## Documentação

Detalhes da arquitetura, contrato da API e verificações estão em [`docs/`](docs/).
