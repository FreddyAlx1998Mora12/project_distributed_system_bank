// src/App.jsx (actualizado)
import { Toaster } from 'react-hot-toast';
import Layout from './components/Layout';
import TransactionForm from './components/TransactionForm';
import TransactionHistory from './components/TransactionHistory';
import ClusterStatus from './components/ClusterStatus';
import CircuitBreakerStatus from './components/CircuitBreakerStatus';
import useTransactions from './hooks/useTransactions';

function App() {
  const txHook = useTransactions();

  return (
    <>
      <Toaster position="top-right" />
      <Layout>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Panel Izquierdo: Transacciones */}
          <div className="space-y-6">
            <TransactionForm txHook={txHook} />
            <TransactionHistory transactions={txHook.history} clearHistory={txHook.clearHistory} />
          </div>
          
          {/* Panel Derecho: Estado del Sistema */}
          <div className="space-y-6">
            <ClusterStatus />
            <CircuitBreakerStatus />
          </div>
        </div>
      </Layout>
    </>
  );
}

export default App;