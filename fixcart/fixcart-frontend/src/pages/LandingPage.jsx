import { Link } from "react-router-dom";
import LanguageRegionSelector from "../components/LanguageRegionSelector";
import { useFixcartSettings } from "../context/FixcartSettingsContext";

const CAPABILITIES = [
  "Voice-first booking with live location detection",
  "Realtime worker discovery and assignment updates",
  "Multi-service marketplace for global regions",
  "Operations-grade admin and worker coordination"
];

export default function LandingPage() {
  const { copy } = useFixcartSettings();

  return (
    <section className="landing-shell">
      <div className="hero-panel">
        <div className="hero-copy">
          <div className="eyebrow">AI-assisted service marketplace</div>
          <h1>{copy.heroTitle}</h1>
          <p>{copy.heroSubtitle}</p>
          <div className="hero-actions">
            <Link className="primary-link" to="/register">
              Get Started
            </Link>
            <Link className="ghost-link" to="/login">
              Login
            </Link>
          </div>
          <div className="voice-chip">{copy.voiceCta}</div>
        </div>
        <div className="hero-side">
          <LanguageRegionSelector />
          <div className="hero-card-stack">
            {CAPABILITIES.map((capability) => (
              <article key={capability} className="hero-card">
                <strong>{capability}</strong>
              </article>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
