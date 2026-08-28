# SkyKyuu Backend

Backend oficial do **SkyKyuu: Wings Over the Court**, um jogo 3D de vôlei para navegador.

## Stack

- Java 25
- Spring Boot 4.1.1
- Spring WebFlux
- Reactor Netty
- Maven Wrapper

## Requisitos

- JDK 25
- Git

Não é necessário ter Maven instalado globalmente: o projeto inclui o Maven Wrapper.

## Como executar

No Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```bash
./mvnw spring-boot:run
```

Por padrão, a aplicação usa a porta `8080`. A variável de ambiente `PORT` pode definir outra porta.

## Como testar

No Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```

No Linux ou macOS:

```bash
./mvnw test
./mvnw verify
```

## Arquitetura

O backend será desenvolvido como um **monólito modular**. Os módulos de domínio serão adicionados de forma incremental, sem antecipar camadas ou integrações que ainda não são necessárias.

O multiplayer online terá arquitetura **server-authoritative**: o cliente enviará inputs e intenções, enquanto o servidor será responsável pelas regras, pelo estado da partida e pela validação dos resultados.

## API atual

| Método | Endpoint | Descrição |
| --- | --- | --- |
| `GET` | `/api/health` | Saúde pública do serviço |
| `GET` | `/actuator/health` | Saúde operacional via Actuator |

Resposta de `GET /api/health`:

```json
{
  "service": "skykyuu-backend",
  "status": "UP"
}
```

Neste estágio não há autenticação, banco de dados, WebSocket ou simulação de gameplay.
