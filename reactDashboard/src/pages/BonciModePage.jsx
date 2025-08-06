import { sendGlobalCommand } from '../api/Api';
import { ReloadIcon } from '../assets/Icons';

export default function BonciModePage() {
  return (
    <div className='bonci-mode-page controls-row'>
      <div className="button-description">
        <button
          onClick={() => {sendGlobalCommand('start_presentation');}}

          className="button-start"
        >
          <ReloadIcon />
        </button>
          <h2>Ricarica presentazione</h2>
      </div>
    </div>
  );
}
