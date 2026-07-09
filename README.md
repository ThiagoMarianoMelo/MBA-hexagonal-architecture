# MBA Hexagonal Architecture

Este repositório contém uma aplicação demonstrando arquitetura hexagonal com domínio, casos de uso e adaptadores de infraestrutura.

**Requisitos**
- JDK 17+ (recomendado)
- Gradle wrapper está no repositório (não precisa instalar Gradle globalmente)

**Como subir o projeto (modo desenvolvimento)**
- No Linux/macOS:

```bash
./gradlew bootRun
```

- No Windows (PowerShell / CMD):

```powershell
gradlew.bat bootRun
```

A aplicação usa Spring Boot e configurações do projeto dentro do módulo `infrastructure`.

**Rodar a suíte de testes**

```bash
# Linux / macOS
./gradlew test

# Windows
gradlew.bat test
```

Os testes de integração usam o profile `test` (`@ActiveProfiles("test")`) e uma base em memória conforme a configuração de `application-test.properties`.

**Rodar a suíte de testes**
- Executar todos os testes (unit + integração):

```bash
# Linux / macOS
./gradlew test

# Windows
gradlew.bat test
```

Os testes de integração usam o profile `test` (`@ActiveProfiles("test")`) e uma base em memória conforme a configuração de `application-test.properties`.

Rodar apenas os testes unitários
- Executa apenas os testes cujo nome termina com `Test` (padrão usado neste repositório para testes unitários):

```bash
# Linux / macOS
./gradlew test --tests "*Test"

# Windows
gradlew.bat test --tests "*Test"
```

Rodar apenas os testes de integração
- Caso queira executar apenas os testes de integração (nome terminando com `IT` neste projeto):

```bash
# Linux / macOS
./gradlew test --tests "*IT"

# Windows
gradlew.bat test --tests "*IT"
```
**Onde a cascata de cancelamento acontece**
- O evento de domínio que inicia a cascata é gerado por `Event.cancel()` em [domain/src/main/java/br/com/fullcycle/domain/event/Event.java](domain/src/main/java/br/com/fullcycle/domain/event/Event.java).
- Ao persistir o agregado (via `EventDatabaseRepository`), os `DomainEvent`s do evento são serializados e gravados na tabela de outbox por [infrastructure/src/main/java/br/com/fullcycle/infrastructure/repositories/EventDatabaseRepository.java](infrastructure/src/main/java/br/com/fullcycle/infrastructure/repositories/EventDatabaseRepository.java).
- Em produção um relay/outbox-publisher publica esses JSONs em uma fila; em runtime o consumidor que recebe a mensagem e roteia para os casos de uso é implementado em [infrastructure/src/main/java/br/com/fullcycle/infrastructure/gateways/ConsumerQueueGateway.java](infrastructure/src/main/java/br/com/fullcycle/infrastructure/gateways/ConsumerQueueGateway.java).
- O `ConsumerQueueGateway` identifica eventos do tipo `event.cancelled` e chama o caso de uso `CancelEventTicketsUseCase` ([application/src/main/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCase.java](application/src/main/java/br/com/fullcycle/application/ticket/CancelEventTicketsUseCase.java)), que consulta `TicketRepository.ticketsByEventId(...)` e marca os ingressos como `CANCELLED` (implementação de exemplo: [infrastructure/src/main/java/br/com/fullcycle/infrastructure/repositories/TicketDatabaseRepository.java](infrastructure/src/main/java/br/com/fullcycle/infrastructure/repositories/TicketDatabaseRepository.java)).

Em resumo: `Event.cancel()` -> outbox (ao salvar o agregado) -> publicação/relay -> `ConsumerQueueGateway` -> `CancelEventTicketsUseCase` -> atualização dos tickets.