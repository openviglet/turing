import { SECustomFacetForm } from "@/components/se/se.custom.facet.form";
import type { TurSECustomFacet } from "@/models/se/se-custom-facet.model";
import { TurSECustomFacetService } from "@/services/se/se.custom.facet.service";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

const turSECustomFacetService = new TurSECustomFacetService();

export default function SECustomFacetPage() {
  const { id } = useParams() as { id: string };
  const [customFacet, setCustomFacet] = useState<TurSECustomFacet>({} as TurSECustomFacet);
  const [isNew, setIsNew] = useState<boolean>(true);
  useEffect(() => {
    if (id !== "new") {
      turSECustomFacetService.get(id).then(setCustomFacet);
      setIsNew(false);
    }
  }, [id])
  return (
    <SECustomFacetForm value={customFacet} isNew={isNew} />
  )
}
