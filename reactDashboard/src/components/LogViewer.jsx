import { useEffect, useState } from 'react';
import { subscribeToLogs, requestLogs } from '../api/Api';
import { CloseIcon } from '../assets/Icons';

export default function LogViewer({ onClose }) {
  const [logs, setLogs] = useState([]); 

  useEffect(() => {
    const handler = (payload) => {
      if (Array.isArray(payload)) {
        const arr = payload.map(item => item).slice(0, 100);
        setLogs(arr);
      } else if (payload && payload.type) {
        setLogs(prev => {
          const updated = [payload, ...prev];
          if (updated.length > 100) updated.splice(100);
          return updated;
        });
      }
    };

    const unsubscribe = subscribeToLogs(handler);

    requestLogs(100);

    return () => unsubscribe();
  }, []);

  return (
    <div className="log-viewer">
      <div className='log-header'>
        <h2>Log</h2>
        <button onClick={onClose} className="button-back">
          <CloseIcon />
        </button>
      </div>

      <div className='log-list'>
        {logs.length === 0 && <div>Nessun log</div>}
        <ul className='log-ul'>
          {logs.map((log, idx) => {
            const ts = typeof log.timestamp === 'number' ? new Date(log.timestamp) : new Date();
            const time = ts.toLocaleString();
            return (
              <li key={idx} className='log-li'>
                <div className='log-row'>
                  <div style={{color:'#666'}}>[{time}] <strong>{log.device}</strong> </div>
                  <div className='log-message'><em>{log.type}</em> - {log.message}</div>
                </div>
              </li>
            );
          })}
        </ul>
        <div className="foot-spacer" />
      </div>
    </div>
  );
}
