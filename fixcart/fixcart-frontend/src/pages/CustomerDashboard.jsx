import { useEffect, useState } from "react";
import {
  confirmPayment,
  createBooking,
  createPaymentOrder,
  findNearbyWorkers,
  getMyBookings,
  getMyPayments,
  getTrackingEvents,
  updateBookingStatus
} from "../api/fixcartApi";
import LiveTrackingMap from "../components/LiveTrackingMap";
import LocationPicker from "../components/LocationPicker";
import { useAuth } from "../context/AuthContext";

export default function CustomerDashboard() {
  const { auth } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [payments, setPayments] = useState([]);
  const [trackingEvents, setTrackingEvents] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [loadingBookings, setLoadingBookings] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [paymentForm, setPaymentForm] = useState({
    bookingId: "",
    amount: "",
    currency: "INR",
    provider: "MOCK"
  });
  const [confirmForm, setConfirmForm] = useState({ providerOrderId: "", providerPaymentId: "" });
  const [trackingBookingId, setTrackingBookingId] = useState("");
  const [selectedBookingIdForMap, setSelectedBookingIdForMap] = useState("");

  const [bookingForm, setBookingForm] = useState({
    serviceType: "PLUMBER",
    serviceAddress: "",
    customerLatitude: null,
    customerLongitude: null,
    scheduledAt: "",
    notes: ""
  });

  const [nearbyForm, setNearbyForm] = useState({
    workerType: "PLUMBER",
    latitude: null,
    longitude: null,
    radiusKm: 20
  });

  const loadBookings = async () => {
    setLoadingBookings(true);
    try {
      const data = await getMyBookings(auth.token);
      setBookings(data);
      const paymentData = await getMyPayments(auth.token);
      setPayments(paymentData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoadingBookings(false);
    }
  };

  useEffect(() => {
    loadBookings();
  }, []);

  const onCreateBooking = async (event) => {
    event.preventDefault();
    setError("");
    setInfo("");
    try {
      await createBooking(
        {
          ...bookingForm,
          customerLatitude: Number(bookingForm.customerLatitude),
          customerLongitude: Number(bookingForm.customerLongitude),
          scheduledAt: bookingForm.scheduledAt || null
        },
        auth.token
      );
      setBookingForm({
        serviceType: "PLUMBER",
        serviceAddress: "",
        customerLatitude: null,
        customerLongitude: null,
        scheduledAt: "",
        notes: ""
      });
      await loadBookings();
      setInfo("Booking created and assignment started.");
    } catch (err) {
      setError(err.message);
    }
  };

  const onFindNearby = async (event) => {
    event.preventDefault();
    setError("");
    setInfo("");
    try {
      const data = await findNearbyWorkers(
        {
          workerType: nearbyForm.workerType,
          latitude: Number(nearbyForm.latitude),
          longitude: Number(nearbyForm.longitude),
          radiusKm: Number(nearbyForm.radiusKm)
        },
        auth.token
      );
      setWorkers(data);
      setInfo(`Found ${data.length} nearby workers.`);
    } catch (err) {
      setError(err.message);
    }
  };

  const onCreatePayment = async (event) => {
    event.preventDefault();
    setError("");
    setInfo("");
    try {
      const payment = await createPaymentOrder(
        {
          ...paymentForm,
          bookingId: Number(paymentForm.bookingId),
          amount: Number(paymentForm.amount)
        },
        auth.token
      );
      setConfirmForm({ ...confirmForm, providerOrderId: payment.providerOrderId });
      setInfo(`Payment order created: ${payment.providerOrderId}`);
      await loadBookings();
    } catch (err) {
      setError(err.message);
    }
  };

  const onConfirmPayment = async (event) => {
    event.preventDefault();
    setError("");
    setInfo("");
    try {
      await confirmPayment(confirmForm, auth.token);
      setInfo("Payment confirmed successfully");
      await loadBookings();
    } catch (err) {
      setError(err.message);
    }
  };

  const onFetchTracking = async () => {
    setError("");
    try {
      const data = await getTrackingEvents(Number(trackingBookingId), auth.token);
      setTrackingEvents(data);
      setSelectedBookingIdForMap(trackingBookingId);
    } catch (err) {
      setError(err.message);
    }
  };

  const onCancelBooking = async (bookingId) => {
    const cancellationReason = window.prompt("Enter cancellation reason for this fixcart booking:");
    if (!cancellationReason) return;
    setError("");
    setInfo("");
    try {
      await updateBookingStatus(bookingId, { status: "CANCELLED", cancellationReason }, auth.token);
      setInfo(`Booking ${bookingId} cancelled.`);
      await loadBookings();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <main className="dashboard-layout">
      <section className="card">
        <h2>Book a Service</h2>
        {error && <div className="error-box">{error}</div>}
        {info && <div className="info-box">{info}</div>}
        <form onSubmit={onCreateBooking} className="form-grid">
          <select
            value={bookingForm.serviceType}
            onChange={(e) => setBookingForm({ ...bookingForm, serviceType: e.target.value })}
          >
            <option value="PLUMBER">PLUMBER</option>
            <option value="CARPENTER">CARPENTER</option>
          </select>
          <input
            placeholder="Service Address"
            value={bookingForm.serviceAddress}
            onChange={(e) => setBookingForm({ ...bookingForm, serviceAddress: e.target.value })}
            required
          />
          <textarea
            placeholder="Notes (optional)"
            value={bookingForm.notes}
            onChange={(e) => setBookingForm({ ...bookingForm, notes: e.target.value })}
          />
          <input
            type="datetime-local"
            value={bookingForm.scheduledAt}
            onChange={(e) => setBookingForm({ ...bookingForm, scheduledAt: e.target.value })}
          />

          <LocationPicker
            title="Select Service Location"
            latitude={bookingForm.customerLatitude}
            longitude={bookingForm.customerLongitude}
            onChange={(latitude, longitude) =>
              setBookingForm({ ...bookingForm, customerLatitude: latitude, customerLongitude: longitude })
            }
          />

          <button type="submit" disabled={!bookingForm.customerLatitude || !bookingForm.customerLongitude}>
            Create Booking
          </button>
        </form>
      </section>

      <section className="card">
        <h2>Find Nearby Workers</h2>
        <form onSubmit={onFindNearby} className="form-grid">
          <select
            value={nearbyForm.workerType}
            onChange={(e) => setNearbyForm({ ...nearbyForm, workerType: e.target.value })}
          >
            <option value="PLUMBER">PLUMBER</option>
            <option value="CARPENTER">CARPENTER</option>
          </select>
          <input
            type="number"
            placeholder="Radius in km"
            value={nearbyForm.radiusKm}
            onChange={(e) => setNearbyForm({ ...nearbyForm, radiusKm: e.target.value })}
          />

          <LocationPicker
            title="Search Around Location"
            latitude={nearbyForm.latitude}
            longitude={nearbyForm.longitude}
            onChange={(latitude, longitude) => setNearbyForm({ ...nearbyForm, latitude, longitude })}
          />

          <button type="submit" disabled={!nearbyForm.latitude || !nearbyForm.longitude}>
            Search Nearby
          </button>
        </form>
        <div className="list">
          {workers.map((worker) => (
            <article className="list-item" key={worker.workerId}>
              <strong>{worker.fullName}</strong>
              <p>
                {worker.workerType} - {worker.distanceKm.toFixed(2)} km - {worker.available ? "Available" : "Busy"}
              </p>
            </article>
          ))}
          {workers.length === 0 && <p className="muted">No nearby worker results yet.</p>}
        </div>
      </section>

      <section className="card full-width">
        <h2>My Bookings</h2>
        <button className="ghost-btn" onClick={loadBookings}>
          Refresh
        </button>
        {loadingBookings ? (
          <p>Loading bookings...</p>
        ) : (
          <div className="list">
            {bookings.map((booking) => (
              <article className="list-item" key={booking.bookingId}>
                <strong>Booking #{booking.bookingId}</strong>
                <p>
                  {booking.serviceType} - {booking.status}
                </p>
                <p>{booking.serviceAddress}</p>
                <p>Estimated price: {booking.estimatedPrice}</p>
                {booking.scheduledAt && <p>Scheduled at: {new Date(booking.scheduledAt).toLocaleString()}</p>}
                {booking.cancellationReason && <p>Cancellation reason: {booking.cancellationReason}</p>}
                {booking.status !== "COMPLETED" && booking.status !== "CANCELLED" && (
                  <button type="button" onClick={() => onCancelBooking(booking.bookingId)}>
                    Cancel Booking
                  </button>
                )}
              </article>
            ))}
            {bookings.length === 0 && <p className="muted">No bookings found.</p>}
          </div>
        )}
      </section>

      <section className="card">
        <h2>Payments</h2>
        <form onSubmit={onCreatePayment} className="form-grid compact-grid">
          <input
            placeholder="Booking ID"
            value={paymentForm.bookingId}
            onChange={(e) => setPaymentForm({ ...paymentForm, bookingId: e.target.value })}
            required
          />
          <input
            placeholder="Amount"
            type="number"
            value={paymentForm.amount}
            onChange={(e) => setPaymentForm({ ...paymentForm, amount: e.target.value })}
            required
          />
          <select
            value={paymentForm.currency}
            onChange={(e) => setPaymentForm({ ...paymentForm, currency: e.target.value })}
          >
            <option value="INR">INR</option>
            <option value="USD">USD</option>
          </select>
          <select
            value={paymentForm.provider}
            onChange={(e) => setPaymentForm({ ...paymentForm, provider: e.target.value })}
          >
            <option value="MOCK">MOCK</option>
            <option value="RAZORPAY">RAZORPAY</option>
            <option value="STRIPE">STRIPE</option>
          </select>
          <button type="submit">Create Payment Order</button>
        </form>

        <form onSubmit={onConfirmPayment} className="form-grid compact-grid">
          <input
            placeholder="Provider Order ID"
            value={confirmForm.providerOrderId}
            onChange={(e) => setConfirmForm({ ...confirmForm, providerOrderId: e.target.value })}
            required
          />
          <input
            placeholder="Provider Payment ID"
            value={confirmForm.providerPaymentId}
            onChange={(e) => setConfirmForm({ ...confirmForm, providerPaymentId: e.target.value })}
            required
          />
          <button type="submit">Confirm Payment</button>
        </form>
        <div className="list">
          {payments.map((payment) => (
            <article key={payment.paymentId} className="list-item">
              <strong>Payment #{payment.paymentId}</strong>
              <p>
                Booking #{payment.bookingId} - {payment.amount} {payment.currency} - {payment.status}
              </p>
            </article>
          ))}
        </div>
      </section>

      <section className="card">
        <h2>Live Tracking Feed</h2>
        <div className="action-row">
          <input
            placeholder="Booking ID"
            value={trackingBookingId}
            onChange={(e) => setTrackingBookingId(e.target.value)}
          />
          <button type="button" onClick={onFetchTracking}>
            Fetch Tracking
          </button>
        </div>
        {selectedBookingIdForMap && (
          <LiveTrackingMap
            bookingId={selectedBookingIdForMap}
            initialEvents={trackingEvents}
            onLiveEvent={(event) => setTrackingEvents((prev) => [event, ...prev].slice(0, 200))}
          />
        )}
        <div className="list">
          {trackingEvents.map((event, idx) => (
            <article key={`${event.createdAt}-${idx}`} className="list-item">
              <p>
                Worker #{event.workerId} - {event.latitude}, {event.longitude} - {event.speedKmh} km/h
              </p>
            </article>
          ))}
          {trackingEvents.length === 0 && <p className="muted">No tracking events yet.</p>}
        </div>
      </section>
    </main>
  );
}
