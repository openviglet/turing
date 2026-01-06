import type { TurIntegrationWcSource } from "@/models/integration/integration-wc-source.model";
import axios, { type AxiosInstance } from "axios";

export class TurIntegrationWcSourceService {
  private integrationId: string;
  private readonly axiosInstance: AxiosInstance;

  constructor(integrationId: string, axiosInstance: AxiosInstance = axios) {
    this.integrationId = integrationId;
    this.axiosInstance = axiosInstance;
  }

  private get url(): string {
    return `/integration/${this.integrationId}/wc/source`;
  }

  setIntegrationId(integrationId: string): this {
    this.integrationId = integrationId;
    return this;
  }

  async query(): Promise<TurIntegrationWcSource[]> {
    const { data } = await this.axiosInstance.get<TurIntegrationWcSource[]>(
      this.url
    );
    return data;
  }

  async get(id: string): Promise<TurIntegrationWcSource> {
    const { data } = await this.axiosInstance.get<TurIntegrationWcSource>(
      `${this.url}/${id}`
    );
    return data;
  }

  async getStructure(): Promise<TurIntegrationWcSource[]> {
    const { data } = await this.axiosInstance.get<TurIntegrationWcSource[]>(
      `${this.url}/structure`
    );
    return data;
  }

  async create(
    source: TurIntegrationWcSource
  ): Promise<TurIntegrationWcSource> {
    const { data } = await this.axiosInstance.post<TurIntegrationWcSource>(
      this.url,
      source
    );
    return data;
  }

  async update(
    source: TurIntegrationWcSource
  ): Promise<TurIntegrationWcSource> {
    const { data } = await this.axiosInstance.put<TurIntegrationWcSource>(
      `${this.url}/${source.id}`,
      source
    );
    return data;
  }

  async delete(source: TurIntegrationWcSource): Promise<boolean> {
    const { status } = await this.axiosInstance.delete(
      `${this.url}/${source.id}`
    );
    return status === 200;
  }
}
