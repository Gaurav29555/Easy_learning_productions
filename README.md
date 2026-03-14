# Fixcart

Fixcart is an on-demand home services marketplace, similar to Rapido, where users can book workers for various services like carpentry, electrical work, plumbing, etc.

## Features

- User and worker registration/login
- Real-time worker booking
- Payment integration with Stripe
- Rating and review system
- Voice command integration
- Real-time notifications via WebSockets
- Search and filter workers

## Tech Stack

### Backend
- Node.js + Express
- MongoDB
- Socket.io for real-time
- JWT for authentication
- Stripe for payments

### Frontend (Web)
- React + Vite
- TailwindCSS
- React Router
- Axios for API calls
- Web Speech API for voice

### Mobile
- React Native (separate app)

## Setup

### Backend
1. cd fixcart-backend
2. npm install
3. Set up MongoDB
4. cp .env.example .env and fill in values
5. npm start

### Frontend
1. cd fixcart/fixcart-frontend
2. npm install
3. npm run dev

### Mobile
1. Set up React Native environment
2. Create new React Native app
3. Implement similar components

## Deployment
- Backend: Docker + cloud (AWS/Azure)
- Frontend: Vercel/Netlify
- Mobile: App stores

## API Documentation
- Base URL: http://localhost:5000/api
- Auth: /api/auth
- Workers: /api/workers
- Bookings: /api/bookings
- Payments: /api/payments
- Reviews: /api/reviews