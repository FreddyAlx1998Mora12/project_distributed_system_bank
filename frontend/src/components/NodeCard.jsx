import { Server, CheckCircle, XCircle, AlertTriangle } from 'lucide-react';

export default function NodeCard({ node }) {
  const statusIcon = node.active 
    ? <CheckCircle className="h-4 w-4 text-green-500" />
    : <XCircle className="h-4 w-4 text-red-500" />;

  const cbStateIcon = {
    CLOSED: <CheckCircle className="h-4 w-4 text-green-500" />,
    OPEN: <XCircle className="h-4 w-4 text-red-500" />,
    HALF_OPEN: <AlertTriangle className="h-4 w-4 text-yellow-500" />,
  };

  return (
    <div className={`
      flex items-center justify-between p-3 rounded-lg border
      ${node.active ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50'}
    `}>
      <div className="flex items-center space-x-3">
        <Server className="h-5 w-5 text-gray-500" />
        <div>
          <div className="flex items-center space-x-2">
            <span className="font-medium text-gray-900">{node.id}</span>
            <span className="text-xs px-2 py-0.5 rounded bg-gray-200 text-gray-700">
              {node.role}
            </span>
          </div>
          <p className="text-xs text-gray-500">{node.host}:{node.port}</p>
        </div>
      </div>

      <div className="flex items-center space-x-4">
        <div className="text-center">
          <p className="text-xs text-gray-500">Peso</p>
          <p className="font-bold text-gray-900">{node.weight}</p>
        </div>
        
        <div className="flex items-center space-x-1">
          {cbStateIcon[node.cbState] || cbStateIcon.CLOSED}
          <span className="text-xs text-gray-600">{node.cbState || 'CLOSED'}</span>
        </div>
        
        <div className="flex items-center space-x-1">
          {statusIcon}
          <span className="text-xs text-gray-600">
            {node.active ? 'UP' : 'DOWN'}
          </span>
        </div>
      </div>
    </div>
  );
}