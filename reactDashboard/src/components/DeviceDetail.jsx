import { useEffect, useState } from 'react';
import { fetchDevice, 
  sendAudioGeneralCommand, 
  sendCommandToDevice,
  sendVideoGeneralCommand,
  sendShellyCommand,
  sendShellyCommandToAll
} from '../api/Api';
import { CloseIcon } from '../assets/Icons';
import {
  ButtonOn,
  ButtonOff,
  ButtonLightDown,
  ButtonLightUp
} from '../components/Buttons';

export default function DeviceDetail({ deviceId, onClose }) {
  const [device, setDevice] = useState(null);
  const [type, setType] = useState('');

  useEffect(() => {
    fetchDevice(deviceId).then(d => {
      setDevice(d);
      if (deviceId.includes('audioPlayer')) {
        setType('audioPlayer');
      } else if (deviceId.includes('videoPlayer')) {
        setType('videoPlayer');
      } else if (deviceId.includes('plafoniera')) {
        setType('plafoniera');
      } else if (deviceId.includes('shelly')) {
        setType('shelly');
      }
    });
  }, [deviceId]);

  if (!device) {
    return <div className="device-detail">Caricamento...</div>;
  }

  return (
    <div className="device-detail">
      <div className='device-detail-header'>
        <h2>{deviceId}</h2>
        <button onClick={onClose} className="button-back">
          <CloseIcon />
        </button>
      </div>
      {type === 'audioPlayer' && (
        <>
          <h2>Controllo audio</h2>
          <div className="controls-row">
            <ButtonOn onClick={() => sendAudioGeneralCommand('ON')} />
            <ButtonOff onClick={() => sendAudioGeneralCommand('OFF')} />
          </div>
        </>
      )}
      {type === 'videoPlayer' && (
        <>
          <h2>Controllo generale video</h2>
          <div className="controls-row">
            <ButtonOn onClick={() => sendVideoGeneralCommand('WAKE')} />
            <ButtonOff onClick={() => sendVideoGeneralCommand('SLEEP')} />
          </div>
        </>
      )}
      {type === 'plafoniera' && (
        <>
          <div className="controls-row">
            <ButtonOn onClick={() => sendCommandToDevice(deviceId, 'ON')} />
            <ButtonOff onClick={() => sendCommandToDevice(deviceId, 'OFF')} />
            <ButtonLightUp onClick={() => sendCommandToDevice(deviceId, 'LIGHT_UP')}/>
            <ButtonLightDown onClick={() => sendCommandToDevice(deviceId, 'LIGHT_DOWN')}/>
          </div>
        </>
      )}
      {type === 'shelly' && (
        <>
          <div className="controls-row">
            <ButtonOn onClick={() => sendShellyCommand(deviceId, "ON")} />
            <ButtonOff onClick={() => sendShellyCommand(deviceId, "OFF")} />
          </div>
          <h2>Controllo generale shelly</h2>
          <div className="controls-row">
            <ButtonOn onClick={() => sendShellyCommandToAll("ON")} />
            <ButtonOff onClick={() => sendShellyCommandToAll("OFF")} />
          </div>
        </>
      )}
    </div>
  );
}
