import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { GridList } from "@/components/grid.list";
import { LoadProvider } from "@/components/loading-provider";
import { useGridAdapter } from "@/hooks/use-grid-adapter";
import type { TurSNSiteCustomFacet } from "@/models/sn/sn-site-custom-facet.model";
import { TurSNSiteCustomFacetService } from "@/services/sn/sn.site.custom.facet.service";
import { IconFilter } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteCustomFacetService = new TurSNSiteCustomFacetService();

export default function SNSiteCustomFacetListPage() {
  const { id } = useParams() as { id: string };
  const [customFacets, setCustomFacets] = useState<TurSNSiteCustomFacet[]>();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    turSNSiteCustomFacetService.query().then(setCustomFacets).catch(() => setError("Connection error or timeout while fetching custom facets."));
  }, [])
  const gridItemList = useGridAdapter(customFacets, {
    name: "label",
    description: "field",
    url: (item) => `${ROUTES.SN_INSTANCE}/${id}/custom-facet/${item.id}`
  });
  return (
    <LoadProvider checkIsNotUndefined={customFacets} error={error} tryAgainUrl={`${ROUTES.SN_INSTANCE}/${id}/custom-facet`}>
      {gridItemList.length > 0 ? (
        <GridList gridItemList={gridItemList} />
      ) : (
        <BlankSlate
          icon={IconFilter}
          title="You don't seem to have any custom facets."
          description="Create a new custom facet to define range-based filters for your search data."
          buttonText="New custom facet"
          urlNew={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/new`} />
      )}
    </LoadProvider>
  )
}
