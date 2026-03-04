import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { LoadProvider } from "@/components/loading-provider";
import { CustomFacetGrid } from "@/components/sn/custom-facet/custom.facet.grid";
import { SubPageHeader } from "@/components/sub.page.header";
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
    description: "label",
    url: (item) => `${ROUTES.SN_INSTANCE}/${id}/custom-facet/${encodeURIComponent(item.parentIdName ?? "")}/${item.id}`
  });
  return (
    <LoadProvider checkIsNotUndefined={customFacets} error={error} tryAgainUrl={`${ROUTES.SN_INSTANCE}/${id}/custom-facet`}>
      {gridItemList.length > 0 ? (
        <>
          <SubPageHeader
            icon={IconFilter}
            feature="Custom Facets"
            name="Custom Facets"
            description="Manage your custom facets for range-based filters."
            urlNew={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/new`}
          />
          <CustomFacetGrid items={customFacets} />
        </>
      ) : (
        <>
          <BlankSlate
            icon={IconFilter}
            title="Custom Facets"
            description="Nenhuma faceta encontrada. Crie novas facetas personalizadas."
            buttonText="New Custom Facets"
            urlNew={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/new`}
          />
        </>
      )}
    </LoadProvider>
  )
}
