const BACKEND = import.meta.env.VITE_BACKEND_URL;
let socket;
let logListeners = [];

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

export function connectSocket(onDeviceMessage) {
  if (!socket || socket.readyState === WebSocket.CLOSED) {
    socket = new WebSocket(BACKEND.replace(/^http/, 'ws') + '/ws');
  }

  socket.onopen = () => {
    console.log('[WS] connected');
  };

  socket.onmessage = e => {
    try {
      const msg = JSON.parse(e.data);
      if (msg.type === 'device') {
        if (onDeviceMessage) onDeviceMessage(msg.payload);
      }
      else if (msg.type === 'log') {
        logListeners.forEach(cb => {
          try { cb(msg.payload); } catch (err) { console.error(err); }
        });
      }
      else if (msg.type === 'logs_initial') {
        const arr = msg.payload || [];
        logListeners.forEach(cb => {
          try { cb(arr); } catch (err) { console.error(err); }
        });
      } else {
        onsole.log('[WS] unknown message', msg);
      }
    } catch (err) {
      console.error('[WS] invalid message', err, e.data);
    }
  };

  socket.onerror = err => console.error('[WS] error', err);
  socket.onclose = ev => console.warn('[WS] closed', ev);

  return () => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.close();
    }
  };
}

export function subscribeToLogs(cb) {
  logListeners.push(cb);
  return () => {
    const idx = logListeners.indexOf(cb);
    if (idx !== -1) logListeners.splice(idx, 1);
  };
}

export function requestLogs(limit = 100) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({ action: 'request_logs', limit }));
  } else {
    console.warn('[WS] socket not open yet, cannot request logs');
  }
}



