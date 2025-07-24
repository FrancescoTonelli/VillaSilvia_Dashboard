import { sendGlobalCommand } from '../api/Api';
import { useState } from 'react';
import { PlayIcon } from '../assets/Icons';

export default function BonciModePage() {
  const [res, setRes] = useState('');
  return (
    <div className="button-description bonci-mode-page">
      <button
        onClick={() => sendGlobalCommand('start_presentation').then(r => setRes(r))}
        className="button-start"
      >
        <PlayIcon />
      </button>
        <h2>Avvia presentazione</h2>
    </div>
  );
}
