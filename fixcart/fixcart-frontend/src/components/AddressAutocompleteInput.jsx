import { useEffect, useState } from "react";
import { searchAddresses } from "../api/fixcartApi";
import { useAuth } from "../context/AuthContext";

export default function AddressAutocompleteInput({
  label,
  value,
  placeholder,
  nearLatitude,
  nearLongitude,
  onChange,
  onSelect
}) {
  const { auth } = useAuth();
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!value || value.trim().length < 3) {
      setResults([]);
      return undefined;
    }

    const timeoutId = setTimeout(async () => {
      setLoading(true);
      try {
        const data = await searchAddresses(
          {
            query: value,
            ...(nearLatitude ? { nearLatitude } : {}),
            ...(nearLongitude ? { nearLongitude } : {})
          },
          auth.token
        );
        setResults(data);
      } catch {
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 350);

    return () => clearTimeout(timeoutId);
  }, [auth.token, nearLatitude, nearLongitude, value]);

  return (
    <div className="autocomplete-wrap">
      <label className="field-label">{label}</label>
      <input
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {loading && <small className="muted">Searching addresses...</small>}
      {results.length > 0 && (
        <div className="autocomplete-panel">
          {results.map((item) => (
            <button
              key={`${item.label}-${item.latitude}-${item.longitude}`}
              type="button"
              className="autocomplete-item"
              onClick={() => {
                onSelect(item);
                setResults([]);
              }}
            >
              <strong>{item.label}</strong>
              <small>{item.provider}</small>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
