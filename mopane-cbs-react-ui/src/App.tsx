import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import ClientList from './pages/Clients/ClientList';
import ClientView from './pages/Clients/ClientView';
import ClientForm from './pages/Clients/ClientForm';
import LoanList from './pages/Loans/LoanList';
import LoanView from './pages/Loans/LoanView';
import LoanForm from './pages/Loans/LoanForm';
import LoanEdit from './pages/Loans/LoanEdit';
import SavingsList from './pages/Savings/SavingsList';
import SavingsView from './pages/Savings/SavingsView';
import SavingsForm from './pages/Savings/SavingsForm';
import GroupList from './pages/Groups/GroupList';
import GroupView from './pages/Groups/GroupView';
import GroupForm from './pages/Groups/GroupForm';
import CenterList from './pages/Centers/CenterList';
import CenterView from './pages/Centers/CenterView';
import CenterForm from './pages/Centers/CenterForm';
import StaffList from './pages/Staff/StaffList';
import StaffView from './pages/Staff/StaffView';
import StaffForm from './pages/Staff/StaffForm';
import LoanProductList from './pages/LoanProducts/LoanProductList';
import LoanProductView from './pages/LoanProducts/LoanProductView';
import LoanProductForm from './pages/LoanProducts/LoanProductForm';
import SavingsProductList from './pages/SavingsProducts/SavingsProductList';
import SavingsProductView from './pages/SavingsProducts/SavingsProductView';
import SavingsProductForm from './pages/SavingsProducts/SavingsProductForm';
import OfficeList from './pages/Offices/OfficeList';
import OfficeView from './pages/Offices/OfficeView';
import OfficeForm from './pages/Offices/OfficeForm';
import UserList from './pages/Users/UserList';
import UserView from './pages/Users/UserView';
import UserForm from './pages/Users/UserForm';
import ReportList from './pages/Reports/ReportList';
import RunReport from './pages/Reports/RunReport';
import RoleList from './pages/Roles/RoleList';
import RoleView from './pages/Roles/RoleView';
import RoleForm from './pages/Roles/RoleForm';
import Profile from './pages/Profile/Profile';
import './App.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <Layout />
                </ProtectedRoute>
              }
            >
              <Route index element={<Navigate to="/dashboard" replace />} />
              <Route path="dashboard" element={<Dashboard />} />
              <Route path="profile" element={<Profile />} />
              <Route path="clients" element={<ClientList />} />
              <Route path="clients/new" element={<ClientForm />} />
              <Route path="clients/:id" element={<ClientView />} />
              <Route path="clients/:id/edit" element={<ClientForm />} />
              <Route path="loans" element={<LoanList />} />
              <Route path="loans/new" element={<LoanForm />} />
              <Route path="loans/:id" element={<LoanView />} />
              <Route path="loans/:id/edit" element={<LoanEdit />} />
              <Route path="savings" element={<SavingsList />} />
              <Route path="savings/new" element={<SavingsForm />} />
              <Route path="savings/:id" element={<SavingsView />} />
              <Route path="groups" element={<GroupList />} />
              <Route path="groups/new" element={<GroupForm />} />
              <Route path="groups/:id" element={<GroupView />} />
              <Route path="groups/:id/edit" element={<GroupForm />} />
              <Route path="centers" element={<CenterList />} />
              <Route path="centers/new" element={<CenterForm />} />
              <Route path="centers/:id" element={<CenterView />} />
              <Route path="centers/:id/edit" element={<CenterForm />} />
              <Route path="staff" element={<StaffList />} />
              <Route path="staff/new" element={<StaffForm />} />
              <Route path="staff/:id" element={<StaffView />} />
              <Route path="staff/:id/edit" element={<StaffForm />} />
              <Route path="loanproducts" element={<LoanProductList />} />
              <Route path="loanproducts/new" element={<LoanProductForm />} />
              <Route path="loanproducts/:id" element={<LoanProductView />} />
              <Route path="loanproducts/:id/edit" element={<LoanProductForm />} />
              <Route path="savingsproducts" element={<SavingsProductList />} />
              <Route path="savingsproducts/new" element={<SavingsProductForm />} />
              <Route path="savingsproducts/:id" element={<SavingsProductView />} />
              <Route path="savingsproducts/:id/edit" element={<SavingsProductForm />} />
              <Route path="offices" element={<OfficeList />} />
              <Route path="offices/new" element={<OfficeForm />} />
              <Route path="offices/:id" element={<OfficeView />} />
              <Route path="offices/:id/edit" element={<OfficeForm />} />
              <Route path="users" element={<UserList />} />
              <Route path="users/new" element={<UserForm />} />
              <Route path="users/:id" element={<UserView />} />
              <Route path="users/:id/edit" element={<UserForm />} />
              <Route path="roles" element={<RoleList />} />
              <Route path="roles/new" element={<RoleForm />} />
              <Route path="roles/:id" element={<RoleView />} />
              <Route path="roles/:id/edit" element={<RoleForm />} />
              <Route path="reports" element={<ReportList />} />
              <Route path="reports/run/:reportName" element={<RunReport />} />
            </Route>
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
