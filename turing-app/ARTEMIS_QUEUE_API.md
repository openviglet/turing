# Artemis Queue Management API

Esta API REST permite gerenciar as filas do Apache Artemis no Turing-app.

## Endpoints Disponíveis

### Base URL: `/api/artemis`

### 1. Listar todas as filas
```http
GET /api/artemis
```
Retorna informações sobre todas as filas existentes no Artemis.

**Resposta:**
```json
[
  {
    "name": "indexing.queue",
    "messageCount": 5,
    "consumerCount": 1,
    "paused": false,
    "status": "ACTIVE",
    "temporary": false,
    "address": "indexing.queue"
  }
]
```

### 2. Obter informações de uma fila específica
```http
GET /api/artemis/{queueName}
```

**Exemplo:**
```bash
curl http://localhost:2700/api/artemis/indexing.queue
```

### 3. Listar mensagens em uma fila
```http
GET /api/artemis/{queueName}/messages?maxMessages=50
```

**Parâmetros:**
- `maxMessages` (opcional): Número máximo de mensagens a retornar (padrão: 50)

**Exemplo:**
```bash
curl http://localhost:2700/api/artemis/indexing.queue/messages?maxMessages=10
```

**Resposta:**
```json
[
  {
    "messageId": "ID:12345",
    "content": "Message content",
    "timestamp": "2025-09-23T22:30:00",
    "deliveryCount": 1,
    "type": "text",
    "size": 1024
  }
]
```

### 4. Pausar uma fila
```http
POST /api/artemis/{queueName}/pause
```

Pausa o processamento de mensagens na fila (suspende o consumo).

**Exemplo:**
```bash
curl -X POST http://localhost:2700/api/artemis/indexing.queue/pause
```

**Resposta:**
```json
{
  "success": true,
  "message": "Queue paused successfully",
  "queueName": "indexing.queue"
}
```

### 5. Retomar uma fila
```http
POST /api/artemis/{queueName}/resume
```

Retoma o processamento de mensagens na fila.

**Exemplo:**
```bash
curl -X POST http://localhost:2700/api/artemis/indexing.queue/resume
```

### 6. Iniciar uma fila (alias para resume)
```http
POST /api/artemis/{queueName}/start
```

### 7. Parar uma fila (alias para pause)
```http
POST /api/artemis/{queueName}/stop
```

### 8. Limpar uma fila
```http
DELETE /api/artemis/{queueName}/messages
```

Remove todas as mensagens da fila especificada.

**Exemplo:**
```bash
curl -X DELETE http://localhost:2700/api/artemis/indexing.queue/messages
```

**Resposta:**
```json
{
  "success": true,
  "message": "Queue cleared successfully",
  "queueName": "indexing.queue"
}
```

## Códigos de Status HTTP

- **200 OK**: Operação realizada com sucesso
- **400 Bad Request**: Erro na operação (ex: fila não encontrada ou operação não suportada)
- **404 Not Found**: Fila não encontrada
- **500 Internal Server Error**: Erro interno do servidor

## Notas Técnicas

- A API utiliza JMX (Java Management Extensions) para interagir com as MBeans do Artemis
- O Artemis está configurado no modo embedded no Turing
- As operações de pausa/retomada afetam apenas o consumo de mensagens, não a produção
- A documentação Swagger está disponível em `/swagger-ui.html`

## Exemplos de Uso com curl

```bash
# Verificar status de todas as filas
curl http://localhost:2700/api/artemis

# Ver mensagens na fila de indexação
curl http://localhost:2700/api/artemis/indexing.queue/messages

# Pausar processamento da fila
curl -X POST http://localhost:2700/api/artemis/indexing.queue/pause

# Retomar processamento
curl -X POST http://localhost:2700/api/artemis/indexing.queue/resume

# Limpar todas as mensagens da fila
curl -X DELETE http://localhost:2700/api/artemis/indexing.queue/messages
```

## Integração

Esta API pode ser integrada com ferramentas de monitoramento e scripts de administração para:
- Monitorar o status das filas
- Automatizar operações de manutenção
- Implementar alertas baseados no número de mensagens
- Realizar limpeza de filas em situações específicas