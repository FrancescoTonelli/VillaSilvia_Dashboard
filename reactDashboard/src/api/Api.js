const BACKEND = import.meta.env.VITE_BACKEND_URL;
let socket;

export async function fetchDevices() {
  const res = await fetch(`${BACKEND}/devices`);
  return res.json();
}

export async function fetchDevice(id) {
  const res = await fetch(`${BACKEND}/devices/${id}`);
  if (!res.ok) throw new Error('Dispositivo non trovato');
  return res.json();
}

export function sendCommandToDevice(deviceId, action, value = null) {
  return fetch(`${BACKEND}/devices/${deviceId}/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action, value })
  }).then(r => r.text());
}

export function sendGlobalCommand(command) {
  return fetch(`${BACKEND}/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command })
  }).then(r => r.text());
}

export function connectSocket(onMessage) {
  socket = new WebSocket(BACKEND.replace(/^http/, 'ws') + '/ws');

  socket.onmessage = e => {
    const update = JSON.parse(e.data);
    onMessage(update);
  };
  socket.onerror = err => console.error('[WS] error', err);
  socket.onclose = ev => console.warn('[WS] closed', ev);

  return () => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.close();
    }
  };
}

