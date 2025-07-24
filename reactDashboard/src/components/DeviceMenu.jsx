import { useState } from 'react';
import {
  ButtonOn,
  ButtonOff,
  ButtonLightDown,
  ButtonLightUp
} from '../components/Buttons';
import { sendGlobalCommand, sendGeneralLightCommand } from '../api/Api';

export default function DeviceMenu({ devices, onSelect }) {
  const [confirmShutdown, setConfirmShutdown] = useState(false);

  const handleShutdownClick = () => {
    setConfirmShutdown(true);
  };

  const handleCancel = () => {
    setConfirmShutdown(false);
  };

  const handleConfirm = () => {
    sendGlobalCommand('shutdown');
    setConfirmShutdown(false);
  };

  return (
    <div className="device-menu">
      <div className='control-panel'>
        <h2>Comandi generali</h2>
        <div className="controls-row">
          <ButtonOn onClick={() => sendGlobalCommand('wake')} />
          <ButtonOff onClick={() => sendGlobalCommand('sleep')} />
        </div>
      </div>

      <div className='control-panel'>
        <h2>Luci</h2>
        <div className="controls-row">
          <ButtonOn onClick={() => sendGeneralLightCommand('ON')} />
          <ButtonOff onClick={() => sendGeneralLightCommand('OFF')} />
          <ButtonLightUp onClick={() => sendGeneralLightCommand('LIGHT_UP')} />
        </div>
        <ButtonLightDown onClick={() => sendGeneralLightCommand('LIGHT_DOWN')} />
      </div>


      <div className="foot-spacer" />
      
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
            <p>Sei sicuro di voler spegnere tutti i dispositivi?</p>
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

      <div className="foot-spacer" />
    </div>
  );
}
