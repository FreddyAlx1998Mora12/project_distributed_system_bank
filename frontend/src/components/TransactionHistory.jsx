// src/components/TransactionHistory.jsx
import { useState } from 'react';
import { 
  Clock, 
  Search, 
  Filter, 
  ChevronDown, 
  ChevronUp,
  ArrowDownCircle, 
  ArrowUpCircle, 
  ArrowLeftRight,
  Copy,
  CheckCircle,
  XCircle,
  Clock3
} from 'lucide-react';

/**
 * Componente que muestra el historial de transacciones realizadas
 * Guarda localmente las transacciones ejecutadas desde el frontend
 */

const OPERATION_ICONS = {
  deposit: ArrowDownCircle,
  withdraw: ArrowUpCircle,
  transfer: ArrowLeftRight,
};

const OPERATION_COLORS = {
  deposit: 'text-green-500',
  withdraw: 'text-red-500',
  transfer: 'text-blue-500',
};

const OPERATION_LABELS = {
  deposit: 'Depósito',
  withdraw: 'Retiro',
  transfer: 'Transferencia',
};

const STATUS_ICONS = {
  COMMITTED: CheckCircle,
  FAILED: XCircle,
  PENDING: Clock3,
};

const STATUS_COLORS = {
  COMMITTED: 'text-green-500',
  FAILED: 'text-red-500',
  PENDING: 'text-yellow-500',
};

export default function TransactionHistory({ transactions, clearHistory }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [filter, setFilter] = useState('all'); // all, deposit, withdraw, transfer
  const [sortOrder, setSortOrder] = useState('desc'); // desc, asc
  const [expandedTx, setExpandedTx] = useState(null);

  const handleClearHistory = () => {
    if (confirm('¿Estás seguro de eliminar todo el historial?')) {
      clearHistory();
    }
  };

  // Filtrar y ordenar transacciones
  const filteredTransactions = transactions
    .filter(tx => {
      // Filtro por tipo
      if (filter !== 'all' && tx.operation !== filter) return false;
      
      // Búsqueda por texto
      if (searchTerm) {
        const term = searchTerm.toLowerCase();
        return (
          tx.id.toLowerCase().includes(term) ||
          tx.sourceAccount.toLowerCase().includes(term) ||
          tx.targetAccount.toLowerCase().includes(term) ||
          tx.operation.toLowerCase().includes(term)
        );
      }
      
      return true;
    })
    .sort((a, b) => {
      const dateA = new Date(a.timestamp).getTime();
      const dateB = new Date(b.timestamp).getTime();
      return sortOrder === 'desc' ? dateB - dateA : dateA - dateB;
    });

  const formatDate = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleString('es-EC', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      {/* Cabecera */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center space-x-2">
          <Clock className="h-5 w-5 text-gray-500" />
          <h2 className="text-lg font-semibold text-gray-900">
            Historial de Transacciones
          </h2>
          <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded-full">
            {transactions.length}
          </span>
        </div>
        {transactions.length > 0 && (
          <button
            onClick={handleClearHistory}
            className="text-xs text-red-500 hover:text-red-700"
          >
            Limpiar
          </button>
        )}
      </div>

      {/* Barra de búsqueda y filtros */}
      <div className="flex items-center space-x-2 mb-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Buscar transacción..."
            className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg text-sm
                       focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
          />
        </div>

        {/* Filtro por tipo */}
        <select
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          className="px-3 py-2 border border-gray-300 rounded-lg text-sm outline-none"
        >
          <option value="all">Todas</option>
          <option value="deposit">Depósitos</option>
          <option value="withdraw">Retiros</option>
          <option value="transfer">Transferencias</option>
        </select>

        {/* Orden */}
        <button
          onClick={() => setSortOrder(sortOrder === 'desc' ? 'asc' : 'desc')}
          className="p-2 border border-gray-300 rounded-lg hover:bg-gray-50"
          title={sortOrder === 'desc' ? 'Más reciente primero' : 'Más antiguo primero'}
        >
          {sortOrder === 'desc' ? (
            <ChevronDown className="h-4 w-4 text-gray-500" />
          ) : (
            <ChevronUp className="h-4 w-4 text-gray-500" />
          )}
        </button>
      </div>

      {/* Lista de transacciones */}
      {filteredTransactions.length === 0 ? (
        <div className="text-center py-8">
          <Clock className="h-12 w-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500 text-sm">
            {transactions.length === 0 
              ? 'No hay transacciones realizadas aún' 
              : 'No se encontraron transacciones con los filtros actuales'
            }
          </p>
          <p className="text-gray-400 text-xs mt-1">
            Las transacciones aparecerán aquí cuando las ejecutes
          </p>
        </div>
      ) : (
        <div className="space-y-2 max-h-96 overflow-y-auto">
          {filteredTransactions.map((tx) => {
            const OperationIcon = OPERATION_ICONS[tx.operation] || Clock;
            const StatusIcon = STATUS_ICONS[tx.status] || Clock3;
            const isExpanded = expandedTx === tx.id;

            return (
              <div
                key={tx.id}
                className="border border-gray-200 rounded-lg hover:border-gray-300 transition-colors"
              >
                {/* Fila principal */}
                <div
                  onClick={() => setExpandedTx(isExpanded ? null : tx.id)}
                  className="flex items-center justify-between p-3 cursor-pointer"
                >
                  <div className="flex items-center space-x-3">
                    <OperationIcon className={`h-5 w-5 ${OPERATION_COLORS[tx.operation] || 'text-gray-500'}`} />
                    <div>
                      <p className="text-sm font-medium text-gray-900">
                        {OPERATION_LABELS[tx.operation] || tx.operation}
                      </p>
                      <p className="text-xs text-gray-500">
                        {formatDate(tx.timestamp)}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center space-x-3">
                    <div className="text-right">
                      <p className="text-sm font-semibold text-gray-900">
                        ${parseFloat(tx.amount).toFixed(2)}
                      </p>
                      <div className="flex items-center space-x-1">
                        <StatusIcon className={`h-3 w-3 ${STATUS_COLORS[tx.status] || 'text-gray-500'}`} />
                        <span className="text-xs text-gray-500">{tx.status}</span>
                      </div>
                    </div>
                    {isExpanded ? (
                      <ChevronUp className="h-4 w-4 text-gray-400" />
                    ) : (
                      <ChevronDown className="h-4 w-4 text-gray-400" />
                    )}
                  </div>
                </div>

                {/* Detalles expandidos */}
                {isExpanded && (
                  <div className="border-t border-gray-200 p-3 bg-gray-50 space-y-2">
                    <DetailRow label="ID" value={tx.id} copyable />
                    <DetailRow label="Operación" value={OPERATION_LABELS[tx.operation] || tx.operation} />
                    <DetailRow label="Monto" value={`$${parseFloat(tx.amount).toFixed(2)}`} />
                    <DetailRow label="Cuenta Origen" value={tx.sourceAccount} />
                    <DetailRow label="Cuenta Destino" value={tx.targetAccount} />
                    <DetailRow label="Estado" value={tx.status} />
                    <DetailRow label="Nodo" value={tx.node} />
                    {tx.message && <DetailRow label="Mensaje" value={tx.message} />}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

/**
 * Componente auxiliar para mostrar detalles
 */
function DetailRow({ label, value, copyable = false }) {
  return (
    <div className="flex items-center justify-between text-sm">
      <span className="text-gray-500">{label}:</span>
      <div className="flex items-center space-x-1">
        <span className="text-gray-900 font-mono text-xs truncate max-w-[200px]">
          {value}
        </span>
        {copyable && (
          <button
            onClick={(e) => {
              e.stopPropagation();
              navigator.clipboard.writeText(value);
            }}
            className="text-gray-400 hover:text-gray-600"
            title="Copiar"
          >
            <Copy className="h-3 w-3" />
          </button>
        )}
      </div>
    </div>
  );
}