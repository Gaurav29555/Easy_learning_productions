import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { registerUser, registerWorker, sendOtp, verifyOtp } from "../api/fixcartApi";
import LocationPicker from "../components/LocationPicker";
import { useAuth } from "../context/AuthContext";

export default function RegisterPage() {
  const { login: saveAuth } = useAuth();
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
    const normalizedPhone = form.phone.replace(/\D/g, "");
    if (!normalizedPhone) {
      setError("Enter phone number first");
      return;
    }
    if (!/^[0-9]{10,15}$/.test(normalizedPhone)) {
      setError("Phone number must contain 10 to 15 digits.");
      return;
    }
    try {
      const data = await sendOtp({ phone: normalizedPhone, purpose: "REGISTER" });
      setForm((current) => ({ ...current, phone: normalizedPhone }));
      setInfo(`OTP sent. ${data.debugOtp ? `Dev OTP: ${data.debugOtp}` : "Check SMS inbox."}`);
    } catch (err) {
      setError(err.message);
    }
  };

  const onVerifyOtp = async () => {
    setError("");
    setInfo("");
    const normalizedPhone = form.phone.replace(/\D/g, "");
    const normalizedOtp = otpCode.replace(/\D/g, "");
    if (!/^[0-9]{10,15}$/.test(normalizedPhone)) {
      setError("Enter the same valid phone number used for OTP send.");
      return;
    }
    if (!/^[0-9]{6}$/.test(normalizedOtp)) {
      setError("OTP must be exactly 6 digits.");
      return;
    }
    try {
      await verifyOtp({ phone: normalizedPhone, purpose: "REGISTER", otpCode: normalizedOtp });
      setForm((current) => ({ ...current, phone: normalizedPhone }));
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
        <h1>Create fixcart account</h1>
        <p>Register as customer or worker and start booking instantly.</p>
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
            onChange={(e) => setForm({ ...form, email: e.target.value })}
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
            placeholder="Phone (10-15 digits)"
            value={form.phone}
            onChange={(e) => {
              setForm({ ...form, phone: e.target.value.replace(/\D/g, "") });
              setOtpVerified(false);
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
