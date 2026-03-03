import type {
  TurSNSiteCustomFacet,
  TurSNSiteCustomFacetFieldOption,
  TurSNSiteCustomFacetItem,
} from "@/models/sn/sn-site-custom-facet.model";
import axios from "axios";

type BackendCustomFacet = {
  id?: string;
  name: string;
  defaultLabel?: string;
  label?: Record<string, string>;
  facetPosition?: number;
  facetType?: "DEFAULT" | "AND" | "OR";
  facetItemType?: "DEFAULT" | "AND" | "OR";
  items?: BackendCustomFacetItem[];
  fieldExtId?: string;
  fieldExtName?: string;
  fieldExtType?: string;
};

type BackendCustomFacetItem = {
  id?: string;
  label: string;
  position?: number;
  rangeStart?: number | null;
  rangeEnd?: number | null;
  rangeStartDate?: string | null;
  rangeEndDate?: string | null;
};

type BackendFieldExt = {
  id: string;
  name: string;
  type?: string;
  customFacets?: BackendCustomFacet[];
};

export class TurSNSiteCustomFacetService {
  private async getFields(snSiteId: string): Promise<BackendFieldExt[]> {
    const response = await axios.get<BackendFieldExt[]>(
      `/sn/${snSiteId}/field/ext`,
    );
    return response.data;
  }

  private async getCustomFacets(
    snSiteId: string,
  ): Promise<TurSNSiteCustomFacet[]> {
    const response = await axios.get<TurSNSiteCustomFacet[]>(
      `/sn/${snSiteId}/custom-facet`,
    );
    return response.data;
  }

  private toUiCustomFacet(
    field: BackendFieldExt,
    customFacet: BackendCustomFacet,
  ): TurSNSiteCustomFacet {
    const items = [...(customFacet.items ?? [])]
      .sort(
        (a, b) =>
          (a.position ?? Number.MAX_SAFE_INTEGER) -
          (b.position ?? Number.MAX_SAFE_INTEGER),
      )
      .map((item, index) => this.toUiItem(item, index));

    return {
      id: customFacet.id,
      name: customFacet.name,
      defaultLabel: customFacet.defaultLabel,
      label: customFacet.label ?? {},
      facetPosition: customFacet.facetPosition,
      facetType: customFacet.facetType,
      facetItemType: customFacet.facetItemType,
      items,
      fieldExtId: customFacet.fieldExtId ?? field.id,
      fieldExtName: customFacet.fieldExtName ?? field.name,
      fieldExtType: customFacet.fieldExtType ?? field.type,
    };
  }

  private toUiItem(
    item: BackendCustomFacetItem,
    index: number,
  ): TurSNSiteCustomFacetItem {
    return {
      id: item.id,
      label: item.label,
      position: item.position ?? index + 1,
      rangeStart: item.rangeStart ?? null,
      rangeEnd: item.rangeEnd ?? null,
      rangeStartDate: item.rangeStartDate ?? null,
      rangeEndDate: item.rangeEndDate ?? null,
    };
  }

  private toBackendItem(
    item: TurSNSiteCustomFacetItem,
    index: number,
  ): BackendCustomFacetItem {
    return {
      id: item.id,
      label: item.label,
      position: index + 1,
      rangeStart:
        item.rangeStart === null || item.rangeStart === undefined
          ? null
          : Number(item.rangeStart),
      rangeEnd:
        item.rangeEnd === null || item.rangeEnd === undefined
          ? null
          : Number(item.rangeEnd),
      rangeStartDate: item.rangeStartDate ?? null,
      rangeEndDate: item.rangeEndDate ?? null,
    };
  }

  private toBackendCustomFacet(
    customFacet: TurSNSiteCustomFacet,
  ): BackendCustomFacet {
    return {
      id: customFacet.id,
      name: customFacet.name,
      defaultLabel: customFacet.defaultLabel,
      label: customFacet.label,
      facetPosition: customFacet.facetPosition,
      facetType: customFacet.facetType,
      facetItemType: customFacet.facetItemType,
      fieldExtId: customFacet.fieldExtId,
      fieldExtName: customFacet.fieldExtName,
      fieldExtType: customFacet.fieldExtType,
      items: (customFacet.items ?? []).map((item, index) =>
        this.toBackendItem(item, index),
      ),
    };
  }

  private getCustomFacetMaxPosition(fields: BackendFieldExt[]): number {
    return fields
      .flatMap((field) => field.customFacets ?? [])
      .map((customFacet) => customFacet.facetPosition ?? 0)
      .reduce(
        (maxPosition, currentPosition) =>
          Math.max(maxPosition, currentPosition),
        0,
      );
  }

  private async saveField(
    snSiteId: string,
    field: BackendFieldExt,
  ): Promise<BackendFieldExt> {
    const response = await axios.put<BackendFieldExt>(
      `/sn/${snSiteId}/field/ext/${field.id}`,
      field,
    );
    return response.data;
  }

  async query(snSiteId: string): Promise<TurSNSiteCustomFacet[]> {
    const customFacets = await this.getCustomFacets(snSiteId);
    return [...customFacets].sort(
      (a, b) =>
        (a.facetPosition ?? Number.MAX_SAFE_INTEGER) -
        (b.facetPosition ?? Number.MAX_SAFE_INTEGER),
    );
  }

  async get(
    snSiteId: string,
    customFacetId: string,
  ): Promise<TurSNSiteCustomFacet> {
    const response = await axios.get<TurSNSiteCustomFacet>(
      `/sn/${snSiteId}/custom-facet/${customFacetId}`,
    );
    return response.data;
  }

  async getFieldOptions(
    snSiteId: string,
  ): Promise<TurSNSiteCustomFacetFieldOption[]> {
    const fields = await this.getFields(snSiteId);
    return fields
      .map((field) => ({ id: field.id, name: field.name, type: field.type }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  async create(
    snSiteId: string,
    customFacet: TurSNSiteCustomFacet,
  ): Promise<TurSNSiteCustomFacet> {
    const response = await axios.post<TurSNSiteCustomFacet>(
      `/sn/${snSiteId}/custom-facet`,
      customFacet,
    );
    return response.data;
  }

  async update(
    snSiteId: string,
    customFacet: TurSNSiteCustomFacet,
  ): Promise<TurSNSiteCustomFacet> {
    if (!customFacet.id) throw new Error("Custom facet id is required.");
    const response = await axios.put<TurSNSiteCustomFacet>(
      `/sn/${snSiteId}/custom-facet/${customFacet.id}`,
      customFacet,
    );
    return response.data;
  }

  async delete(snSiteId: string, customFacetId: string): Promise<boolean> {
    const response = await axios.delete<boolean>(
      `/sn/${snSiteId}/custom-facet/${customFacetId}`,
    );
    return response.data === true || response.status === 200;
  }
}
