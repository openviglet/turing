import type { TurSNSiteCustomFacet } from "@/models/sn/sn-site-custom-facet.model";
import axios from "axios";

export class TurSNSiteCustomFacetService {
  async query(): Promise<TurSNSiteCustomFacet[]> {
    const response =
      await axios.get<TurSNSiteCustomFacet[]>("/sn/custom-facet");
    return response.data;
  }
  async get(id: string): Promise<TurSNSiteCustomFacet> {
    const response = await axios.get<TurSNSiteCustomFacet>(
      `/sn/custom-facet/${id}`,
    );
    return response.data;
  }
  async create(
    customFacet: TurSNSiteCustomFacet,
  ): Promise<TurSNSiteCustomFacet> {
    const response = await axios.post<TurSNSiteCustomFacet>(
      "/sn/custom-facet",
      customFacet,
    );
    return response.data;
  }
  async update(
    customFacet: TurSNSiteCustomFacet,
  ): Promise<TurSNSiteCustomFacet> {
    const response = await axios.put<TurSNSiteCustomFacet>(
      `/sn/custom-facet/${customFacet.id.toString()}`,
      customFacet,
    );
    return response.data;
  }
  async delete(customFacet: TurSNSiteCustomFacet): Promise<boolean> {
    const response = await axios.delete<TurSNSiteCustomFacet>(
      `/sn/custom-facet/${customFacet.id.toString()}`,
    );
    return response.status === 200;
  }
}
