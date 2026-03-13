import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import { AuthProvider } from "./context/AuthContext";
import { FixcartSettingsProvider } from "./context/FixcartSettingsContext";
import "./styles.css";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <BrowserRouter>
      <FixcartSettingsProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </FixcartSettingsProvider>
    </BrowserRouter>
  </React.StrictMode>
);
