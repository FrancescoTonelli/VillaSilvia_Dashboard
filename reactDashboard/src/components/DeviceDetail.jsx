import { useEffect, useState } from 'react';
import { fetchDevice, sendCommandToDevice } from '../api/Api';
import SliderControl from './SliderControl';

export default function DeviceDetail({ deviceId }) {
  const [device, setDevice] = useState(null);
  const [volume, setVolume] = useState(50);
  const [intensity, setIntensity] = useState(50);
  const [status, setStatus] = useState('');
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

  const commonOnOff = (action) =>
    sendCommandToDevice(deviceId, action).then(res => setStatus(res));

  return (
    <div className="p-4">
      <h1>{deviceId} — {type}</h1>
      {type === 'audioPlayer' && (
        <>
          <button onClick={() => commonOnOff('play')}>Play</button>
          <button onClick={() => commonOnOff('stop')}>Stop</button>
          <SliderControl
            label="Volume"
            value={volume}
            onChange={v => {
              setVolume(v);
              sendCommandToDevice(deviceId, 'volume', v);
            }}
          />
        </>
      )}
      {type === 'videoPlayer' && (
        <>
          <button onClick={() => commonOnOff('start')}>Start</button>
          <button onClick={() => commonOnOff('stop')}>Stop</button>
          <p>Stato: {device.state || '–'}</p>
        </>
      )}
      {type === 'plafoniera' && (
        <>
          <button onClick={() => commonOnOff('on')}>Accendi</button>
          <button onClick={() => commonOnOff('off')}>Spegni</button>
          <SliderControl
            label="Intensità"
            value={intensity}
            onChange={v => {
              setIntensity(v);
              sendCommandToDevice(deviceId, 'intensity', v);
            }}
          />
        </>
      )}
      <p className="mt-2">{status}</p>
    </div>
  );
}
