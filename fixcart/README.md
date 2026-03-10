# fixcart backend

Spring Boot backend for a service marketplace platform (like Uber for plumbers and carpenters).

## Tech stack
- Java 17
- Spring Boot 3
- Spring Web, Spring Security, Spring Data JPA
- Oracle Database
- JWT authentication

## Run configuration
Set environment variables (optional, defaults are provided in `application.properties`):

- `FIXCART_ORACLE_URL` (default `jdbc:oracle:thin:@localhost:1521/ORCLPDB`)
- `FIXCART_ORACLE_USERNAME` (default `FIXCART`)
- `FIXCART_ORACLE_PASSWORD` (default `gk1372`)
- `FIXCART_SMS_PROVIDER` (`MOCK`, `TWILIO`, `FAST2SMS`)
- `FIXCART_TWILIO_ACCOUNT_SID`, `FIXCART_TWILIO_AUTH_TOKEN`, `FIXCART_TWILIO_FROM_NUMBER`
- `FIXCART_FAST2SMS_API_KEY`, `FIXCART_FAST2SMS_ROUTE`, `FIXCART_FAST2SMS_SENDER_ID`
- `FIXCART_RAZORPAY_KEY_ID`, `FIXCART_RAZORPAY_KEY_SECRET`, `FIXCART_RAZORPAY_WEBHOOK_SECRET`
- `FIXCART_STRIPE_SECRET_KEY`, `FIXCART_STRIPE_WEBHOOK_SECRET`
- `FIXCART_JWT_SECRET` (base64 key)
- `FIXCART_JWT_EXPIRATION_MILLIS` (default `86400000`)
- `FIXCART_ASSIGNMENT_RADIUS_KM` (default `20`)

## SQL*Plus database setup (Oracle)
Run these commands in SQL*Plus.

1. Connect as SYSDBA:
```sql
sqlplus / as sysdba
```
If needed:
```sql
sqlplus sys/<SYS_PASSWORD>@localhost:1521/XEPDB1 as sysdba
```

2. Create fixcart user/schema:
```sql
CREATE USER FIXCART IDENTIFIED BY fixcart_password;
GRANT CONNECT, RESOURCE TO FIXCART;
ALTER USER FIXCART QUOTA UNLIMITED ON USERS;
```

3. Verify login with app user:
```sql
CONNECT FIXCART/fixcart_password@localhost:1521/XEPDB1;
SELECT USER FROM dual;
```

Tables are auto-created by Spring Boot (`spring.jpa.hibernate.ddl-auto=update`) when app starts.

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
  -e FIXCART_ORACLE_URL=jdbc:oracle:thin:@<host>:1521/<service> \
  -e FIXCART_ORACLE_USERNAME=FIXCART \
  -e FIXCART_ORACLE_PASSWORD=<password> \
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
