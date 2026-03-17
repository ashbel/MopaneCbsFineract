import { useState, useRef, useEffect } from 'react';
import { Link, NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Layout.css';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [adminOpen, setAdminOpen] = useState(false);
  const adminRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setAdminOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (adminRef.current && !adminRef.current.contains(e.target as Node)) {
        setAdminOpen(false);
      }
    }
    if (adminOpen) {
      document.addEventListener('click', handleClickOutside);
      return () => document.removeEventListener('click', handleClickOutside);
    }
  }, [adminOpen]);

  const isAdminActive = location.pathname.startsWith('/offices') || location.pathname.startsWith('/users') || location.pathname.startsWith('/roles') || location.pathname.startsWith('/staff') || location.pathname.startsWith('/loanproducts') || location.pathname.startsWith('/savingsproducts');

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="app-layout">
      <header className="app-header">
        <div className="header-brand">
          <Link to="/dashboard">Mopane CBS</Link>
        </div>
        <nav className="header-nav">
          <NavLink to="/dashboard" end>Dashboard</NavLink>
          <NavLink to="/clients">Clients</NavLink>
          <NavLink to="/loans">Loans</NavLink>
          <NavLink to="/savings">Savings</NavLink>
          <NavLink to="/groups">Groups</NavLink>
          <NavLink to="/centers">Centers</NavLink>
          <div className="nav-dropdown" ref={adminRef}>
            <button
              type="button"
              className={`nav-dropdown-trigger ${isAdminActive ? 'active' : ''}`}
              onClick={() => setAdminOpen((o) => !o)}
              aria-expanded={adminOpen}
              aria-haspopup="true"
            >
              Admin
            </button>
            {adminOpen && (
              <div className="nav-dropdown-menu">
                <Link to="/offices" className="nav-dropdown-item">Offices</Link>
                <Link to="/staff" className="nav-dropdown-item">Staff</Link>
                <Link to="/users" className="nav-dropdown-item">Users</Link>
                <Link to="/roles" className="nav-dropdown-item">Roles</Link>
                <Link to="/loanproducts" className="nav-dropdown-item">Loan products</Link>
                <Link to="/savingsproducts" className="nav-dropdown-item">Savings products</Link>
              </div>
            )}
          </div>
          <NavLink to="/reports">Reports</NavLink>
        </nav>
        <div className="header-user">
          <Link to="/profile" className="user-name">{user?.username ?? 'User'}</Link>
          <button type="button" onClick={handleLogout} className="btn-logout">
            Logout
          </button>
        </div>
      </header>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
