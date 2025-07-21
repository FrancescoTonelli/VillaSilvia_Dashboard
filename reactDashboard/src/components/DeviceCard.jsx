import '../App.css';

export default function DeviceCard({ device, onClick }) {
  const online = device.online;
  
  let typeClass = '';
  if (device.id.includes('audioPlayer')) {
    typeClass = 'device-audioPlayer';
  } else if (device.id.includes('videoPlayer')) {
    typeClass = 'device-videoPlayer';
  } else if (device.id.includes('plafoniera')) {
    typeClass = 'device-plafoniera';
  }

  return (
    <div
      onClick={onClick}
      className={`device-card ${typeClass}`}
    >
      <div className="device-card-inner">
        <a>{device.id}</a>
        <span
          className={`status-circle ${online ? 'circle-green' : 'circle-red'}`}
        ></span>
      </div>
    </div>
  );
}
