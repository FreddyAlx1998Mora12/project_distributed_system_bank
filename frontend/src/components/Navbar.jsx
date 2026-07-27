import { Banknote, Activity } from 'lucide-react';

export default function Navbar() {
  return (
    <nav className="bg-white shadow-sm border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          {/* Logo */}
          <div className="flex items-center space-x-3">
            <Banknote className="h-8 w-8 text-blue-600" />
            <div>
              <h1 className="text-xl font-bold text-gray-900">
                Banking Distributed System
              </h1>
              <p className="text-xs text-gray-500">
                Tolerante a Fallos · WAL · Quorum
              </p>
            </div>
          </div>
          
          {/* Status */}
          <div className="flex items-center space-x-2 px-3 py-1.5 bg-green-50 rounded-full">
            <Activity className="h-4 w-4 text-green-500 animate-pulse" />
            <span className="text-sm font-medium text-green-600">Sistema Operativo</span>
          </div>
        </div>
      </div>
    </nav>
  );
}