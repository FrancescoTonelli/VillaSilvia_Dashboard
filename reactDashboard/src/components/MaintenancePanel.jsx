import '../App.css';
import { useState } from 'react';
import { ButtonOff } from './Buttons';

export default function MaintenancePanel({ text, onConfirm }) {
    const [confirmShutdown, setConfirmShutdown] = useState(false);

    const handleShutdownClick = () => {
        setConfirmShutdown(true);
    };

    const handleCancel = () => {
        setConfirmShutdown(false);
    };

    const handleConfirm = () => {
        setConfirmShutdown(false);
        onConfirm();
    };

    return (
      <div className='control-panel'>
          <h2>Manutenzione</h2>
          <p>
            Da utilizzare solo nel caso sia necessario<br/>
            staccare la corrente
          </p>
          <div className="spacer" />

          {!confirmShutdown && (<ButtonOff onClick={handleShutdownClick} />)}


          {confirmShutdown && (
            <div className="confirmation-box">
              <p>{text}</p>
              <div className="controls-row">
                <button className="button" onClick={handleCancel}>
                  Annulla
                </button>
                <button className="button button-off" onClick={handleConfirm}>
                  Conferma
                </button>
              </div>
            </div>
          )}
      </div>
    );
}
