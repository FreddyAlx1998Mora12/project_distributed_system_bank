import { useEffect, useState } from 'react';
import { Shield, ShieldAlert, ShieldCheck } from 'lucide-react';
import { bankingApi } from '../api/bankingApi';

export default function CircuitBreakerStatus() {
  const [breakers, setBreakers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchBreakers = async () => {
      try {
        const data = await bankingApi.getCircuitBreakers();
        setBreakers(data);
      } catch (error) {
        console.error('Error al obtener CBs:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchBreakers();
    const interval = setInterval(fetchBreakers, 3000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">
        Circuit Breakers
      </h2>

      {loading ? (
        <div className="animate-pulse space-y-2">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-10 bg-gray-100 rounded"></div>
          ))}
        </div>
      ) : (
        <div className="space-y-2">
          {breakers.length > 0 ? breakers.map((cb) => (
            <CBRow key={cb.name} breaker={cb} />
          )) : (
            <p className="text-sm text-gray-500 text-center py-4">
              Esperando datos de Circuit Breakers...
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function CBRow({ breaker }) {
  const stateConfig = {
    CLOSED: { icon: ShieldCheck, color: 'green' },
    OPEN: { icon: ShieldAlert, color: 'red' },
    HALF_OPEN: { icon: Shield, color: 'yellow' },
  };

  const config = stateConfig[breaker.state] || stateConfig.CLOSED;
  const Icon = config.icon;

  return (
    <div className={`flex items-center justify-between p-2 rounded bg-${config.color}-50`}>
      <div className="flex items-center space-x-2">
        <Icon className={`h-4 w-4 text-${config.color}-500`} />
        <span className="text-sm font-medium">{breaker.name}</span>
      </div>
      
      <div className="flex items-center space-x-4">
        <span className={`text-xs px-2 py-0.5 rounded-full 
          ${breaker.state === 'CLOSED' ? 'bg-green-200 text-green-700' : 
            breaker.state === 'OPEN' ? 'bg-red-200 text-red-700' : 
            'bg-yellow-200 text-yellow-700'
          }`}>
          {breaker.state}
        </span>
        <span className="text-xs text-gray-500">
          Fallos: {((breaker.failureRate || 0) * 100).toFixed(1)}%
        </span>
      </div>
    </div>
  );
}