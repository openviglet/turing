import { ROUTES } from "@/app/routes.const";
import { Page } from "@/components/page";
import { IconFilter } from "@tabler/icons-react";
import { useParams } from "react-router-dom";

export default function SECustomFacetRootPage() {
  const { id } = useParams() as { id: string };
  return (
    <Page turIcon={IconFilter} title="Custom Facets" urlBase={`${ROUTES.SN_INSTANCE}/${id}/custom-facet`} urlNew={`${ROUTES.SN_INSTANCE}/${id}/custom-facet/new`} />
  )
}
