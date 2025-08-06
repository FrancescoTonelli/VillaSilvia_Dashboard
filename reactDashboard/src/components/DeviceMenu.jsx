import {
  ButtonOn,
  ButtonOff,
  ButtonLightDown,
  ButtonLightUp,
  ButtonCold,
  ButtonHot
} from '../components/Buttons';
import { sendGlobalCommand, sendGeneralLightCommand, sendVideoGeneralCommand } from '../api/Api';
import MaintenancePanel from './MaintenancePanel';

export default function DeviceMenu({ devices, onSelect }) {

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
        <div className="controls-row">
          <ButtonLightDown onClick={() => sendGeneralLightCommand('LIGHT_DOWN')} />
          <ButtonCold onClick={() => sendGeneralLightCommand('COLD_UP')} />
          <ButtonHot onClick={() => sendGeneralLightCommand('WARM_UP')} />
        </div>
      </div>

      <div className='control-panel'>
          <h2>Video</h2>
          <div className="controls-row">
            <ButtonOn onClick={() => sendVideoGeneralCommand('WAKE')} />
            <ButtonOff onClick={() => sendVideoGeneralCommand('SLEEP')} />
          </div>
      </div>

      <div className="foot-spacer" />

      <MaintenancePanel 
        text="Sei sicuro di voler spegnere tutti i dispositivi?" 
        onConfirm={() => sendGlobalCommand("shutdown")}
      />

      <div className="foot-spacer" />
    </div>
  );
}