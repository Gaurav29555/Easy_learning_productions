import { createContext, useContext, useMemo, useState } from "react";

const AuthContext = createContext(null);
const FIXCART_AUTH_KEY = "fixcart_auth";

function readInitialAuthState() {
  const raw = localStorage.getItem(FIXCART_AUTH_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (error) {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(readInitialAuthState());

  const value = useMemo(
    () => ({
      auth,
      isAuthenticated: Boolean(auth?.token),
      login: (payload) => {
        localStorage.setItem(FIXCART_AUTH_KEY, JSON.stringify(payload));
        setAuth(payload);
      },
      logout: () => {
        localStorage.removeItem(FIXCART_AUTH_KEY);
        setAuth(null);
      }
    }),
    [auth]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }
  return context;
}
