import { DraggableTable } from "@/components/draggable-table";
import { SubPageHeader } from "@/components/sub.page.header";
import type { TurSNSiteField } from "@/models/sn/sn-site-field.model";
import { TurSNSiteService } from "@/services/sn.service";
import { IconReorder } from "@tabler/icons-react";
import React from "react";
import { useParams } from "react-router-dom";
const turSNSiteService = new TurSNSiteService();
export default function SNSiteFacetOrderingPage() {
  const { id } = useParams() as { id: string };
  const [snField, setSnField] = React.useState<TurSNSiteField[]>([]);
  React.useEffect(() => {
    turSNSiteService.getFacetedFields(id).then(setSnField);
    console.log("Teste")
    console.log(snField)
  }, [id])
  return (
    <><SubPageHeader icon={IconReorder} title="Facet Ordering" description="Order the facets of the search." />
      <DraggableTable id={id} /></>
  )
}
