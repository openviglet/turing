import type { TurIntegrationIndexingRule } from "@/models/integration/integration-indexing-rule.model";
import axios, { type AxiosInstance } from "axios";

export class TurIntegrationIndexingRuleService {
  private integrationId: string;
  private endpointPrefix: string;
  private readonly axiosInstance: AxiosInstance;

  constructor(integrationId: string, axiosInstance: AxiosInstance = axios) {
    this.integrationId = integrationId;
    this.axiosInstance = axiosInstance;
    this.endpointPrefix = `/v2/integration/${this.integrationId}/connector/indexing-rule`;
  }

  setIntegrationId(integrationId: string): this {
    this.integrationId = integrationId;
    return this;
  }

  async query(): Promise<TurIntegrationIndexingRule[]> {
    const { data } = await this.axiosInstance.get<TurIntegrationIndexingRule[]>(
      `${this.endpointPrefix}`,
    );
    return data;
  }

  async get(id: string): Promise<TurIntegrationIndexingRule> {
    const { data } = await this.axiosInstance.get<TurIntegrationIndexingRule>(
      `${this.endpointPrefix}/${id}`,
    );
    return data;
  }

  async getStructure(): Promise<TurIntegrationIndexingRule[]> {
    const { data } = await this.axiosInstance.get<TurIntegrationIndexingRule[]>(
      `${this.endpointPrefix}/structure`,
    );
    return data;
  }

  async create(
    source: TurIntegrationIndexingRule,
  ): Promise<TurIntegrationIndexingRule> {
    const { data } = await this.axiosInstance.post<TurIntegrationIndexingRule>(
      `${this.endpointPrefix}`,
      source,
    );
    return data;
  }

  async update(
    source: TurIntegrationIndexingRule,
  ): Promise<TurIntegrationIndexingRule> {
    const { data } = await this.axiosInstance.put<TurIntegrationIndexingRule>(
      `${this.endpointPrefix}/${source.id}`,
      source,
    );
    return data;
  }

  async delete(source: TurIntegrationIndexingRule): Promise<boolean> {
    const { status } = await this.axiosInstance.delete(
      `${this.endpointPrefix}/${source.id}`,
    );
    return status === 200;
  }
}
