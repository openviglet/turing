import type { TurSNSiteField } from "@/models/sn/sn-site-field.model.ts";
import type { TurSNSite } from "@/models/sn/sn-site.model.ts";
import axios from "axios";

export class TurSNSiteService {
  async query(): Promise<TurSNSite[]> {
    const response = await axios.get<TurSNSite[]>("/sn");
    return response.data;
  }
  async get(id: string): Promise<TurSNSite> {
    const response = await axios.get<TurSNSite>(`/sn/${id}`);
    return response.data;
  }
  async getFields(id: string): Promise<TurSNSiteField[]> {
    const response = await axios.get<TurSNSiteField[]>(`/sn/${id}/field/ext`);
    return response.data;
  }
  async getField(id: string, fieldId: string): Promise<TurSNSiteField> {
    const response = await axios.get<TurSNSiteField>(
      `/sn/${id}/field/ext/${fieldId}`
    );
    return response.data;
  }
  async getFacetedFields(id: string): Promise<TurSNSiteField[]> {
    const response = await axios.get<TurSNSiteField[]>(`/sn/${id}/facet`);
    return response.data;
  }
  async create(turSNSite: TurSNSite): Promise<TurSNSite> {
    const response = await axios.post<TurSNSite>("/sn", turSNSite);
    return response.data;
  }
  async update(turSNSite: TurSNSite): Promise<TurSNSite> {
    const response = await axios.put<TurSNSite>(
      `/sn/${turSNSite.id.toString()}`,
      turSNSite
    );
    return response.data;
  }
  async delete(turSNSite: TurSNSite): Promise<boolean> {
    const response = await axios.delete<TurSNSite>(
      `/sn/${turSNSite.id.toString()}`
    );
    return response.status == 200;
  }
  async createField(
    turSNSiteId: string,
    turSNField: TurSNSiteField
  ): Promise<TurSNSiteField> {
    const response = await axios.post<TurSNSiteField>(
      `/sn/${turSNSiteId}/field/ext`,
      turSNField
    );
    return response.data;
  }
  async updateField(
    turSNSiteId: string,
    turSNField: TurSNSiteField
  ): Promise<TurSNSiteField> {
    const response = await axios.put<TurSNSiteField>(
      `/sn/${turSNSiteId}/field/ext/${turSNField.id.toString()}`,
      turSNField
    );
    return response.data;
  }
  async deleteField(
    turSNSiteId: string,
    turSNField: TurSNSiteField
  ): Promise<boolean> {
    const response = await axios.delete<TurSNSiteField>(
      `/sn/${turSNSiteId}/field/ext/${turSNField.id.toString()}`
    );
    return response.status == 200;
  }
}
