// src/components/Toast.jsx
import { useEffect, useState } from 'react';
import { CheckCircle, XCircle, AlertTriangle, X } from 'lucide-react';

/**
 * Componente Toast personalizado (alternativa a react-hot-toast)
 * Muestra notificaciones temporales en la esquina superior derecha
 */

const TOAST_TYPES = {
  success: {
    icon: CheckCircle,
    bg: 'bg-green-50 border-green-200',
    text: 'text-green-800',
    iconColor: 'text-green-500'
  },
  error: {
    icon: XCircle,
    bg: 'bg-red-50 border-red-200',
    text: 'text-red-800',
    iconColor: 'text-red-500'
  },
  warning: {
    icon: AlertTriangle,
    bg: 'bg-yellow-50 border-yellow-200',
    text: 'text-yellow-800',
    iconColor: 'text-yellow-500'
  },
  info: {
    icon: CheckCircle,
    bg: 'bg-blue-50 border-blue-200',
    text: 'text-blue-800',
    iconColor: 'text-blue-500'
  }
};

export default function Toast({ message, type = 'info', onClose, duration = 4000 }) {
  const [isVisible, setIsVisible] = useState(true);
  const [isLeaving, setIsLeaving] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setIsLeaving(true);
      setTimeout(() => {
        setIsVisible(false);
        onClose && onClose();
      }, 300); // Duración de la animación de salida
    }, duration);

    return () => clearTimeout(timer);
  }, [duration, onClose]);

  if (!isVisible) return null;

  const config = TOAST_TYPES[type] || TOAST_TYPES.info;
  const Icon = config.icon;

  return (
    <div
      className={`
        flex items-center space-x-3 px-4 py-3 rounded-lg border shadow-lg
        transition-all duration-300 max-w-sm
        ${config.bg} ${config.text}
        ${isLeaving ? 'opacity-0 translate-x-full' : 'opacity-100 translate-x-0'}
      `}
    >
      <Icon className={`h-5 w-5 flex-shrink-0 ${config.iconColor}`} />
      <p className="text-sm font-medium flex-1">{message}</p>
      <button
        onClick={() => {
          setIsLeaving(true);
          setTimeout(() => {
            setIsVisible(false);
            onClose && onClose();
          }, 300);
        }}
        className="flex-shrink-0 hover:opacity-70"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}

/**
 * Contenedor de Toasts - Renderiza múltiples toasts
 */
export function ToastContainer({ toasts, removeToast }) {
  return (
    <div className="fixed top-4 right-4 z-50 space-y-2">
      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          message={toast.message}
          type={toast.type}
          duration={toast.duration || 4000}
          onClose={() => removeToast(toast.id)}
        />
      ))}
    </div>
  );
}