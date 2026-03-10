# fixcart startup blueprint

## executive assessment
- Current maturity: pilot-ready, not launch-ready.
- Strongest assets: end-to-end booking flow, OTP and payment foundations, real-time worker tracking, admin APIs, Oracle-backed backend, Docker and CI support.
- Largest gaps: worker trust/compliance, pricing sophistication, abuse controls, ops tooling, customer support workflows, production monitoring, and formal release process.
- Top risks: unverified workers harming trust, weak fraud controls around OTP/payments, no structured service catalog, and limited audit/ops visibility.

## must-have before launch
- Approve workers before they can receive bookings.
- Require KYC/document review and maintain audit logs.
- Add booking price estimate, scheduling, and cancellation reason handling.
- Harden OTP/login abuse controls and payment webhook flows.
- Add observability, backups, and staged deployment.

## important soon after launch
- Service catalog and pricing rules by city/service.
- Ratings, disputes, refunds, and support console.
- Coupons, referrals, and retention loops.
- Notification center for booking lifecycle.
- Worker earnings and payout reporting.

## scale-stage improvements
- Redis caching and queue-based jobs.
- Geospatial indexing beyond naive worker scanning.
- Full migration management with Flyway.
- Dedicated worker/customer mobile apps.
- Advanced fraud detection and idempotent webhook/event processing.

## implementation blueprint

### backend modules
- worker verification and approval pipeline
- booking scheduling and pricing engine
- audit logging service
- auth/OTP request throttling
- support and dispute module
- payout and earnings module
- service catalog and city configuration module

### frontend pages/components
- worker onboarding review flow
- customer booking scheduling and cancellation UX
- admin audit feed and worker moderation UI
- support/dispute submission pages
- pricing estimate cards and checkout confirmation flow

### infrastructure/services
- Oracle backup automation
- Redis for OTP/rate limits/caching
- queue processing for notifications and retries
- centralized monitoring and log aggregation
- staging environment with production-like config

## next build order
1. worker verification and admin moderation
2. booking pricing and scheduled services
3. audit logs and support operations tooling
4. Flyway migrations and Redis-backed rate limiting
5. service catalog, ratings, disputes, and payouts
