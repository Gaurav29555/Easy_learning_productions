import { useMemo, useState } from "react";
import { MapContainer, Marker, TileLayer, useMapEvents } from "react-leaflet";
import L from "leaflet";
import iconRetinaUrl from "leaflet/dist/images/marker-icon-2x.png";
import iconUrl from "leaflet/dist/images/marker-icon.png";
import shadowUrl from "leaflet/dist/images/marker-shadow.png";

delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl,
  iconUrl,
  shadowUrl
});

const FIXCART_SAMPLE_LOCATIONS = [
  { label: "Pune (Baner)", latitude: 18.5679, longitude: 73.7671 },
  { label: "Mumbai (Andheri)", latitude: 19.1197, longitude: 72.8468 },
  { label: "Bengaluru (HSR)", latitude: 12.9121, longitude: 77.6446 },
  { label: "Delhi (Connaught Place)", latitude: 28.6315, longitude: 77.2167 }
];

function ClickMarker({ onPick }) {
  useMapEvents({
    click(event) {
      onPick(event.latlng.lat, event.latlng.lng);
    }
  });
  return null;
}

export default function LocationPicker({
  title,
  latitude,
  longitude,
  onChange,
  zoom = 13
}) {
  const [status, setStatus] = useState("");
  const hasLocation = Number.isFinite(latitude) && Number.isFinite(longitude);
  const center = useMemo(() => {
    if (hasLocation) return [latitude, longitude];
    return [18.5204, 73.8567];
  }, [hasLocation, latitude, longitude]);

  const useCurrentLocation = () => {
    if (!navigator.geolocation) {
      setStatus("Geolocation is not supported in this browser.");
      return;
    }
    setStatus("Detecting current location...");
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const lat = Number(position.coords.latitude.toFixed(6));
        const lng = Number(position.coords.longitude.toFixed(6));
        onChange(lat, lng);
        setStatus("Current location detected.");
      },
      () => setStatus("Could not fetch current location. Allow location permission."),
      { enableHighAccuracy: true, timeout: 8000 }
    );
  };

  const applySampleLocation = (event) => {
    const value = event.target.value;
    if (!value) return;
    const match = FIXCART_SAMPLE_LOCATIONS.find((item) => item.label === value);
    if (match) {
      onChange(match.latitude, match.longitude);
      setStatus(`Selected sample location: ${match.label}`);
    }
  };

  return (
    <div className="location-picker">
      <div className="location-picker-head">
        <h3>{title}</h3>
        <div className="action-row">
          <button type="button" onClick={useCurrentLocation}>
            Use Current Location
          </button>
          <select defaultValue="" onChange={applySampleLocation}>
            <option value="">Use Sample Location</option>
            {FIXCART_SAMPLE_LOCATIONS.map((item) => (
              <option key={item.label} value={item.label}>
                {item.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <MapContainer center={center} zoom={zoom} scrollWheelZoom style={{ height: "240px", width: "100%" }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <ClickMarker onPick={onChange} />
        {hasLocation && <Marker position={[latitude, longitude]} />}
      </MapContainer>

      <div className="location-values">
        <span>Latitude: {hasLocation ? latitude : "-"}</span>
        <span>Longitude: {hasLocation ? longitude : "-"}</span>
      </div>
      {status && <p className="muted">{status}</p>}
    </div>
  );
}
