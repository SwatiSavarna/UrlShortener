# FlashLink | High-Throughput URL Shortener & Click Analytics

A production-grade, distributed URL Shortener microservice built with **Spring Boot 3**, **Redis**, and **PostgreSQL / H2**, designed for low-latency redirects, high-concurrency read/write scaling, token-bucket rate limiting, and custom TTL link expiration.

---

## 🚀 Key Scalability & Performance Talking Points

### 1. Read-Heavy Asymmetry & Redis Cache-Aside Pattern
- **Access Pattern**: URL shortening systems typically exhibit a **100:1 to 1000:1 read-to-write ratio**.
- **Latency Optimization**: The redirection path (`GET /{shortCode}`) bypasses relational database queries completely when hot keys are cached in Redis (`shorturl:{shortCode}`), delivering responses in **$< 2\text{ ms}$**.
- **Negative Caching (Cache Penetration Defense)**:
  - When non-existent or brute-forced short codes are requested, the service caches a sentinel (`shorturl:notfound:{code}`) in Redis with a 60-second TTL.
  - This prevents malicious scrapers from exhausting database connection pools with random 404 lookups.

### 2. Zero-Latency Analytics via Asynchronous Event Decoupling
- **The Problem**: Synchronously parsing User-Agents, geolocating IPs, and persisting click events during HTTP redirection causes redirection latency to jump from $2\text{ ms}$ to $50\text{--}100\text{ ms}$, creating database write locks.
- **The Solution**: 
  - The redirect handler immediately returns an **`HTTP 302 Found`** with the target `Location` header.
  - An internal **`UrlClickEvent`** is emitted to Spring's event bus and handled out-of-band by a dedicated **`ThreadPoolTaskExecutor`** (`@Async("analyticsExecutor")`).
  - The worker pool is configured with **bounded queues** (10,000 capacity) and a **`CallerRunsPolicy`** rejection handler, ensuring backpressure protection without dropping analytical records under sudden traffic spikes.

### 3. Bijective Base62 Encoding & ID Generation
- **Alphabet**: `[0-9, a-z, A-Z]` (62 alphanumeric characters).
- **Collision-Free Addressing**:
  - Instead of hashing URLs (which suffers from collisions, requiring multi-roundtrip retry loops), database sequence IDs or distributed Snowflake IDs are bijectively mapped into Base62.
  - With a 7-character Base62 string, the total addressable namespace is:
    $$62^7 = 3,521,614,606,208 \quad (\approx 3.52\text{ Trillion unique URLs})$$
- **Vanity Custom Aliases**:
  - Supports vanity custom slugs (e.g. `/my-promo-2026`) with atomic uniqueness validation and cache pre-warming.

### 4. Distributed Token Bucket Rate Limiting
- **Quota Enforcement**:
  - URL Creation: **15 requests/minute** per IP (burst-friendly token bucket).
  - Redirection: **120 requests/minute** per IP (prevents automated link harvesting).
  - Analytics API: **60 requests/minute** per IP.
- **RFC-Compliant HTTP Headers**:
  - `X-RateLimit-Limit`: Maximum tokens available in the bucket.
  - `X-RateLimit-Remaining`: Current available tokens for the client.
  - `X-RateLimit-Reset`: Time in seconds until the bucket is completely replenished.
  - Status `HTTP 429 Too Many Requests` returned when exhausted.
- **Dual Engine**: Redis-backed for distributed multi-instance deployments, with transparent in-memory `ConcurrentHashMap` fallback for standalone local dev.

### 5. Custom Expiry & Redis TTL Synchronization
- **Flexible Expiration**: Supports duration strings (`1h`, `24h`, `7d`, `30d`) or explicit ISO-8601 timestamps.
- **TTL Synchronization**: The Redis cache TTL is calculated dynamically to match the exact remaining lifetime of the short link:
  $$\text{TTL}_{\text{Redis}} = \text{Duration}(\text{now}, \text{expiresAt})$$
- **Passive vs. Active Expiration**:
  - **Passive**: Any lookup of an expired link immediately yields `HTTP 410 Gone` and evicts any lingering cache entry.
  - **Active**: A background scheduled job (`@Scheduled`) periodically purges expired links and historical click records in indexed batches to eliminate database table bloat.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language & Framework** | Java 21 LTS, Spring Boot 3 / 4 |
| **Persistence** | Spring Data JPA, Hibernate, H2 (dev) / PostgreSQL 16 (prod) |
| **In-Memory Cache** | Spring Data Redis (Lettuce / Jedis) |
| **Concurrency & Async** | Spring `@Async` ThreadPoolTaskExecutor, Spring ApplicationEventPublisher |
| **Traffic Control** | Token Bucket Algorithm (Redis + In-Memory Fallback) |
| **Frontend UI** | React 18, Vite 6, Tailwind CSS, Lucide Icons, Chart.js, QRCode.react |
| **Containerization** | Docker, Docker Compose |

---

## 📦 API Endpoints

### 1. Shorten URL
```http
POST /api/v1/urls
Content-Type: application/json

{
  "originalUrl": "https://engineering.atspotify.com/2023/12/how-we-scale-distributed-systems",
  "customAlias": "spotify-scale",
  "ttlDuration": "24h"
}
```
**Response (`201 Created`):**
```json
{
  "shortCode": "spotify-scale",
  "shortUrl": "http://localhost:8080/spotify-scale",
  "originalUrl": "https://engineering.atspotify.com/2023/12/how-we-scale-distributed-systems",
  "createdAt": "2026-10-17T01:10:00Z",
  "expiresAt": "2026-10-18T01:10:00Z",
  "clickCount": 0,
  "customAlias": true,
  "isExpired": false
}
```

### 2. Redirect URL (Fast Cache Path)
```http
GET /{shortCode}
```
**Response (`302 Found`):**
```http
HTTP/1.1 302 Found
Location: https://engineering.atspotify.com/2023/12/how-we-scale-distributed-systems
X-RateLimit-Limit: 120
X-RateLimit-Remaining: 119
X-RateLimit-Reset: 1
```

### 3. Click Analytics Breakdown
```http
GET /api/v1/analytics/{shortCode}
```
**Response (`200 OK`):**
```json
{
  "shortCode": "spotify-scale",
  "originalUrl": "https://...",
  "totalClicks": 1420,
  "uniqueVisitors": 980,
  "deviceBreakdown": { "Desktop": 820, "Mobile": 580, "Tablet": 20 },
  "browserBreakdown": { "Chrome": 950, "Safari": 340, "Firefox": 130 },
  "topReferrers": { "Direct": 600, "twitter.com": 480, "linkedin.com": 340 },
  "recentClicks": [ ... ]
}
```

### 4. Global Platform Overview
```http
GET /api/v1/analytics/overview
```

---

## 🏃 Running the Application

### Option A: Local Development (Zero Dependencies)
Run using the included Maven wrapper. Runs on an embedded in-memory H2 database with in-memory caching fallback if Redis is not running:
```bash
./mvnw spring-boot:run
```
- React Web Dashboard: [http://localhost:8080](http://localhost:8080)
- H2 Database Console: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:urlshortener`)
- Actuator Health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

### Option B: Frontend Hot-Reloading Development (Vite)
If you want to modify or develop the React frontend with instant Hot Module Replacement:
```bash
cd frontend
npm install
npm run dev
```
- Opens Vite Dev Server on [http://localhost:5173](http://localhost:5173) with automated proxying to the Spring Boot backend on `:8080`.
- To rebuild static assets for Spring Boot:
```bash
npm run build
```

### Option B: Docker Compose (Full Distributed Stack with Redis & PostgreSQL)
```bash
docker compose up -d --build
```
This boots:
1. `url-shortener-service` on port `8080`
2. `redis` (v7) on port `6379`
3. `postgres` (v16) on port `5432`

---

## 🧪 Running Automated Tests
```bash
./mvnw clean test
```
All unit tests and full-stack MockMvc integration tests validate Base62 encoding, User-Agent detection, Cache-Aside behavior, negative caching, rate limits, and async event dispatching.
