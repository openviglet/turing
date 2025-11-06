import axios from 'axios';

export interface TurDiscovery {
  keycloak?: boolean;
}

export class TurAuthorizationService {
  async discovery(): Promise<TurDiscovery> {
    try {
      const response = await axios.get('/v2/discovery');
      return response.data;
    } catch (error) {
      console.error('Discovery error:', error);
      return { keycloak: false };
    }
  }
}
