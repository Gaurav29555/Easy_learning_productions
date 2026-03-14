import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login, loginWithOtp, sendOtp } from "../api/fixcartApi";
import { useAuth } from "../context/AuthContext";
import { useFixcartSettings } from "../context/FixcartSettingsContext";

function createCaptcha() {
  const left = Math.floor(Math.random() * 9) + 1;
  const right = Math.floor(Math.random() * 9) + 1;
  return { left, right, answer: left + right };
}

export default function LoginPage() {
  const { login: saveAuth } = useAuth();
  const { copy } = useFixcartSettings();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [info, setInfo] = useState("");
  const [otpForm, setOtpForm] = useState({ email: "", otpCode: "" });
  const [resendCooldown, setResendCooldown] = useState(0);
  const [captcha, setCaptcha] = useState(() => createCaptcha());
  const [captchaAnswer, setCaptchaAnswer] = useState("");

  useEffect(() => {
    if (resendCooldown <= 0) return undefined;
    const timer = window.setTimeout(() => setResendCooldown((current) => current - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [resendCooldown]);

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
    if (resendCooldown > 0) {
      setError(`Wait ${resendCooldown}s before requesting another OTP.`);
      return;
    }
    if (Number(captchaAnswer) !== captcha.answer) {
      setError("Solve the captcha correctly before sending OTP.");
      return;
    }
    try {
      const data = await sendOtp({ email: normalizedEmail, purpose: "LOGIN" });
      setOtpForm((current) => ({ ...current, email: normalizedEmail }));
      setResendCooldown(30);
      setCaptcha(createCaptcha());
      setCaptchaAnswer("");
      setInfo(data.debugOtp ? `Your login OTP is ${data.debugOtp}. Enter it below to continue.` : "OTP generated. Enter it below to continue.");
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
          <div className="captcha-row">
            <div className="captcha-box">Captcha: {captcha.left} + {captcha.right} = ?</div>
            <input
              type="text"
              placeholder="Captcha answer"
              value={captchaAnswer}
              onChange={(e) => setCaptchaAnswer(e.target.value.replace(/\D/g, ""))}
              required
            />
          </div>
          <div className="action-row">
            <button type="button" onClick={onSendLoginOtp} disabled={resendCooldown > 0}>
              {resendCooldown > 0 ? `Resend OTP in ${resendCooldown}s` : "Send Login OTP"}
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
