# fixcart free deployment guide

## reality check
- I cannot deploy automatically to your cloud accounts from this environment because I do not have your cloud login/API keys.
- I can, however, give you exact steps and env values. Follow this once and your app will be public.

## recommended free architecture
1. Oracle Cloud Always Free:
   - Oracle Database (free)
   - One Always Free VM for `fixcart` backend
2. Cloudflare Pages (or Vercel) for `fixcart-frontend`

## step 1: deploy backend on Oracle VM
1. Create Oracle Cloud Always Free account.
2. Create Always Free compute instance (Ubuntu).
3. SSH into VM.
4. Install Docker:
```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable docker
sudo systemctl start docker
```
5. Push code to GitHub and clone on VM:
```bash
git clone <your-repo-url>
cd fixcart
```
6. Build and run backend container:
```bash
sudo docker build -t fixcart-backend .
sudo docker run -d --name fixcart-backend -p 8080:8080 \
  -e FIXCART_ORACLE_URL='jdbc:oracle:thin:@<oracle-host>:1521/<service>' \
  -e FIXCART_ORACLE_USERNAME='FIXCART' \
  -e FIXCART_ORACLE_PASSWORD='<oracle-password>' \
  -e FIXCART_SMS_PROVIDER='TWILIO' \
  -e FIXCART_TWILIO_ACCOUNT_SID='<sid>' \
  -e FIXCART_TWILIO_AUTH_TOKEN='<token>' \
  -e FIXCART_TWILIO_FROM_NUMBER='<twilio-number>' \
  -e FIXCART_RAZORPAY_KEY_ID='<key-id>' \
  -e FIXCART_RAZORPAY_KEY_SECRET='<key-secret>' \
  -e FIXCART_RAZORPAY_WEBHOOK_SECRET='<webhook-secret>' \
  -e FIXCART_STRIPE_SECRET_KEY='<stripe-secret>' \
  -e FIXCART_STRIPE_WEBHOOK_SECRET='<stripe-webhook-secret>' \
  -e FIXCART_CORS_ALLOWED_ORIGINS='https://<your-frontend-domain>' \
  fixcart-backend
```
7. Open VM security list / firewall for port `8080`.

## step 2: deploy frontend on Cloudflare Pages
1. Push `fixcart-frontend` to GitHub (same repo is fine).
2. Go to Cloudflare Pages -> Create project -> Connect GitHub repo.
3. Build settings:
   - Root directory: `fixcart-frontend`
   - Build command: `npm run build`
   - Output directory: `dist`
4. Add environment variable:
   - `VITE_FIXCART_API_URL=https://<your-backend-domain-or-ip>:8080`
5. Deploy.

## step 3: configure payment webhooks

### Razorpay
- Webhook URL: `https://<backend-domain>/api/payments/webhook/razorpay`
- Secret: same as `FIXCART_RAZORPAY_WEBHOOK_SECRET`

### Stripe
- Webhook URL: `https://<backend-domain>/api/payments/webhook/stripe`
- Events:
  - `payment_intent.succeeded`
  - `payment_intent.payment_failed`
- Secret: same as `FIXCART_STRIPE_WEBHOOK_SECRET`

## step 4: verify production
1. `GET /actuator/health`
2. Register user with OTP
3. Create booking
4. Auto worker assignment
5. Payment order creation + webhook callback
6. Worker tracking updates reflected on customer live map
