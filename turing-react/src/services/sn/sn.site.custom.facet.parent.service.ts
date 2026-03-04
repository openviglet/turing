import type { TurSNSiteCustomFacetParent } from "@/models/sn/sn-site-custom-facet-parent.model";
import axios from "axios";

export class TurSNSiteCustomFacetParentService {
  async query(): Promise<TurSNSiteCustomFacetParent[]> {
    const response = await axios.get("/sn/custom-facet/parent");
    const data = Array.isArray(response.data) ? response.data : [];
    return data.map((item: any) => ({
      id: String(item.id ?? ""),
      idName: String(item.idName ?? ""),
      attribute: String(item.attribute ?? ""),
      selection: String(item.selection ?? ""),
    })) as TurSNSiteCustomFacetParent[];
  }
  async get(idName: string): Promise<TurSNSiteCustomFacetParent> {
    const response = await axios.get(`/sn/custom-facet/parent/${idName}`);
    const item = response.data ?? {};
    return {
      id: String(item.id ?? ""),
      idName: String(item.idName ?? ""),
      attribute: String(item.attribute ?? ""),
      selection: String(item.selection ?? ""),
    } as TurSNSiteCustomFacetParent;
  }
  async create(group: TurSNSiteCustomFacetParent): Promise<TurSNSiteCustomFacetParent> {
    const payload = {
      idName: group.idName,
      attribute: group.attribute,
      selection: group.selection,
    };
    const response = await axios.post("/sn/custom-facet/parent", payload);
    const item = response.data ?? {};
    return {
      id: String(item.id ?? ""),
      idName: String(item.idName ?? ""),
      attribute: String(item.attribute ?? ""),
      selection: String(item.selection ?? ""),
    } as TurSNSiteCustomFacetParent;
  }
  async update(group: TurSNSiteCustomFacetParent): Promise<TurSNSiteCustomFacetParent> {
    const payload = {
      attribute: group.attribute,
      selection: group.selection,
    };
    const response = await axios.put(`/sn/custom-facet/parent/${group.idName}`, payload);
    const item = response.data ?? {};
    return {
      id: String(item.id ?? ""),
      idName: String(item.idName ?? ""),
      attribute: String(item.attribute ?? ""),
      selection: String(item.selection ?? ""),
    } as TurSNSiteCustomFacetParent;
  }
  async delete(group: TurSNSiteCustomFacetParent): Promise<boolean> {
    const response = await axios.delete(`/sn/custom-facet/parent/${group.idName}`);
    return response.status === 200;
  }
}
