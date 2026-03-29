## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running

No other dependencies (Java, Node.js, MongoDB) are required — everything runs inside Docker containers.

### Running the Application

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/esg-platform.git
   cd esg-platform
   ```

2. Start all services:
   ```bash
   docker-compose up --build
   ```

3. Wait for the backend to fully initialize (approximately 20–30 seconds). You'll know it's ready when you see:
   ```
   Started EsgplatformApplication in XX seconds
   ```

4. Access the application:
   - **Frontend:** http://localhost:4200
   - **API:** http://localhost:8080/api/companies

### Stopping the Application

Press `Ctrl+C` in the terminal running Docker Compose, or run from a separate terminal:

```bash
docker-compose down
```

To stop and remove all stored data:

```bash
docker-compose down -v
```

### API Authentication

GET endpoints are publicly accessible. POST, PUT, and DELETE endpoints require HTTP Basic Authentication:

- **Username:** `admin`
- **Password:** `esg-admin-2024`