const API_BASE = import.meta.env.VITE_API_URL || '';

class BankingApi {
  
  async processTransaction({ operation, amount, sourceAccount, targetAccount }) {
    const url = `${API_BASE}/api/transactions/${operation}`;
    
    const body = {
      amount: parseFloat(amount)
    };
    
    if (operation !== 'deposit') {
      body.fromAccountId = sourceAccount;
    }
    
    if (operation !== 'withdraw') {
      body.toAccountId = targetAccount;
    }
    
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    
    if (!response.ok) {
      throw new Error(`Error HTTP: ${response.status}`);
    }
    
    return response.json();
  }

  async getClusterHealth() {
    const response = await fetch(`${API_BASE}/api/cluster/topology`);
    if (!response.ok) throw new Error('Error al obtener salud del cluster');
    return response.json();
  }

  async getGatewayHealth() {
    const response = await fetch(`${API_BASE}/health`);
    if (!response.ok) throw new Error('Gateway no disponible');
    return response.json();
  }

  async getCircuitBreakers() {
    const response = await fetch(`${API_BASE}/api/circuit-breakers/status`);
    if (!response.ok) throw new Error('Error al obtener estado de CBs');
    return response.json();
  }
}

export const bankingApi = new BankingApi();