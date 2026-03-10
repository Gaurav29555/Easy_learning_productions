import { useEffect, useState } from "react";
import {
  getTrackingEvents,
  getWorkerBookings,
  publishTracking,
  updateBookingStatus,
  updateWorkerLocation
} from "../api/fixcartApi";
import LiveTrackingMap from "../components/LiveTrackingMap";
import LocationPicker from "../components/LocationPicker";
import { useAuth } from "../context/AuthContext";

export default function WorkerDashboard() {
  const { auth } = useAuth();
  const [bookings, setBookings] = useState([]);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [trackingEvents, setTrackingEvents] = useState([]);
  const [selectedBookingIdForMap, setSelectedBookingIdForMap] = useState("");
  const [locationForm, setLocationForm] = useState({ latitude: null, longitude: null });
  const [trackingForm, setTrackingForm] = useState({
    bookingId: "",
    latitude: null,
    longitude: null,
    speedKmh: "0"
  });

  const loadBookings = async () => {
    try {
      const data = await getWorkerBookings(auth.token);
      setBookings(data);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    loadBookings();
  }, []);

  const onUpdateLocation = async (event) => {
    event.preventDefault();
    setError("");
    setInfo("");
    try {
      await updateWorkerLocation(
        {
          latitude: Number(locationForm.latitude),
          longitude: Number(locationForm.longitude)
        },
        auth.token
      );
      setInfo("Location updated");
    } catch (err) {
      setError(err.message);
    }
  };

  const onStatusChange = async (bookingId, status) => {
    setError("");
    setInfo("");
    try {
      await updateBookingStatus(bookingId, { status }, auth.token);
      await loadBookings();
      setInfo(`Booking ${bookingId} updated to ${status}`);
    } catch (err) {
      setError(err.message);
    }
  };

  const onPublishTracking = async (event) => {
    event.preventDefault();
    setError("");
    setInfo("");
    try {
      const eventData = await publishTracking(
        Number(trackingForm.bookingId),
        {
          latitude: Number(trackingForm.latitude),
          longitude: Number(trackingForm.longitude),
          speedKmh: Number(trackingForm.speedKmh)
        },
        auth.token
      );
      setSelectedBookingIdForMap(String(trackingForm.bookingId));
      setTrackingEvents((prev) => [eventData, ...prev].slice(0, 200));
      setInfo("Tracking event published");
    } catch (err) {
      setError(err.message);
    }
  };

  const onLoadTracking = async () => {
    setError("");
    try {
      const data = await getTrackingEvents(Number(trackingForm.bookingId), auth.token);
      setTrackingEvents(data);
      setSelectedBookingIdForMap(String(trackingForm.bookingId));
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <main className="dashboard-layout">
      <section className="card">
        <h2>Update Worker Location</h2>
        {error && <div className="error-box">{error}</div>}
        {info && <div className="info-box">{info}</div>}
        <form onSubmit={onUpdateLocation} className="form-grid">
          <LocationPicker
            title="Current Worker Position"
            latitude={locationForm.latitude}
            longitude={locationForm.longitude}
            onChange={(latitude, longitude) => setLocationForm({ latitude, longitude })}
          />
          <button type="submit" disabled={!locationForm.latitude || !locationForm.longitude}>
            Update Location
          </button>
        </form>
      </section>

      <section className="card">
        <h2>Publish Live Tracking</h2>
        <form onSubmit={onPublishTracking} className="form-grid">
          <input
            placeholder="Booking ID"
            value={trackingForm.bookingId}
            onChange={(e) => setTrackingForm({ ...trackingForm, bookingId: e.target.value })}
            required
          />
          <input
            type="number"
            step="any"
            placeholder="Speed km/h"
            value={trackingForm.speedKmh}
            onChange={(e) => setTrackingForm({ ...trackingForm, speedKmh: e.target.value })}
          />
          <LocationPicker
            title="Tracking Pin"
            latitude={trackingForm.latitude}
            longitude={trackingForm.longitude}
            onChange={(latitude, longitude) => setTrackingForm({ ...trackingForm, latitude, longitude })}
          />
          <div className="action-row">
            <button type="submit" disabled={!trackingForm.latitude || !trackingForm.longitude}>
              Send Tracking Update
            </button>
            <button type="button" className="ghost-btn" onClick={onLoadTracking}>
              Load Tracking Feed
            </button>
          </div>
        </form>
        {selectedBookingIdForMap && (
          <LiveTrackingMap
            bookingId={selectedBookingIdForMap}
            initialEvents={trackingEvents}
            onLiveEvent={(event) => setTrackingEvents((prev) => [event, ...prev].slice(0, 200))}
          />
        )}
      </section>

      <section className="card full-width">
        <h2>My Assigned Bookings</h2>
        <button className="ghost-btn" onClick={loadBookings}>
          Refresh
        </button>
        <div className="list">
          {bookings.map((booking) => (
            <article className="list-item" key={booking.bookingId}>
              <strong>Booking #{booking.bookingId}</strong>
              <p>
                {booking.serviceType} - {booking.status}
              </p>
              <p>{booking.serviceAddress}</p>
              <div className="action-row">
                <button onClick={() => onStatusChange(booking.bookingId, "IN_PROGRESS")}>
                  Mark In Progress
                </button>
                <button onClick={() => onStatusChange(booking.bookingId, "COMPLETED")}>
                  Mark Completed
                </button>
              </div>
            </article>
          ))}
          {bookings.length === 0 && <p className="muted">No assigned bookings.</p>}
        </div>
      </section>
    </main>
  );
}
