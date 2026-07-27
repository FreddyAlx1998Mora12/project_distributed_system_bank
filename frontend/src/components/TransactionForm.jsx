// src/components/TransactionForm.jsx (actualizado)
import { useState } from 'react';
import { ArrowDown, ArrowUp, ArrowLeftRight, Send, Loader2 } from 'lucide-react';
import useTransactions from '../hooks/useTransactions';

const OPERATIONS = [
  { type: 'deposit', icon: ArrowDown, label: 'Depósito', color: 'green' },
  { type: 'withdraw', icon: ArrowUp, label: 'Retiro', color: 'red' },
  { type: 'transfer', icon: ArrowLeftRight, label: 'Transferencia', color: 'blue' },
];

export default function TransactionForm() {
  const [operation, setOperation] = useState('deposit');
  const [amount, setAmount] = useState('');
  const [sourceAccount, setSourceAccount] = useState('');
  const [targetAccount, setTargetAccount] = useState('');

  // Usar el hook de transacciones
  const { loading, lastTransaction, processTransaction, error } = useTransactions();

  const handleSubmit = async (e) => {
    e.preventDefault();

    await processTransaction({
      operation,
      amount: parseFloat(amount),
      sourceAccount: operation !== 'deposit' ? sourceAccount : undefined,
      targetAccount: operation !== 'withdraw' ? targetAccount : undefined,
    });

    // Limpiar formulario si fue exitoso
    if (lastTransaction?.status === 'SUCCESS') {
      setAmount('');
      setSourceAccount('');
      setTargetAccount('');
    }
  };

  // Determinar el color según operación seleccionada
  const getColorClasses = (type) => {
    if (type !== operation) return 'border-gray-200 hover:border-gray-300';
    switch (type) {
      case 'deposit': return 'border-green-500 bg-green-50';
      case 'withdraw': return 'border-red-500 bg-red-50';
      case 'transfer': return 'border-blue-500 bg-blue-50';
      default: return 'border-gray-200';
    }
  };

  const getIconColor = (type) => {
    switch (type) {
      case 'deposit': return 'text-green-500';
      case 'withdraw': return 'text-red-500';
      case 'transfer': return 'text-blue-500';
      default: return 'text-gray-500';
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">
        Nueva Transacción
      </h2>

      {/* Selector de Operación */}
      <div className="grid grid-cols-3 gap-2 mb-6">
        {OPERATIONS.map(({ type, icon: Icon, label }) => (
          <button
            key={type}
            type="button"
            onClick={() => setOperation(type)}
            className={`
              flex flex-col items-center p-3 rounded-lg border-2 transition-all
              ${getColorClasses(type)}
            `}
          >
            <Icon className={`h-5 w-5 ${getIconColor(type)} mb-1`} />
            <span className="text-xs font-medium text-gray-700">{label}</span>
          </button>
        ))}
      </div>

      {/* Formulario */}
      <form onSubmit={handleSubmit} className="space-y-4">
        {operation !== 'deposit' && (
          <InputField
            label="Cuenta Origen"
            value={sourceAccount}
            onChange={setSourceAccount}
            placeholder="ACC-001"
            disabled={loading}
          />
        )}

        {operation !== 'withdraw' && (
          <InputField
            label="Cuenta Destino"
            value={targetAccount}
            onChange={setTargetAccount}
            placeholder="ACC-002"
            disabled={loading}
          />
        )}

        <InputField
          label="Monto ($)"
          value={amount}
          onChange={setAmount}
          placeholder="100.00"
          type="number"
          disabled={loading}
          min="0.01"
          step="0.01"
        />

        <button
          type="submit"
          disabled={loading || !amount || parseFloat(amount) <= 0}
          className="w-full flex items-center justify-center space-x-2 px-4 py-2.5 
                     bg-blue-600 text-white rounded-lg hover:bg-blue-700 
                     disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {loading ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>Procesando...</span>
            </>
          ) : (
            <>
              <Send className="h-4 w-4" />
              <span>Ejecutar Transacción</span>
            </>
          )}
        </button>
      </form>

      {/* Resultado */}
      {lastTransaction && (
        <div className={`
          mt-4 p-4 rounded-lg border
          ${lastTransaction.status === 'SUCCESS' 
            ? 'bg-green-50 border-green-200' 
            : lastTransaction.status === 'PENDING' 
              ? 'bg-yellow-50 border-yellow-200' 
              : 'bg-red-50 border-red-200'}
        `}>
          <p className="font-medium text-sm">
            Estado: {lastTransaction.status}
          </p>
          {lastTransaction.id && (
            <p className="text-xs text-gray-600 mt-1">
              ID: {lastTransaction.id}
            </p>
          )}
          {lastTransaction.node && (
            <p className="text-xs text-gray-600">
              Nodo: {lastTransaction.node}
            </p>
          )}
          {lastTransaction.message && (
            <p className="text-xs text-gray-600 mt-1">{lastTransaction.message}</p>
          )}
        </div>
      )}

      {/* Error */}
      {error && (
        <div className="mt-4 p-4 rounded-lg border bg-red-50 border-red-200">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}
    </div>
  );
}

function InputField({ label, value, onChange, placeholder, type = 'text', disabled = false, ...props }) {
  return (
    <div>
      <label className="block text-sm font-medium text-gray-700 mb-1">
        {label}
      </label>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        className="w-full px-3 py-2 border border-gray-300 rounded-lg 
                   focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none
                   disabled:bg-gray-100 disabled:cursor-not-allowed"
        {...props}
      />
    </div>
  );
}