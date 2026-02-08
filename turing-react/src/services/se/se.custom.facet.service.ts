import axios from "axios";
import type { TurSECustomFacet } from "@/models/se/se-custom-facet.model";

export class TurSECustomFacetService {
  async query(): Promise<TurSECustomFacet[]> {
    const response = await axios.get<TurSECustomFacet[]>("/se/custom-facet");
    return response.data;
  }
  async get(id: string): Promise<TurSECustomFacet> {
    const response = await axios.get<TurSECustomFacet>(`/se/custom-facet/${id}`);
    return response.data;
  }
  async create(customFacet: TurSECustomFacet): Promise<TurSECustomFacet> {
    const response = await axios.post<TurSECustomFacet>("/se/custom-facet",
      customFacet
    );
    return response.data;
  }
  async update(customFacet: TurSECustomFacet): Promise<TurSECustomFacet> {
    const response = await axios.put<TurSECustomFacet>(
      `/se/custom-facet/${customFacet.id.toString()}`,
      customFacet
    );
    return response.data;
  }
  async delete(customFacet: TurSECustomFacet): Promise<boolean> {
    const response = await axios.delete<TurSECustomFacet>(
      `/se/custom-facet/${customFacet.id.toString()}`
    );
    return response.status === 200;
  }
}
