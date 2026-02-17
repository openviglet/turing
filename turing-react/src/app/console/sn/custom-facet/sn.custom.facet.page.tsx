import { ROUTES } from "@/app/routes.const";
import { LoadProvider } from "@/components/loading-provider";
import { SNSiteCustomFacetForm } from "@/components/sn/custom-facet/sn.custom.facet.form";
import type { TurSNSiteCustomFacet } from "@/models/sn/sn-site-custom-facet.model";
import { TurSNSiteCustomFacetService } from "@/services/sn/sn.site.custom.facet.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSNSiteCustomFacetService = new TurSNSiteCustomFacetService();

export default function SNSiteCustomFacetPage() {
  const { id, customFacetId } = useParams() as { id: string, customFacetId: string };
  const [customFacet, setCustomFacet] = useState<TurSNSiteCustomFacet>();
  const [isNew, setIsNew] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (customFacetId === "new") {
      turSNSiteCustomFacetService.query().then(() => setCustomFacet({} as TurSNSiteCustomFacet)).catch(() => setError("Connection error or timeout while fetching custom facets."));
    } else {
      turSNSiteCustomFacetService.get(customFacetId).then(setCustomFacet).catch(() => setError("Connection error or timeout while fetching custom facet details."));
      setIsNew(false);
    }
  }, [customFacetId])
  return (
    <LoadProvider checkIsNotUndefined={customFacet} error={error} tryAgainUrl={`${ROUTES.SN_INSTANCE}/${id}/custom-facet`}>
      {customFacet && <SNSiteCustomFacetForm value={customFacet} isNew={isNew} />}
    </LoadProvider>
  )
}
