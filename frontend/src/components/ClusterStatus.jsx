// src/components/ClusterStatus.jsx (actualizado)
import { Server, CheckCircle, Crown, AlertTriangle, RefreshCw, Loader2 } from 'lucide-react';
import useClusterHealth from '../hooks/useClusterHealth';
import NodeCard from './NodeCard';

export default function ClusterStatus() {
  const { 
    health, 
    loading, 
    error, 
    refetch, 
    isDegraded, 
    hasQuorum, 
    leader,
    activeNodes,
    totalNodes
  } = useClusterHealth(5000); // Polling cada 5 segundos

  if (loading && !health) {
    return (
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 animate-pulse">
        <div className="h-6 bg-gray-200 rounded w-1/3 mb-4"></div>
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-12 bg-gray-100 rounded"></div>
          ))}
        </div>
      </div>
    );
  }

  if (error && !health) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6">
        <div className="flex items-center space-x-2 mb-2">
          <AlertTriangle className="h-5 w-5 text-red-500" />
          <h2 className="text-lg font-semibold text-red-800">Error de Conexión</h2>
        </div>
        <p className="text-red-600 text-sm mb-3">{error}</p>
        <button
          onClick={refetch}
          className="flex items-center space-x-2 px-3 py-1.5 bg-red-100 text-red-700 
                     rounded-lg hover:bg-red-200 transition-colors text-sm"
        >
          <RefreshCw className="h-4 w-4" />
          <span>Reintentar</span>
        </button>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      {/* Cabecera */}
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-lg font-semibold text-gray-900">
          Estado del Cluster
        </h2>
        <div className="flex items-center space-x-2">
          {/* Estado general */}
          <span className={`
            px-3 py-1 rounded-full text-xs font-medium
            ${isDegraded 
              ? 'bg-yellow-100 text-yellow-700' 
              : 'bg-green-100 text-green-700'
            }
          `}>
            {isDegraded ? 'DEGRADADO' : 'SALUDABLE'}
          </span>
          
          {/* Botón refrescar */}
          <button
            onClick={refetch}
            className="p-1 hover:bg-gray-100 rounded transition-colors"
            title="Refrescar"
          >
            <RefreshCw className="h-4 w-4 text-gray-400 hover:text-gray-600" />
          </button>
        </div>
      </div>

      {/* Resumen de métricas */}
      <div className="grid grid-cols-3 gap-4 mb-4">
        <StatCard
          icon={Server}
          label="Nodos Activos"
          value={`${activeNodes}/${totalNodes}`}
          alert={activeNodes < totalNodes}
        />
        <StatCard
          icon={CheckCircle}
          label="Quorum"
          value={hasQuorum ? 'SÍ' : 'NO'}
          alert={!hasQuorum}
        />
        <StatCard
          icon={Crown}
          label="Líder"
          value={leader || 'N/A'}
          alert={!leader}
        />
      </div>

      {/* Lista de nodos */}
      <div className="space-y-2">
        {health?.nodes && health.nodes.length > 0 ? (
          health.nodes.map((node) => (
            <NodeCard key={node.id} node={node} />
          ))
        ) : (
          <p className="text-sm text-gray-500 text-center py-4">
            No hay información de nodos disponible
          </p>
        )}
      </div>

      {/* Indicador de carga durante refetch */}
      {loading && (
        <div className="flex justify-center mt-3">
          <Loader2 className="h-4 w-4 text-gray-400 animate-spin" />
        </div>
      )}
    </div>
  );
}

function StatCard({ icon: Icon, label, value, alert = false }) {
  return (
    <div className={`
      rounded-lg p-3 text-center transition-colors
      ${alert ? 'bg-yellow-50' : 'bg-gray-50'}
    `}>
      <Icon className={`h-5 w-5 mx-auto mb-1 ${alert ? 'text-yellow-500' : 'text-gray-500'}`} />
      <p className={`text-lg font-bold ${alert ? 'text-yellow-700' : 'text-gray-700'}`}>
        {value}
      </p>
      <p className="text-xs text-gray-600">{label}</p>
    </div>
  );
}