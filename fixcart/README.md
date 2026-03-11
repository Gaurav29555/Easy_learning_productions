# fixcart backend

Spring Boot backend for the `fixcart` on-demand home services marketplace.

## Tech stack
- Java 17
- Spring Boot 3
- Spring Web, Spring Security, Spring Data JPA
- PostgreSQL
- JWT authentication

## Run configuration
Set environment variables if needed. Defaults are provided in `application.properties`.

- `FIXCART_DATABASE_URL` (default `jdbc:postgresql://localhost:5432/fixcart`)
- `FIXCART_DATABASE_USERNAME` (default `postgres`)
- `FIXCART_DATABASE_PASSWORD` (default `postgres`)
- `FIXCART_SMS_PROVIDER` (`MOCK`, `TWILIO`, `FAST2SMS`)
- `FIXCART_TWILIO_ACCOUNT_SID`, `FIXCART_TWILIO_AUTH_TOKEN`, `FIXCART_TWILIO_FROM_NUMBER`
- `FIXCART_FAST2SMS_API_KEY`, `FIXCART_FAST2SMS_ROUTE`, `FIXCART_FAST2SMS_SENDER_ID`
- `FIXCART_RAZORPAY_KEY_ID`, `FIXCART_RAZORPAY_KEY_SECRET`, `FIXCART_RAZORPAY_WEBHOOK_SECRET`
- `FIXCART_STRIPE_SECRET_KEY`, `FIXCART_STRIPE_WEBHOOK_SECRET`
- `FIXCART_JWT_SECRET` (base64 key)
- `FIXCART_JWT_EXPIRATION_MILLIS` (default `86400000`)
- `FIXCART_ASSIGNMENT_RADIUS_KM` (default `20`)

## Local PostgreSQL setup
1. Install PostgreSQL.
2. Create a database:

```sql
CREATE DATABASE fixcart;
```

3. If you want to use the default local credentials from `application.properties`, create/update a local user accordingly, or override with env vars.

Tables are auto-created by Spring Boot (`spring.jpa.hibernate.ddl-auto=update`) when the app starts.

## Run
```bash
mvn spring-boot:run
```

API docs:
- `http://localhost:8080/swagger-ui.html`

Health check:
- `http://localhost:8080/actuator/health`

## Docker run
```bash
docker build -t fixcart-backend .
docker run -p 8080:8080 \
  -e FIXCART_DATABASE_URL=jdbc:postgresql://<host>:5432/<database> \
  -e FIXCART_DATABASE_USERNAME=<username> \
  -e FIXCART_DATABASE_PASSWORD=<password> \
  fixcart-backend
```

## API overview
Auth:
- `POST /api/auth/register/user`
- `POST /api/auth/register/worker`
- `POST /api/auth/login`
- `POST /api/auth/otp/send`
- `POST /api/auth/otp/verify`
- `POST /api/auth/login/otp`

Workers:
- `GET /api/workers/nearby?latitude=&longitude=&workerType=PLUMBER|CARPENTER&radiusKm=20`
- `PATCH /api/workers/me/location` (WORKER only)

Bookings:
- `POST /api/bookings` (CUSTOMER/ADMIN)
- `GET /api/bookings/{bookingId}`
- `GET /api/bookings/my` (CUSTOMER/ADMIN)
- `GET /api/bookings/worker` (WORKER/ADMIN)
- `POST /api/bookings/{bookingId}/assign` (ADMIN)
- `PATCH /api/bookings/{bookingId}/status`

Payments:
- `POST /api/payments/order`
- `PATCH /api/payments/confirm`
- `GET /api/payments/my`
- `POST /api/payments/webhook/razorpay`
- `POST /api/payments/webhook/stripe`

Tracking:
- `POST /api/tracking/bookings/{bookingId}/location` (WORKER/ADMIN)
- `GET /api/tracking/bookings/{bookingId}/events`

Admin:
- `GET /api/admin/metrics`
- `GET /api/admin/bookings`
- `GET /api/admin/workers`
- `PATCH /api/admin/workers/{workerId}/availability`
- `POST /api/admin/bookings/{bookingId}/assign`
- `GET /api/admin/payments`

Use header:
`Authorization: Bearer <jwt-token>`
