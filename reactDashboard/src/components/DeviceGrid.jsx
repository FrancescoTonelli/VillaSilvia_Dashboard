import DeviceCard from './DeviceCard'

export default function DeviceGrid({ devices, onSelect }) {
  return (
    <div className="device-grid">
      {devices.map(device => (
        <DeviceCard
          key={device.id}
          device={device}
          onClick={() => onSelect(device.id)}
        />
      ))}
    </div>
  );
}