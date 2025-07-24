import { useState } from 'react';
import Navbar from './components/Navbar';
import DevicesPage from './pages/DevicesPage';
import BonciModePage from './pages/BonciModePage';

export default function App() {
  const [page, setPage] = useState('devices');

  const goTo = (newPage) => setPage(newPage);

  return (
    <>
      <Navbar currentPage={page} setPage={goTo} />
      {page === 'devices' && <DevicesPage />}
      {page === 'bonci' && <BonciModePage />}
    </>
  );
}