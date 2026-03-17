import { Link } from 'react-router-dom';
import './Dashboard.css';

export default function Dashboard() {
  return (
    <div className="dashboard">
      <h1>Dashboard</h1>
      <p className="dashboard-intro">Welcome to Mopane CBS. Use the links below to get started.</p>
      <div className="dashboard-cards">
        <Link to="/clients" className="dashboard-card">
          <span className="card-title">Clients</span>
          <span className="card-desc">View and manage clients</span>
        </Link>
        <Link to="/loans" className="dashboard-card">
          <span className="card-title">Loans</span>
          <span className="card-desc">View and manage loans</span>
        </Link>
        <Link to="/savings" className="dashboard-card">
          <span className="card-title">Savings</span>
          <span className="card-desc">View and manage savings accounts</span>
        </Link>
        <Link to="/groups" className="dashboard-card">
          <span className="card-title">Groups</span>
          <span className="card-desc">View and manage groups</span>
        </Link>
        <Link to="/centers" className="dashboard-card">
          <span className="card-title">Centers</span>
          <span className="card-desc">View and manage centers</span>
        </Link>
        <Link to="/offices" className="dashboard-card">
          <span className="card-title">Offices</span>
          <span className="card-desc">View and manage offices</span>
        </Link>
        <Link to="/staff" className="dashboard-card">
          <span className="card-title">Staff</span>
          <span className="card-desc">View and manage staff</span>
        </Link>
        <Link to="/users" className="dashboard-card">
          <span className="card-title">Users</span>
          <span className="card-desc">View and manage users</span>
        </Link>
        <Link to="/reports" className="dashboard-card">
          <span className="card-title">Reports</span>
          <span className="card-desc">Run and export reports</span>
        </Link>
      </div>
    </div>
  );
}
