const FIXCART_API_BASE_URL =
  import.meta.env.VITE_FIXCART_API_URL || "http://localhost:5000";

async function request(path, method = "GET", body, token) {
  let response;
  try {
    response = await fetch(`${FIXCART_API_BASE_URL}${path}`, {
      method,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: body ? JSON.stringify(body) : undefined
    });
  } catch (error) {
    throw new Error("Cannot reach the fixcart backend right now. Check deployment and CORS settings.");
  }

  if (!response.ok) {
    let message = "Request failed";
    try {
      const errorPayload = await response.json();
      message = errorPayload.error || message;
    } catch (error) {
      message = response.statusText || message;
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }
  return response.json();
}

export function register(payload) {
  return request("/api/auth/register", "POST", payload);
}

export function login(payload) {
  return request("/api/auth/login", "POST", payload);
}

export function sendOtp(payload) {
  return request("/api/auth/otp/send", "POST", payload);
}

export function verifyOtp(payload) {
  return request("/api/auth/otp/verify", "POST", payload);
}

export function getWorkers(params) {
  const search = new URLSearchParams(params);
  return request(`/api/workers?${search.toString()}`);
}

export function getWorker(id) {
  return request(`/api/workers/${id}`);
}

export function createBooking(payload, token) {
  return request("/api/bookings", "POST", payload, token);
}

export function getUserBookings(token) {
  return request("/api/bookings/user", "GET", undefined, token);
}

export function getWorkerBookings(token) {
  return request("/api/bookings/worker", "GET", undefined, token);
}

export function updateBookingStatus(id, payload, token) {
  return request(`/api/bookings/${id}/status`, "PUT", payload, token);
}

export function createPaymentIntent(payload, token) {
  return request("/api/payments/create-payment-intent", "POST", payload, token);
}

export function confirmPayment(id, token) {
  return request(`/api/payments/confirm/${id}`, "POST", {}, token);
}

export function createReview(payload, token) {
  return request("/api/reviews", "POST", payload, token);
}

export function getWorkerReviews(id) {
  return request(`/api/reviews/worker/${id}`);
}

export function executeVoiceCommand(payload, token) {
  return request("/api/voice/commands", "POST", payload, token);
}
}

export function getMyPayments(token) {
  return request("/api/payments/my", "GET", undefined, token);
}

export function publishTracking(bookingId, payload, token) {
  return request(`/api/tracking/bookings/${bookingId}/location`, "POST", payload, token);
}

export function getTrackingEvents(bookingId, token) {
  return request(`/api/tracking/bookings/${bookingId}/events`, "GET", undefined, token);
}

export function getRouteSimulation(bookingId, token) {
  return request(`/api/tracking/bookings/${bookingId}/route`, "GET", undefined, token);
}

export function getAdminMetrics(token) {
  return request("/api/admin/metrics", "GET", undefined, token);
}

export function getAdminWorkers(token) {
  return request("/api/admin/workers", "GET", undefined, token);
}

export function getAdminBookings(token) {
  return request("/api/admin/bookings", "GET", undefined, token);
}

export function updateAdminWorkerAvailability(workerId, payload, token) {
  return request(`/api/admin/workers/${workerId}/availability`, "PATCH", payload, token);
}

export function updateAdminWorkerApproval(workerId, payload, token) {
  return request(`/api/admin/workers/${workerId}/approval`, "PATCH", payload, token);
}

export function getAdminAuditLogs(token) {
  return request("/api/admin/audit-logs", "GET", undefined, token);
}

export function getDispatchConfiguration(token) {
  return request("/api/admin/dispatch-config", "GET", undefined, token);
}

export function updateDispatchConfiguration(payload, token) {
  return request("/api/admin/dispatch-config", "PATCH", payload, token);
}

export function getMyNotifications(token) {
  return request("/api/notifications/my", "GET", undefined, token);
}

export function getUnreadNotificationCount(token) {
  return request("/api/notifications/my/unread-count", "GET", undefined, token);
}

export function markNotificationRead(notificationId, token) {
  return request(`/api/notifications/${notificationId}/read`, "PATCH", undefined, token);
}
