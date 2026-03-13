import { useFixcartSettings } from "../context/FixcartSettingsContext";

export default function LanguageRegionSelector() {
  const {
    settings,
    languageOptions,
    regionOptions,
    updateSettings,
    copy
  } = useFixcartSettings();

  return (
    <div className="settings-strip">
      <label>
        <span>{copy.languageLabel}</span>
        <select
          value={settings.language}
          onChange={(event) => updateSettings({ language: event.target.value })}
        >
          {languageOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
      <label>
        <span>{copy.regionLabel}</span>
        <select
          value={settings.region}
          onChange={(event) => updateSettings({ region: event.target.value })}
        >
          {regionOptions.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
