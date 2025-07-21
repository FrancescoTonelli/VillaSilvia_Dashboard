import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import DevicesPage from './pages/DevicesPage';
import BonciModePage from './pages/BonciModePage';


export default function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/" element={<DevicesPage />} />
        <Route path="/bonci" element={<BonciModePage />} />
      </Routes>
    </BrowserRouter>
  );
}
