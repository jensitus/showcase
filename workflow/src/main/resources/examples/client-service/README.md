# Workflow API Client Configuration

This directory contains example code for configuring a client service to communicate with the Workflow API using API key authentication.

## Setup Steps

### 1. Get an API Key

**Development:**
Start the workflow service with `dev` profile. The API key will be printed to the console:
```
DEVELOPMENT API KEY GENERATED
Service: camunda-service
API Key: <your-key-here>
```

**Production:**
Generate via admin API (requires ADMIN role):
```bash
curl -X POST "http://localhost:8080/api/admin/api-keys?serviceName=your-service-name" \
  -H "Authorization: Bearer <admin-jwt-token>"
```

### 2. Configure Your Service

Copy the configuration classes to your service and update `application.yaml`:

```yaml
workflow-api:
  base-url: http://localhost:8080
  api-key: ${WORKFLOW_API_KEY:your-dev-key}
```

### 3. Set Environment Variable (Production)

```bash
export WORKFLOW_API_KEY=your-production-key
```

Or in Docker:
```yaml
environment:
  - WORKFLOW_API_KEY=your-production-key
```

Or in Kubernetes:
```yaml
env:
  - name: WORKFLOW_API_KEY
    valueFrom:
      secretKeyRef:
        name: workflow-api-secrets
        key: api-key
```

### 4. Use the Client

```java
@Autowired
private WorkflowApiClient workflowApiClient;

public void createTask() {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setName("My Task");
    // ... set other fields

    TaskDto task = workflowApiClient.createTask(request);
}
```

## Files

| File | Description |
|------|-------------|
| `WorkflowApiConfig.java` | Configuration properties class |
| `WorkflowApiClient.java` | REST client with API key auth |
| `ExampleUsage.java` | Example service using the client |
| `application.yaml` | Example configuration |

## Security Notes

- Never commit API keys to version control
- Use environment variables or secrets management
- Rotate keys periodically in production
- Monitor `last_used_at` in the `api_keys` table for audit
