# Banking System

A Java 17 and Spring Boot microservices demo for account management, internal
account transfers, fraud checks, notifications, and Razorpay payment order
creation. The API Gateway is the entry point for the business APIs.

> **Development/demo project:** There is no authentication or authorization in
> the current API. Do not expose it to the public internet or use it to process
> real customer funds without appropriate security, operational controls, and
> payment verification.

## Services and technologies

## System Architecture & Default Ports

| Component | Default Port | Responsibility |
|:---|:---:|:---|
| **API Gateway** | `8080` | Routes account, transaction, and payment API requests and applies Redis-backed request rate limiting. |
| **Account Service** | `8081` | Creates accounts, retrieves account and balance information, and performs balance operations required for transfers. |
| **Transaction Service** | `8082` | Initiates transfers, manages transaction state, handles OTP verification, and coordinates transfer completion or compensation. |
| **Payment Service** | `8083` | Creates Razorpay orders and stores payment records. |
| **Fraud Detection Service** | `8084` | Consumes transaction events and evaluates configured fraud detection rules. |
| **Notification Service** | `8085` | Consumes transaction and payment events to process notifications. |
| **MySQL** | `3306` | Provides persistent relational storage, with separate databases for the account, transaction, and payment services. |
| **Apache Kafka** | `9092` | Enables asynchronous communication between microservices through events. |
| **ZooKeeper** | Internal | Coordinates Kafka operations in the configured ZooKeeper-based setup. |
| **Redis** | `6379` | Supports API rate limiting and temporary OTP storage. |


### Technology stack

- Java 17, Spring Boot, Spring Web MVC, Spring Cloud Gateway
- Spring Data JPA and MySQL
- Apache Kafka with ZooKeeper
- Redis and Spring Data Redis
- Maven, Docker, and Docker Compose
- Razorpay Java SDK and Razorpay Checkout for online payment initiation
- Jakarta Bean Validation and Lombok

### How the microservices work together

**Internal account transfer:** A client calls the Gateway's transaction transfer
route. Transaction Service asks Account Service to deduct the sender's balance,
saves a `PROCESSING` transaction, then publishes `transaction.initiated` to
Kafka. Fraud Detection Service consumes the event and returns its decision
through transaction events. On a clean result, Transaction Service completes
the transfer and publishes `transaction.completed`; Account Service credits
the receiver and Notification Service consumes events. When an OTP is required,
Transaction Service stores it in Redis for five minutes and publishes it for
notification. Notification Service currently reports alerts in its application
logs; it does not send email or SMS. When a transfer is rejected or cannot be
verified, Transaction Service can compensate by crediting the sender and
publishing a refund event.

**Razorpay payment:** The client calls Payment Service through the Gateway to
create a Razorpay order. Payment Service uses its server-side Razorpay
credentials, creates an order with Razorpay, persists a payment record, and
returns the order ID and public key ID for Razorpay Checkout. Razorpay payment
completion must be verified server-side before it is treated as successful.
The current payment controller does **not** map a webhook route or provide a
payment verification endpoint; the browser checkout callback alone is not
proof of payment.

The Gateway currently routes `/api/v1/accounts/**`,
`/api/v1/transactions/**`, and `/api/v1/payments/**`. Fraud Detection Service
and Notification Service are Kafka consumers and do not currently expose
business REST endpoints.

## Run with Docker Compose

Prerequisites: Docker Desktop (or Docker Engine) with Compose, and a Razorpay
test account if you want to create payment orders.

1. In the repository root, optionally create a `.env` file:

   ```dotenv
   RAZORPAY_KEY_ID=rzp_test_your_key_id
   RAZORPAY_KEY_SECRET=your_test_key_secret
   ```

   Do not commit `.env`. Razorpay credentials are only used by Payment Service;
   never put the secret in browser code.

2. Build and start the services:

   ```powershell
   docker compose up --build
   ```

3. Use the Gateway at `http://localhost:8080`. View container logs with:

   ```powershell
   docker compose logs -f api-gateway transaction-service
   ```

4. Stop the stack:

   ```powershell
   docker compose down
   ```

   MySQL data is kept in the named `mysql-data` volume. To remove the database
   volume as well, run `docker compose down -v` (this permanently deletes its
   stored data).

Compose starts infrastructure before application containers, but `depends_on`
does not guarantee that MySQL/Kafka are ready to accept connections. If a
service exits during the first startup, inspect its logs and restart it after
the dependencies are ready, for example `docker compose restart account-service`.

### Run a service locally instead

Start the infrastructure with `docker compose up -d mysql redis zookeeper kafka`.
Then, from a service folder, run `.\mvnw.cmd spring-boot:run` on Windows. The
application YAML defaults to local hostnames and ports for this mode. Start
Account Service before Transaction Service. The API Gateway listens on port
8080.

## API examples

Examples below use PowerShell and the Gateway at `http://localhost:8080`.
Replace sample account numbers with IDs returned when creating accounts.

### Accounts

Create an account (`POST /api/v1/accounts`, returns `201 Created`):

```powershell
$account = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/accounts `
  -ContentType 'application/json' `
  -Body (@{
    accountHolderName = 'Alex Example'
    email = 'alex@example.com'
    phone = '5550100'
    accountType = 'SAVINGS'
    initialDeposit = 1000.00
  } | ConvertTo-Json)

$account
```

`accountType` must be a value defined by the project's `AccountType` enum. The
response includes `accountNumber`; use that value in subsequent requests.

Get an account (`GET /api/v1/accounts/{accountNumber}`):

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/accounts/$($account.accountNumber)
```

Get the balance (`GET /api/v1/accounts/{accountNumber}/balance`):

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/accounts/$($account.accountNumber)/balance
```

Block an account (`PUT /api/v1/accounts/{accountNumber}/block`):

```powershell
Invoke-RestMethod -Method Put `
  -Uri http://localhost:8080/api/v1/accounts/$($account.accountNumber)/block
```

The following account operations are primarily intended for internal
service-to-service transfer orchestration. Do not expose them to untrusted
clients:

- `PUT /api/v1/accounts/{accountNumber}/deduct?amount=10.00`
- `PUT /api/v1/accounts/{accountNumber}/credit?amount=10.00`

### Internal account transfers

Initiate a transfer (`POST /api/v1/transactions/transfer`, returns
`201 Created`):

```powershell
$transfer = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/transactions/transfer `
  -ContentType 'application/json' `
  -Body (@{
    senderAccountNumber = 'SENDER_ACCOUNT_NUMBER'
    receiverAccountNumber = 'RECEIVER_ACCOUNT_NUMBER'
    amount = 25.00
    description = 'Example transfer'
  } | ConvertTo-Json)

$transfer
```

Transfers are asynchronous and can initially be `PROCESSING`. Inspect the
transaction (`GET /api/v1/transactions/{transactionId}`):

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/transactions/$($transfer.id)
```

Get transactions sent from an account
(`GET /api/v1/transactions/account/{accountNumber}`):

```powershell
Invoke-RestMethod `
  http://localhost:8080/api/v1/transactions/account/SENDER_ACCOUNT_NUMBER
```

Verify a transaction OTP
(`POST /api/v1/transactions/{transactionId}/verify?otp={otp}`):

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/transactions/$($transfer.id)/verify?otp=123456"
```

The sample OTP above is only an example. In this demo, get a generated OTP from
the Notification Service logs (`docker compose logs notification-service`);
the OTP expires after five minutes. Notifications are log output only, not
email or SMS.

### Razorpay payment order

Create an order (`POST /api/v1/payments/create-order`, returns `201 Created`):

```powershell
$order = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/payments/create-order `
  -ContentType 'application/json' `
  -Body (@{
    accountNumber = 'ACCOUNT_NUMBER'
    amount = 10.00
    description = 'Example Razorpay payment'
  } | ConvertTo-Json)

$order
```

The response contains the payment ID, Razorpay order ID, amount, currency,
status, and Razorpay key ID. Use `razorpayOrderId` and the public key ID to
initialize Razorpay Checkout in a frontend. The backend secret must remain
private. This API creates an order; it does not by itself establish that the
payment succeeded.

### Health and Gateway routes

The Gateway exposes Spring Boot Actuator endpoints according to its
configuration, including:

- `GET http://localhost:8080/actuator/health`
- `GET http://localhost:8080/actuator/info`
- `GET http://localhost:8080/actuator/gateway`

Other services expose health/info actuator endpoints on their own ports as
configured. Do not expose actuator endpoints publicly without reviewing their
security and exposure settings.

## Docker files

Each service has its own multi-stage `Dockerfile`. The build stage uses Maven
and Java 17 to package the service; the runtime stage runs the generated jar on
a Java 17 JRE. `docker-compose.yml` builds each image, starts the shared
infrastructure, and provides container DNS names and configuration to the
services. The application YAML supports environment overrides for Docker while
retaining localhost defaults for local development.

## Configuration and known limitations

- Razorpay configuration: `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, and
  `RAZORPAY_WEBHOOK_SECRET` environment variables.
- Database and broker credentials in Compose are development defaults, not
  production secrets. Replace them and add production authentication, network
  controls, and secret management before deployment.
- The account, transaction, and payment services use `spring.jpa.hibernate.ddl-auto: update`;
  use managed schema migrations for production.
- The payment webhook handler is not mapped to a URL, and payment signature
  verification is not implemented. Do not mark an order paid based only on
  client-side results.
- The current demo stack uses single-node Kafka and MySQL with root credentials.
  It is intended for local development, not high availability.
