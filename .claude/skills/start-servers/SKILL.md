---
name: start-servers
description: Start all three servers (PostgreSQL, Backend, Frontend) for this project on their designated ports. Always checks for port conflicts first and kills any occupying process before starting.
---

# start-servers

Start all services for this SNS app in the correct order and on the correct ports.

## Fixed Port Assignment

| Service    | Port | Must not change |
|------------|------|-----------------|
| PostgreSQL | 5432 | docker-compose.yml |
| Backend    | 8080 | application.properties |
| Frontend   | 5173 | Vite default |

**Never start a service on an alternative port.** The Vite proxy and Spring Boot CORS config are hardcoded to these ports.

## Steps to Execute

### 1. Check and free all three ports

For each port (5432, 8080, 5173):
- Run: `netstat -ano | grep ":<PORT>"`
- If any PID is shown, kill it: `taskkill /PID <PID> /F`
- Confirm the port is free before proceeding

### 2. Ensure Docker Desktop is running

```bash
docker info
```

If Docker is not running, start Docker Desktop:
```bash
start "" "C:/Program Files/Docker/Docker/Docker Desktop.exe"
```
Wait until `docker info` succeeds (poll every 10 seconds, up to 2 minutes).

### 3. Start PostgreSQL on port 5432

```bash
cd <project-root>
docker compose up -d postgres
```

Wait until status is `healthy`:
```bash
docker compose ps postgres
```

### 4. Start Backend on port 8080

```bash
cd <project-root>/backend
./gradlew bootRun > /tmp/backend.log 2>&1 &
```

Poll until ready (up to 60 seconds):
```bash
curl -s http://localhost:8080/api/health
```
Expected response: `{"status":"UP", ...}`

### 5. Start Frontend on port 5173

```bash
cd <project-root>/frontend
npm run dev > /tmp/frontend.log 2>&1 &
```

Poll until ready:
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:5173
```
Expected: `200`

### 6. Verify all three services

```bash
# PostgreSQL
docker compose ps postgres

# Backend
curl -s http://localhost:8080/api/health

# Frontend
curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost:5173
```

Report the status of each service to the user. If all three are up, open `http://localhost:5173` in the browser:
```bash
start "" "http://localhost:5173"
```

## Port Conflict Resolution

If a port is occupied by an unknown process:
1. Identify: `netstat -ano | grep ":<PORT>"`
2. Kill: `taskkill /PID <PID> /F`
3. Verify port is free, then start the service

**Do NOT use an alternative port under any circumstance.**
