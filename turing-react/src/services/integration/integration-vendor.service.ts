import type { TurIntegrationVendor } from "@/models/integration/integration-vendor.model";
import axios, { type AxiosInstance } from "axios";

export class TurIntegrationVendorService {
  private readonly axiosInstance: AxiosInstance;

  constructor(axiosInstance: AxiosInstance = axios) {
    this.axiosInstance = axiosInstance;
  }

  async query(): Promise<TurIntegrationVendor[]> {
    const { data } = await this.axiosInstance.get<TurIntegrationVendor[]>(
      "/integration/vendor"
    );
    return data;
  }

  async get(id: string): Promise<TurIntegrationVendor> {
    const { data } = await this.axiosInstance.get<TurIntegrationVendor>(
      `/integration/vendor/${id}`
    );
    return data;
  }

  async create(
    turIntegrationVendor: TurIntegrationVendor
  ): Promise<TurIntegrationVendor> {
    const { data } = await this.axiosInstance.post<TurIntegrationVendor>(
      `/integration/vendor/${turIntegrationVendor.id}`,
      turIntegrationVendor
    );
    return data;
  }
}
