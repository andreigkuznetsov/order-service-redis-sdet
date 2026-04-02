# Order Service Redis SDET

REST API сервис для работы с заказами, в котором собраны несколько типичных backend-задач:

- создание и чтение заказов;
- обновление статуса заказа;
- идемпотентное создание заказа через `Idempotency-Key`;
- кеширование заказов в Redis;
- rate limiting по `X-Client-Id`;
- защита процесса обработки заказа от конкурентного запуска через Redis lock;
- интеграционные тесты с Testcontainers;
- нагрузочные сценарии на k6.

Проект хорошо подходит как демонстрация QA/AQA/SDET-подхода: здесь есть не только API и тесты, но и воспроизведение реальных проблем конкурентности, кеширования и ограничений нагрузки.

---

## Что умеет сервис

### Основные операции

- `POST /api/orders` — создать заказ;
- `GET /api/orders/{id}` — получить заказ по `id`;
- `PATCH /api/orders/{id}/status` — обновить статус заказа;
- `POST /api/orders/{id}/process` — запустить обработку заказа.

### Дополнительное поведение

- **Idempotency** — повторный `POST /api/orders` с тем же `Idempotency-Key` не создаёт новый заказ;
- **Redis cache** — заказ кешируется после первого чтения;
- **Cache invalidation** — кеш сбрасывается после смены статуса;
- **Rate limiting** — ограничение количества запросов на создание заказа по клиенту;
- **Distributed lock / processing lock** — один и тот же заказ нельзя одновременно обработать из двух параллельных запросов.

---

## Технологический стек

| Технология | Где используется |
|---|---|
| Java 21 | основной язык |
| Spring Boot 3.3.5 | приложение и REST API |
| Spring Web | HTTP API |
| Spring Validation | валидация входных данных |
| Spring Data JPA | работа с PostgreSQL |
| Spring Data Redis | кеш, rate limit, idempotency, lock |
| PostgreSQL | основное хранилище заказов |
| Flyway | миграции схемы БД |
| Jackson | сериализация DTO в Redis и HTTP |
| Lombok | сокращение boilerplate-кода |
| JUnit 5 | тесты |
| Rest Assured | API-проверки |
| Awaitility | ожидания в тестах TTL/окон |
| Testcontainers | поднятие PostgreSQL и Redis для интеграционных тестов |
| k6 | нагрузочные и поведенческие сценарии |
| Gradle | сборка проекта |

---

## Архитектура проекта

```text
Controller
  -> Service
      -> Repository
          -> PostgreSQL

Service
  -> Redis
```

### Основные слои

#### Controller
`OrderController`

Отвечает за HTTP endpoints:
- принимает запросы;
- валидирует входные DTO;
- вызывает сервисы;
- возвращает DTO-ответы.

#### Service
- `OrderService` — основная бизнес-логика заказов;
- `IdempotencyService` — работа с ключами идемпотентности в Redis;
- `OrderCacheService` — кеширование `OrderResponse` в Redis;
- `RateLimitService` — ограничение запросов по клиенту;
- `OrderProcessingService` — защита обработки заказа через lock и смена статусов.

#### Repository
- `OrderRepository` — доступ к таблице `orders` через JPA.

#### Infrastructure
- `PostgreSQL` — хранение заказов;
- `Redis` — хранение idempotency-ключей, кеша, rate-limit счётчиков и processing-lock.

---

## Структура проекта

```text
order-service-redis-sdet/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
├── README.md
│
├── src/main/java/com/example/orders/
│   ├── OrderServiceApplication.java
│   ├── config/
│   │   └── RedisConfig.java
│   ├── controller/
│   │   └── OrderController.java
│   ├── dto/
│   │   ├── CreateOrderRequest.java
│   │   ├── ErrorResponse.java
│   │   ├── OrderResponse.java
│   │   ├── ProcessOrderResponse.java
│   │   └── UpdateOrderStatusRequest.java
│   ├── entity/
│   │   ├── OrderEntity.java
│   │   └── OrderStatus.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── IdempotencyProcessingException.java
│   │   ├── OrderNotFoundException.java
│   │   ├── OrderProcessingLockedException.java
│   │   └── RateLimitExceededException.java
│   ├── mapper/
│   │   └── OrderMapper.java
│   ├── repository/
│   │   └── OrderRepository.java
│   └── service/
│       ├── IdempotencyService.java
│       ├── OrderCacheService.java
│       ├── OrderProcessingService.java
│       ├── OrderService.java
│       └── RateLimitService.java
│
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__create_orders_table.sql
│
├── src/test/java/com/example/orders/
│   ├── base/
│   │   └── BaseIntegrationTest.java
│   ├── factory/
│   │   └── OrderRequestFactory.java
│   ├── integration/
│   │   ├── IdempotencyIntegrationTest.java
│   │   ├── OrderApiIntegrationTest.java
│   │   ├── OrderCacheIntegrationTest.java
│   │   ├── OrderProcessingLockIntegrationTest.java
│   │   └── RateLimitIntegrationTest.java
│   └── support/
│       ├── OrderApiSupport.java
│       ├── OrderAssertions.java
│       └── RedisKeys.java
│
├── src/test/resources/
│   └── application-test.yml
│
└── performance/
    ├── scenarios/
    │   ├── smoke.js
    │   ├── idempotency.js
    │   ├── rate-limit.js
    │   └── load.js
    └── utils/
        └── order-api.js
```

---

## Доменные сущности

### OrderStatus
Поддерживаемые статусы заказа:

- `NEW`
- `PROCESSING`
- `COMPLETED`
- `CANCELLED`

### OrderEntity
Поля заказа в БД:

- `id: UUID`
- `product: String`
- `quantity: Integer`
- `price: BigDecimal`
- `status: OrderStatus`
- `createdAt: Instant`
- `updatedAt: Instant`

---

## Конфигурация приложения

Основные настройки из `application.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/orders_db
    username: orders_user
    password: orders_pass
  data:
    redis:
      host: localhost
      port: 6379

app:
  cache:
    order-ttl-seconds: 60
  idempotency:
    ttl-hours: 24
  rate-limit:
    max-requests: 5
    window-seconds: 10
  lock:
    ttl-seconds: 15
```

### Что означают параметры

- `app.cache.order-ttl-seconds` — TTL кеша заказа в Redis;
- `app.idempotency.ttl-hours` — TTL записи для `Idempotency-Key`;
- `app.rate-limit.max-requests` — лимит запросов в окне;
- `app.rate-limit.window-seconds` — длительность окна rate limiting;
- `app.lock.ttl-seconds` — TTL Redis lock для обработки заказа.

В `application-test.yml` TTL и окна уменьшены, чтобы интеграционные тесты проходили быстрее.

---

## Как работает идемпотентность

Идемпотентность реализована в `OrderService` + `IdempotencyService`.

### Сценарий

1. Клиент отправляет `POST /api/orders` с `Idempotency-Key`.
2. Сервис проверяет Redis-ключ `order:idempotency:{key}`.
3. Если там уже лежит `orderId`, сервис возвращает ранее созданный заказ.
4. Если ключа нет, сервис пытается атомарно записать значение `PROCESSING` через `SETNX`.
5. Если lock получен — создаётся новый заказ в PostgreSQL.
6. После успешного создания в Redis вместо `PROCESSING` сохраняется `orderId`.
7. Если другой поток приходит в момент обработки, он ждёт до 2 секунд появления итогового `orderId`.
8. Если за это время результат не появился, возвращается `409 Conflict`.

### Redis-ключи

- `order:idempotency:{key}`

### Что это даёт

- защита от повторной отправки одного и того же запроса;
- уменьшение риска дублей при retry клиента;
- частичная защита от race condition при конкурентных запросах на создание.

> Важно: контроллер всегда помечен `@ResponseStatus(HttpStatus.CREATED)`, поэтому повторный идемпотентный запрос тоже сейчас возвращает `201`, а не `200`. Это соответствует текущей реализации кода, даже если в некоторых описаниях проекта ожидается `200`.

---

## Как работает кеширование заказов

Кеширование реализовано в `OrderCacheService`.

### Поведение

- при первом `GET /api/orders/{id}` заказ читается из PostgreSQL и сохраняется в Redis;
- при повторном `GET` читается из Redis;
- после `PATCH /api/orders/{id}/status` кеш инвалидируется;
- при истечении TTL запись исчезает автоматически.

### Redis-ключи

- `order:cache:{orderId}`

### Что кешируется

В Redis сериализуется `OrderResponse` в JSON.

---

## Как работает rate limiting

Ограничение реализовано в `RateLimitService`.

### Механика

- используется заголовок `X-Client-Id`;
- для каждого клиента создаётся Redis-ключ `order:rate_limit:{clientId}`;
- счётчик увеличивается на каждый `POST /api/orders`;
- при первом запросе для ключа выставляется TTL окна;
- после превышения лимита сервис возвращает `429 Too Many Requests`.

### Redis-ключи

- `order:rate_limit:{clientId}`

### Текущие значения по умолчанию

- максимум `5` запросов;
- окно `10` секунд.

---

## Как работает защита обработки заказа от конкуренции

Логика находится в `OrderProcessingService`.

### Endpoint

```http
POST /api/orders/{id}/process
```

### Поведение

- сервис пытается захватить Redis lock `order:lock:{orderId}`;
- если lock уже занят, возвращается `409 Conflict`;
- если lock получен:
  - статус заказа меняется на `PROCESSING`;
  - выполняется искусственная задержка `2` секунды;
  - статус меняется на `COMPLETED`;
  - lock удаляется в `finally`.

### Redis-ключи

- `order:lock:{orderId}`

Это демонстрирует простой паттерн защиты от двойной обработки одного и того же ресурса.

---

## API

## 1. Создание заказа

```http
POST /api/orders
```

### Headers

```http
Content-Type: application/json
X-Client-Id: client-123
Idempotency-Key: order-req-001   # optional
```

### Body

```json
{
  "product": "iPhone 15",
  "quantity": 1,
  "price": 999.99
}
```

### Валидация

- `product` — обязателен и не должен быть пустым;
- `quantity` — минимум `1`;
- `price` — минимум `0.01`.

### Успешный ответ

```json
{
  "id": "0d8f3a46-6d15-47e6-bfd8-8b69fe534d5c",
  "product": "iPhone 15",
  "quantity": 1,
  "price": 999.99,
  "status": "NEW"
}
```

### Возможные статусы

- `201 Created` — заказ создан;
- `409 Conflict` — запрос с тем же `Idempotency-Key` уже обрабатывается;
- `429 Too Many Requests` — превышен rate limit;
- `400 Bad Request` — ошибка валидации.

---

## 2. Получение заказа по id

```http
GET /api/orders/{id}
```

### Ответ

```json
{
  "id": "0d8f3a46-6d15-47e6-bfd8-8b69fe534d5c",
  "product": "iPhone 15",
  "quantity": 1,
  "price": 999.99,
  "status": "NEW"
}
```

### Возможные статусы

- `200 OK`
- `404 Not Found`

---

## 3. Обновление статуса заказа

```http
PATCH /api/orders/{id}/status
```

### Body

```json
{
  "status": "COMPLETED"
}
```

### Возможные статусы

- `200 OK`
- `400 Bad Request`
- `404 Not Found`

---

## 4. Обработка заказа

```http
POST /api/orders/{id}/process
```

### Успешный ответ

```json
{
  "message": "Order processed",
  "orderId": "0d8f3a46-6d15-47e6-bfd8-8b69fe534d5c"
}
```

### Возможные статусы

- `200 OK`
- `404 Not Found`
- `409 Conflict` — заказ уже обрабатывается параллельным запросом.

---

## Формат ошибок

Ошибки централизованно отдаются через `GlobalExceptionHandler` в виде:

```json
{
  "timestamp": "2026-04-02T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found: ...",
  "path": "/api/orders/..."
}
```

Обрабатываются:

- `OrderNotFoundException` -> `404`
- `RateLimitExceededException` -> `429`
- `OrderProcessingLockedException` -> `409`
- `IdempotencyProcessingException` -> `409`
- `MethodArgumentNotValidException` -> `400`

---

## База данных

Flyway-миграция `V1__create_orders_table.sql` создаёт таблицу:

```sql
create table orders (
    id uuid primary key,
    product varchar(255) not null,
    quantity integer not null,
    price numeric(19, 2) not null,
    status varchar(50) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
```

---

## Запуск проекта локально

### Требования

- Java 21
- Docker или отдельно поднятые PostgreSQL и Redis
- k6 — если хотите запускать нагрузочные сценарии

### 1. Поднять инфраструктуру

В репозитории **нет docker-compose.yml**, поэтому есть два варианта:

#### Вариант A. Поднять вручную через Docker

```bash
docker run --name orders-postgres \
  -e POSTGRES_DB=orders_db \
  -e POSTGRES_USER=orders_user \
  -e POSTGRES_PASSWORD=orders_pass \
  -p 5433:5432 \
  -d postgres:16-alpine

docker run --name orders-redis \
  -p 6379:6379 \
  -d redis:7-alpine
```

#### Вариант B. Использовать свои локальные PostgreSQL и Redis

Тогда нужно подставить значения в `application.yml`.

### 2. Запустить приложение

```bash
./gradlew bootRun
```

Для Windows:

```bat
gradlew.bat bootRun
```

### 3. Собрать проект

```bash
./gradlew clean build
```

---

## Интеграционные тесты

Проект содержит полноценные интеграционные тесты на Spring Boot + Testcontainers.

### Что поднимается в тестах

- PostgreSQL `postgres:16-alpine`
- Redis `redis:7-alpine`

### Особенности тестовой базы

`BaseIntegrationTest`:
- поднимает контейнеры один раз;
- пробрасывает динамические свойства через `@DynamicPropertySource`;
- очищает PostgreSQL и Redis перед каждым тестом;
- настраивает Rest Assured на случайный порт Spring Boot.

### Запуск тестов

```bash
./gradlew test
```

### Какие сценарии покрыты

#### `OrderApiIntegrationTest`
- создание заказа;
- получение заказа по `id`;
- `404` для несуществующего заказа.

#### `IdempotencyIntegrationTest`
- один заказ для одинакового `Idempotency-Key`;
- разные заказы для разных ключей;
- разные заказы без `Idempotency-Key`.

#### `OrderCacheIntegrationTest`
- кеш появляется после первого `GET`;
- повторное чтение работает через кеш;
- кеш сбрасывается после обновления статуса;
- кеш истекает по TTL.

#### `RateLimitIntegrationTest`
- запросы в пределах лимита проходят;
- после превышения лимита возвращается `429`;
- после истечения окна запросы снова разрешены.

#### `OrderProcessingLockIntegrationTest`
- два параллельных вызова обработки одного заказа дают `200` и `409`;
- lock удаляется после завершения обработки;
- конечный статус заказа становится `COMPLETED`.

---

## Нагрузочные сценарии k6

Сценарии лежат в `performance/scenarios`.

Перед запуском убедитесь, что приложение доступно по `http://localhost:8080` или передайте другой адрес через `BASE_URL`.

### Smoke

```bash
k6 run performance/scenarios/smoke.js
```

Проверяет, что базовое создание заказа работает стабильно.

### Idempotency

```bash
k6 run performance/scenarios/idempotency.js
```

Параллельные запросы с одним и тем же `Idempotency-Key`.

Ожидаемое поведение:
- ответы `201`, а в некоторых гонках — возможно `409`;
- успешные ответы содержат один и тот же `orderId`.

### Rate limit

```bash
k6 run performance/scenarios/rate-limit.js
```

Несколько VU используют один `X-Client-Id`, чтобы спровоцировать `429 Too Many Requests`.

### Load

```bash
k6 run performance/scenarios/load.js
```

Базовый нагрузочный профиль:
- `20` VUs
- `20s` duration

### Передача кастомного URL

```bash
BASE_URL=http://localhost:8080 k6 run performance/scenarios/load.js
```

Для Windows PowerShell:

```powershell
$env:BASE_URL="http://localhost:8080"
k6 run performance/scenarios/load.js
```

---

## Что именно демонстрирует проект как SDET/QA-портфолио

- умение тестировать REST API не только функционально, но и с точки зрения устойчивости;
- понимание race condition и способов её воспроизведения;
- работу с Redis не только как с кешем, но и как с инструментом синхронизации;
- проверку TTL, окон лимитов и поведения под конкуренцией;
- построение интеграционных тестов, близких к реальной инфраструктуре;
- использование k6 для проверки нефункциональных требований.

---

## Ограничения и что можно улучшить

Сейчас проект покрывает основные демонстрационные сценарии, но его можно усилить:

- добавить `docker-compose.yml` для запуска одной командой;
- вернуть разные HTTP-статусы для первичного и повторного идемпотентного ответа, если это бизнес-требование;
- добавить OpenAPI/Swagger;
- добавить unit-тесты на сервисы помимо интеграционных;
- добавить метрики на rate limit, cache hit/miss, idempotency hit;
- заменить `Thread.sleep()` в `process` на имитацию или реальный обработчик;
- добавить retry/timeout policy для более сложных сценариев;
- доработать `OrderAssertions`, который сейчас пустой и не используется.

---

## Краткий итог

Это небольшой, но цельный backend-проект, в котором объединены:

- CRUD-подобное API для заказов;
- Redis как инструмент для кеша, идемпотентности, rate limiting и locking;
- PostgreSQL + Flyway;
- интеграционные тесты на Testcontainers;
- k6-сценарии для поведенческой и нагрузочной проверки.

Именно за счёт сочетания функциональности, конкурентности и тестовой инфраструктуры проект выглядит сильнее обычного demo CRUD-сервиса.
