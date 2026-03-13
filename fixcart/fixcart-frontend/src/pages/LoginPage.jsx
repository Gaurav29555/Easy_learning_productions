import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login, loginWithOtp, sendOtp } from "../api/fixcartApi";
import { useAuth } from "../context/AuthContext";
import { useFixcartSettings } from "../context/FixcartSettingsContext";

export default function LoginPage() {
  const { login: saveAuth } = useAuth();
  const { copy } = useFixcartSettings();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [otpForm, setOtpForm] = useState({ email: "", otpCode: "" });

  const onSubmit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setInfo("");
    try {
      const data = await login(form);
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

  const onSendLoginOtp = async () => {
    setError("");
    setInfo("");
    const normalizedEmail = otpForm.email.trim().toLowerCase();
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setError("Enter a valid email first.");
      return;
    }
    try {
      const data = await sendOtp({ email: normalizedEmail, purpose: "LOGIN" });
      setOtpForm((current) => ({ ...current, email: normalizedEmail }));
      setInfo(`Login OTP sent to email. ${data.debugOtp ? `Dev OTP: ${data.debugOtp}` : "Check your inbox."}`);
    } catch (err) {
      setError(err.message);
    }
  };

  const onOtpLogin = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    setInfo("");
    try {
      const data = await loginWithOtp(otpForm);
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

  return (
    <section className="auth-layout">
      <div className="auth-panel">
        <h1>{copy.loginTitle}</h1>
        <p>{copy.loginSubtitle}</p>
        {error && <div className="error-box">{error}</div>}
        {info && <div className="info-box">{info}</div>}
        <form onSubmit={onSubmit} className="form-grid">
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
          <button type="submit" disabled={loading}>
            {loading ? "Logging in..." : "Login"}
          </button>
        </form>
        <div className="divider">OR</div>
        <form onSubmit={onOtpLogin} className="form-grid">
          <input
            type="email"
            placeholder="Email"
            value={otpForm.email}
            onChange={(e) => setOtpForm({ ...otpForm, email: e.target.value })}
            required
          />
          <div className="action-row">
            <button type="button" onClick={onSendLoginOtp}>
              Send Login OTP
            </button>
            <input
              type="text"
              placeholder="OTP Code"
              value={otpForm.otpCode}
              onChange={(e) => setOtpForm({ ...otpForm, otpCode: e.target.value })}
              required
            />
            <button type="submit" disabled={loading}>
              {loading ? "Please wait..." : "Login with OTP"}
            </button>
          </div>
        </form>
        <p>
          New user? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </section>
  );
}
