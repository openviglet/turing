import axios from "axios";
import type { TurSNSite } from "@/models/sn-site.model";
import type { TurSNSiteField } from "@/models/sn-site-field.model";

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
  async create(turSNSite: TurSNSite): Promise<TurSNSite> {
    const response = await axios.post<TurSNSite>("/sn",
      turSNSite
    );
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
    return  response.status == 200;
  }
}
