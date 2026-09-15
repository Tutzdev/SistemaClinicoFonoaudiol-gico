# AGENTS.md — Meu jeito de desenvolver backend Java

## Missão

Atue como minha extensão técnica ao trabalhar neste backend. Quero código simples, bonito de ler, explícito, seguro e fácil de entender, depurar e modificar. Faça o código pertencer ao projeto, inclusive nos nomes, espaços, quebras de linha e organização das responsabilidades.

Meu objetivo não é usar o maior número de recursos do Java. É resolver o problema com clareza e manter o controle sobre o que foi escrito.

Este documento orienta decisões; não substitui a leitura do código atual e não instala skills. As preferências observadas abaixo são uma fotografia de referências públicas, não uma afirmação de que todos os arquivos ou commits foram escritos pessoalmente por mim.

## 1. Como decidir

Siga esta ordem:

1. Requisitos e instruções explícitas da tarefa.
2. Correção, segurança, integridade dos dados e contratos existentes.
3. Convenções configuradas e padrões consistentes do módulo afetado.
4. Minhas preferências descritas neste arquivo.
5. Clean Code, Effective Java e SOLID aplicados com bom senso.

Reproduza meu estilo, sem reproduzir falhas ou trechos incompletos. Um padrão visual não justifica uma vulnerabilidade. Quando precisar se afastar de um padrão por um problema concreto, faça a menor mudança coerente e explique a razão.

Não transforme preferências em proibições absolutas. Um getter curto pode ser claro; uma expressão com várias regras escondidas pode não ser. Julgue pela leitura e pelo comportamento.

## 2. Leia antes de escrever

Antes de implementar:

- Leia as instruções aplicáveis ao diretório.
- Examine `pom.xml`, wrapper, versão do Java e dependências existentes.
- Confira `.editorconfig`, formatter e lint, quando existirem.
- Leia uma implementação semelhante no domínio afetado: controller, service, entidade, DTO e repository, conforme a tarefa.
- Localize o tratamento de erros, a segurança e os testes relacionados.
- Identifique contratos públicos, regras de negócio e possíveis efeitos colaterais.

Use o módulo atual como referência principal. Os repositórios de referência servem para desempate e orientação inicial, não para impor a mesma arquitetura a qualquer projeto.

Não é necessário consultar meu GitHub em toda alteração. Reutilize o contexto que já foi verificado. Se não houver acesso, trabalhe com o código disponível e não invente observações.

## 3. Minha assinatura de código

### Fluxo explícito

Quero ler um método de cima para baixo e entender a ação:

1. Buscar o que a operação precisa.
2. Verificar permissões e pré-condições antes de efeitos sensíveis.
3. Validar as regras da operação.
4. Criar ou alterar o objeto.
5. Persistir quando necessário.
6. Produzir o resultado.

Adapte a ordem quando uma validação precisar acontecer antes da busca. Não desreferencie uma entrada antes de validar sua presença quando isso fizer parte do contrato do método.

Use variáveis intermediárias quando elas derem nome a uma etapa. Não comprima busca, validação, mutação e mapeamento em uma expressão difícil de acompanhar.

### Nomes

- Use vocabulário do domínio e verbos diretos.
- Preserve o idioma do módulo: `criar`, `buscarEntidade` e `marcarComoEnviado` fazem sentido no AgroNotify; `create`, `findAppointment` e `changeStatus` aparecem nas referências em inglês.
- Não traduza identificadores existentes apenas para padronizar sua preferência.
- Mensagens ao usuário podem estar em português mesmo com identificadores em inglês. Siga o contrato do projeto.
- Use nomes como `appointmentRepository`, `startTime`, `responsibleUser` e `clienteAtualizado` quando representarem os conceitos reais.
- Evite nomes vagos como `obj`, `aux`, `temp` e `processStep1`.
- Não alongue nomes sem informação adicional. `id`, `request` e `response` podem ser claros no contexto.

### Formatação

Nos arquivos Java implementados da amostra, quatro espaços por nível são recorrentes. Adote isso somente quando não houver configuração ou padrão local diferente.

- Abra chaves na mesma linha da declaração.
- Separe campos, construtor e métodos com linhas em branco.
- Dentro do método, separe etapas lógicas; mantenha juntas as atribuições relacionadas.
- Para assinaturas e chamadas extensas, prefira argumentos em várias linhas quando facilitar a leitura.
- Nas referências do BarberShop, assinaturas multilinha frequentemente deixam `) {` em uma linha própria. Preserve esse formato quando ele for o padrão local.
- Quebre cadeias de chamadas de forma legível, sem obrigar que cada chamada trivial ocupe uma linha.
- Há alinhamento horizontal de campos, atribuições e getters na agenda jurídica. Preserve blocos existentes, sem expandir esse alinhamento para todo o projeto.
- Getters triviais em uma linha são aceitáveis quando já adotados pela entidade. Métodos com decisões e efeitos merecem corpo expandido.
- Não replique espaçamentos acidentais, linhas com whitespace residual ou instruções coladas à abertura de métodos.
- Não aplique uma ordenação universal de imports: ela varia nas referências. Siga a configuração e a vizinhança, removendo imports sem uso.
- Não converta tabs de XML para espaços apenas porque os arquivos Java usam espaços.
- Não reformate arquivos alheios à tarefa.

Exemplo ilustrativo de fluxo, sem exigir esta assinatura em todo service:

```java
@Transactional
public Appointment cancel(Long id) {
    Appointment appointment = findAppointment(id);

    appointment.cancel();

    return appointment;
}
```

O exemplo pressupõe entidade gerenciada em uma transação e autorização resolvida no fluxo apropriado. Ele não determina o contrato HTTP nem dispensa controles de acesso.

## 4. Arquitetura proporcional

Organize por domínio quando esse for o padrão do projeto.

As referências mostram variações válidas:

- AgroNotify: classes de cliente próximas dentro de `cliente`, sem subpastas obrigatórias por camada.
- BarberShopAgendamento: domínio funcional com `controller`, `domain`, `dto/request`, `dto/response`, `repository` e `service`.
- Agenda jurídica: classes de tarefa próximas em `task`, com DTOs em `task/dto` e responsabilidades compartilhadas em outros pacotes.
- EscritorioMedicoBase: estrutura com `entity`, `dto`, `service`, `mapper` e outras subpastas; arquivos amostrados estavam vazios e não comprovam implementação dessas responsabilidades.

Não renomeie `domain` para `entity`, ou o inverso, por preferência. Não crie todas as pastas de antemão. Não introduza arquitetura hexagonal, CQRS, eventos ou camadas extras sem uma necessidade real.

Uma classe deve ter uma responsabilidade identificável. Extraia uma classe quando houver coesão própria, não para deixar a árvore de diretórios mais sofisticada.

Não crie interface para todo service. Não crie factories, builders, strategies, managers ou wrappers sem benefício concreto. Use composição antes de recorrer à herança.

## 5. Métodos e objetos

- Dê a cada método uma operação clara ou uma ação de negócio compreensível.
- Extraia regras com nomes significativos, como `validateAvailability`, `validateConflict` e `findAppointment`, quando o contexto justificar.
- Não fragmente uma regra simples em vários arquivos ou métodos sem significado.
- Prefira guard clauses quando reduzirem aninhamento.
- Evite parâmetros booleanos ambíguos.
- Reduza listas de parâmetros quando existir um agrupamento de domínio real; não crie objetos intermediários apenas para passar tudo adiante.
- Use streams para transformação, filtro e consulta legíveis. Um `for` simples é adequado quando facilita a leitura ou o fluxo de controle.
- Evite efeitos colaterais escondidos em streams e encadeamentos de Optional.
- Use tipos explícitos quando ajudarem a leitura. Não introduza `var` em massa.

Entidades como `Appointment` e `Task` demonstram operações nomeadas: cancelar, reagendar, atualizar detalhes e mudar status. Prefira esse caminho quando houver invariantes a preservar.

Isso não exige converter todo CRUD em domínio rico. O AgroNotify também contém mutações explícitas por setters. Preserve o modelo local quando for suficiente; encapsule alterações que precisam permanecer consistentes.

Use construtor protegido sem argumentos para JPA quando apropriado. Construtores públicos devem receber o necessário para uma criação válida. Não coloque chamadas a repositories ou serviços externos em entidades.

## 6. Contratos, DTOs e mapeamento

- Mantenha o contrato da API separado da persistência.
- DTOs podem ser records ou classes: ambos aparecem nas referências. Siga o módulo.
- Use records para dados de transporte imutáveis quando a stack e o contrato permitirem; não os imponha às entidades JPA.
- Respeite nomes existentes como `ClienteRequest`, `CreateAppointmentRequest` e `TaskResponse`.
- Não crie múltiplas camadas de DTOs idênticos sem motivo.
- Prefira mapeamento explícito quando ele for curto e fácil de ler.

O local do mapeamento varia: construtor de resposta no AgroNotify, método privado no controller de agendamento e `TaskMapper` dedicado na agenda jurídica. Portanto, não imponha mapper separado a toda transformação.

Extraia o mapeamento quando ele crescer ou for reutilizado. Não introduza MapStruct ou outra dependência só para substituir poucas atribuições claras.

## 7. Spring e responsabilidades

### Controller

Cuida de HTTP: entrada, validação estrutural, delegação e resposta. Não acessa banco diretamente nem decide regras de negócio complexas.

Respeite métodos HTTP, status e contratos existentes. Use rotas orientadas a recursos; ações como cancelamento e reagendamento podem ter endpoints próprios quando expressarem o domínio.

### Service

Coordena a ação da aplicação, incluindo autorização contextual, consultas e regras que dependem de outros objetos ou da persistência.

Não obrigue todos os services a retornarem DTOs: as referências usam tanto entidades internamente quanto respostas mapeadas. O que não deve vazar inadvertidamente é o modelo de persistência no contrato HTTP.

### Repository

Concentra acesso à persistência. Use métodos derivados quando legíveis. Se o nome ficar difícil de verificar, considere consulta explícita ou specifications adequadas ao problema. Avalie o SQL e o volume de dados, não apenas o tamanho do Java.

### Injeção e dependências

Prefira campos `private final` e construtor explícito, padrão recorrente na amostra. Não introduza injeção em campos.

A presença de Lombok no build não significa que todo arquivo deva usá-lo. Preserve o padrão implementado ao redor. Não adicione `@Data` a entidades indiscriminadamente.

Use anotações Spring conforme a responsabilidade. Não transforme todo objeto em bean.

## 8. Effective Java sem exagero

- Prefira imutabilidade em valores e contratos quando prática.
- Não exponha coleções mutáveis internas sem necessidade; considere também a mutabilidade dos elementos.
- Use enums para conjuntos fechados com significado de domínio.
- Trate `equals`, `hashCode` e `toString` conscientemente, especialmente em entidades e relacionamentos lazy.
- Evite Optional em campos e parâmetros por hábito. Use-o quando ausência fizer parte do contrato de retorno.
- Não retorne null onde uma coleção vazia ou um contrato explícito for a solução estabelecida.
- Não capture exceções apenas para ignorá-las.
- Feche recursos sob sua responsabilidade com mecanismos apropriados.
- Use BigDecimal para valores monetários quando aplicável, com arredondamento definido.
- Não faça micro-otimizações sacrificando clareza sem evidência de um problema.

## 9. Validação e erros

Separe três responsabilidades:

1. Estrutura de entrada: Bean Validation e validação na fronteira.
2. Invariantes: regras que o objeto não pode violar.
3. Regras da operação: permissões, disponibilidade, conflitos e dados externos.

Uma annotation no DTO não protege automaticamente chamadas internas ao service. Valide pré-condições onde o contrato realmente exigir.

Use exceções específicas quando elas distinguirem falhas úteis ao domínio. IllegalArgumentException e IllegalStateException podem fazer sentido para contratos internos; não crie uma classe de exceção para cada `if` por obrigação.

Mantenha tradução consistente para HTTP no tratamento centralizado quando existente. Diferencie entrada inválida, ausência, conflito e falta de acesso. Não retorne stack traces, SQL, detalhes internos ou segredos ao cliente. Não converta todo erro em sucesso ou em 400 genérico.

## 10. Persistência, transações e concorrência

- Delimite transações por operações que precisam ser atômicas.
- Use transações de leitura quando adequadas.
- Entenda o mecanismo de proxy; não conte com autochamadas para iniciar uma nova transação.
- Em entidades gerenciadas, alterações podem ser persistidas por dirty checking. Não adicione `save` redundante mecanicamente nem o remova sem compreender o estado da entidade.
- Não use EAGER ou Open Session in View para esconder um problema de consulta.
- Verifique N+1, paginação, limites e índices relevantes.
- Use constraints para garantir integridade quando cabível.
- Se existir Flyway, versione mudanças; não edite migrations já aplicadas em ambientes compartilhados como se fossem arquivos descartáveis.

Consultar se existe conflito e depois inserir não garante exclusão entre requisições simultâneas. Em agenda ou reserva, avalie uma proteção compatível com a regra e o banco: constraint apropriada, bloqueio de recurso estável ou estratégia transacional com tratamento de conflito.

Não prometa que `@Transactional` ou `@Version` sozinhos impedem duas novas reservas sobrepostas. Escolha e teste o mecanismo para a situação concreta.

Não mantenha uma transação longa aberta durante chamadas externas sem necessidade. Para integrações com tentativas repetidas, avalie timeout, idempotência e tratamento de falha conforme a operação exigir.

## 11. Segurança prática

Estas são exigências de qualidade, não afirmações de que todos os repositórios já as cumprem:

- Nunca versionar senhas, tokens, chaves ou credenciais reais.
- Não registrar senhas, tokens ou dados pessoais desnecessários em logs.
- Usar hash apropriado para senhas conforme a stack.
- Autorizar operações no servidor; não confiar no papel ou no proprietário enviados pelo frontend.
- Verificar acesso ao objeto solicitado, não apenas se houve login.
- Em rotas públicas, limitar campos e operações ao que o público realmente pode acessar.
- Não considerar um ID conhecido como autorização.
- Não copiar `permitAll`, CORS amplo ou CSRF desabilitado sem avaliar o modelo de autenticação e as rotas.
- Preservar proteção CSRF em autenticação por cookies quando aplicável; avaliar explicitamente APIs com bearer token.
- Usar consultas parametrizadas e validar campos dinâmicos de ordenação.
- Limitar paginação e consumo de recursos onde necessário.
- Não aplicar campos sensíveis diretamente a partir de um DTO genérico.

Mudanças em autenticação e permissões exigem verificar também os cenários negados.

## 12. Tempo e testes

Para regras que dependem de “agora”, considere `Clock` injetável. A agenda jurídica contém esse padrão e teste com relógio fixo. Não trate isso como regra já uniforme em todas as entidades.

Defina o significado de data, hora e fuso para o domínio. Não substitua todos os LocalDateTime por Instant sem avaliar contratos. Torne testes temporais determinísticos.

Use o stack de testes do projeto. Priorize comportamento:

- Caminho principal.
- Entradas e intervalos inválidos.
- Recurso inexistente.
- Acesso negado e acesso de outro usuário.
- Conflitos e transições de estado.
- Limites temporais.
- Persistência e concorrência quando afetadas.

Use testes unitários para regras isoladas e integração para comportamento dependente do banco ou framework. Não adicione Testcontainers por hábito, mas não considere um teste H2 prova de comportamento específico do PostgreSQL.

Não crie testes de getters apenas para aumentar cobertura. Não confunda arquivos de teste vazios com cenários implementados. Nomes dos testes devem comunicar o comportamento.

## 13. Mudanças e comunicação

- Faça a menor alteração que resolva o problema inteiro.
- Preserve contratos e comportamento fora do escopo.
- Não renomeie classes, traduza o projeto ou adicione dependências sem necessidade.
- Não faça refatoração oportunista de arquivos não relacionados.
- Comente o motivo de decisões incomuns, não a sintaxe óbvia.
- Não use comentários para compensar nomes confusos.
- Tome decisões rotineiras autonomamente; pergunte quando a ambiguidade afetar regra de negócio, contrato ou operação irreversível.
- Resuma resultado, verificação e limitações com linguagem direta.

## 14. Verificação proporcional

Antes de concluir:

1. Revise o diff, imports, formatação e arquivos alterados.
2. Compile e execute testes relevantes quando o ambiente permitir.
3. Amplie para a suíte completa se houver impacto transversal, exigência do projeto ou risco ainda não resolvido.
4. Verifique migrations, contrato e segurança quando afetados.
5. Declare exatamente o que foi executado e o que ficou bloqueado.

Prefira o Maven Wrapper do projeto. Não atualize versões do Java ou Spring Boot só porque um repositório de referência usa outra versão.

Não afirme que o código foi testado sem executar as verificações. Não continue repetindo testes sem uma razão concreta.

## Regra final

O código deve parecer parte natural do meu projeto: claro nos nomes, organizado visualmente, explícito nas regras e seguro nas mudanças.

Antes de adicionar complexidade, pergunte: isso resolve um problema real e facilita a próxima manutenção?

Antes de imitar um trecho, pergunte: isso é um padrão consistente ou um detalhe isolado?

Quando houver dúvida de estilo, leia o código próximo. Quando houver dúvida de segurança ou correção, investigue o comportamento.

## Referências da revisão

Revisão realizada em 15/09/2026 sobre uma amostra de arquivos públicos da branch `main`. Não foi uma auditoria completa e os projetos não foram compilados nesta revisão documental.

- [AgroNotify — ClienteService](https://github.com/Tutzdev/AgroNotify/blob/2e9d2f9fbe8668b53661024cf83213ff1a06634d/src/main/java/com/agronotify/cliente/ClienteService.java): fluxo explícito, nomes em português, separação visual e injeção por construtor.
- [AgroNotify — ClienteRequest](https://github.com/Tutzdev/AgroNotify/blob/2e9d2f9fbe8668b53661024cf83213ff1a06634d/src/main/java/com/agronotify/cliente/ClienteRequest.java): DTO em classe e validação de entrada.
- [BarberShop — AppointmentService](https://github.com/Tutzdev/BarberShopAgendamento/blob/6979cd7bd042b18025243eafedf4f9cad2fafc76/src/main/java/br/com/boostsites/barbershop/appointment/service/AppointmentService.java): parâmetros multilinha, métodos de validação e transações.
- [BarberShop — Appointment](https://github.com/Tutzdev/BarberShopAgendamento/blob/6979cd7bd042b18025243eafedf4f9cad2fafc76/src/main/java/br/com/boostsites/barbershop/appointment/domain/Appointment.java): invariantes, operações de domínio e getters compactos.
- [BarberShop — AppointmentController](https://github.com/Tutzdev/BarberShopAgendamento/blob/6979cd7bd042b18025243eafedf4f9cad2fafc76/src/main/java/br/com/boostsites/barbershop/appointment/controller/AppointmentController.java): delegação e mapeamento privado de resposta.
- [Agenda jurídica — TaskService](https://github.com/Tutzdev/sistema-agenda-juridica/blob/331bd271feda752eafd8c21bb65d5578d5bc3518/src/main/java/com/escritorio/agenda_juridica/task/TaskService.java): alinhamento, Clock, paginação limitada e validação de ordenação.
- [Agenda jurídica — Task](https://github.com/Tutzdev/sistema-agenda-juridica/blob/331bd271feda752eafd8c21bb65d5578d5bc3518/src/main/java/com/escritorio/agenda_juridica/task/Task.java): operações explícitas e transições de estado.
- [Agenda jurídica — TaskMapper](https://github.com/Tutzdev/sistema-agenda-juridica/blob/331bd271feda752eafd8c21bb65d5578d5bc3518/src/main/java/com/escritorio/agenda_juridica/task/TaskMapper.java): mapeamento dedicado.
- [Agenda jurídica — DeadlineClassifierTest](https://github.com/Tutzdev/sistema-agenda-juridica/blob/331bd271feda752eafd8c21bb65d5578d5bc3518/src/test/java/com/escritorio/agenda_juridica/task/DeadlineClassifierTest.java): teste de comportamento com Clock fixo.
- [EscritorioMedicoBase](https://github.com/Tutzdev/EscritorioMedicoBase): árvore e build consultados; Patient, PatientService e CreatePatientRequest estavam vazios. Não foram utilizados para inferir comportamento ou formatação Java.

Na amostra do BarberShop, AppointmentMapper e AppointmentServiceTest também estavam vazios. A existência dessas pastas e arquivos não foi interpretada como implementação concluída.
