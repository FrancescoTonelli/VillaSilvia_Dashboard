export default function SliderControl({ label, value, onChange, min=0, max=100 }) {
  return (
    <div className="flex flex-col">
      <label>{label}: {value}</label>
      <input
        type="range"
        min={min}
        max={max}
        value={value}
        onChange={e => onChange(Number(e.target.value))}
      />
    </div>
  );
}
