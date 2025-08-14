import DeviceCard from './DeviceCard'

export default function DeviceGrid({ devices, onSelect, onLog }) {
  if (!devices || devices.length === 0) {
    return <div className="device-grid-empty">
      Nessun device disponibile.
    </div>;
  }
  return (
    <div className="device-grid">
      {devices.map(device => (
        <DeviceCard
          key={device.id}
          device={device}
          onClick={() => { if (device.online) onSelect(device.id); }}
        />
      ))}
      <div className="foot-spacer" />
      <button onClick={onLog} className='log-button'>Mostra Log</button>
      <div className="foot-spacer" />
    </div>
  );
}