import { sendGlobalCommand, sendVideoGeneralCommand } from '../api/Api';
import { useState } from 'react';
import { PlayIcon, ReloadIcon } from '../assets/Icons';

export default function BonciModePage() {
  return (
    <div className='bonci-mode-page controls-row'>
      <div className="button-description">
        <button
          onClick={async () => {
            await sendGlobalCommand('start_presentation');
            await sendVideoGeneralCommand('SLEEP');
            await new Promise(resolve => setTimeout(resolve, 30000));
            await sendVideoGeneralCommand('WAKE');
          }}

          className="button-start"
        >
          <ReloadIcon />
        </button>
          <h2>Ricarica presentazione</h2>
      </div>
    </div>
  );
}
