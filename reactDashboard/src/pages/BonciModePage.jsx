import { sendGlobalCommand, sendVideoGeneralCommand } from '../api/Api';
import { useState } from 'react';
import { PlayIcon, ReloadIcon } from '../assets/Icons';

export default function BonciModePage() {
  const [res, setRes] = useState('');
  return (
    <div className='bonci-mode-page controls-row'>
      <div className="button-description">
        <button
          onClick={() => sendGlobalCommand('start_presentation').then(r => setRes(r))}
          className="button-start"
        >
          <PlayIcon />
        </button>
          <h2>Avvia presentazione</h2>
      </div>
      <div className="button-description">
        <button
          onClick={async () => {
            await sendVideoGeneralCommand('SLEEP');
            await new Promise(resolve => setTimeout(resolve, 20000));
            await sendVideoGeneralCommand('WAKE');
          }}

          className="button-start"
        >
          <ReloadIcon />
        </button>
          <h2>Ricarica schermi</h2>
      </div>
    </div>
  );
}
