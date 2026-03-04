import { ROUTES } from "@/app/routes.const";
import { LoadProvider } from "@/components/loading-provider";
import { CustomFacetGrid } from "@/components/sn/custom-facet/custom.facet.grid";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteCustomFacet } from "@/models/sn/sn-site-custom-facet.model";
import { TurSNSiteCustomFacetService } from "@/services/sn/sn.site.custom.facet.service";
import { IconFilter } from "@tabler/icons-react";
import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";

const service = new TurSNSiteCustomFacetService();

export default function SNSiteCustomFacetChildListPage() {
  const { id, groupIdName } = useParams() as { id: string, groupIdName: string };
  const [items, setItems] = useState<TurSNSiteCustomFacet[]>();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    service.query().then(setItems).catch(() => setError("Connection error or timeout while fetching custom facets."));
  }, [])

  const filtered = useMemo(() => {
    const list = items ?? [];
    return list.filter((i) => (i.parentIdName ?? "") === groupIdName);
  }, [items, groupIdName]);

  return (
    <LoadProvider checkIsNotUndefined={items} error={error} tryAgainUrl={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/${groupIdName}`}>
      <>
        <SubPageHeader
          icon={IconFilter}
          feature="Custom Facets"
          name={`Group: ${groupIdName}`}
          description="Manage validation items (ranges) for the selected group."
          urlNew={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/${groupIdName}/new`}
        />
        <CustomFacetGrid items={filtered} parentIdName={groupIdName} />
      </>
    </LoadProvider>
  )
}
