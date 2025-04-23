import React from 'react';
import { Link } from 'react-router-dom';
import './styles/Navbar.css';
import { useTheme } from './Components/Theme';

const Navbar = () => {
  const { isDarkMode, toggleTheme } = useTheme(); // Correct usage of the hook

  return (
    <nav className={`navbar ${isDarkMode ? 'dark' : 'light'}`}>
      <div className="navbar-logo">
        <h1>Coders</h1>
      </div>
      <div className="navbar-auth">
        <Link to="/signin" className="navbar-link">Sign In</Link>
        <Link to="/signup" className="navbar-link">Sign Up</Link>
      </div>
    </nav>
  );
}

export default Navbar;
