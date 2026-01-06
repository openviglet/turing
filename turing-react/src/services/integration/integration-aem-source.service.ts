import type { TurIntegrationAemSource } from "@/models/integration/integration-aem-source.model";
import axios, { type AxiosInstance } from "axios";

export class TurIntegrationAemSourceService {
  private integrationId: string;
  private readonly axiosInstance: AxiosInstance;

  constructor(integrationId: string, axiosInstance: AxiosInstance = axios) {
    this.integrationId = integrationId;
    this.axiosInstance = axiosInstance;
  }

  private get aemUrl(): string {
    return `/integration/${this.integrationId}/aem/source`;
  }

  private get connectorUrl(): string {
    return `/integration/${this.integrationId}/connector`;
  }

  setIntegrationId(integrationId: string): this {
    this.integrationId = integrationId;
    return this;
  }

  async query(): Promise<TurIntegrationAemSource[]> {
    const { data } = await this.axiosInstance.get<TurIntegrationAemSource[]>(
      this.aemUrl
    );
    return data;
  }

  async get(id: string): Promise<TurIntegrationAemSource> {
    const { data } = await this.axiosInstance.get<TurIntegrationAemSource>(
      `${this.aemUrl}/${id}`
    );
    return data;
  }

  async getStructure(): Promise<TurIntegrationAemSource[]> {
    const { data } = await this.axiosInstance.get<TurIntegrationAemSource[]>(
      `${this.aemUrl}/structure`
    );
    return data;
  }

  async create(
    source: TurIntegrationAemSource
  ): Promise<TurIntegrationAemSource> {
    const { data } = await this.axiosInstance.post<TurIntegrationAemSource>(
      this.aemUrl,
      source
    );
    return data;
  }

  async update(
    source: TurIntegrationAemSource
  ): Promise<TurIntegrationAemSource> {
    const { data } = await this.axiosInstance.put<TurIntegrationAemSource>(
      `${this.aemUrl}/${source.id}`,
      source
    );
    return data;
  }

  async delete(source: TurIntegrationAemSource): Promise<boolean> {
    const { status } = await this.axiosInstance.delete(
      `${this.aemUrl}/${source.id}`
    );
    return status === 200;
  }

  async indexAll(source: TurIntegrationAemSource): Promise<void> {
    await this.axiosInstance.get(
      `${this.connectorUrl}/${source.id}/indexAll`
    );
  }

  async reindexAll(source: TurIntegrationAemSource): Promise<void> {
    await this.axiosInstance.get(
      `${this.connectorUrl}/${source.id}/reindexAll`
    );
  }
}
