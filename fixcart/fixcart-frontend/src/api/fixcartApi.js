const FIXCART_API_BASE_URL =
  import.meta.env.VITE_FIXCART_API_URL || "http://localhost:8080";

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
    throw new Error("Cannot reach the fixcart backend right now. Check Render deployment and CORS settings.");
  }

  if (!response.ok) {
    let message = "Request failed";
    try {
      const errorPayload = await response.json();
      message = errorPayload.message || message;
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

export function registerUser(payload) {
  return request("/api/auth/register/user", "POST", payload);
}

export function registerWorker(payload) {
  return request("/api/auth/register/worker", "POST", payload);
}

export function login(payload) {
  return request("/api/auth/login", "POST", payload);
}

export function loginWithOtp(payload) {
  return request("/api/auth/login/otp", "POST", payload);
}

export function sendOtp(payload) {
  return request("/api/auth/otp/send", "POST", payload);
}

export function verifyOtp(payload) {
  return request("/api/auth/otp/verify", "POST", payload);
}

export function getServiceCatalog() {
  return request("/api/catalog/services", "GET");
}

export function executeVoiceCommand(payload, token) {
  return request("/api/voice/commands", "POST", payload, token);
}

export function searchAddresses(params, token) {
  const search = new URLSearchParams(params);
  return request(`/api/locations/search?${search.toString()}`, "GET", undefined, token);
}

export function getRouteEta(params, token) {
  const search = new URLSearchParams(params);
  return request(`/api/locations/route-eta?${search.toString()}`, "GET", undefined, token);
}

export function findNearbyWorkers(params, token) {
  const search = new URLSearchParams(params);
  return request(`/api/workers/nearby?${search.toString()}`, "GET", undefined, token);
}

export function updateWorkerLocation(payload, token) {
  return request("/api/workers/me/location", "PATCH", payload, token);
}

export function createBooking(payload, token) {
  return request("/api/bookings", "POST", payload, token);
}

export function getMyBookings(token) {
  return request("/api/bookings/my", "GET", undefined, token);
}

export function getWorkerBookings(token) {
  return request("/api/bookings/worker", "GET", undefined, token);
}

export function updateBookingStatus(bookingId, payload, token) {
  return request(`/api/bookings/${bookingId}/status`, "PATCH", payload, token);
}

export function createPaymentOrder(payload, token) {
  return request("/api/payments/order", "POST", payload, token);
}

export function confirmPayment(payload, token) {
  return request("/api/payments/confirm", "PATCH", payload, token);
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
