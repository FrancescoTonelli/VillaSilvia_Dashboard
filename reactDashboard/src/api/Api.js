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

export async function sendCommandToDevice(deviceId, action) {
  const res = await fetch(`${BACKEND}/devices/${deviceId}/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action })
  });
  if (!res.ok) throw new Error(`Errore ${res.status}`);
  return res.text();
}

export async function sendGlobalCommand(command) {
  const res = await fetch(`${BACKEND}/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command })
  });
  if (!res.ok) throw new Error(`Errore ${res.status}`);
  return res.text();
}

export async function sendGeneralLightCommand(command) {
  const res = await fetch(`${BACKEND}/light/general/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command })
  });
  if (!res.ok) throw new Error(`Errore ${res.status}`);
  return res.text();
}

export async function sendAudioGeneralCommand(command) {
  const res = await fetch(`${BACKEND}/audio/general/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command })
  });
  if (!res.ok) throw new Error(`Errore ${res.status}`);
  return res.text();
}

export async function sendVideoGeneralCommand(command) {
  const res = await fetch(`${BACKEND}/video/general/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command })
  });
  if (!res.ok) throw new Error(`Errore ${res.status}`);
  return res.text();
}

export async function sendShellyCommand(shellyId, command /* "ON"|"OFF" */) {
  const res = await fetch(`${BACKEND}/shelly/${encodeURIComponent(shellyId)}/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command })
  });
  if (!res.ok) throw new Error(`Errore ${res.status}`);
  return res.json();
}

export async function sendShellyCommandToAll(command) {
  const res = await fetch(`${BACKEND}/shelly/command`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command })
  });
  if (!res.ok) throw new Error(`Errore ${res.status}`);
  return res.json();
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


