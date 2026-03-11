import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import logo from "../assets/fixcart-logo.svg";
import NotificationCenter from "./NotificationCenter";

export default function Navbar() {
  const { auth, logout, isAuthenticated } = useAuth();
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
