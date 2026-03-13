import { useEffect, useMemo, useState } from "react";
import { executeVoiceCommand } from "../api/fixcartApi";
import { useAuth } from "../context/AuthContext";
import { useFixcartSettings } from "../context/FixcartSettingsContext";

const SpeechRecognitionApi = window.SpeechRecognition || window.webkitSpeechRecognition;
const speechSynthesisApi = window.speechSynthesis;
const FIXCART_VOICE_TIMELINE_KEY = "fixcart_voice_timeline";

export default function VoiceAssistant({ onCommandResult }) {
  const { auth } = useAuth();
  const { speechCode, copy, addressHint } = useFixcartSettings();
  const [transcript, setTranscript] = useState("");
  const [serviceAddress, setServiceAddress] = useState("");
  const [loading, setLoading] = useState(false);
  const [listening, setListening] = useState(false);
  const [message, setMessage] = useState(copy.voiceCta);
  const [timeline, setTimeline] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem(FIXCART_VOICE_TIMELINE_KEY) || "[]");
    } catch {
      return [];
    }
  });

  const isSupported = useMemo(() => Boolean(SpeechRecognitionApi), []);

  useEffect(() => {
    localStorage.setItem(FIXCART_VOICE_TIMELINE_KEY, JSON.stringify(timeline.slice(0, 20)));
  }, [timeline]);

  const speak = (text) => {
    if (!speechSynthesisApi || !text) return;
    speechSynthesisApi.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = speechCode;
    utterance.rate = 1;
    utterance.pitch = 1;
    speechSynthesisApi.speak(utterance);
  };

  const detectLocation = () =>
    new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        reject(new Error("Geolocation is not supported in this browser."));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (position) =>
          resolve({
            latitude: Number(position.coords.latitude.toFixed(6)),
            longitude: Number(position.coords.longitude.toFixed(6))
          }),
        () => reject(new Error("Location access is required for fixcart voice automation.")),
        { enableHighAccuracy: true, timeout: 10000 }
      );
    });

  const submitCommand = async (spokenText) => {
    setLoading(true);
    try {
      const location = await detectLocation();
      const response = await executeVoiceCommand(
        {
          transcript: spokenText,
          serviceAddress: serviceAddress || "Current detected location",
          languageCode: speechCode,
          latitude: location.latitude,
          longitude: location.longitude
        },
        auth.token
      );
      setMessage(response.spokenResponse);
      setTimeline((current) => [
        {
          id: Date.now(),
          role: "assistant",
          transcript: spokenText,
          response: response.spokenResponse,
          action: response.action,
          thresholdKm: response.etaNotificationThresholdKm,
          createdAt: new Date().toISOString()
        },
        ...current
      ].slice(0, 20));
      speak(response.spokenResponse);
      onCommandResult?.(response);
    } catch (error) {
      setMessage(error.message);
      setTimeline((current) => [
        {
          id: Date.now(),
          role: "assistant",
          transcript: spokenText,
          response: error.message,
          action: "ERROR",
          createdAt: new Date().toISOString()
        },
        ...current
      ].slice(0, 20));
      speak(error.message);
    } finally {
      setLoading(false);
    }
  };

  const startListening = () => {
    if (!SpeechRecognitionApi) {
      setMessage("Voice recognition is not available in this browser.");
      return;
    }
    const recognition = new SpeechRecognitionApi();
    recognition.lang = speechCode;
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    setListening(true);
    recognition.onresult = async (event) => {
      const spokenText = event.results[0][0].transcript;
      setTranscript(spokenText);
      setTimeline((current) => [
        {
          id: Date.now(),
          role: "user",
          transcript: spokenText,
          createdAt: new Date().toISOString()
        },
        ...current
      ].slice(0, 20));
      await submitCommand(spokenText);
    };
    recognition.onerror = () => {
      setMessage("Voice capture failed. Try again or type the command.");
      setListening(false);
    };
    recognition.onend = () => {
      setListening(false);
    };
    recognition.start();
  };

  const onManualSubmit = async (event) => {
    event.preventDefault();
    if (!transcript.trim()) {
      setMessage("Enter or speak a command first.");
      return;
    }
    setTimeline((current) => [
      {
        id: Date.now(),
        role: "user",
        transcript: transcript.trim(),
        createdAt: new Date().toISOString()
      },
      ...current
    ].slice(0, 20));
    await submitCommand(transcript.trim());
  };

  return (
    <section className="voice-assistant card full-width">
      <div className="voice-assistant-head">
        <div>
          <h2>{copy.voiceTitle}</h2>
          <p className="muted">{copy.voiceSubtitle}</p>
        </div>
        <button type="button" onClick={startListening} disabled={loading || listening}>
          {listening ? "Listening..." : "Start Voice Command"}
        </button>
      </div>
      <form onSubmit={onManualSubmit} className="form-grid">
        <textarea
          placeholder={copy.voiceCta}
          value={transcript}
          onChange={(event) => setTranscript(event.target.value)}
          rows={3}
        />
        <input
          placeholder={`Service address hint: ${addressHint}`}
          value={serviceAddress}
          onChange={(event) => setServiceAddress(event.target.value)}
        />
        <div className="action-row">
          <button type="submit" disabled={loading}>
            {loading ? "Executing..." : "Run Command"}
          </button>
          {!isSupported && <span className="muted">Voice recognition support depends on browser.</span>}
        </div>
      </form>
      <div className="assistant-response">
        <strong>Fixcart says</strong>
        <p>{message}</p>
      </div>
      <div className="voice-timeline">
        <div className="voice-timeline-head">
          <strong>Voice Timeline</strong>
          <button type="button" className="ghost-btn" onClick={() => setTimeline([])}>
            Clear
          </button>
        </div>
        <div className="list">
          {timeline.map((item) => (
            <article key={item.id} className={`list-item voice-entry voice-entry-${item.role}`}>
              <strong>{item.role === "user" ? "You" : "fixcart"}</strong>
              <p>{item.role === "user" ? item.transcript : item.response}</p>
              {item.role === "assistant" && item.transcript && (
                <small className="muted">Command: {item.transcript}</small>
              )}
              {item.thresholdKm && (
                <small className="muted">ETA alert threshold: {item.thresholdKm} km</small>
              )}
            </article>
          ))}
          {timeline.length === 0 && <p className="muted">Your multi-turn voice timeline will appear here.</p>}
        </div>
      </div>
    </section>
  );
}
