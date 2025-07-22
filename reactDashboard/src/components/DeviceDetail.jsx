import { useEffect, useState } from 'react';
import { fetchDevice, 
  sendAudioGeneralCommand, 
  sendCommandToDevice, 
  sendGeneralLightCommand,
  sendVideoGeneralCommand } from '../api/Api';
import { CloseIcon, OnOffIcon, BulbIcon } from '../assets/Icons';

export default function DeviceDetail({ deviceId, onClose }) {
  const [device, setDevice] = useState(null);
  const [volume, setVolume] = useState(50);
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
      }
      if (type === 'audioPlayer') setVolume(d.volume || 50);
      if (type === 'plafoniera') setIntensity(d.intensity || 50);
    });
  }, [deviceId]);

  if (!device) return <p>Caricamento…</p>;

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
          <h2>Controllo generale</h2>
          <div className="controls-row">
            <div className="button-description">
              <button onClick={() => sendAudioGeneralCommand('ON')} className='button-command button-on'>
                <OnOffIcon fill="#ffffff" />
              </button>
              <p>ON</p>
            </div>
            <div className="button-description">
              <button onClick={() => sendAudioGeneralCommand('OFF')} className="button-command button-off">
                <OnOffIcon fill="#ffffff" />
              </button>
              <p>OFF</p>
            </div>
          </div>
        </>
      )}
      {type === 'videoPlayer' && (
        <>
          <h2>Controllo generale</h2>
          <div className="controls-row">
            <div className="button-description">
              <button onClick={() => sendVideoGeneralCommand('WAKE')} className='button-command button-on'>
                <OnOffIcon fill="#ffffff" />
              </button>
              <p>ON</p>
            </div>
            <div className="button-description">
              <button onClick={() => sendVideoGeneralCommand('SLEEP')} className="button-command button-off">
                <OnOffIcon fill="#ffffff" />
              </button>
              <p>OFF</p>
            </div>
          </div>
        </>
      )}
      {type === 'plafoniera' && (
        <>
          <div className="controls-row">
            <div className="button-description">
              <button onClick={() => sendCommandToDevice(deviceId, 'ON')} className='button-command button-on'>
                <OnOffIcon fill="#ffffff" />
              </button>
              <p>ON</p>
            </div>
            <div className="button-description">
              <button onClick={() => sendCommandToDevice(deviceId, 'OFF')} className="button-command button-off">
                <OnOffIcon fill="#ffffff" />
              </button>
              <p>OFF</p>
            </div>
            <div className="button-description">
              <button onClick={() => sendCommandToDevice(deviceId, 'LIGHT_DOWN')} className='button-command button-50'>
                <BulbIcon />
              </button>
              <p>ABBASSA</p>
            </div>
            <div className="button-description">
              <button onClick={() => sendCommandToDevice(deviceId, 'LIGHT_UP')} className='button-command button-100'>
                <BulbIcon />
              </button>
              <p>ALZA</p>
            </div>
          </div>
          <h2>Controllo generale</h2>
          <div className="controls-row">
            <div className="button-description">
              <button onClick={() => sendGeneralLightCommand('ON')} className='button-command button-on'>
                <OnOffIcon fill="#ffffff" />
              </button>
              <p>ON</p>
            </div>
            <div className="button-description">
              <button onClick={() => sendGeneralLightCommand('OFF')} className="button-command button-off">
                <OnOffIcon fill="#ffffff" />
              </button>
              <p>OFF</p>
            </div>
            <div className="button-description">
              <button onClick={() => sendGeneralLightCommand('LIGHT_DOWN')} className='button-command button-50'>
                <BulbIcon />
              </button>
              <p>50%</p>
            </div>
            <div className="button-description">
              <button onClick={() => sendGeneralLightCommand('LIGHT_UP')} className='button-command button-100'>
                <BulbIcon />
              </button>
              <p>100%</p>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
