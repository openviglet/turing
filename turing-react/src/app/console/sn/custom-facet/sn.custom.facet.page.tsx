import { ROUTES } from "@/app/routes.const";
import { LoadProvider } from "@/components/loading-provider";
import { SNSiteCustomFacetForm } from "@/components/se/se.custom.facet.form";
import type { TurSECustomFacet } from "@/models/se/se-custom-facet.model";
import { TurSECustomFacetService } from "@/services/se/se.custom.facet.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSECustomFacetService = new TurSECustomFacetService();

export default function SNSiteCustomFacetPage() {
  const { id, customFacetId } = useParams() as { id: string, customFacetId: string };
  const [customFacet, setCustomFacet] = useState<TurSECustomFacet>();
  const [isNew, setIsNew] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (customFacetId === "new") {
      turSECustomFacetService.query().then(() => setCustomFacet({} as TurSECustomFacet)).catch(() => setError("Connection error or timeout while fetching custom facets."));
    } else {
      turSECustomFacetService.get(customFacetId).then(setCustomFacet).catch(() => setError("Connection error or timeout while fetching custom facet details."));
      setIsNew(false);
    }
  }, [customFacetId])
  return (
    <LoadProvider checkIsNotUndefined={customFacet} error={error} tryAgainUrl={`${ROUTES.SN_INSTANCE}/${id}/custom-facet`}>
      {customFacet && <SNSiteCustomFacetForm value={customFacet} isNew={isNew} />}
    </LoadProvider>
  )
}
