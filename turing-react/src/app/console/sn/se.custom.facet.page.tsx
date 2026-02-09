import { SECustomFacetForm } from "@/components/se/se.custom.facet.form";
import type { TurSECustomFacet } from "@/models/se/se-custom-facet.model";
import { TurSECustomFacetService } from "@/services/se/se.custom.facet.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSECustomFacetService = new TurSECustomFacetService();

export default function SECustomFacetPage() {
  const { customFacetId } = useParams() as { customFacetId: string };
  const [customFacet, setCustomFacet] = useState<TurSECustomFacet>({} as TurSECustomFacet);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (customFacetId !== "new") {
      turSECustomFacetService.get(customFacetId).then(setCustomFacet);
      setIsNew(false);
    }
  }, [customFacetId])
  return (
    <SECustomFacetForm value={customFacet} isNew={isNew} />
  )
}
