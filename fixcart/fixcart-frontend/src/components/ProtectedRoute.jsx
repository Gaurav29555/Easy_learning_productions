import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ children, allowedRoles }) {
  const { isAuthenticated, auth } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(auth.role)) {
    if (auth.role === "WORKER") return <Navigate to="/worker" replace />;
    if (auth.role === "CUSTOMER") return <Navigate to="/customer" replace />;
    return <Navigate to="/login" replace />;
  }

  return children;
}
