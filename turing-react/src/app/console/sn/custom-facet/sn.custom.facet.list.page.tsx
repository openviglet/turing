import { ROUTES } from "@/app/routes.const";
import { BlankSlate } from "@/components/blank-slate";
import { GridList } from "@/components/grid.list";
import { useGridAdapter } from "@/hooks/use-grid-adapter";
import type { TurSECustomFacet } from "@/models/se/se-custom-facet.model";
import { TurSECustomFacetService } from "@/services/se/se.custom.facet.service";
import { IconFilter } from "@tabler/icons-react";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSECustomFacetService = new TurSECustomFacetService();

export default function SNSiteCustomFacetListPage() {
  const { id } = useParams() as { id: string };
  const [customFacets, setCustomFacets] = useState<TurSECustomFacet[]>();

  useEffect(() => {
    turSECustomFacetService.query().then(setCustomFacets)

  }, [])
  const gridItemList = useGridAdapter(customFacets, {
    name: "label",
    description: "field",
    url: (item) => `${ROUTES.SN_INSTANCE}/${id}/custom-facet/${item.id}`
  });
  return (
    <>
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
    </>
  )
}
