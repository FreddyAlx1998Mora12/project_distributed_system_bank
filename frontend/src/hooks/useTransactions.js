// src/hooks/useTransactions.js
import { useState, useCallback } from 'react';
import { bankingApi } from '../api/bankingApi';
import toast from 'react-hot-toast';

/**
 * Hook personalizado para gestionar transacciones bancarias
 * 
 * Funcionalidades:
 * - Ejecutar transacciones (deposit, withdraw, transfer)
 * - Estado de carga por operación
 * - Historial local
 * - Resultado de la última transacción
 * - Manejo de errores con toast
 * 
 * @returns {Object} - Métodos y estado para transacciones
 */
export default function useTransactions() {
  const [loading, setLoading] = useState(false);
  const [lastTransaction, setLastTransaction] = useState(null);
  const [history, setHistory] = useState(() => {
    // Cargar historial desde localStorage al iniciar
    const saved = localStorage.getItem('banking_transactions');
    return saved ? JSON.parse(saved) : [];
  });
  const [error, setError] = useState(null);

  /**
   * Guarda una transacción en el historial
   */
  const saveToHistory = useCallback((transaction) => {
    const txRecord = {
      id: transaction.transactionId || `tx-${Date.now()}`,
      timestamp: new Date().toISOString(),
      operation: transaction.operation || 'unknown',
      amount: parseFloat(transaction.amount) || 0,
      sourceAccount: transaction.sourceAccount || 'N/A',
      targetAccount: transaction.targetAccount || 'N/A',
      status: transaction.status || 'PENDING',
      node: transaction.node || 'unknown',
      message: transaction.message || '',
    };

    setHistory(prev => {
      const updated = [txRecord, ...prev].slice(0, 50); // Máximo 50 registros
      localStorage.setItem('banking_transactions', JSON.stringify(updated));
      return updated;
    });

    return txRecord;
  }, []);

  /**
   * Limpiar historial
   */
  const clearHistory = useCallback(() => {
    setHistory([]);
    localStorage.removeItem('banking_transactions');
    toast.success('Historial limpiado');
  }, []);

  /**
   * Ejecutar un depósito
   */
  const deposit = useCallback(async (amount, targetAccount) => {
    setLoading(true);
    setError(null);

    try {
      const response = await bankingApi.deposit(amount, targetAccount);
      
      const txRecord = saveToHistory({
        ...response,
        operation: 'deposit',
        amount,
        targetAccount
      });
      
      setLastTransaction(txRecord);
      toast.success(`Depósito exitoso: $${amount} a ${targetAccount}`);
      return response;
    } catch (err) {
      setError(err.message);
      toast.error(`Error en depósito: ${err.message}`);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [saveToHistory]);

  /**
   * Ejecutar un retiro
   */
  const withdraw = useCallback(async (amount, sourceAccount) => {
    setLoading(true);
    setError(null);

    try {
      const response = await bankingApi.withdraw(amount, sourceAccount);
      
      const txRecord = saveToHistory({
        ...response,
        operation: 'withdraw',
        amount,
        sourceAccount
      });
      
      setLastTransaction(txRecord);
      toast.success(`Retiro exitoso: $${amount} de ${sourceAccount}`);
      return response;
    } catch (err) {
      setError(err.message);
      toast.error(`Error en retiro: ${err.message}`);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [saveToHistory]);

  /**
   * Ejecutar una transferencia
   */
  const transfer = useCallback(async (amount, sourceAccount, targetAccount) => {
    setLoading(true);
    setError(null);

    try {
      const response = await bankingApi.transfer(amount, sourceAccount, targetAccount);
      
      const txRecord = saveToHistory({
        ...response,
        operation: 'transfer',
        amount,
        sourceAccount,
        targetAccount
      });
      
      setLastTransaction(txRecord);
      toast.success(`Transferencia exitosa: $${amount} de ${sourceAccount} a ${targetAccount}`);
      return response;
    } catch (err) {
      setError(err.message);
      toast.error(`Error en transferencia: ${err.message}`);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [saveToHistory]);

  /**
   * Ejecutar cualquier tipo de transacción
   */
  const processTransaction = useCallback(async ({ operation, amount, sourceAccount, targetAccount }) => {
    setLoading(true);
    setError(null);

    try {
      const response = await bankingApi.processTransaction({
        operation,
        amount,
        sourceAccount,
        targetAccount
      });
      
      const txRecord = saveToHistory({
        ...response,
        operation,
        amount,
        sourceAccount,
        targetAccount
      });
      
      setLastTransaction(txRecord);
      
      if (response.status === 'COMMITTED') {
        toast.success(`Transacción completada: ${operation}`);
      } else if (response.status === 'PENDING') {
        toast(`${response.message || 'Transacción pendiente'}`, { icon: '⏳' });
      } else {
        toast.error(`Transacción fallida: ${response.message || 'Error desconocido'}`);
      }
      
      return response;
    } catch (err) {
      setError(err.message);
      toast.error(`Error: ${err.message}`);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [saveToHistory]);

  /**
   * Limpiar error y última transacción
   */
  const reset = useCallback(() => {
    setError(null);
    setLastTransaction(null);
  }, []);

  return {
    // Estado
    loading,
    error,
    lastTransaction,
    history,
    
    // Acciones
    deposit,
    withdraw,
    transfer,
    processTransaction,
    clearHistory,
    reset,
    
    // Utilidades
    hasHistory: history.length > 0,
    transactionCount: history.length
  };
}