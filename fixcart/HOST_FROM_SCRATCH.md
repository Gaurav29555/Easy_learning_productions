# fixcart Hosting From Scratch

This is the simplest no-card deployment path currently wired into `fixcart`.

## Architecture

- Frontend: Cloudflare Pages
- Backend: Render Web Service
- Database: Neon PostgreSQL
- OTP: Gmail SMTP email delivery

## 1. Push `fixcart` to GitHub

```powershell
cd c:\Users\gaura\OneDrive\Desktop\easy_learning_production\fixcart
git add .
git commit -m "fixcart production updates"
git push origin master
```

## 2. Create Neon PostgreSQL

1. Sign up at `https://console.neon.tech/signup`
2. Create project `fixcart`
3. Copy:
   - host
   - database
   - username
   - password
4. Build JDBC URL:

```text
jdbc:postgresql://<host>/<database>?sslmode=require&channel_binding=require
```

## 3. Deploy backend on Render

1. Sign in at `https://dashboard.render.com/`
2. Create `Web Service`
3. Connect GitHub repository
4. Use:
   - Name: `fixcart-backend`
   - Runtime: `Docker`
   - Branch: `master`
   - Root directory: `fixcart`
   - Docker Build Context Directory: `fixcart`
   - Dockerfile Path: `fixcart/Dockerfile`
   - Health Check Path: `/actuator/health`
5. Add environment variables:

```text
FIXCART_DATABASE_URL=jdbc:postgresql://<host>/<database>?sslmode=require&channel_binding=require
FIXCART_DATABASE_USERNAME=<username>
FIXCART_DATABASE_PASSWORD=<password>
FIXCART_JWT_SECRET=<long-random-secret>
FIXCART_CORS_ALLOWED_ORIGINS=https://easy-learning-productions.pages.dev,https://*.easy-learning-productions.pages.dev
FIXCART_SMS_PROVIDER=MOCK
FIXCART_EMAIL_PROVIDER=GMAIL_SMTP
FIXCART_EMAIL_FROM_ADDRESS=<your-gmail>
FIXCART_EMAIL_FROM_NAME=fixcart
FIXCART_MAIL_HOST=smtp.gmail.com
FIXCART_MAIL_PORT=587
FIXCART_MAIL_USERNAME=<your-gmail>
FIXCART_MAIL_PASSWORD=<gmail-app-password>
```

6. Deploy and verify:

```text
https://fixcart-backend.onrender.com/actuator/health
```

You should see JSON with `"status":"UP"`.

## 4. Create Gmail App Password

1. Open `https://myaccount.google.com/security`
2. Enable `2-Step Verification`
3. Open `https://myaccount.google.com/apppasswords`
4. Create app password named `fixcart`
5. Put that value into `FIXCART_MAIL_PASSWORD`

## 5. Deploy frontend on Cloudflare Pages

1. Open `https://dash.cloudflare.com/`
2. Go to `Workers & Pages`
3. Create Pages project from GitHub repo
4. Use build settings:
   - Framework preset: `React (Vite)`
   - Build command: `npm run build`
   - Build output directory: `dist`
   - Root directory: `fixcart/fixcart-frontend`
5. Add environment variable:

```text
VITE_FIXCART_API_URL=https://fixcart-backend.onrender.com
```

6. Deploy

## 6. Validate end-to-end

1. Open frontend URL
2. Register customer
3. Register worker
4. Login as admin
5. Approve worker
6. Create booking
7. Update worker location
8. Confirm realtime list/tracking updates
9. Test voice command:

```text
hello fixcart book electrician for me
```

## 7. Common failures

### Frontend says `Failed to fetch`

- backend not live
- wrong `VITE_FIXCART_API_URL`
- CORS origin missing in `FIXCART_CORS_ALLOWED_ORIGINS`

### Render is live but health fails

- stale environment variables
- database URL/credentials invalid
- old deployment not rebuilt

### Cloudflare tries `vitepress`

Your Pages build settings are wrong. Reset to:

- Framework preset: `React (Vite)`
- Build command: `npm run build`
- Output directory: `dist`
- Root directory: `fixcart/fixcart-frontend`

## 8. Production gaps you should still close

- move to a paid SMS provider if phone OTP is required
- replace Gmail SMTP with a proper transactional email provider
- rotate all secrets exposed during setup
- add DB backups
- add error monitoring
- add real map geocoding and route provider
