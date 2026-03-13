import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { registerUser, registerWorker, sendOtp, verifyOtp } from "../api/fixcartApi";
import LocationPicker from "../components/LocationPicker";
import { useAuth } from "../context/AuthContext";
import { useFixcartSettings } from "../context/FixcartSettingsContext";

export default function RegisterPage() {
  const { login: saveAuth } = useAuth();
  const { copy, phonePlaceholder } = useFixcartSettings();
  const navigate = useNavigate();
  const [accountType, setAccountType] = useState("CUSTOMER");
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [loading, setLoading] = useState(false);
  const [otpCode, setOtpCode] = useState("");
  const [otpVerified, setOtpVerified] = useState(false);
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
    phone: "",
    workerType: "PLUMBER",
    kycDocumentUrl: "",
    yearsOfExperience: 0,
    latitude: null,
    longitude: null
  });

  const isWorker = accountType === "WORKER";

  const onSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setInfo("");
    setLoading(true);

    const commonPayload = {
      fullName: form.fullName,
      email: form.email,
      password: form.password,
      phone: form.phone
    };

    try {
      const data = isWorker
        ? await registerWorker({
            ...commonPayload,
            workerType: form.workerType,
            kycDocumentUrl: form.kycDocumentUrl,
            yearsOfExperience: Number(form.yearsOfExperience),
            latitude: Number(form.latitude),
            longitude: Number(form.longitude)
          })
        : await registerUser(commonPayload);

      saveAuth(data);
      if (data.role === "WORKER") navigate("/worker");
      else if (data.role === "ADMIN") navigate("/admin");
      else navigate("/customer");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const onSendOtp = async () => {
    setError("");
    setInfo("");
    const normalizedEmail = form.email.trim().toLowerCase();
    if (!normalizedEmail) {
      setError("Enter email first");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setError("Enter a valid email address.");
      return;
    }
    try {
      const data = await sendOtp({ email: normalizedEmail, purpose: "REGISTER" });
      setForm((current) => ({ ...current, email: normalizedEmail }));
      setInfo(`OTP sent to email. ${data.debugOtp ? `Dev OTP: ${data.debugOtp}` : "Check your inbox."}`);
    } catch (err) {
      setError(err.message);
    }
  };

  const onVerifyOtp = async () => {
    setError("");
    setInfo("");
    const normalizedEmail = form.email.trim().toLowerCase();
    const normalizedOtp = otpCode.replace(/\D/g, "");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setError("Enter the same valid email used for OTP send.");
      return;
    }
    if (!/^[0-9]{6}$/.test(normalizedOtp)) {
      setError("OTP must be exactly 6 digits.");
      return;
    }
    try {
      await verifyOtp({ email: normalizedEmail, purpose: "REGISTER", otpCode: normalizedOtp });
      setForm((current) => ({ ...current, email: normalizedEmail }));
      setOtpCode(normalizedOtp);
      setOtpVerified(true);
      setInfo("OTP verified. You can now register.");
    } catch (err) {
      setOtpVerified(false);
      setError(err.message);
    }
  };

  return (
    <section className="auth-layout">
      <div className="auth-panel wide-auth-panel">
        <h1>{copy.registerTitle}</h1>
        <p>{copy.registerSubtitle}</p>
        {error && <div className="error-box">{error}</div>}
        {info && <div className="info-box">{info}</div>}

        <div className="toggle-wrap">
          <button
            type="button"
            className={accountType === "CUSTOMER" ? "toggle-active" : ""}
            onClick={() => setAccountType("CUSTOMER")}
          >
            Customer
          </button>
          <button
            type="button"
            className={accountType === "WORKER" ? "toggle-active" : ""}
            onClick={() => setAccountType("WORKER")}
          >
            Worker
          </button>
        </div>

        <form onSubmit={onSubmit} className="form-grid">
          <input
            type="text"
            placeholder="Full Name"
            value={form.fullName}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })}
            required
          />
          <input
            type="email"
            placeholder="Email"
            value={form.email}
            onChange={(e) => {
              setForm({ ...form, email: e.target.value });
              setOtpVerified(false);
            }}
            required
          />
          <input
            type="password"
            placeholder="Password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
          <input
            type="text"
            placeholder={`Phone (${phonePlaceholder})`}
            value={form.phone}
            onChange={(e) => {
              setForm({ ...form, phone: e.target.value.replace(/\D/g, "") });
            }}
            required
          />
          <div className="action-row">
            <button type="button" onClick={onSendOtp}>
              Send OTP
            </button>
            <input
              type="text"
              placeholder="Enter OTP"
              value={otpCode}
              onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ""))}
            />
            <button type="button" onClick={onVerifyOtp}>
              Verify OTP
            </button>
          </div>

          {isWorker && (
            <>
              <select
                value={form.workerType}
                onChange={(e) => setForm({ ...form, workerType: e.target.value })}
              >
                <option value="PLUMBER">PLUMBER</option>
                <option value="CARPENTER">CARPENTER</option>
                <option value="ELECTRICIAN">ELECTRICIAN</option>
                <option value="CLEANER">CLEANER</option>
                <option value="AC_REPAIR">AC_REPAIR</option>
                <option value="APPLIANCE_REPAIR">APPLIANCE_REPAIR</option>
                <option value="PAINTER">PAINTER</option>
              </select>
              <input
                type="url"
                placeholder="KYC Document URL (Google Drive / CDN link)"
                value={form.kycDocumentUrl}
                onChange={(e) => setForm({ ...form, kycDocumentUrl: e.target.value })}
              />
              <input
                type="number"
                min="0"
                placeholder="Years of experience"
                value={form.yearsOfExperience}
                onChange={(e) => setForm({ ...form, yearsOfExperience: e.target.value })}
              />

              <LocationPicker
                title="Select Worker Base Location"
                latitude={form.latitude}
                longitude={form.longitude}
                onChange={(latitude, longitude) => setForm({ ...form, latitude, longitude })}
              />
              <p className="muted">Worker accounts enter `fixcart` in pending review state until admin approval.</p>
            </>
          )}

          <button type="submit" disabled={loading || !otpVerified || (isWorker && (!form.latitude || !form.longitude))}>
            {loading ? "Creating..." : "Create Account"}
          </button>
        </form>
        <p>
          Already have an account? <Link to="/login">Login</Link>
        </p>
      </div>
    </section>
  );
}
