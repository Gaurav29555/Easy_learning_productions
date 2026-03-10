import { useEffect, useMemo, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";
import { MapContainer, Marker, Polyline, TileLayer, Tooltip } from "react-leaflet";
import "leaflet/dist/leaflet.css";

const FIXCART_API_BASE_URL =
  import.meta.env.VITE_FIXCART_API_URL || "http://localhost:8080";

function normalizeEvents(trackingEvents) {
  return trackingEvents
    .slice()
    .reverse()
    .map((event) => [event.latitude, event.longitude]);
}

export default function LiveTrackingMap({ bookingId, initialEvents = [], onLiveEvent }) {
  const [events, setEvents] = useState(initialEvents);

  useEffect(() => {
    setEvents(initialEvents);
  }, [initialEvents]);

  useEffect(() => {
    if (!bookingId) return undefined;
    const client = new Client({
      webSocketFactory: () => new SockJS(`${FIXCART_API_BASE_URL}/ws-fixcart`),
      reconnectDelay: 3000
    });

    client.onConnect = () => {
      client.subscribe(`/topic/booking/${bookingId}/tracking`, (message) => {
        const payload = JSON.parse(message.body);
        setEvents((prev) => [payload, ...prev].slice(0, 200));
        if (onLiveEvent) {
          onLiveEvent(payload);
        }
      });
    };

    client.activate();
    return () => client.deactivate();
  }, [bookingId, onLiveEvent]);

  const path = useMemo(() => normalizeEvents(events), [events]);
  const latest = events.length > 0 ? events[0] : null;

  const center = latest ? [latest.latitude, latest.longitude] : [20.5937, 78.9629];

  return (
    <div className="map-shell">
      <MapContainer center={center} zoom={13} scrollWheelZoom style={{ height: "320px", width: "100%" }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {latest && (
          <Marker position={[latest.latitude, latest.longitude]}>
            <Tooltip direction="top" offset={[0, -8]} permanent>
              Worker live location
            </Tooltip>
          </Marker>
        )}
        {path.length > 1 && <Polyline positions={path} color="#158f63" />}
      </MapContainer>
      <div className="map-meta">
        <strong>Booking #{bookingId || "-"}</strong>
        <span>{events.length} location points</span>
      </div>
    </div>
  );
}
