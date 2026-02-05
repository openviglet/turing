import { DraggableTable } from "@/components/draggable-table";
import { SubPageHeader } from "@/components/sub.page.header";
import { IconReorder } from "@tabler/icons-react";
import { useParams } from "react-router-dom";

export default function SNSiteFacetOrderingPage() {
  const { id } = useParams() as { id: string };
  return (
    <><SubPageHeader icon={IconReorder} name="Facet Ordering" feature="Facet Ordering" description="Order the facets of the search." />
      <DraggableTable id={id} /></>
  )
}
