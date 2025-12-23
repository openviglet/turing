import axios from "axios";
import type { TurSNSiteLocale } from "@/models/sn/sn-site-locale.model";

export class TurSNSiteLocaleService {
  async query(siteId: string): Promise<TurSNSiteLocale[]> {
    const response = await axios.get<TurSNSiteLocale[]>(`/sn/${siteId}/locale`);
    return response.data;
  }
  async get(siteId: string, id: string): Promise<TurSNSiteLocale> {
    const response = await axios.get<TurSNSiteLocale>(
      `/sn/${siteId}/locale/${id}`
    );
    return response.data;
  }
  async create(turSNSiteLocale: TurSNSiteLocale): Promise<TurSNSiteLocale> {
    const response = await axios.post<TurSNSiteLocale>(
      `/sn/${turSNSiteLocale.turSNSite.id.toString()}/locale`,
      turSNSiteLocale
    );
    return response.data;
  }
  async update(turSNSiteLocale: TurSNSiteLocale): Promise<TurSNSiteLocale> {
    const response = await axios.put<TurSNSiteLocale>(
      `/sn/${turSNSiteLocale.turSNSite.id.toString()}/locale/${turSNSiteLocale.id.toString()}`,
      turSNSiteLocale
    );
    return response.data;
  }
  async delete(turSNSiteLocale: TurSNSiteLocale): Promise<boolean> {
    const response = await axios.delete<TurSNSiteLocale>(
      `/sn/${turSNSiteLocale.turSNSite.id.toString()}/locale/${turSNSiteLocale.id.toString()}`
    );
    return response.status == 200;
  }
}
