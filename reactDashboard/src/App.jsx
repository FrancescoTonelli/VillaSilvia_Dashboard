import { useEffect, useState } from 'react';

function App() {
  const [devices, setDevices] = useState({});
  const [loading, setLoading] = useState(true);
  const [commandResult, setCommandResult] = useState(null);
  const backendUrl = import.meta.env.VITE_BACKEND_URL;

  // Caricamento iniziale
  useEffect(() => {
    fetch(`${backendUrl}/devices`)
      .then((res) => res.json())
      .then((data) => {
        setDevices(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Errore nel caricamento dispositivi:', err);
        setLoading(false);
      });
  }, [backendUrl]);

  // WebSocket per aggiornamenti
  useEffect(() => {
    const wsUrl = backendUrl.replace(/^http/, 'ws') + '/ws';
    const socket = new WebSocket(wsUrl);

    socket.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      const { deviceId, ...rest } = msg;

      setDevices((prev) => ({
        ...prev,
        [deviceId]: {
          // conserva eventuali campi esistenti
          ...prev[deviceId],
          ...rest
        }
      }));
    };

    socket.onerror = (err) => {
      console.error("Errore WebSocket:", err);
    };

    socket.onclose = () => {
      console.warn("WebSocket chiuso");
    };

    return () => {
      socket.close();
    };
  }, [backendUrl]);

  // Invio comandi
  const sendCommand = (cmd) => {
    fetch(`${backendUrl}/command`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ command: cmd })
    })
      .then((res) => res.text())
      .then((msg) => setCommandResult(`Risposta: ${msg}`))
      .catch((err) => {
        console.error('Errore invio comando:', err);
        setCommandResult('Errore!');
      });
  };

  return (
    <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1> Dashboard</h1>

      <div style={{ marginBottom: '1rem' }}>
        <button onClick={() => sendCommand('shutdown')}>Shutdown</button>{' '}
        <button onClick={() => sendCommand('sleep')}>Sleep</button>{' '}
        <button onClick={() => sendCommand('wake')}>Wake</button>
      </div>

      {commandResult && <p>{commandResult}</p>}

      {loading ? (
        <p>Caricamento dispositivi...</p>
      ) : (
        <table border="1" cellPadding="8">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nome</th>
              <th>Stato</th>
            </tr>
          </thead>
          <tbody>
            {Object.entries(devices).map(([deviceId, device]) => (
              <tr key={deviceId}>
                <td>{deviceId}</td>
                <td>{device?.name || '-'}</td>
                <td>{device?.online ? 'online' : 'offline'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default App;
