import { useEffect, useState } from "react";
import {
  getAdminAuditLogs,
  getAdminBookings,
  getDispatchConfiguration,
  getAdminMetrics,
  getAdminWorkers,
  updateDispatchConfiguration,
  updateAdminWorkerApproval,
  updateAdminWorkerAvailability
} from "../api/fixcartApi";
import { useAuth } from "../context/AuthContext";

export default function AdminDashboard() {
  const { auth } = useAuth();
  const [metrics, setMetrics] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [workers, setWorkers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [dispatchConfig, setDispatchConfig] = useState(null);
  const [error, setError] = useState("");

  const loadAll = async () => {
    setError("");
    try {
      const [metricsData, workersData, bookingsData, auditLogData, dispatchData] = await Promise.all([
        getAdminMetrics(auth.token),
        getAdminWorkers(auth.token),
        getAdminBookings(auth.token),
        getAdminAuditLogs(auth.token),
        getDispatchConfiguration(auth.token)
      ]);
      setMetrics(metricsData);
      setWorkers(workersData);
      setBookings(bookingsData);
      setAuditLogs(auditLogData);
      setDispatchConfig(dispatchData);
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

  const onApprovalChange = async (workerId, approvalStatus) => {
    try {
      await updateAdminWorkerApproval(workerId, { approvalStatus }, auth.token);
      await loadAll();
    } catch (err) {
      setError(err.message);
    }
  };

  const onDispatchConfigChange = async (event) => {
    event.preventDefault();
    try {
      await updateDispatchConfiguration(
        {
          stalledMinutesThreshold: Number(dispatchConfig.stalledMinutesThreshold),
          regressionDistanceKm: Number(dispatchConfig.regressionDistanceKm),
          etaRegressionMinutes: Number(dispatchConfig.etaRegressionMinutes),
          inactiveSpeedThresholdKmh: Number(dispatchConfig.inactiveSpeedThresholdKmh)
        },
        auth.token
      );
      await loadAll();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <main className="dashboard-layout">
      <section className="card full-width">
        <h2>Founder Ops Pulse</h2>
        {error && <div className="error-box">{error}</div>}
        {metrics ? (
          <div className="stats-grid founder-grid">
            <div className="stat-box">Customers: {metrics.totalCustomers}</div>
            <div className="stat-box">Workers: {metrics.totalWorkers}</div>
            <div className="stat-box">Available: {metrics.availableWorkers}</div>
            <div className="stat-box">Bookings: {metrics.totalBookings}</div>
            <div className="stat-box">Completed: {metrics.completedBookings}</div>
            <div className="stat-box">Payments: {metrics.successfulPayments}</div>
            <div className="stat-box">Active Jobs: {metrics.activeBookings}</div>
            <div className="stat-box">Pending Approvals: {metrics.pendingWorkerApprovals}</div>
            <div className="stat-box">GMV: {metrics.grossMerchandiseValue}</div>
            <div className="stat-box">Avg Order: {metrics.averageOrderValue}</div>
            <div className="stat-box">Completion Rate: {metrics.completionRate.toFixed(1)}%</div>
            <div className="stat-box">Cancellation Rate: {metrics.cancellationRate.toFixed(1)}%</div>
            <div className="stat-box">Worker Utilization: {metrics.workerUtilizationRate.toFixed(1)}%</div>
            <div className="stat-box">Assignment Rate: {metrics.assignmentRate.toFixed(1)}%</div>
          </div>
        ) : (
          <p>Loading metrics...</p>
        )}
      </section>

      <section className="card">
        <h2>Live Dispatch Controls</h2>
        {dispatchConfig ? (
          <form onSubmit={onDispatchConfigChange} className="form-grid">
            <input
              type="number"
              value={dispatchConfig.stalledMinutesThreshold}
              onChange={(e) => setDispatchConfig({ ...dispatchConfig, stalledMinutesThreshold: e.target.value })}
              placeholder="Stalled minutes threshold"
            />
            <input
              type="number"
              step="0.1"
              value={dispatchConfig.regressionDistanceKm}
              onChange={(e) => setDispatchConfig({ ...dispatchConfig, regressionDistanceKm: e.target.value })}
              placeholder="Regression distance km"
            />
            <input
              type="number"
              value={dispatchConfig.etaRegressionMinutes}
              onChange={(e) => setDispatchConfig({ ...dispatchConfig, etaRegressionMinutes: e.target.value })}
              placeholder="ETA regression minutes"
            />
            <input
              type="number"
              step="0.1"
              value={dispatchConfig.inactiveSpeedThresholdKmh}
              onChange={(e) => setDispatchConfig({ ...dispatchConfig, inactiveSpeedThresholdKmh: e.target.value })}
              placeholder="Inactive speed threshold"
            />
            <button type="submit">Save Dispatch Rules</button>
          </form>
        ) : (
          <p className="muted">Loading dispatch controls...</p>
        )}
      </section>

      <section className="card">
        <h2>Top Service Demand</h2>
        <div className="list">
          {metrics?.topServiceDemand?.map((item) => (
            <article key={item.serviceType} className="list-item">
              <strong>{item.serviceType}</strong>
              <p>{item.totalBookings} bookings</p>
            </article>
          ))}
          {!metrics?.topServiceDemand?.length && <p className="muted">No service demand data yet.</p>}
        </div>
      </section>

      <section className="card">
        <h2>Workers</h2>
        <div className="list">
          {workers.map((worker) => (
            <article key={worker.workerId} className="list-item">
              <strong>{worker.fullName}</strong>
              <p>{worker.workerType}</p>
              <p>Approval: {worker.approvalStatus}</p>
              <p>Experience: {worker.yearsOfExperience} years</p>
              <div className="action-row">
                <button onClick={() => onToggleAvailability(worker.workerId, worker.available)}>
                  Set {worker.available ? "Unavailable" : "Available"}
                </button>
                <button onClick={() => onApprovalChange(worker.workerId, "APPROVED")}>Approve</button>
                <button onClick={() => onApprovalChange(worker.workerId, "REJECTED")}>Reject</button>
                <button onClick={() => onApprovalChange(worker.workerId, "SUSPENDED")}>Suspend</button>
              </div>
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
                {booking.serviceType} - {booking.status}
              </p>
              <p>Estimated price: {booking.estimatedPrice}</p>
              {booking.scheduledAt && <p>Scheduled at: {new Date(booking.scheduledAt).toLocaleString()}</p>}
            </article>
          ))}
        </div>
      </section>

      <section className="card full-width">
        <h2>Audit Logs</h2>
        <div className="list">
          {auditLogs.map((log) => (
            <article key={log.id} className="list-item">
              <strong>{log.actionType}</strong>
              <p>{log.entityType} #{log.entityId ?? "-"}</p>
              <p>{log.details}</p>
              <p>{new Date(log.createdAt).toLocaleString()}</p>
            </article>
          ))}
          {auditLogs.length === 0 && <p className="muted">No audit logs yet.</p>}
        </div>
      </section>
    </main>
  );
}
