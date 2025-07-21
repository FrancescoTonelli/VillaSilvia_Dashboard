import { sendGlobalCommand } from '../api/Api';
import { useState } from 'react';

export default function BonciModePage() {
  const [res, setRes] = useState('');
  return (
    <div className="p-4">
      <button
        onClick={() => sendGlobalCommand('start_presentation').then(r => setRes(r))}
      >
        Avvia presentazione
      </button>
      {res && <p>{res}</p>}
    </div>
  );
}
