import { NavLink } from 'react-router-dom';
import '../App.css';

export default function Navbar() {
  return (
    <nav className="navbar">
      <NavLink to="/" end className={({isActive}) => isActive ? 'navbar-selected' : 'navbar-link'}>
        Dispositivi
      </NavLink>
      <NavLink to="/bonci" className={({isActive}) => isActive ? 'navbar-selected' : 'navbar-link'}>
        Modalità Bonci
      </NavLink>
    </nav>
  );
}
