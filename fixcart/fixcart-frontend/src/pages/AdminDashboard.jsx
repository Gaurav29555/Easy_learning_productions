import { useEffect, useState } from "react";
import {
  getAdminBookings,
  getAdminMetrics,
  getAdminWorkers,
  updateAdminWorkerAvailability
} from "../api/fixcartApi";
import { useAuth } from "../context/AuthContext";

export default function AdminDashboard() {
  const { auth } = useAuth();
  const [metrics, setMetrics] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [error, setError] = useState("");

  const loadAll = async () => {
    setError("");
    try {
      const [metricsData, workersData, bookingsData] = await Promise.all([
        getAdminMetrics(auth.token),
        getAdminWorkers(auth.token),
        getAdminBookings(auth.token)
      ]);
      setMetrics(metricsData);
      setWorkers(workersData);
      setBookings(bookingsData);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  const onToggleAvailability = async (workerId, available) => {
    try {
      await updateAdminWorkerAvailability(workerId, { available: !available }, auth.token);
      await loadAll();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <main className="dashboard-layout">
      <section className="card full-width">
        <h2>Admin Metrics</h2>
        {error && <div className="error-box">{error}</div>}
        {metrics ? (
          <div className="stats-grid">
            <div className="stat-box">Customers: {metrics.totalCustomers}</div>
            <div className="stat-box">Workers: {metrics.totalWorkers}</div>
            <div className="stat-box">Available: {metrics.availableWorkers}</div>
            <div className="stat-box">Bookings: {metrics.totalBookings}</div>
            <div className="stat-box">Completed: {metrics.completedBookings}</div>
            <div className="stat-box">Payments: {metrics.successfulPayments}</div>
          </div>
        ) : (
          <p>Loading metrics...</p>
        )}
      </section>

      <section className="card">
        <h2>Workers</h2>
        <div className="list">
          {workers.map((worker) => (
            <article key={worker.workerId} className="list-item">
              <strong>{worker.fullName}</strong>
              <p>{worker.workerType}</p>
              <button onClick={() => onToggleAvailability(worker.workerId, worker.available)}>
                Set {worker.available ? "Unavailable" : "Available"}
              </button>
            </article>
          ))}
        </div>
      </section>

      <section className="card">
        <h2>Recent Bookings</h2>
        <div className="list">
          {bookings.map((booking) => (
            <article key={booking.bookingId} className="list-item">
              <strong>Booking #{booking.bookingId}</strong>
              <p>
                {booking.serviceType} · {booking.status}
              </p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
