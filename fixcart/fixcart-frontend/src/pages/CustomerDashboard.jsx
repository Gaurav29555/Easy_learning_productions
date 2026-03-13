import { useEffect, useMemo, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";
import {
  confirmPayment,
  createBooking,
  createPaymentOrder,
  getServiceCatalog,
  getRouteSimulation,
  findNearbyWorkers,
  getMyBookings,
  getMyPayments,
  getTrackingEvents,
  updateBookingStatus
} from "../api/fixcartApi";
import LiveTrackingMap from "../components/LiveTrackingMap";
import AddressAutocompleteInput from "../components/AddressAutocompleteInput";
import LocationPicker from "../components/LocationPicker";
import VoiceAssistant from "../components/VoiceAssistant";
import { useAuth } from "../context/AuthContext";

const FIXCART_API_BASE_URL =
  import.meta.env.VITE_FIXCART_API_URL || "http://localhost:8080";

export default function CustomerDashboard() {
  const { auth } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [payments, setPayments] = useState([]);
  const [catalog, setCatalog] = useState([]);
  const [trackingEvents, setTrackingEvents] = useState([]);
  const [routeSimulation, setRouteSimulation] = useState(null);
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
    searchAddress: "",
    latitude: null,
    longitude: null,
    radiusKm: 20
  });
  const serviceOptions = useMemo(
    () => catalog.map((item) => item.workerType),
    [catalog]
  );
  const liveWorkerTopic = useMemo(
    () => `/topic/workers/${nearbyForm.workerType}`,
    [nearbyForm.workerType]
  );

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
    getServiceCatalog().then(setCatalog).catch(() => {});
  }, []);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${FIXCART_API_BASE_URL}/ws-fixcart`),
      reconnectDelay: 3000
    });

    client.onConnect = () => {
      client.subscribe(`/topic/customer/${auth.userId}/bookings`, (message) => {
        const payload = JSON.parse(message.body);
        const incoming = payload.booking;
        setBookings((prev) => {
          const existingIndex = prev.findIndex((booking) => booking.bookingId === incoming.bookingId);
          if (existingIndex === -1) return [incoming, ...prev];
          const clone = prev.slice();
          clone[existingIndex] = incoming;
          return clone;
        });
        setInfo(payload.message);
      });
    };

    client.activate();
    return () => client.deactivate();
  }, [auth.userId]);

  useEffect(() => {
    if (!nearbyForm.latitude || !nearbyForm.longitude) return undefined;

    const client = new Client({
      webSocketFactory: () => new SockJS(`${FIXCART_API_BASE_URL}/ws-fixcart`),
      reconnectDelay: 3000
    });

    client.onConnect = () => {
      client.subscribe(liveWorkerTopic, (message) => {
        const payload = JSON.parse(message.body);
        const worker = payload.worker;
        const toRadians = (value) => (value * Math.PI) / 180;
        const earthRadiusKm = 6371;
        const dLat = toRadians(worker.latitude - Number(nearbyForm.latitude));
        const dLon = toRadians(worker.longitude - Number(nearbyForm.longitude));
        const lat1 = toRadians(Number(nearbyForm.latitude));
        const lat2 = toRadians(worker.latitude);
        const a =
          Math.sin(dLat / 2) ** 2 +
          Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) ** 2;
        const distanceKm = earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        if (distanceKm > Number(nearbyForm.radiusKm || 20)) {
          setWorkers((prev) => prev.filter((item) => item.workerId !== worker.workerId));
          return;
        }

        const enrichedWorker = { ...worker, distanceKm };
        setWorkers((prev) => {
          const existingIndex = prev.findIndex((item) => item.workerId === enrichedWorker.workerId);
          const next = existingIndex === -1
            ? [enrichedWorker, ...prev]
            : prev.map((item) => (item.workerId === enrichedWorker.workerId ? enrichedWorker : item));
          return next.sort((left, right) => left.distanceKm - right.distanceKm);
        });
        setInfo(`Live worker update: ${worker.fullName} moved near your selected area.`);
      });
    };

    client.activate();
    return () => client.deactivate();
  }, [liveWorkerTopic, nearbyForm.latitude, nearbyForm.longitude, nearbyForm.radiusKm]);

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
    if (!trackingBookingId || Number.isNaN(Number(trackingBookingId))) {
      setError("Enter a valid booking ID before loading tracking.");
      return;
    }
    try {
      const data = await getTrackingEvents(Number(trackingBookingId), auth.token);
      const route = await getRouteSimulation(Number(trackingBookingId), auth.token);
      setTrackingEvents(data);
      setRouteSimulation(route);
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
      <VoiceAssistant
        onCommandResult={(response) => {
          if (response.booking) {
            setBookings((prev) => [response.booking, ...prev.filter((item) => item.bookingId !== response.booking.bookingId)]);
            setSelectedBookingIdForMap(String(response.booking.bookingId));
            setTrackingBookingId(String(response.booking.bookingId));
          }
          if (response.workers) {
            setWorkers(response.workers);
          }
          setInfo(response.spokenResponse);
        }}
      />

      <section className="card">
        <h2>Service Catalog</h2>
        <div className="service-grid">
          {catalog.map((item) => (
            <article className="service-tile" key={item.workerType}>
              <strong>{item.title}</strong>
              <p>{item.description}</p>
              <span>From {item.startingPrice}</span>
              <small>{item.etaLabel}</small>
            </article>
          ))}
          {catalog.length === 0 && <p className="muted">Loading service catalog...</p>}
        </div>
      </section>

      <section className="card">
        <h2>Book a Service</h2>
        {error && <div className="error-box">{error}</div>}
        {info && <div className="info-box">{info}</div>}
        <form onSubmit={onCreateBooking} className="form-grid">
          <select
            value={bookingForm.serviceType}
            onChange={(e) => setBookingForm({ ...bookingForm, serviceType: e.target.value })}
          >
            {(serviceOptions.length > 0 ? serviceOptions : ["PLUMBER", "CARPENTER", "ELECTRICIAN", "CLEANER", "AC_REPAIR", "APPLIANCE_REPAIR", "PAINTER"]).map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
          <AddressAutocompleteInput
            label="Service Address"
            value={bookingForm.serviceAddress}
            placeholder="Start typing an address or landmark"
            nearLatitude={bookingForm.customerLatitude}
            nearLongitude={bookingForm.customerLongitude}
            onChange={(nextValue) => setBookingForm({ ...bookingForm, serviceAddress: nextValue })}
            onSelect={(item) => {
              setBookingForm((current) => ({
                ...current,
                serviceAddress: item.label,
                customerLatitude: item.latitude,
                customerLongitude: item.longitude
              }));
              setInfo(`Resolved address to ${item.label}.`);
            }}
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
            {(serviceOptions.length > 0 ? serviceOptions : ["PLUMBER", "CARPENTER", "ELECTRICIAN", "CLEANER", "AC_REPAIR", "APPLIANCE_REPAIR", "PAINTER"]).map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
          <input
            type="number"
            placeholder="Radius in km"
            value={nearbyForm.radiusKm}
            onChange={(e) => setNearbyForm({ ...nearbyForm, radiusKm: e.target.value })}
          />
          <AddressAutocompleteInput
            label="Search Area"
            value={nearbyForm.searchAddress || ""}
            placeholder="Type area, landmark, or city"
            nearLatitude={nearbyForm.latitude}
            nearLongitude={nearbyForm.longitude}
            onChange={(nextValue) => setNearbyForm({ ...nearbyForm, searchAddress: nextValue })}
            onSelect={(item) => {
              setNearbyForm((current) => ({
                ...current,
                searchAddress: item.label,
                latitude: item.latitude,
                longitude: item.longitude
              }));
              setInfo(`Nearby search anchored to ${item.label}.`);
            }}
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
            onChange={(e) => setTrackingBookingId(e.target.value.replace(/\D/g, ""))}
          />
          <button type="button" onClick={onFetchTracking}>
            Fetch Tracking
          </button>
        </div>
        {selectedBookingIdForMap && (
          <LiveTrackingMap
            bookingId={selectedBookingIdForMap}
            initialEvents={trackingEvents}
            routePoints={routeSimulation?.routePoints ?? []}
            onLiveEvent={(event) => setTrackingEvents((prev) => [event, ...prev].slice(0, 200))}
          />
        )}
        {routeSimulation && (
          <p className="muted">
            Simulated route distance: {routeSimulation.totalDistanceKm.toFixed(2)} km | ETA {routeSimulation.etaMinutes} min
          </p>
        )}
        <div className="list">
          {trackingEvents.map((event, idx) => (
            <article key={`${event.createdAt}-${idx}`} className="list-item">
              <p>
                Worker #{event.workerId} - {event.latitude}, {event.longitude} - {event.speedKmh} km/h - {event.distanceToDestinationKm.toFixed(2)} km away - ETA {event.etaMinutes} min
              </p>
            </article>
          ))}
          {trackingEvents.length === 0 && <p className="muted">No tracking events yet.</p>}
        </div>
      </section>
    </main>
  );
}
