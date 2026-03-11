import { useEffect, useMemo, useState } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client/dist/sockjs";
import {
  getMyNotifications,
  getUnreadNotificationCount,
  markNotificationRead
} from "../api/fixcartApi";
import { useAuth } from "../context/AuthContext";

const FIXCART_API_BASE_URL =
  import.meta.env.VITE_FIXCART_API_URL || "http://localhost:8080";

export default function NotificationCenter() {
  const { auth, isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState([]);
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (!isAuthenticated) return undefined;
    getMyNotifications(auth.token).then(setNotifications).catch(() => {});
    getUnreadNotificationCount(auth.token).then((data) => setUnreadCount(data.unreadCount ?? 0)).catch(() => {});

    const client = new Client({
      webSocketFactory: () => new SockJS(`${FIXCART_API_BASE_URL}/ws-fixcart`),
      reconnectDelay: 3000
    });

    client.onConnect = () => {
      client.subscribe(`/topic/user/${auth.userId}/notifications`, (message) => {
        const payload = JSON.parse(message.body);
        setNotifications((prev) => [payload, ...prev].slice(0, 50));
        setUnreadCount((prev) => prev + 1);
      });
    };

    client.activate();
    return () => client.deactivate();
  }, [auth.token, auth.userId, isAuthenticated]);

  const recentNotifications = useMemo(() => notifications.slice(0, 8), [notifications]);

  const onMarkRead = async (notificationId) => {
    try {
      const updated = await markNotificationRead(notificationId, auth.token);
      setNotifications((prev) =>
        prev.map((item) => (item.id === updated.id ? updated : item))
      );
      setUnreadCount((prev) => Math.max(0, prev - 1));
    } catch {
      // Keep UI responsive if mark-read fails.
    }
  };

  if (!isAuthenticated) return null;

  return (
    <div className="notification-center">
      <button type="button" className="notification-trigger ghost-btn" onClick={() => setOpen((value) => !value)}>
        Alerts {unreadCount > 0 && <span className="notification-badge">{unreadCount}</span>}
      </button>
      {open && (
        <div className="notification-panel">
          <h3>Live Notifications</h3>
          <div className="list">
            {recentNotifications.map((item) => (
              <article key={item.id} className={`list-item notification-item ${item.read ? "notification-read" : ""}`}>
                <strong>{item.title}</strong>
                <p>{item.message}</p>
                <div className="action-row">
                  <small>{new Date(item.createdAt).toLocaleString()}</small>
                  {!item.read && (
                    <button type="button" className="ghost-btn" onClick={() => onMarkRead(item.id)}>
                      Mark read
                    </button>
                  )}
                </div>
              </article>
            ))}
            {recentNotifications.length === 0 && <p className="muted">No notifications yet.</p>}
          </div>
        </div>
      )}
    </div>
  );
}
