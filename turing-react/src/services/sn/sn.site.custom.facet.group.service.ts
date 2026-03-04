import type { TurSNSiteCustomFacetGroup } from "@/models/sn/sn-site-custom-facet-group.model";
import axios from "axios";

export class TurSNSiteCustomFacetGroupService {
  async query(): Promise<TurSNSiteCustomFacetGroup[]> {
    const response = await axios.get("/sn/custom-facet/custom/all");
    const data = Array.isArray(response.data) ? response.data : [];
    return data.map((item: any) => ({
      idName: String(item.attribute ?? item.label ?? ""),
      count: Array.isArray(item.facetItems) ? item.facetItems.length : 0,
    })) as TurSNSiteCustomFacetGroup[];
  }
}
