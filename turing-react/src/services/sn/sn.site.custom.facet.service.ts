import type { TurSNSiteCustomFacet } from "@/models/sn/sn-site-custom-facet.model";
import axios from "axios";

export class TurSNSiteCustomFacetService {
  async query(): Promise<TurSNSiteCustomFacet[]> {
    const response = await axios.get("/sn/custom-facet/all");
    const data = Array.isArray(response.data) ? response.data : [];
    return data.map((item: any) => ({
      id: String(item.id ?? ""),
      label: String(item.label ?? ""),
      rangeStart: item.rangeStart ?? "",
      rangeEnd: item.rangeEnd ?? "",
      parentIdName: String(item.parent?.idName ?? ""),
    })) as TurSNSiteCustomFacet[];
  }
  async get(id: string): Promise<TurSNSiteCustomFacet> {
    const response = await axios.get(`/sn/custom-facet/${id}`);
    const item = response.data ?? {};
    return {
      id: String(item.id ?? ""),
      label: String(item.label ?? ""),
      rangeStart: item.rangeStart ?? "",
      rangeEnd: item.rangeEnd ?? "",
      parentIdName: String(item.parent?.idName ?? ""),
    } as TurSNSiteCustomFacet;
  }
  async create(customFacet: TurSNSiteCustomFacet, parentIdName: string): Promise<TurSNSiteCustomFacet> {
    const payload = {
      label: customFacet.label,
      rangeStart: customFacet.rangeStart || null,
      rangeEnd: customFacet.rangeEnd || null,
    };
    const response = await axios.post(`/sn/custom-facet?parentIdName=${encodeURIComponent(parentIdName)}`, payload);
    const item = response.data ?? {};
    return {
      id: String(item.id ?? ""),
      label: String(item.label ?? ""),
      rangeStart: item.rangeStart ?? "",
      rangeEnd: item.rangeEnd ?? "",
    } as TurSNSiteCustomFacet;
  }
  async update(customFacet: TurSNSiteCustomFacet, parentIdName?: string): Promise<TurSNSiteCustomFacet> {
    const payload = {
      label: customFacet.label,
      rangeStart: customFacet.rangeStart || null,
      rangeEnd: customFacet.rangeEnd || null,
    };
    const url = parentIdName ? `/sn/custom-facet/${customFacet.id}?parentIdName=${encodeURIComponent(parentIdName)}` : `/sn/custom-facet/${customFacet.id}`;
    const response = await axios.put(url, payload);
    const item = response.data ?? {};
    return {
      id: String(item.id ?? ""),
      label: String(item.label ?? ""),
      rangeStart: item.rangeStart ?? "",
      rangeEnd: item.rangeEnd ?? "",
    } as TurSNSiteCustomFacet;
  }
  async delete(customFacet: TurSNSiteCustomFacet): Promise<boolean> {
    const response = await axios.delete(`/sn/custom-facet/${customFacet.id}`);
    return response.status === 200;
  }
}
