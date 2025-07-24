import '../App.css';

export default function Navbar({ currentPage, setPage }) {
  return (
    <nav className="navbar">
      <a onClick={() => setPage('devices')} className={currentPage === 'devices' ? 'navbar-selected' : 'navbar-link'}>
        Dispositivi
      </a>
      <a onClick={() => setPage('bonci')} className={currentPage === 'bonci' ? 'navbar-selected' : 'navbar-link'}>
        Modalità Bonci
      </a>
    </nav>
  );
}

