import { useEffect, useState } from 'react';
import { fetchDevices, connectSocket } from '../api/Api';
import DeviceGrid from '../components/DeviceGrid';
import DeviceDetail from '../components/DeviceDetail';
import DeviceMenu from '../components/DeviceMenu';

export default function DevicesPage() {
  const [devices, setDevices] = useState([]);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    fetchDevices().then(data =>
      setDevices(Object.entries(data).map(([id, d]) => ({ id, ...d })))
    );

    const cleanup = connectSocket(update => {
      setDevices(prev => {
        const { deviceId, ...rest } = update;
        const idx = prev.findIndex(d => d.id === deviceId);

        if (idx > -1) {
          const copy = [...prev];
          copy[idx] = { ...copy[idx], ...rest };
          return copy;
        } else {
          return [...prev, { id: deviceId, ...rest }];
        }
      });
    });

    return cleanup;
  }, []);


  return (
  <div className='device-page'>
    <DeviceMenu />
    {selected && (
      <DeviceDetail deviceId={selected} onClose={() => setSelected(null)} />
    )}
    {!selected && (
      <DeviceGrid devices={devices} onSelect={setSelected} />
    )}
  </div>
);
}
