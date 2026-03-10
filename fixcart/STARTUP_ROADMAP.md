# fixcart startup roadmap

## already implemented in codebase
- Oracle-ready backend configuration
- JWT auth + role-based APIs
- Worker proximity matching + booking assignment flow
- React frontend integrated with backend APIs
- CORS configuration
- Swagger/OpenAPI (`/swagger-ui.html`)
- Actuator health/info (`/actuator/health`, `/actuator/info`)
- Dockerfiles for backend and frontend
- GitHub Actions CI pipeline

## immediate next features (must-have)
1. OTP phone verification during signup/login
2. Payment gateway integration (Razorpay/Stripe)
3. Worker onboarding docs + KYC verification
4. Service pricing catalog (base fee, distance fee, surge)
5. Booking cancellation policy + penalties
6. Push notifications (booking accepted, started, completed)
7. Admin panel for dispute handling and worker moderation
8. Ratings + reviews with abuse filtering

## scale and reliability (startup-grade)
1. Redis caching for nearby worker queries
2. Message queue for async jobs (notifications, analytics)
3. Database migrations using Flyway
4. Idempotency keys for booking/payment endpoints
5. Observability: central logs + tracing + alerting
6. API rate limiting per IP/user
7. Secrets manager (no plaintext credentials in files)
8. Blue/green deployment + rollback strategy

## free deployment blueprint

### frontend (free)
- Deploy `fixcart-frontend` on Cloudflare Pages or Vercel Hobby.
- Set frontend env `VITE_FIXCART_API_URL` to your backend public URL.

### backend + database (free)
- Use Oracle Cloud Always Free:
  - Always Free VM for Spring Boot backend
  - Always Free Oracle Autonomous DB or Oracle DB on VM
- Build and run backend with Docker or Java jar on VM.

## recommended release order
1. Launch MVP publicly with current stack + SSL + domain
2. Add OTP + payments + admin panel
3. Add worker app features (live location ping, availability toggles)
4. Add analytics dashboard (GMV, completion rate, repeat users)
5. Add referral and promo engine
