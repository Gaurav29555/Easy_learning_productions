import { useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const SpeechRecognitionApi = window.SpeechRecognition || window.webkitSpeechRecognition;

export default function VoiceLauncherButton() {
  const { auth, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const canUseVoice = useMemo(
    () => isAuthenticated && (auth.role === "CUSTOMER" || auth.role === "ADMIN"),
    [auth.role, isAuthenticated]
  );

  if (!canUseVoice) {
    return null;
  }

  const onOpenAssistant = () => {
    if (location.pathname !== "/assistant") {
      navigate("/assistant");
      return;
    }

    const assistant = document.querySelector(".voice-assistant");
    if (assistant) {
      assistant.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  };

  return (
    <button
      type="button"
      className="voice-launcher"
      onClick={onOpenAssistant}
      title={SpeechRecognitionApi ? "Open fixcart voice assistant" : "Open fixcart assistant. Your browser has limited voice support."}
    >
      <span className="voice-launcher-icon" aria-hidden="true">Mic</span>
      <span className="voice-launcher-copy">
        <strong>Speak to fixcart</strong>
        <small>{SpeechRecognitionApi ? "Open the mic assistant" : "Open assistant"}</small>
      </span>
    </button>
  );
}
