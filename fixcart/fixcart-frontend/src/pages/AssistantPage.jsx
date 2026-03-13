import { useState } from "react";
import VoiceAssistant from "../components/VoiceAssistant";

export default function AssistantPage() {
  const [lastResult, setLastResult] = useState(null);

  return (
    <main className="dashboard-layout assistant-page">
      <VoiceAssistant onCommandResult={setLastResult} />
      <section className="card">
        <h2>Latest Assistant Context</h2>
        {lastResult ? (
          <div className="list">
            <article className="list-item">
              <strong>Action</strong>
              <p>{lastResult.action}</p>
            </article>
            {lastResult.booking && (
              <article className="list-item">
                <strong>Booking #{lastResult.booking.bookingId}</strong>
                <p>{lastResult.booking.serviceType} - {lastResult.booking.status}</p>
              </article>
            )}
            {lastResult.route && (
              <article className="list-item">
                <strong>Route Snapshot</strong>
                <p>ETA {lastResult.route.etaMinutes} min</p>
                <p>Distance {lastResult.route.totalDistanceKm.toFixed(2)} km</p>
              </article>
            )}
            {lastResult.suggestions?.map((item) => (
              <article key={item} className="list-item">
                <p>{item}</p>
              </article>
            ))}
          </div>
        ) : (
          <p className="muted">Use the assistant to build a full voice/chat flow here.</p>
        )}
      </section>
    </main>
  );
}
