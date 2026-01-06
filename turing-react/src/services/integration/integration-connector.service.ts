import axios, { type AxiosInstance } from "axios";

export class TurIntegrationConnectorService {
  private integrationId: string;
  private readonly axiosInstance: AxiosInstance;

  constructor(integrationId: string, axiosInstance: AxiosInstance = axios) {
    this.integrationId = integrationId;
    this.axiosInstance = axiosInstance;
  }

  private get connectorUrl(): string {
    return `/integration/${this.integrationId}/connector`;
  }

  async indexAll(source: string): Promise<void> {
    await this.axiosInstance.get(`${this.connectorUrl}/index/${source}/all`);
  }

  async reindexAll(source: string): Promise<void> {
    await this.axiosInstance.get(`${this.connectorUrl}/reindex/${source}/all`);
  }
}
