import { createContext, useContext, useMemo, useState } from "react";

const FixcartSettingsContext = createContext(null);
const FIXCART_SETTINGS_KEY = "fixcart_settings";

const LANGUAGE_OPTIONS = [
  { value: "en", label: "English", speechCode: "en-US" },
  { value: "hi", label: "Hindi", speechCode: "hi-IN" },
  { value: "es", label: "Spanish", speechCode: "es-ES" }
];

const REGION_OPTIONS = [
  {
    value: "GLOBAL",
    label: "Global",
    currency: "USD",
    phonePlaceholder: "+1 415 555 0132",
    addressHint: "Street, area, city, country"
  },
  {
    value: "IN",
    label: "India",
    currency: "INR",
    phonePlaceholder: "+91 9876543210",
    addressHint: "Flat, area, landmark, city"
  },
  {
    value: "US",
    label: "United States",
    currency: "USD",
    phonePlaceholder: "+1 415 555 0132",
    addressHint: "Apartment, street, city, state"
  },
  {
    value: "EU",
    label: "Europe",
    currency: "EUR",
    phonePlaceholder: "+49 1512 3456789",
    addressHint: "Street, postal code, city"
  }
];

const COPY = {
  en: {
    heroTitle: "Book trusted home service workers in minutes.",
    heroSubtitle:
      "fixcart combines live worker discovery, AI voice booking, realtime tracking and operations-grade updates for any region.",
    voiceCta: "Try saying: hello fixcart book an electrician near me",
    loginTitle: "Access fixcart anywhere",
    loginSubtitle: "Use password or OTP login with a flow designed for international customers and workers.",
    registerTitle: "Create your fixcart account",
    registerSubtitle: "Register as a customer or worker with a globally usable profile and email verification.",
    voiceTitle: "fixcart Voice Concierge",
    voiceSubtitle: "Speak naturally. fixcart can detect intent, current location and next action.",
    regionLabel: "Region",
    languageLabel: "Language"
  },
  hi: {
    heroTitle: "Kuch minute mein trusted service worker book kijiye.",
    heroSubtitle:
      "fixcart live worker discovery, AI voice booking, realtime tracking aur operational updates ko ek hi flow mein deta hai.",
    voiceCta: "Bol kar dekhiye: hello fixcart plumber book karo",
    loginTitle: "fixcart mein wapas aayein",
    loginSubtitle: "Password ya OTP se login kijiye. Flow multi-region users ke liye tayar hai.",
    registerTitle: "Apna fixcart account banaiye",
    registerSubtitle: "Customer ya worker ke roop mein register kijiye aur email verify kijiye.",
    voiceTitle: "fixcart Voice Concierge",
    voiceSubtitle: "Normal language mein boliye. fixcart intent aur location samajhkar action leta hai.",
    regionLabel: "Region",
    languageLabel: "Language"
  },
  es: {
    heroTitle: "Reserva trabajadores de confianza para el hogar en minutos.",
    heroSubtitle:
      "fixcart combina descubrimiento en vivo, reservas por voz con IA, rastreo en tiempo real y actualizaciones operativas.",
    voiceCta: "Prueba: hello fixcart book a plumber near me",
    loginTitle: "Accede a fixcart desde cualquier lugar",
    loginSubtitle: "Inicia sesión con contraseña u OTP en un flujo preparado para uso internacional.",
    registerTitle: "Crea tu cuenta de fixcart",
    registerSubtitle: "Regístrate como cliente o trabajador con un perfil usable globalmente y verificación por email.",
    voiceTitle: "fixcart Voice Concierge",
    voiceSubtitle: "Habla con naturalidad. fixcart detecta intención, ubicación y siguiente acción.",
    regionLabel: "Región",
    languageLabel: "Idioma"
  }
};

function readInitialSettings() {
  const raw = localStorage.getItem(FIXCART_SETTINGS_KEY);
  if (!raw) {
    return { language: "en", region: "GLOBAL" };
  }
  try {
    const parsed = JSON.parse(raw);
    return {
      language: parsed.language || "en",
      region: parsed.region || "GLOBAL"
    };
  } catch (error) {
    return { language: "en", region: "GLOBAL" };
  }
}

export function FixcartSettingsProvider({ children }) {
  const [settings, setSettings] = useState(readInitialSettings());

  const value = useMemo(() => {
    const languageConfig =
      LANGUAGE_OPTIONS.find((option) => option.value === settings.language) || LANGUAGE_OPTIONS[0];
    const regionConfig =
      REGION_OPTIONS.find((option) => option.value === settings.region) || REGION_OPTIONS[0];
    return {
      settings,
      languageOptions: LANGUAGE_OPTIONS,
      regionOptions: REGION_OPTIONS,
      copy: COPY[languageConfig.value] || COPY.en,
      speechCode: languageConfig.speechCode,
      phonePlaceholder: regionConfig.phonePlaceholder,
      addressHint: regionConfig.addressHint,
      currency: regionConfig.currency,
      updateSettings: (patch) => {
        const next = { ...settings, ...patch };
        localStorage.setItem(FIXCART_SETTINGS_KEY, JSON.stringify(next));
        setSettings(next);
      }
    };
  }, [settings]);

  return (
    <FixcartSettingsContext.Provider value={value}>
      {children}
    </FixcartSettingsContext.Provider>
  );
}

export function useFixcartSettings() {
  const context = useContext(FixcartSettingsContext);
  if (!context) {
    throw new Error("useFixcartSettings must be used inside FixcartSettingsProvider");
  }
  return context;
}
