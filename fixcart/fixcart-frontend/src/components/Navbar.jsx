import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useFixcartSettings } from "../context/FixcartSettingsContext";
import logo from "../assets/fixcart-logo.svg";
import LanguageRegionSelector from "./LanguageRegionSelector";
import NotificationCenter from "./NotificationCenter";

export default function Navbar() {
  const { auth, logout, isAuthenticated } = useAuth();
  const { settings } = useFixcartSettings();
  const navigate = useNavigate();

  const onLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <header className="fixcart-navbar">
      <Link className="brand" to={isAuthenticated ? "/" : "/login"}>
        <img src={logo} alt="fixcart logo" className="brand-logo" />
      </Link>
      <div className="nav-actions">
        <div className="navbar-meta">
          <LanguageRegionSelector />
          <span className="market-pill">{settings.region}</span>
        </div>
        {isAuthenticated ? (
          <>
            <NotificationCenter />
            <span className="role-pill">{auth.role}</span>
            <button className="ghost-btn" onClick={onLogout}>
              Logout
            </button>
          </>
        ) : (
          <>
            <Link className="ghost-link" to="/login">
              Login
            </Link>
            <Link className="primary-link" to="/register">
              Register
            </Link>
          </>
        )}
      </div>
    </header>
  );
}
