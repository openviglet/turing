import type { TurSNSiteField } from "@/models/sn/sn-site-field.model.ts";
import axios from "axios";

export class TurSNFacetedFieldService {
  async query(id: string): Promise<TurSNSiteField[]> {
    const response = await axios.get<TurSNSiteField[]>(`/sn/${id}/facet`);
    return response.data;
  }
}
