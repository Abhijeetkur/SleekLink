# 🚀 SleekLink: Scalable URL Shortener & Analytics Engine

A high-performance, distributed URL shortener built with a microservice-oriented architecture. Designed to handle high-throughput traffic, this system provides near-instantaneous URL redirection, real-time analytics processing via event streaming, and secure dashboard access.

Built as a demonstration of scalable system design principles for Software Engineering roles.

---

## 🏗️ System Architecture & Design Choices
SleekLink is built to solve the classic "URL Shortener" system design interview question, emphasizing high availability (HA), low latency, and asynchronous event processing.

1. **High-Speed Redirection (`Read-Heavy` Optimization)**:
   - Utilizes **Redis** as a distributed caching layer. When a user requests a short link, the system hits Redis first, ensuring sub-millisecond resolution times.
   - Cache misses fallback to the primary **PostgreSQL** database and automatically re-populate the cache.
   
2. **Asynchronous Analytics Processing (`CQRS Pattern`)**:
   - Redirecting a user and recording analytics are decoupled. 
   - Click events (IP, User-Agent, Timestamp) are published to an **Apache Kafka** topic.
   - A background consumer asynchronously processes these streams, parsing User-Agent data, updating Redis counters, and syncing to PostgreSQL.
   - *Why?* This prevents database write locks from slowing down the critical path of redirecting a user.

3. **Stringent Relational Data Modeling (`JPA Mapping`)**:
   - To securely maintain distributed counts without orphans, the database enforces strict `@ManyToOne` foreign keys mapping all aggregated analytical data (e.g., Browsers, Geos, OS) directly back to centralized `UrlMapping` entities. 
   - This strict relational model utilizes multi-column unique indexes to prevent the Kafka workers from inserting duplicate race-condition rows concurrently.

4. **Secure Dashboard Access**:
   - Implements Stateless **JWT (JSON Web Token)** Authentication using Spring Security.
   - Secures the React-based analytics dashboard, while keeping the shortening and redirection endpoints natively public.

---

## ✨ Key Features
- **URL Shortening:** Generate collision-resistant short aliases for long URLs.
- **Lightning Fast Redirects:** Sub-millisecond read response times powered by Redis.
- **Deep Analytics Dashboard:** Track granular metrics including:
  - Total click volume and lifetime engagements
  - Time-series data (Daily traffic, Hourly heatmaps)
  - Technical metrics (Operating system, Browser types, Device formats)
  - Geographic distribution tracking
- **Event-Driven Architecture:** Uses Kafka to process clickstreams asynchronously without blocking client redirects.
- **Secure Authentication:** JWT-based login and registration system for managing application access.
- **Modern UI:** Responsive, glassmorphism-styled React dashboard with interactive `Recharts` visualizations.

---

## 🛠️ Technology Stack

### Backend
* **Language & Framework:** Java 17, Spring Boot 3
* **Security:** Spring Security, JSON Web Tokens (JJWT)
* **Message Broker:** Apache Kafka (Event Streaming)
* **Databases & Caching:** PostgreSQL (Relational DB), Redis (In-memory Cache)
* **Data Parsing:** Browscap-java (User-Agent Parsing)

### Frontend
* **Core:** React 18, Vite
* **Styling:** Vanilla CSS, CSS Modules (Glassmorphism design)
* **Visualizations:** Recharts (Data visualization)
* **Icons:** Lucide-React

---

## 🚀 Getting Started

### Prerequisites
- JDK 17+
- Node.js 18+ & npm
- PostgreSQL running on port 5432
- Redis running on port 6379
- Apache Kafka & Zookeeper clustered locally (default port 9092)

### 1. Database Setup
Create a PostgreSQL database database named `url_shortener` (or update `application.properties` to match your local setup).

### 2. Backend Setup
Navigate to the backend directory and configure your `application.properties` with your database credentials and preferred JWT Secret.

```bash
cd backend
# Compile and run the Spring Boot application
mvn clean install
mvn spring-boot:run
```
*The backend will boot up on `http://localhost:8080`*

### 3. Frontend Setup
Navigate to the frontend directory to run the React application.

```bash
cd frontend
# Install dependencies
npm install

# Start the Vite development server
npm run dev
```
*The UI will boot up on `http://localhost:5173`*

---

## 🧠 What I Learned & Future Enhancements
Developing this project reinforced patterns commonly used in planetary-scale systems:
- **Event sourcing** and decoupling microservices.
- **Read-Through / Write-Behind** caching strategies.
- Handling race conditions within distributed counters.

**Future Considerations:**
- Implement a **Rate Limiter** (e.g., Token Bucket via Redis) to prevent malicious scraping/DDoS.
- Integrate **Docker** and `docker-compose` to containerize all dependencies for 1-click bootup.
- Add horizontal autoscaling for the Kafka consumer pool to handle extreme spikes in traffic.
