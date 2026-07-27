// src/hooks/useClusterHealth.js
import { useState, useEffect, useCallback } from 'react';
import { bankingApi } from '../api/bankingApi';

/**
 * Hook personalizado para monitorear la salud del cluster
 * 
 * Funcionalidades:
 * - Polling automático cada N segundos
 * - Reintentos en caso de error
 * - Estado de carga y error
 * - Refetch manual
 * 
 * @param {number} interval - Intervalo de polling en ms (default: 5000)
 * @returns {Object} - { health, loading, error, refetch, isDegraded }
 */
export default function useClusterHealth(interval = 5000) {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);
  const MAX_RETRIES = 3;

  const fetchHealth = useCallback(async () => {
    try {
      setError(null);
      const data = await bankingApi.getClusterHealth();
      setHealth(data);
      setRetryCount(0); // Resetear reintentos al tener éxito
      return data;
    } catch (err) {
      console.error('Error al obtener salud del cluster:', err);
      setRetryCount(prev => prev + 1);
      
      if (retryCount < MAX_RETRIES) {
        setError(`Reintentando conexión (${retryCount + 1}/${MAX_RETRIES})...`);
      } else {
        setError('No se pudo conectar al cluster después de varios intentos');
      }
      
      return null;
    } finally {
      setLoading(false);
    }
  }, [retryCount]);

  // Polling automático
  useEffect(() => {
    // Primera carga inmediata
    fetchHealth();

    // Polling periódico
    const timer = setInterval(fetchHealth, interval);

    // Cleanup al desmontar
    return () => clearInterval(timer);
  }, [fetchHealth, interval]);

  // Refetch manual (útil después de una acción)
  const refetch = useCallback(() => {
    setLoading(true);
    setError(null);
    fetchHealth();
  }, [fetchHealth]);

  // Determinar si el sistema está degradado
  const isDegraded = health?.status === 'DEGRADED' || 
                     (health?.activeNodes && health?.totalNodes && 
                      health.activeNodes < health.totalNodes);

  // Determinar si hay quorum
  const hasQuorum = health?.activeNodes >= 3;

  // Nodo líder actual
  const leader = health?.leader || null;

  // Nodos activos vs totales
  const activeNodes = health?.activeNodes || 0;
  const totalNodes = health?.totalNodes || 0;

  return {
    health,
    loading,
    error,
    refetch,
    isDegraded,
    hasQuorum,
    leader,
    activeNodes,
    totalNodes
  };
}