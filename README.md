# Terning's Final Gift - Server

This is the high-performance back-end server for the 'Terning's Final Gift' platform. It is engineered to manage massive concurrent traffic and ensure data consistency during a real-time, first-come, first-served event.

## 🎯 Core Technical Challenges Solved

This server is specifically designed to solve the following problems:

- **High Concurrency (Thundering Herd):** Efficiently handles thousands of simultaneous requests at the event's start time without crashing.
- **Race Condition & Data Integrity:** Prevents overselling of limited-quantity gifts by using distributed locks and atomic operations, ensuring data consistency.
- **System Responsiveness:** Delivers immediate feedback to users by leveraging an asynchronous, event-driven architecture.

## 🏛️ Architecture

The system processes requests through an event-driven flow to maximize performance and resilience:

`User Request` → `API Server (Spring Boot)` → `Redis (Lock & Atomic Op)` → `Kafka (Message Queue)` → `Fast API Response to User`

Meanwhile, a separate `Kafka Consumer` processes the message queue to safely update the `MySQL Database` without impacting user-facing API performance.

## 🛠️ Tech Stack

- **Language**: Kotlin, Java
- **Framework**: Spring Boot, Spring Cloud Gateway
- **Database**: MySQL
- **In-Memory Store**: Redis (for Caching, Distributed Locks, Atomic Operations)
- **Message Queue**: Kafka
- **DevOps**: Docker, Docker Compose
- **API Documentation**: SpringDoc OpenAPI (Swagger)

## 🚀 Getting Started

### Prerequisites

- JDK 17 or higher
- Docker and Docker Compose

### Build & Run

The easiest way to run the entire back-end system (including dependencies) is by using Docker Compose.

1.  **Navigate to the server directory:**
    ```bash
    cd server
    ```

2.  **Review configurations:**
    Key configurations for the database, Redis, and Kafka are located in `src/main/resources/application.yml`. You may adjust them if needed.

3.  **Run with Docker Compose:**
    This command will build the Spring Boot application and start all necessary services (Server, MySQL, Redis, Kafka, Zookeeper) in detached mode.

    ```bash
    docker-compose up --build -d
    ```

4.  **To shut down the services:**
    ```bash
    docker-compose down
    ```

## 🌐 API Endpoints

Once the server is running, the full API documentation is available via Swagger UI.

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Key Endpoints:
- `POST /api/auth/login`: User login.
- `POST /api/event/apply`: Apply for the event.
- `GET /api/event/status`: Check the user's event application status.

## ✅ Testing

Run the test suite using the Gradle wrapper.

```bash
./gradlew test

