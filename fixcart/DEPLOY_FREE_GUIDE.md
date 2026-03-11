# fixcart free deployment guide

## target stack
- Frontend: Cloudflare Pages
- Backend: Render free web service
- Database: Neon free PostgreSQL

This stack avoids Oracle and is the cleanest no-card deployment path for `fixcart`.

## before you deploy
- Frontend repo is already live on Cloudflare Pages.
- Backend now expects PostgreSQL, not Oracle.
- Use SSL-enabled PostgreSQL URLs in hosted environments.

## step 1: create a Neon database
1. Sign up at `https://neon.tech/`.
2. Create a new project named `fixcart`.
3. Copy these values from the dashboard:
   - host
   - database name
   - username
   - password
4. Build the JDBC URL in this format:

```text
jdbc:postgresql://<host>/<database>?sslmode=require
```

Example:

```text
jdbc:postgresql://ep-cool-darkness-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
```

## step 2: deploy backend on Render
1. Sign up at `https://render.com/`.
2. Click `New +` -> `Web Service`.
3. Connect your GitHub repo.
4. Select repo `Gaurav29555/Easy_learning_productions`.
5. Render should detect `render.yaml`. If it does not, configure:
   - Runtime: `Docker`
   - Dockerfile path: `./fixcart/Dockerfile`
   - Root directory: `fixcart`
6. Add environment variables:
   - `FIXCART_DATABASE_URL`
   - `FIXCART_DATABASE_USERNAME`
   - `FIXCART_DATABASE_PASSWORD`
   - `FIXCART_JWT_SECRET`
   - `FIXCART_CORS_ALLOWED_ORIGINS=https://easy-learning-productions.pages.dev`
   - `FIXCART_SMS_PROVIDER=MOCK`
   - `FIXCART_OTP_EXPOSE_IN_RESPONSE=false`
7. Deploy the service.

## step 3: update Cloudflare Pages frontend env
After Render gives you a backend URL such as:

```text
https://fixcart-backend.onrender.com
```

update the Pages environment variable:

```text
VITE_FIXCART_API_URL=https://fixcart-backend.onrender.com
```

Then redeploy the frontend.

## step 4: verify production
Check these URLs:
- Frontend: `https://easy-learning-productions.pages.dev`
- Backend health: `https://<your-render-url>/actuator/health`
- Swagger: `https://<your-render-url>/swagger-ui.html`

Run a real smoke test:
1. Register a customer
2. Register a worker
3. Approve the worker from admin
4. Create a booking
5. Confirm worker auto-assignment
6. Confirm real-time location updates on the map

## production notes
- Free backend instances usually sleep after inactivity. The first request may be slow.
- Keep `FIXCART_SMS_PROVIDER=MOCK` until you have a real SMS provider.
- Keep payment provider keys empty until you are ready to test live webhooks.
- For launch, replace mock OTP with Twilio or Fast2SMS and add real payment keys.
